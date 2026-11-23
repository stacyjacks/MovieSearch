package kurmakaeva.anastasia.moviesearch.data.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import java.util.concurrent.ConcurrentLinkedQueue

class InMemoryCacheTest {
    private val time = TestTimeSource()
    private val cache = InMemoryCache<String, String>(ttl = 5.minutes, maxEntries = 2, timeSource = time)

    @Test
    fun returnsValueWithinTtl() {
        cache.put("a", "1")
        time += 4.minutes + 59.seconds

        assertEquals("1", cache.get("a"))
    }

    @Test
    fun expiresAfterTtl_andReadsDoNotExtendIt() {
        cache.put("a", "1")
        time += 3.minutes
        cache.get("a")
        time += 2.minutes

        assertNull(cache.get("a"))
    }

    @Test
    fun overwritingRestartsTtl() {
        cache.put("a", "1")
        time += 4.minutes
        cache.put("a", "2")
        time += 4.minutes

        assertEquals("2", cache.get("a"))
    }

    @Test
    fun evictsLeastRecentlyUsedWhenFull() {
        cache.put("a", "1")
        cache.put("b", "2")
        cache.get("a") // "b" is now least recently used
        cache.put("c", "3")

        assertEquals("1", cache.get("a"))
        assertNull(cache.get("b"))
        assertEquals("3", cache.get("c"))
    }

    /**
     * The cache is read on the main thread and written from IO threads. LinkedHashMap in access order
     * mutates on every get(), so without synchronization this fails with ConcurrentModificationException,
     * NPEs or corrupted entries.
     */
    @Test
    fun concurrentReadsAndWritesFromManyThreads_staySafe() = runBlocking {
        val shared = InMemoryCache<Int, String>(ttl = 5.minutes, maxEntries = 50)
        val errors = ConcurrentLinkedQueue<Throwable>()

        List(8) { worker ->
            launch(Dispatchers.Default) {
                repeat(20_000) { i ->
                    val key = (i * 31 + worker) % 100
                    try {
                        if (i % 3 == 0) {
                            shared.put(key, "value-$key")
                        } else {
                            val value = shared.get(key)
                            if (value != null && value != "value-$key") error("key $key returned $value")
                        }
                    } catch (t: Throwable) {
                        errors += t
                    }
                }
            }
        }.joinAll()

        assertTrue("concurrent access failed: ${errors.firstOrNull()}", errors.isEmpty())
        assertTrue("LRU bound exceeded", (0 until 100).count { shared.get(it) != null } <= 50)
    }
}
