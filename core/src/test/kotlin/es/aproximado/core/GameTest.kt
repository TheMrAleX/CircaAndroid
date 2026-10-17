package es.aproximado.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameTest {
    private fun q(id: String = "q", answer: Double = 100.0, scale: String = "linear", difficulty: Int = 1, category: String = "A") = Question(id, category, "¿Cuánto?", answer, null, if (scale == "log") 1.0 else 0.0, 1000.0, scale, difficulty)
    @Test fun linearRankingAndBonus() {
        val result = Scoring.round(q(), listOf(100.0, 100.5, 130.0, 60.0)).associateBy { it.playerId }
        assertEquals(6, result.getValue(0).points)
        assertEquals(4, result.getValue(1).points)
        assertEquals(2, result.getValue(2).points)
        assertEquals(1, result.getValue(3).points)
    }
    @Test fun logarithmicRatiosTie() {
        val result = Scoring.round(q(scale = "log"), listOf(50.0, 200.0, 10.0))
        assertEquals(1, result[0].rank); assertEquals(1, result[1].rank)
        assertEquals(3, result[2].rank)
        assertEquals(result[0].points, result[1].points)
    }
    @Test fun exactZeroAndBoundary() {
        assertEquals(.2, Scoring.error(q(answer=.5),.6),1e-9)
        val zero = Scoring.round(q(answer = 0.0), listOf(0.0, 1.0))
        assertEquals(4, zero.first().points)
        assertEquals(0, Scoring.round(q(), listOf(99.0, 101.0)).first().bonus)
    }
    @Test fun totalsSharePositionAndStableOrder() {
        val players = (0..2).map { Player(it, "P$it") }
        val r = Scoring.round(q(), listOf(90.0, 110.0, 200.0))
        val standings = Scoring.standings(players, listOf(r, r))
        assertEquals(listOf(1,1,3), standings.map { it.rank })
        assertEquals(listOf(0,1,2), standings.map { it.player.id })
    }
    @Test fun questionFiltersAndNoRepeats() {
        val pool = (0..39).map { q("q$it", difficulty = it % 3 + 1, category = if (it % 2 == 0) "A" else "B") }
        val selected = Selection.select(pool, 5, setOf("A"), 1, emptyList(), Random(1))
        assertEquals(5, selected.map { it.id }.toSet().size)
        assertTrue(selected.all { it.category == "A" && it.difficulty == 1 })
    }
    @Test fun historyExclusionAndOldestFallback() {
        val pool = (0..9).map { q("q$it") }
        val selected = Selection.select(pool, 5, emptySet(), null, (0..7).map { "q$it" }, Random(2))
        assertEquals(setOf("q8","q9"), selected.take(2).map { it.id }.toSet())
        assertEquals(listOf("q0","q1","q2"), selected.drop(2).map { it.id })
        assertEquals(listOf("b","c","a"), Selection.remember(listOf("a","b"), listOf("c","a")))
    }
    @Test(expected = IllegalArgumentException::class) fun insufficientPoolFails() { Selection.select(listOf(q()), 2, emptySet(), null, emptyList()) }
    @Test fun scalesClampRoundAndAvoidAnswerHint() {
        assertEquals(.57, RulerScale.initial(q(answer = 500.0)), 1e-9)
        assertEquals(0.0, RulerScale.value(q(), -2.0), 1e-9)
        assertEquals(1000.0, RulerScale.value(q(), 2.0), 1e-9)
        val log = q(scale = "log")
        assertEquals(.4, RulerScale.position(log, RulerScale.value(log,.4)), 1e-9)
        for (i in 0..1000) assertTrue(RulerScale.snapped(log,i/1000.0) in log.min..log.max)
    }
    @Test fun spanishNumbersAndCompactSuffixes() {
        assertEquals("1.400.000.000",formatNumber(1.4e9))
        assertEquals("0,5",formatNumber(.5))
        assertEquals("17 mil",formatNumber(17000.0,true))
        assertEquals("1,4 M",formatNumber(1.4e6,true))
        assertEquals("1.400 M",formatNumber(1.4e9,true))
        assertEquals("1 B",formatNumber(1e12,true))
    }
    @Test fun wholeMatchesForTwoAndEightPlayers() {
        for (n in listOf(2,8)) {
            val players = (0 until n).map { Player(it,"J$it") }
            val selected = Selection.select((0..24).map { q("q$it") },20,emptySet(),null,emptyList(),Random(4))
            val rounds = selected.map { question -> Scoring.round(question,(0 until n).map { 100.0 + it * 3 }) }
            assertEquals(20,rounds.size)
            assertTrue(rounds.all { it.size == n })
            assertEquals(0,Scoring.standings(players,rounds).first().player.id)
            assertEquals(n,Scoring.standings(players,rounds).size)
        }
    }
}
