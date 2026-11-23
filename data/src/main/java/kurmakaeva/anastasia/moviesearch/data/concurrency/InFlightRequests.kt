package kurmakaeva.anastasia.moviesearch.data.concurrency

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/**
 * Shares in-flight work between concurrent callers asking for the same [K] ("request coalescing").
 *
 * - The first caller for a key starts [getOrStart]'s `fetch` in [scope]; callers arriving while it runs
 *   join the same request instead of starting another one.
 * - The shared request runs in [scope], not in any caller's coroutine, so one caller being cancelled
 *   can't cancel it for the others.
 * - Each request counts its waiters. When a waiter is cancelled and it was the last one, the request is
 *   cancelled too (nobody needs the result any more) - e.g. cancelling the HTTP call.
 * - Once a request completes it is forgotten; the next caller starts a fresh one.
 *
 * All bookkeeping happens under [lock]. The lock is never held across a suspension point, so a plain
 * JVM monitor is the right tool here (no Mutex needed).
 */
internal class InFlightRequests<K : Any, V>(private val scope: CoroutineScope) {
    private class Request<T>(val deferred: Deferred<T>) {
        /** Number of callers currently awaiting [deferred]. Guarded by [lock]. */
        var waiters = 0
    }

    private val lock = Any()
    private val requests = HashMap<K, Request<V>>()

    suspend fun getOrStart(key: K, fetch: suspend () -> V): V {
        val request = synchronized(lock) {
            requests.getOrPut(key) { createRequest(key, fetch) }.also { it.waiters++ }
        }
        // Started lazily so it's registered in the map (and has its cleanup handler) before it can complete.
        request.deferred.start()
        try {
            return request.deferred.await()
        } finally {
            // Runs on success, failure and cancellation of this waiter.
            synchronized(lock) {
                request.waiters--
                if (request.waiters == 0 && request.deferred.isActive) {
                    // The last interested caller left before the result arrived: stop the work.
                    requests.remove(key, request)
                    request.deferred.cancel()
                }
            }
        }
    }

    private fun createRequest(key: K, fetch: suspend () -> V): Request<V> {
        val deferred = scope.async(start = CoroutineStart.LAZY) { fetch() }
        val request = Request(deferred)
        // Forget the request once it's done, but only if the map still points at this exact request.
        deferred.invokeOnCompletion { synchronized(lock) { requests.remove(key, request) } }
        return request
    }

    /** Test hook: how many callers are currently waiting on the request for [key]. */
    internal fun waiterCount(key: K): Int = synchronized(lock) { requests[key]?.waiters ?: 0 }
}
