package jp.sh.githubreposearcher.presentation.search

import androidx.lifecycle.SavedStateHandle
import jp.sh.githubreposearcher.R
import jp.sh.githubreposearcher.application.SearchGitHubReposUseCase
import jp.sh.githubreposearcher.domain.GitHubRepo
import jp.sh.githubreposearcher.domain.GitHubRepoSearchGateway
import jp.sh.githubreposearcher.domain.RepoId
import jp.sh.githubreposearcher.domain.RepoName
import jp.sh.githubreposearcher.domain.RepoSearchResult
import jp.sh.githubreposearcher.domain.SearchError
import jp.sh.githubreposearcher.domain.SearchQuery
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepoSearchViewModelTest {

    /** 呼び出しを記録し、あらかじめ用意した結果を順に返す Fake。 */
    private class FakeGateway(vararg results: RepoSearchResult) : GitHubRepoSearchGateway {
        private val pending = ArrayDeque(results.toList())
        val requestedQueries = mutableListOf<SearchQuery>()

        override suspend fun search(query: SearchQuery): RepoSearchResult {
            requestedQueries += query
            return pending.removeFirst()
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(query: String?, gateway: GitHubRepoSearchGateway) = RepoSearchViewModel(
        savedStateHandle = SavedStateHandle(mapOf(RepoSearchFragment.ARG_QUERY to query)),
        searchGitHubRepos = SearchGitHubReposUseCase(gateway),
    )

    private val repos = listOf(
        GitHubRepo(RepoId(1), RepoName("nowinandroid")),
        GitHubRepo(RepoId(2), RepoName("architecture-samples")),
    )

    @Test
    fun `引数のクエリ文字列を状態に保持し、そのクエリで検索する`() = runTest {
        val gateway = FakeGateway(RepoSearchResult.Success(repos))

        val viewModel = viewModel("Android", gateway)

        assertEquals("Android", viewModel.uiState.value.query)
        assertEquals(listOf("Android"), gateway.requestedQueries.map { it.value })
    }

    @Test
    fun `検索に成功したときリポジトリ名の一覧を表示する`() = runTest {
        val viewModel = viewModel("Android", FakeGateway(RepoSearchResult.Success(repos)))

        val expected = RepoSearchContent.Loaded(
            listOf(RepoItemUiModel(1, "nowinandroid"), RepoItemUiModel(2, "architecture-samples")),
        )
        assertEquals(expected, viewModel.uiState.value.content)
    }

    @Test
    fun `検索結果が 0 件のとき Empty を表示する`() = runTest {
        val viewModel = viewModel("Android", FakeGateway(RepoSearchResult.Success(emptyList())))

        assertEquals(RepoSearchContent.Empty, viewModel.uiState.value.content)
    }

    @Test
    fun `検索結果が返るまでは Loading を表示する`() = runTest {
        val response = CompletableDeferred<RepoSearchResult>()
        val viewModel = viewModel("Android", GitHubRepoSearchGateway { response.await() })

        assertEquals(RepoSearchContent.Loading, viewModel.uiState.value.content)

        response.complete(RepoSearchResult.Success(repos))
        assertTrue(viewModel.uiState.value.content is RepoSearchContent.Loaded)
    }

    @Test
    fun `通信エラーのときリトライ可能なエラーを表示する`() = runTest {
        val viewModel = viewModel("Android", FakeGateway(RepoSearchResult.Failure(SearchError.Network)))

        assertEquals(
            RepoSearchContent.Error(R.string.repo_search_error_network, isRetryable = true),
            viewModel.uiState.value.content,
        )
    }

    @Test
    fun `Retry イベントで再検索し、結果を表示する`() = runTest {
        val gateway = FakeGateway(
            RepoSearchResult.Failure(SearchError.RateLimited),
            RepoSearchResult.Success(repos),
        )
        val viewModel = viewModel("Android", gateway)

        viewModel.onEvent(RepoSearchEvent.Retry)

        assertEquals(2, gateway.requestedQueries.size)
        assertTrue(viewModel.uiState.value.content is RepoSearchContent.Loaded)
    }

    @Test
    fun `クエリが空白のとき API を呼ばずにリトライ不可のエラーを表示する`() = runTest {
        val gateway = FakeGateway()

        val viewModel = viewModel("  ", gateway)

        assertTrue(gateway.requestedQueries.isEmpty())
        assertEquals(
            RepoSearchContent.Error(R.string.repo_search_error_invalid_query, isRetryable = false),
            viewModel.uiState.value.content,
        )
    }

    @Test
    fun `クエリ引数がないときもリトライ不可のエラーを表示する`() = runTest {
        val viewModel = viewModel(null, FakeGateway())

        assertEquals("", viewModel.uiState.value.query)
        assertEquals(
            RepoSearchContent.Error(R.string.repo_search_error_invalid_query, isRetryable = false),
            viewModel.uiState.value.content,
        )
    }
}
