package jp.sh.githubreposearcher.reposearch.infrastructure

import jp.sh.githubreposearcher.core.di.NetworkModule
import jp.sh.githubreposearcher.core.network.GitHubHeadersInterceptor
import jp.sh.githubreposearcher.reposearch.di.GitHubApiModule
import jp.sh.githubreposearcher.reposearch.domain.RepoId
import jp.sh.githubreposearcher.reposearch.domain.RepoName
import jp.sh.githubreposearcher.reposearch.domain.RepoPageUrl
import jp.sh.githubreposearcher.reposearch.domain.RepoSearchResult
import jp.sh.githubreposearcher.reposearch.domain.SearchError
import jp.sh.githubreposearcher.reposearch.domain.SearchQuery
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 実際の HTTP レスポンスを使って、GitHub Search API との間の取り決め(リクエストの形式、JSON の項目名、ヘッダー)を検証する。
 * Retrofit/OkHttp/Json は本番の DI module で組み立て、接続先だけを MockWebServer に差し替える。
 */
class GitHubSearchApiContractTest {

    @get:Rule
    val server = MockWebServer()

    private val query = checkNotNull(SearchQuery.ofOrNull("Android"))

    private fun TestScope.repository(): RetrofitGitHubRepoRepository {
        val retrofit = NetworkModule.provideRetrofit(
            okHttpClient = NetworkModule.provideOkHttpClient(GitHubHeadersInterceptor()),
            json = NetworkModule.provideJson(),
        ).newBuilder()
            .baseUrl(server.url("/"))
            .build()
        return RetrofitGitHubRepoRepository(GitHubApiModule.provideGitHubApi(retrofit), StandardTestDispatcher(testScheduler))
    }

    private fun enqueueJson(body: String, code: Int = 200) {
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))
    }

    @Test
    fun `検索 API に q パラメータと GitHub 推奨ヘッダーを付けてリクエストする`() = runTest {
        enqueueJson("""{"total_count": 0, "incomplete_results": false, "items": []}""")

        repository().search(query)

        val request = server.takeRequest()
        assertEquals("/search/repositories", request.requestUrl?.encodedPath)
        assertEquals("Android", request.requestUrl?.queryParameter("q"))
        assertEquals("application/vnd.github+json", request.getHeader("Accept"))
        assertEquals("2022-11-28", request.getHeader("X-GitHub-Api-Version"))
    }

    @Test
    fun `レスポンスの JSON を、知らない項目を無視してドメインモデルに変換する`() = runTest {
        enqueueJson(
            """
            {
              "total_count": 1,
              "incomplete_results": false,
              "items": [
                {
                  "id": 90792131,
                  "name": "nowinandroid",
                  "full_name": "android/nowinandroid",
                  "owner": {"login": "android", "id": 32689599},
                  "html_url": "https://github.com/android/nowinandroid",
                  "stargazers_count": 17000,
                  "language": "Kotlin"
                }
              ]
            }
            """.trimIndent(),
        )

        val result = repository().search(query)

        val repo = (result as RepoSearchResult.Success).repos.single()
        assertEquals(RepoId(90792131), repo.id)
        assertEquals(RepoName("nowinandroid"), repo.name)
        assertEquals(RepoPageUrl("https://github.com/android/nowinandroid"), repo.pageUrl)
    }

    @Test
    fun `必須の項目がない JSON のとき Unexpected を返す`() = runTest {
        enqueueJson("""{"items": [{"id": 1, "name": "nowinandroid"}]}""")

        assertEquals(RepoSearchResult.Failure(SearchError.Unexpected), repository().search(query))
    }

    @Test
    fun `GitHub の rate-limit ヘッダー付きの 403 のとき RateLimited を返す`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setHeader("X-RateLimit-Remaining", "0")
                .setBody("""{"message": "API rate limit exceeded"}"""),
        )

        assertEquals(RepoSearchResult.Failure(SearchError.RateLimited), repository().search(query))
    }

    @Test
    fun `API が 422 を返したとき InvalidQuery を返す`() = runTest {
        enqueueJson("""{"message": "Validation Failed"}""", code = 422)

        assertEquals(RepoSearchResult.Failure(SearchError.InvalidQuery), repository().search(query))
    }
}
