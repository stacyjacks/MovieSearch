package kurmakaeva.anastasia.moviesearch.data.concurrency

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class InFlightRequestsTest {

    /** A fetch that blocks until [release] is called, recording starts and cancellations. */
    private class ControllableFetch {
        var starts = 0
        var cancellations = 0
        private val gate = CompletableDeferred<Unit>()

        fun release() = gate.complete(Unit)

        suspend fun run(value: String): String {
            starts++
            try {
                gate.await()
            } catch (e: CancellationException) {
                cancellations++
                throw e
            }
            return value
        }
    }

    private fun TestScope.newRequests() =
        InFlightRequests<String, String>(CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)))

    @Test
    fun concurrentCallsForSameKey_shareASingleFetch() = runTest {
        val requests = newRequests()
        val fetch = ControllableFetch()

        val callers = List(5) { async { requests.getOrStart("matrix") { fetch.run("result") } } }
        advanceUntilIdle()
        assertEquals(1, fetch.starts)
        assertEquals(5, requests.waiterCount("matrix"))

        fetch.release()
        assertEquals(List(5) { "result" }, callers.awaitAll())
        assertEquals(1, fetch.starts)
    }

    @Test
    fun differentKeys_areFetchedIndependently() = runTest {
        val requests = newRequests()
        val matrix = ControllableFetch()
        val inception = ControllableFetch()

        val a = async { requests.getOrStart("matrix") { matrix.run("m") } }
        val b = async { requests.getOrStart("inception") { inception.run("i") } }
        advanceUntilIdle()
        inception.release()
        matrix.release()

        assertEquals("m", a.await())
        assertEquals("i", b.await())
        assertEquals(1, matrix.starts)
        assertEquals(1, inception.starts)
    }

    @Test
    fun oneWaiterCancelled_othersStillGetTheResult() = runTest {
        val requests = newRequests()
        val fetch = ControllableFetch()

        val leaving = async { requests.getOrStart("matrix") { fetch.run("result") } }
        val staying = async { requests.getOrStart("matrix") { fetch.run("result") } }
        advanceUntilIdle()

        leaving.cancel()
        advanceUntilIdle()
        assertEquals(0, fetch.cancellations)
        assertEquals(1, requests.waiterCount("matrix"))

        fetch.release()
        assertEquals("result", staying.await())
        assertTrue(leaving.isCancelled)
    }

    @Test
    fun allWaitersCancelled_cancelsTheFetch_andNextCallStartsFresh() = runTest {
        val requests = newRequests()
        val first = ControllableFetch()

        val callers = List(3) { async { requests.getOrStart("matrix") { first.run("stale") } } }
        advanceUntilIdle()
        callers.forEach { it.cancel() }
        advanceUntilIdle()

        assertEquals(1, first.cancellations)
        assertEquals(0, requests.waiterCount("matrix"))

        val second = ControllableFetch().apply { release() }
        assertEquals("fresh", requests.getOrStart("matrix") { second.run("fresh") })
        assertEquals(1, second.starts)
    }

    @Test
    fun completedRequest_isForgotten_soTheNextCallFetchesAgain() = runTest {
        val requests = newRequests()
        val starts = AtomicInteger()

        requests.getOrStart("matrix") { starts.incrementAndGet(); "first" }
        val second = requests.getOrStart("matrix") { starts.incrementAndGet(); "second" }

        assertEquals("second", second)
        assertEquals(2, starts.get())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun failure_isDeliveredToEveryWaiter_andForgotten() = runTest {
        val requests = newRequests()
        val gate = CompletableDeferred<Unit>()

        val callers = List(2) {
            async {
                runCatching { requests.getOrStart("matrix") { gate.await(); throw IOException("offline") } }
            }
        }

        advanceUntilIdle()
        gate.complete(Unit)

        callers.awaitAll().forEach { assertTrue(it.exceptionOrNull() is IOException) }
        assertEquals("recovered", requests.getOrStart("matrix") { "recovered" })
    }

    /**
     * Same guarantee as [concurrentCallsForSameKey_shareASingleFetch], but with real threads racing.
     * Each round, 400 callers on Dispatchers.Default are released at the same moment and spread over
     * 8 keys; every round must cause exactly one fetch per key. Many rounds make a race very likely
     * to show up if the bookkeeping weren't properly synchronized.
     */
    @Test
    fun realThreads_manyConcurrentCallers_causeOneFetchPerKey() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val requests = InFlightRequests<Int, String>(scope)
        val keys = 0 until 8
        val callerCount = 400

        repeat(50) { round ->
            val fetches = AtomicInteger()
            val startSignal = CompletableDeferred<Unit>()
            val gate = CompletableDeferred<Unit>()

            val callers = List(callerCount) { i ->
                async(Dispatchers.Default) {
                    val key = i % keys.count()
                    startSignal.await() // release all callers at once to maximise contention
                    requests.getOrStart(key) {
                        fetches.incrementAndGet()
                        gate.await()
                        "value-$key"
                    }
                }
            }
            startSignal.complete(Unit)

            // Keep every request open until all callers have joined, so none can legitimately start a second fetch.
            withTimeout(5_000) {
                while (keys.sumOf { requests.waiterCount(it) } < callerCount || fetches.get() < keys.count()) delay(1)
            }
            gate.complete(Unit)

            val results = callers.awaitAll()
            results.forEachIndexed { i, result -> assertEquals("value-${i % keys.count()}", result) }
            assertEquals("round $round: fetches", keys.count(), fetches.get())
            if (keys.any { requests.waiterCount(it) != 0 }) fail("round $round: requests were not cleaned up")
        }
        scope.cancel()
    }
}
