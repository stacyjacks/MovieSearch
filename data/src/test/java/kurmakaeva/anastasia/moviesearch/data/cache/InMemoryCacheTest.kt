package kurmakaeva.anastasia.moviesearch.data.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

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
}
