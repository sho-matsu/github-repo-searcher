package jp.sh.githubreposearcher.reposearch.infrastructure

import java.io.IOException
import jp.sh.githubreposearcher.reposearch.domain.RepoId
import jp.sh.githubreposearcher.reposearch.domain.RepoName
import jp.sh.githubreposearcher.reposearch.domain.RepoPageUrl
import jp.sh.githubreposearcher.reposearch.domain.RepoSearchResult
import jp.sh.githubreposearcher.reposearch.domain.SearchError
import jp.sh.githubreposearcher.reposearch.domain.SearchQuery
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.Headers.Companion.headersOf
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RetrofitGitHubRepoRepositoryTest {

    private val query = checkNotNull(SearchQuery.ofOrNull("Android"))

    private fun TestScope.repository(api: GitHubApi) =
        RetrofitGitHubRepoRepository(api, StandardTestDispatcher(testScheduler))

    private fun throwingApi(throwable: Throwable) = GitHubApi { throw throwable }

    private fun httpError(code: Int, vararg headers: String): HttpException {
        val rawResponse = okhttp3.Response.Builder()
            .request(Request.Builder().url("https://api.github.com/search/repositories").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("error")
            .headers(headersOf(*headers))
            .build()
        return HttpException(Response.error<Any>("{}".toResponseBody(), rawResponse))
    }

    @Test
    fun `クエリをそのまま API に渡し、結果をドメインモデルに変換して返す`() = runTest {
        var requestedQuery: String? = null
        val api = GitHubApi { q ->
            requestedQuery = q
            SearchRepositoriesResponseDto(listOf(RepoDto(id = 1, name = "nowinandroid", htmlUrl = "https://github.com/android/nowinandroid")))
        }

        val result = repository(api).search(query)

        assertEquals("Android", requestedQuery)
        val repos = (result as RepoSearchResult.Success).repos
        val repo = repos.single()
        assertEquals(RepoId(1), repo.id)
        assertEquals(RepoName("nowinandroid"), repo.name)
        assertEquals(RepoPageUrl("https://github.com/android/nowinandroid"), repo.pageUrl)
    }

    @Test
    fun `通信エラーのとき Network を返す`() = runTest {
        assertEquals(RepoSearchResult.Failure(SearchError.Network), repository(throwingApi(IOException())).search(query))
    }

    @Test
    fun `API が rate-limit ヘッダー付きの 403 を返したとき RateLimited を返す`() = runTest {
        val error = httpError(403, "x-ratelimit-remaining", "0")
        assertEquals(RepoSearchResult.Failure(SearchError.RateLimited), repository(throwingApi(error)).search(query))
    }

    @Test
    fun `API がセカンダリレート制限の retry-after 付きの 403 を返したとき RateLimited を返す`() = runTest {
        val error = httpError(403, "x-ratelimit-remaining", "10", "retry-after", "60")
        assertEquals(RepoSearchResult.Failure(SearchError.RateLimited), repository(throwingApi(error)).search(query))
    }

    @Test
    fun `API が 429 を返したとき RateLimited を返す`() = runTest {
        assertEquals(RepoSearchResult.Failure(SearchError.RateLimited), repository(throwingApi(httpError(429))).search(query))
    }

    @Test
    fun `rate-limit に達していない 403 のとき Unexpected を返す`() = runTest {
        val error = httpError(403, "x-ratelimit-remaining", "10")
        assertEquals(RepoSearchResult.Failure(SearchError.Unexpected), repository(throwingApi(error)).search(query))
    }

    @Test
    fun `API が 422 を返したとき InvalidQuery を返す`() = runTest {
        assertEquals(RepoSearchResult.Failure(SearchError.InvalidQuery), repository(throwingApi(httpError(422))).search(query))
    }

    @Test
    fun `API が 500 を返したとき Unexpected を返す`() = runTest {
        assertEquals(RepoSearchResult.Failure(SearchError.Unexpected), repository(throwingApi(httpError(500))).search(query))
    }

    @Test
    fun `レスポンスを解析できないとき Unexpected を返す`() = runTest {
        val result = repository(throwingApi(SerializationException("broken"))).search(query)
        assertEquals(RepoSearchResult.Failure(SearchError.Unexpected), result)
    }

    @Test
    fun `ドメインモデルの制約を満たさない項目を含むとき Unexpected を返す`() = runTest {
        val invalidRepos = listOf(
            RepoDto(id = 0, name = "nowinandroid", htmlUrl = "https://github.com/android/nowinandroid"),
            RepoDto(id = 1, name = " ", htmlUrl = "https://github.com/android/nowinandroid"),
            RepoDto(id = 1, name = "nowinandroid", htmlUrl = "http://github.com/android/nowinandroid"),
        )
        invalidRepos.forEach { invalidRepo ->
            val api = GitHubApi { SearchRepositoriesResponseDto(listOf(invalidRepo)) }

            assertEquals(RepoSearchResult.Failure(SearchError.Unexpected), repository(api).search(query))
        }
    }
}
