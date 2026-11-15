package jp.sh.githubreposearcher.reposearch.infrastructure

import java.io.IOException
import javax.inject.Inject
import jp.sh.githubreposearcher.core.di.IoDispatcher
import jp.sh.githubreposearcher.reposearch.domain.GitHubRepoRepository
import jp.sh.githubreposearcher.reposearch.domain.RepoSearchResult
import jp.sh.githubreposearcher.reposearch.domain.SearchError
import jp.sh.githubreposearcher.reposearch.domain.SearchQuery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

internal class RetrofitGitHubRepoRepository @Inject constructor(
    private val api: GitHubApi,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GitHubRepoRepository {

    override suspend fun search(query: SearchQuery): RepoSearchResult = withContext(ioDispatcher) {
        try {
            val response = api.searchRepositories(query.value)
            RepoSearchResult.Success(response.items.map { it.toDomain() })
        } catch (e: HttpException) {
            RepoSearchResult.Failure(e.toSearchError())
        } catch (_: IOException) {
            RepoSearchResult.Failure(SearchError.Network)
        } catch (_: SerializationException) {
            RepoSearchResult.Failure(SearchError.Unexpected)
        }
    }

    private fun HttpException.toSearchError(): SearchError = when {
        code() == HTTP_TOO_MANY_REQUESTS -> SearchError.RateLimited
        code() == HTTP_FORBIDDEN && isRateLimitExhausted() -> SearchError.RateLimited
        code() == HTTP_UNPROCESSABLE_ENTITY -> SearchError.InvalidQuery
        else -> SearchError.Unexpected
    }

    private fun HttpException.isRateLimitExhausted(): Boolean =
        response()?.headers()?.get(HEADER_RATE_LIMIT_REMAINING) == RATE_LIMIT_EXHAUSTED

    private companion object {
        const val HTTP_FORBIDDEN = 403
        const val HTTP_UNPROCESSABLE_ENTITY = 422
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HEADER_RATE_LIMIT_REMAINING = "x-ratelimit-remaining"
        const val RATE_LIMIT_EXHAUSTED = "0"
    }
}
