package jp.sh.githubreposearcher.reposearch.presentation

import androidx.lifecycle.SavedStateHandle
import jp.sh.githubreposearcher.R
import jp.sh.githubreposearcher.reposearch.application.SearchGitHubReposUseCase
import jp.sh.githubreposearcher.reposearch.domain.GitHubRepo
import jp.sh.githubreposearcher.reposearch.domain.GitHubRepoRepository
import jp.sh.githubreposearcher.reposearch.domain.RepoId
import jp.sh.githubreposearcher.reposearch.domain.RepoName
import jp.sh.githubreposearcher.reposearch.domain.RepoPageUrl
import jp.sh.githubreposearcher.reposearch.domain.RepoSearchResult
import jp.sh.githubreposearcher.reposearch.domain.SearchError
import jp.sh.githubreposearcher.reposearch.domain.SearchQuery
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepoSearchViewModelTest {

    /** 呼び出しを記録し、あらかじめ用意した結果を順に返す Fake。 */
    private class FakeGitHubRepoRepository(vararg results: RepoSearchResult) : GitHubRepoRepository {
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

    private fun viewModel(query: String?, repository: GitHubRepoRepository) = RepoSearchViewModel(
        savedStateHandle = SavedStateHandle(mapOf(RepoSearchViewModel.ARG_QUERY to query)),
        searchGitHubRepos = SearchGitHubReposUseCase(repository),
    )

    private val repos = listOf(
        GitHubRepo(RepoId(1), RepoName("nowinandroid"), RepoPageUrl("https://github.com/android/nowinandroid")),
        GitHubRepo(RepoId(2), RepoName("architecture-samples"), RepoPageUrl("https://github.com/android/architecture-samples")),
    )

    @Test
    fun `引数のクエリ文字列を検索窓に表示し、そのクエリで検索する`() = runTest {
        val repository = FakeGitHubRepoRepository(RepoSearchResult.Success(repos))

        val viewModel = viewModel("Android", repository)

        assertEquals("Android", viewModel.uiState.value.queryInput)
        assertEquals(listOf("Android"), repository.requestedQueries.map { it.value })
    }

    @Test
    fun `検索に成功したときリポジトリ名とページ URL の一覧を表示する`() = runTest {
        val viewModel = viewModel("Android", FakeGitHubRepoRepository(RepoSearchResult.Success(repos)))

        val expected = RepoSearchContent.Loaded(
            listOf(
                RepoItemUiModel(1, "nowinandroid", "https://github.com/android/nowinandroid"),
                RepoItemUiModel(2, "architecture-samples", "https://github.com/android/architecture-samples"),
            ),
        )
        assertEquals(expected, viewModel.uiState.value.content)
    }

    @Test
    fun `検索結果が 0 件のとき Empty を表示する`() = runTest {
        val viewModel = viewModel("Android", FakeGitHubRepoRepository(RepoSearchResult.Success(emptyList())))

        assertEquals(RepoSearchContent.Empty, viewModel.uiState.value.content)
    }

    @Test
    fun `検索結果が返るまでは Loading を表示する`() = runTest {
        val response = CompletableDeferred<RepoSearchResult>()
        val viewModel = viewModel("Android", GitHubRepoRepository { response.await() })

        assertEquals(RepoSearchContent.Loading, viewModel.uiState.value.content)

        response.complete(RepoSearchResult.Success(repos))
        assertTrue(viewModel.uiState.value.content is RepoSearchContent.Loaded)
    }

    @Test
    fun `通信エラーのときリトライ可能なエラーを表示する`() = runTest {
        val viewModel = viewModel("Android", FakeGitHubRepoRepository(RepoSearchResult.Failure(SearchError.Network)))

        assertEquals(
            RepoSearchContent.Error(R.string.repo_search_error_network, isRetryable = true),
            viewModel.uiState.value.content,
        )
    }

    @Test
    fun `API がクエリを受け付けなかったときリトライ不可のエラーを表示する`() = runTest {
        val viewModel = viewModel("Android", FakeGitHubRepoRepository(RepoSearchResult.Failure(SearchError.InvalidQuery)))

        assertEquals(
            RepoSearchContent.Error(R.string.repo_search_error_invalid_query, isRetryable = false),
            viewModel.uiState.value.content,
        )
    }

    @Test
    fun `Retry イベントで再検索し、結果を表示する`() = runTest {
        val repository = FakeGitHubRepoRepository(
            RepoSearchResult.Failure(SearchError.RateLimited),
            RepoSearchResult.Success(repos),
        )
        val viewModel = viewModel("Android", repository)

        viewModel.onEvent(RepoSearchEvent.Retry)

        assertEquals(listOf("Android", "Android"), repository.requestedQueries.map { it.value })
        assertTrue(viewModel.uiState.value.content is RepoSearchContent.Loaded)
    }

    @Test
    fun `Retry は入力中の文字列ではなく最後に検索したクエリで再検索する`() = runTest {
        val repository = FakeGitHubRepoRepository(
            RepoSearchResult.Failure(SearchError.Network),
            RepoSearchResult.Success(repos),
        )
        val viewModel = viewModel("Android", repository)

        viewModel.onEvent(RepoSearchEvent.QueryChanged("Kotlin"))
        viewModel.onEvent(RepoSearchEvent.Retry)

        assertEquals(listOf("Android", "Android"), repository.requestedQueries.map { it.value })
    }

    @Test
    fun `クエリ引数が空白またはないとき検索せず Idle を表示する`() = runTest {
        listOf("  ", null).forEach { query ->
            val repository = FakeGitHubRepoRepository()

            val viewModel = viewModel(query, repository)

            assertTrue(repository.requestedQueries.isEmpty())
            assertEquals(RepoSearchContent.Idle, viewModel.uiState.value.content)
        }
    }

    @Test
    fun `クエリを入力しただけでは検索しない`() = runTest {
        val repository = FakeGitHubRepoRepository(RepoSearchResult.Success(repos))
        val viewModel = viewModel(null, repository)

        viewModel.onEvent(RepoSearchEvent.QueryChanged("Kotlin"))

        assertEquals("Kotlin", viewModel.uiState.value.queryInput)
        assertTrue(repository.requestedQueries.isEmpty())
        assertEquals(RepoSearchContent.Idle, viewModel.uiState.value.content)
    }

    @Test
    fun `Search イベントで入力中のクエリを前後の空白を除いて検索する`() = runTest {
        val repository = FakeGitHubRepoRepository(RepoSearchResult.Success(emptyList()), RepoSearchResult.Success(repos))
        val viewModel = viewModel("Android", repository)

        viewModel.onEvent(RepoSearchEvent.QueryChanged("  Kotlin "))
        viewModel.onEvent(RepoSearchEvent.Search)

        assertEquals(listOf("Android", "Kotlin"), repository.requestedQueries.map { it.value })
        assertTrue(viewModel.uiState.value.content is RepoSearchContent.Loaded)
    }

    @Test
    fun `入力が空白のみのとき Search イベントでは検索しない`() = runTest {
        val repository = FakeGitHubRepoRepository()
        val viewModel = viewModel(null, repository)

        viewModel.onEvent(RepoSearchEvent.QueryChanged("   "))
        viewModel.onEvent(RepoSearchEvent.Search)

        assertFalse(viewModel.uiState.value.canSearch)
        assertTrue(repository.requestedQueries.isEmpty())
        assertEquals(RepoSearchContent.Idle, viewModel.uiState.value.content)
    }

    @Test
    fun `ClearQuery イベントでクエリと検索結果をクリアする`() = runTest {
        val viewModel = viewModel("Android", FakeGitHubRepoRepository(RepoSearchResult.Success(repos)))

        viewModel.onEvent(RepoSearchEvent.ClearQuery)

        assertEquals("", viewModel.uiState.value.queryInput)
        assertFalse(viewModel.uiState.value.canClear)
        assertEquals(RepoSearchContent.Idle, viewModel.uiState.value.content)
    }

    @Test
    fun `検索中にクリアしたとき、後から返った結果で一覧を上書きしない`() = runTest {
        val response = CompletableDeferred<RepoSearchResult>()
        val viewModel = viewModel("Android", GitHubRepoRepository { response.await() })

        viewModel.onEvent(RepoSearchEvent.ClearQuery)
        response.complete(RepoSearchResult.Success(repos))

        assertEquals(RepoSearchContent.Idle, viewModel.uiState.value.content)
    }

    @Test
    fun `クリア後の Retry では検索しない`() = runTest {
        val repository = FakeGitHubRepoRepository(RepoSearchResult.Failure(SearchError.Network))
        val viewModel = viewModel("Android", repository)

        viewModel.onEvent(RepoSearchEvent.ClearQuery)
        viewModel.onEvent(RepoSearchEvent.Retry)

        assertEquals(1, repository.requestedQueries.size)
        assertEquals(RepoSearchContent.Idle, viewModel.uiState.value.content)
    }
}
