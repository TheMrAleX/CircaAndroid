package es.aproximado.core

import es.aproximado.core.lan.*
import org.junit.Test
import org.junit.Assert.*
import kotlin.random.Random
import java.util.UUID

class SelectionHistoryTest {
    private fun pool(n: Int)=List(n) { Question("q$it",if(it%2==0) "A" else "B","¿Cuántas unidades tiene la referencia $it?",12.0,null,0.0,50.0,"linear",it%3+1) }
    @Test fun wholeEightHundredBankIsUsedBeforeRepeatingAndRecentSixHundredStayProtected() {
        val pool=pool(800);val seen=mutableSetOf<String>();var history=emptyList<String>();val random=Random(109)
        repeat(40) {
            val picked=Selection.select(pool,20,emptySet(),null,history,random)
            assertEquals(20,picked.size);picked.forEach { assertTrue("Unexpected repeat ${it.id}",seen.add(it.id)) }
            history=Selection.remember(history,picked.flatMap { listOf(it.id,Selection.fingerprint(it.text)) })
        }
        assertEquals(800,seen.size);assertEquals(1600,history.size)
        repeat(60) {
            val protected=history.filterNot { it.startsWith("text:") }.takeLast(600).toSet()
            val picked=Selection.select(pool,20,emptySet(),null,history,random)
            assertTrue(picked.none { it.id in protected });assertEquals(20,picked.map { it.id }.toSet().size)
            history=Selection.remember(history,picked.flatMap { listOf(it.id,Selection.fingerprint(it.text)) })
        }
    }
    @Test fun exhaustionReshufflesOldCandidatesInsteadOfRepeatingOneFixedOrder() {
        val pool=pool(100);val history=pool.map { it.id }
        val choices=(0..30).map { seed -> Selection.select(pool,10,emptySet(),null,history,Random(seed)).map { it.id } }
        assertTrue(choices.toSet().size>20)
        assertTrue(choices.flatten().all { it.substring(1).toInt()<25 })
    }
    @Test fun limitedCategoriesReuseOnlyWhenNecessaryAndMinimizePreviousGameOverlap() {
        val pool=pool(24);val eligible=pool.filter { it.category=="A" }
        val history=eligible.map { it.id };val previous=history.takeLast(10).toSet()
        val picked=Selection.select(pool,10,setOf("A"),null,history,Random(1))
        assertEquals(10,picked.map { it.id }.toSet().size);assertTrue(picked.all { it.category=="A" })
        assertEquals(8,picked.count { it.id in previous }) // 12 eligible, 10 requested: overlap of eight is unavoidable.
        val easy=Selection.select(pool,4,setOf("A"),1,emptyList(),Random(1))
        assertTrue(easy.all { it.difficulty==1 && it.category=="A" })
    }
    @Test fun textFingerprintsPreventRepetitionWhenAFormerGuestBecomesHostOrIdsChange() {
        val pool=pool(20);val seen=pool.take(10)
        val fingerprints=seen.map { Selection.fingerprint(it.text) }
        val renamed=pool.map { it.copy(id="new-${it.id}") }
        val selected=Selection.select(renamed,10,emptySet(),null,fingerprints,Random(1))
        assertTrue(selected.none { q -> seen.any { it.text==q.text } })
        assertEquals(Selection.fingerprint("¿Cuántos ÁRBOLES?"),Selection.fingerprint("  ¿Cuántos   a\u0301rboles?  "))
    }
    @Test fun duplicateTextCannotAppearTwiceEvenIfItHasDifferentIds() {
        val pool=pool(8);val duplicate=pool.first().copy(id="clone")
        val selected=Selection.select(pool+duplicate,8,emptySet(),null,emptyList(),Random(1))
        assertEquals(8,selected.map { Selection.fingerprint(it.text) }.toSet().size)
        assertThrows(IllegalArgumentException::class.java) { Selection.select(listOf(pool.first(),duplicate),2,emptySet(),null,emptyList()) }
    }
    @Test fun memoryKeepsTheWholeBankAndNormalizesDuplicateHistoryChronologically() {
        val old=(0..799).map { "q$it" }
        assertEquals(800,Selection.remember(old,listOf("q0")).size)
        assertEquals("q0",Selection.remember(old,listOf("q0")).last())
        assertEquals(listOf("a","c","b"),Selection.remember(listOf("a","b","a","c"),listOf("b","b")))
    }
    @Test fun abandonedLanRoundsAreRememberedAcrossNewMatchesAndNewRoomInstances() {
        val pool=pool(30);val history=mutableListOf<String>();val host=UUID.randomUUID().toString();val guest=UUID.randomUUID().toString()
        val shown=mutableSetOf<String>()
        repeat(30) {
            val room=LanRoom(pool,host,"Host",history);room.join(guest,"Invitado");room.configure(5,null,emptySet());room.start()
            val text=room.snapshot(host).prompt!!.text
            assertTrue("Abandoned prompt repeated",shown.add(text))
            assertTrue(history.contains(Selection.fingerprint(text)))
            assertTrue(room.snapshot(host).results.isEmpty())
        }
        assertEquals(30,shown.size)
    }
}
