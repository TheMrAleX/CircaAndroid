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
    @Test fun comparisonUsesCommonScaleAndCentersIdenticalValues() {
        val question=q(answer=23.0)
        val scale=ComparisonScale(question,listOf(28.0,38.0))
        assertTrue(scale.position(23.0)<scale.position(28.0))
        assertTrue(scale.position(28.0)<scale.position(38.0))
        assertEquals(1f/3f,(scale.position(28.0)-scale.position(23.0))/(scale.position(38.0)-scale.position(23.0)),1e-6f)
        for(answer in listOf(0.0,.5,1.4e9)) {
            val equal=ComparisonScale(q(answer=answer),List(8){answer})
            assertTrue(equal.position(answer) in 0f..1f)
            assertEquals(0f,equal.position(0.0),0f)
        }
        val log=ComparisonScale(q(scale="log"),listOf(50.0,200.0))
        assertEquals(log.position(100.0)-log.position(50.0),log.position(200.0)-log.position(100.0),1e-6f)
    }
    @Test fun rulerPrecisionDoesNotRevealAnswerAndEndpointsAreReachable() {
        val integer=q(answer=100.0).copy(min=.5,max=999.5,decimalPlaces=3)
        val fractional=integer.copy(answer=100.5)
        for(i in 0..1000) assertEquals(RulerScale.snapped(integer,i/1000.0),RulerScale.snapped(fractional,i/1000.0),0.0)
        assertEquals(.5,RulerScale.snapped(integer,0.0),0.0)
        assertEquals(999.5,RulerScale.snapped(integer,1.0),0.0)
        val fraction=q(answer=.5).copy(min=0.0,max=1.0,decimalPlaces=3)
        assertEquals(.57,RulerScale.initial(integer.copy(answer=RulerScale.snapped(integer,.5))),1e-9)
        assertEquals(.001,RulerScale.step(fraction,.5),1e-9)
        assertEquals(.501,RulerScale.snapped(fraction,.501),1e-9)
    }
    @Test fun screenshotRoundAwardsTheClosestPlayerAndLabelsTotalBonus() {
        val scores=Scoring.round(q(answer=23.0),listOf(28.0,38.0))
        assertEquals(listOf(0,1),scores.map{it.playerId})
        assertEquals(listOf(2,1),scores.map{it.points})
        val tied=Scoring.round(q(answer=23.0),List(8){23.0})
        assertTrue(tied.all{it.rank==1 && it.points==10 && it.bonus==2})
        // Logarithmic proximity is a ratio, not the absolute difference shown as context.
        assertEquals(1,Scoring.round(q(scale="log"),listOf(50.0,180.0)).first().playerId)
    }

    @Test fun selectorDragFollowsFingerAndClampsAtBothEnds() {
        assertEquals(.75,RulerScale.drag(.5,-50.0,200.0),1e-9)
        assertEquals(.25,RulerScale.drag(.5,50.0,200.0),1e-9)
        assertEquals(1.0,RulerScale.drag(.5,-10000.0,200.0),0.0)
        assertEquals(0.0,RulerScale.drag(.5,10000.0,200.0),0.0)
        for(scale in listOf("linear","log")) {
            val question=q(scale=scale)
            val up=RulerScale.value(question,RulerScale.drag(.5,-30.0,200.0))
            val down=RulerScale.value(question,RulerScale.drag(.5,30.0,200.0))
            assertTrue(up>down)
        }
        assertEquals(.5,RulerScale.drag(RulerScale.drag(.5,-20.0,200.0),20.0,200.0),1e-9)
    }
    @Test fun movingEstimateKeepsDecimalColumnsAndPrecision() {
        val question=q().copy(min=0.0,max=10.0,decimalPlaces=2)
        assertEquals("3,32",formatEstimate(question,3.32))
        assertEquals("3,40",formatEstimate(question,3.4))
        assertEquals("3,00",formatEstimate(question,3.0))
        assertEquals("10,00",formatEstimate(question,10.0))
        for(i in 300..400) assertEquals(4,formatEstimate(question,i/100.0).length)
        assertEquals("0,500",formatEstimate(question.copy(max=1.0,decimalPlaces=3),.5))
        assertEquals("1.400.000.000",formatEstimate(question.copy(max=1e12,decimalPlaces=0),1.4e9))
        assertEquals("3,321",formatEstimate(question.copy(decimalPlaces=3),3.321))
        assertEquals("3,32",formatEstimate(question.copy(max=10000.0),3.32))
    }

    @Test fun discreteQuestionsOnlyAllowWholeNumbersInEveryInputPath() {
        val zodiac=q(answer=12.0).copy(min=0.0,max=36.0)
        assertEquals(0,RulerScale.fractionDigits(zodiac))
        assertEquals(1.0,EstimateInput.fineStep(zodiac),0.0)
        assertTrue(EstimateInput.accepts(zodiac,12.0))
        assertFalse(EstimateInput.accepts(zodiac,12.5))
        assertFalse(EstimateInput.accepts(zodiac,Double.NaN))
        for(scale in listOf("linear","log")) {
            val question=zodiac.copy(scale=scale,min=if(scale=="log") 1.0 else 0.0)
            for(i in 0..1000) {
                val value=RulerScale.snapped(question,i/1000.0)
                assertTrue(EstimateInput.accepts(question,value))
                assertEquals(value,kotlin.math.floor(value),0.0)
                assertFalse(formatEstimate(question,value).contains(','))
            }
        }
        val goal=zodiac.copy(answer=7.32,max=22.0,decimalPlaces=2)
        assertEquals(.01,EstimateInput.fineStep(goal),0.0)
        assertTrue(EstimateInput.accepts(goal,7.32))
        assertFalse(EstimateInput.accepts(goal,7.321))
        assertEquals("7,30",formatEstimate(goal,7.3))
        assertTrue(EstimateInput.accepts(goal,EstimateInput.round(goal,.1+.2)))
    }
    @Test fun closeBarsHaveProportionalHeightsWithoutAnAutomaticZoom() {
        val question=q(answer=10.0).copy(max=30.0,decimalPlaces=2)
        val scale=ComparisonScale(question,listOf(9.99,8.99))
        assertEquals(0.0,scale.minimum,0.0)
        assertEquals(0f,scale.position(0.0),0f)
        assertTrue(scale.position(9.99)-scale.position(8.99)<.1f)
        assertEquals(9.99/8.99,(scale.position(9.99)/scale.position(8.99)).toDouble(),1e-6)
        assertEquals(10.0/scale.maximum,scale.position(10.0).toDouble(),1e-6)
        assertEquals(listOf(0.0,scale.maximum/2,scale.maximum),scale.ticks)
        val log=ComparisonScale(question.copy(scale="log",min=1.0,max=1000.0),listOf(9.99,8.99))
        assertEquals(1.0,log.minimum,0.0)
        assertEquals(1000.0,log.maximum,0.0)
        assertTrue(log.position(9.99)-log.position(8.99)<.02f)
        assertTrue(ComparisonScale(question.copy(answer=0.0),List(8){0.0}).maximum>0)
        val tiny=ComparisonScale(question.copy(answer=.005,decimalPlaces=3),listOf(.004,.005))
        assertTrue(tiny.maximum<.01)
        assertEquals(1.25,(tiny.position(.005)/tiny.position(.004)).toDouble(),1e-6)
    }

}
