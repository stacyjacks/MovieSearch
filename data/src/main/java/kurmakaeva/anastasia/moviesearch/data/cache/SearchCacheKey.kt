package kurmakaeva.anastasia.moviesearch.data.cache

/** TMDB search is case-insensitive, so queries are normalised to share cache entries. */
@ConsistentCopyVisibility
internal data class SearchCacheKey private constructor(val query: String, val page: Int) {
    companion object {
        fun of(query: String, page: Int) = SearchCacheKey(query.trim().lowercase(), page)
    }
}
