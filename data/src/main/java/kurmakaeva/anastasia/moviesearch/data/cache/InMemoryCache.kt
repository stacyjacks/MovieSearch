package kurmakaeva.anastasia.moviesearch.data.cache

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Small thread-safe in-memory cache with a fixed time-to-live and an LRU size bound.
 *
 * - Entries expire [ttl] after they were written (reads don't extend their lifetime).
 * - Expired entries are dropped lazily when read; no timers or background work.
 * - When more than [maxEntries] are stored, the least recently used entry is evicted.
 * - Time is measured with a monotonic [timeSource], so wall-clock changes can't affect expiry.
 */
internal class InMemoryCache<K : Any, V : Any>(
    private val ttl: Duration,
    private val maxEntries: Int,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    init {
        require(ttl.isPositive()) { "ttl must be positive" }
        require(maxEntries > 0) { "maxEntries must be positive" }
    }

    private class Entry<V>(val value: V, val expiresAt: TimeMark)

    // accessOrder = true: iteration order is least- to most-recently used, so the eldest entry is the LRU one.
    private val entries = object : LinkedHashMap<K, Entry<V>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, Entry<V>>): Boolean =
            size > maxEntries
    }

    /** Returns the cached value, or null if there is none or it has expired. */
    @Synchronized
    fun get(key: K): V? {
        val entry = entries[key] ?: return null
        if (entry.expiresAt.hasPassedNow()) {
            entries.remove(key)
            return null
        }
        return entry.value
    }

    @Synchronized
    fun put(key: K, value: V) {
        entries[key] = Entry(value, timeSource.markNow() + ttl)
    }
}
