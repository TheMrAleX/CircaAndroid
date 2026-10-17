package es.aproximado.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

data class Question(
    val id: String, val category: String, val text: String, val answer: Double,
    val unit: String?, val min: Double, val max: Double, val scale: String,
    val difficulty: Int, val year: Int? = null, val source: String = ""
)
data class Player(val id: Int, val name: String, val style: Int = id)
data class RoundScore(val playerId: Int, val guess: Double, val error: Double, val rank: Int, val points: Int, val bonus: Int)
data class Standing(val player: Player, val total: Int, val rank: Int)

object Scoring {
    fun error(q: Question, guess: Double): Double {
        require(guess.isFinite() && guess >= 0)
        return if (q.scale == "log") {
            require(guess > 0 && q.answer > 0)
            abs(ln(guess) - ln(q.answer))
        } else abs(guess - q.answer) / (if (q.answer == 0.0) 1.0 else abs(q.answer))
    }
    fun round(q: Question, guesses: List<Double>): List<RoundScore> {
        require(guesses.size in 2..8)
        val errors = guesses.map { error(q, it) }
        val ordered = errors.sorted()
        return guesses.mapIndexed { id, guess ->
            val rank = ordered.indexOfFirst { abs(it - errors[id]) <= 1e-10 } + 1
            val bonus = when {
                abs(guess - q.answer) <= 1e-9 -> 2
                q.answer != 0.0 && abs(guess - q.answer) / abs(q.answer) < .01 -> 1
                else -> 0
            }
            RoundScore(id, guess, errors[id], rank, guesses.size - rank + 1 + bonus, bonus)
        }.sortedWith(compareBy<RoundScore> { it.rank }.thenBy { it.playerId })
    }
    fun standings(players: List<Player>, rounds: List<List<RoundScore>>): List<Standing> {
        val totals = players.associate { p -> p.id to rounds.sumOf { r -> r.firstOrNull { it.playerId == p.id }?.points ?: 0 } }
        return players.sortedWith(compareByDescending<Player> { totals.getValue(it.id) }.thenBy { it.id }).map { p ->
            Standing(p, totals.getValue(p.id), 1 + totals.values.count { it > totals.getValue(p.id) })
        }
    }
}

object Selection {
    fun select(pool: List<Question>, count: Int, categories: Set<String>, difficulty: Int?, recent: List<String>, random: Random = Random.Default): List<Question> {
        require(count > 0)
        val eligible = pool.filter { (categories.isEmpty() || it.category in categories) && (difficulty == null || it.difficulty == difficulty) }.distinctBy { it.id }
        require(eligible.size >= count) { "Preguntas insuficientes" }
        // Recent is chronological, oldest first. Shuffle fresh questions but prefer older history if exhausted.
        val history = recent.withIndex().associate { it.value to it.index }
        val fresh = eligible.filterNot { it.id in history }.shuffled(random)
        val reused = eligible.filter { it.id in history }.sortedBy { history[it.id] }
        return (fresh + reused).take(count)
    }
    fun remember(recent: List<String>, played: List<String>, limit: Int = 80): List<String> =
        (recent.filterNot { it in played } + played.distinct()).takeLast(limit)
}

object RulerScale {
    fun value(q: Question, position: Double): Double {
        val t = position.coerceIn(0.0, 1.0)
        return if (q.scale == "log") exp(ln(q.min) + t * (ln(q.max) - ln(q.min))) else q.min + t * (q.max - q.min)
    }
    fun position(q: Question, value: Double): Double =
        (if (q.scale == "log") (ln(value.coerceAtLeast(q.min)) - ln(q.min)) / (ln(q.max) - ln(q.min)) else (value - q.min) / (q.max - q.min)).coerceIn(0.0, 1.0)
    fun initial(q: Question): Double {
        val center = value(q, .5)
        return if (abs(center - q.answer) <= max(1e-9, abs(q.answer) * 1e-9)) .57 else .5
    }
    fun niceStep(raw: Double): Double {
        require(raw > 0 && raw.isFinite())
        val power = 10.0.pow(floor(log10(raw)))
        val unit = raw / power
        return power * when { unit < 1.5 -> 1; unit < 3.5 -> 2; unit < 7.5 -> 5; else -> 10 }
    }
    fun snapped(q: Question, position: Double): Double {
        val value = value(q, position)
        val mark = if (q.scale == "log") max(value * (exp((ln(q.max) - ln(q.min)) / 1000) - 1), .001) else (q.max - q.min) / 1000
        val step = niceStep(mark).coerceAtLeast(if (q.answer == floor(q.answer)) 1.0 else .01)
        return (round(value / step) * step).coerceIn(q.min, q.max)
    }
}

fun formatNumber(value: Double, compact: Boolean = false): String {
    require(value.isFinite())
    val symbols = DecimalFormatSymbols(Locale.forLanguageTag("es-ES")).apply { groupingSeparator = '.'; decimalSeparator = ',' }
    val abs = abs(value)
    val divisor: Double
    val suffix: String
    when {
        compact && abs >= 1e12 -> { divisor = 1e12; suffix = " B" }
        compact && abs >= 1e6 -> { divisor = 1e6; suffix = " M" }
        compact && abs >= 1e4 -> { divisor = 1e3; suffix = " mil" }
        else -> { divisor = 1.0; suffix = "" }
    }
    val pattern = if (divisor != 1.0) "#,##0.#" else "#,##0.###"
    return DecimalFormat(pattern, symbols).format(if (value == 0.0) 0.0 else value / divisor) + suffix
}
