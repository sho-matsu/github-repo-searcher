package jp.sh.githubreposearcher.reposearch.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import jp.sh.githubreposearcher.R
import jp.sh.githubreposearcher.reposearch.application.SearchGitHubReposUseCase
import jp.sh.githubreposearcher.reposearch.domain.GitHubRepo
import jp.sh.githubreposearcher.reposearch.domain.RepoSearchResult
import jp.sh.githubreposearcher.reposearch.domain.SearchError
import jp.sh.githubreposearcher.reposearch.domain.SearchQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RepoSearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val searchGitHubRepos: SearchGitHubReposUseCase,
) : ViewModel() {

    /**
     * 最後に確定した(検索を実行した)クエリ。Retry やプロセス再生成後の再検索に使う。
     * Fragment 引数と同じキーに書き戻すことで、初期クエリと「確定済みクエリ」を一つの値として扱う。
     */
    private var submittedQuery: SearchQuery?
        get() = savedStateHandle.get<String>(ARG_QUERY)?.let(SearchQuery::ofOrNull)
        set(value) {
            savedStateHandle[ARG_QUERY] = value?.value
        }

    private val _uiState = MutableStateFlow(
        RepoSearchUiState(
            queryInput = savedStateHandle[KEY_QUERY_INPUT]
                ?: savedStateHandle[ARG_QUERY]
                ?: "",
        ),
    )
    val uiState: StateFlow<RepoSearchUiState> = _uiState.asStateFlow()

    /**
     * CONFLATED にしているのは、収集開始前に送られたリクエストを取りこぼさず、かつ処理が追いつかない場合は
     * 最新のリクエストだけを残すため。flatMapLatest により、新しい検索やクリアで進行中の検索はキャンセルされ、
     * 古い結果が後から表示されることはない。
     */
    private val searchRequests = Channel<SearchRequest>(Channel.CONFLATED)

    init {
        searchRequests.receiveAsFlow()
            .flatMapLatest { request ->
                when (request) {
                    is SearchRequest.Execute -> searchContent(request.query)
                    SearchRequest.Clear -> flowOf(RepoSearchContent.Idle)
                }
            }
            .onEach { content -> _uiState.update { it.copy(content = content) } }
            .launchIn(viewModelScope)

        submittedQuery?.let { searchRequests.trySend(SearchRequest.Execute(it)) }
    }

    fun onEvent(event: RepoSearchEvent) {
        when (event) {
            is RepoSearchEvent.QueryChanged -> updateQueryInput(event.input)
            RepoSearchEvent.Search -> submit()
            RepoSearchEvent.ClearQuery -> clear()
            RepoSearchEvent.Retry -> submittedQuery?.let { searchRequests.trySend(SearchRequest.Execute(it)) }
        }
    }

    private fun updateQueryInput(input: String) {
        savedStateHandle[KEY_QUERY_INPUT] = input
        _uiState.update { it.copy(queryInput = input) }
    }

    /** 空白のみのクエリでは検索しない(検索ボタンも無効化している)。 */
    private fun submit() {
        val query = SearchQuery.ofOrNull(_uiState.value.queryInput) ?: return
        submittedQuery = query
        searchRequests.trySend(SearchRequest.Execute(query))
    }

    private fun clear() {
        updateQueryInput("")
        submittedQuery = null
        searchRequests.trySend(SearchRequest.Clear)
    }

    private fun searchContent(query: SearchQuery): Flow<RepoSearchContent> = flow {
        emit(RepoSearchContent.Loading)
        emit(searchGitHubRepos(query).toContent())
    }

    private sealed interface SearchRequest {
        data class Execute(val query: SearchQuery) : SearchRequest
        data object Clear : SearchRequest
    }

    internal companion object {
        /** 初期クエリを受け取る引数キー。呼び出し元(Fragment)はこのキーで引数を渡す。 */
        const val ARG_QUERY = "query"
        private const val KEY_QUERY_INPUT = "query_input"
    }
}

private fun RepoSearchResult.toContent(): RepoSearchContent = when (this) {
    is RepoSearchResult.Success ->
        if (repos.isEmpty()) RepoSearchContent.Empty else RepoSearchContent.Loaded(repos.map { it.toUiModel() })

    is RepoSearchResult.Failure -> error.toContent()
}

private fun GitHubRepo.toUiModel() = RepoItemUiModel(id = id.value, name = name.value, pageUrl = pageUrl.value)

private fun SearchError.toContent(): RepoSearchContent.Error = when (this) {
    SearchError.Network -> RepoSearchContent.Error(R.string.repo_search_error_network, isRetryable = true)
    SearchError.RateLimited -> RepoSearchContent.Error(R.string.repo_search_error_rate_limited, isRetryable = true)
    SearchError.InvalidQuery -> RepoSearchContent.Error(R.string.repo_search_error_invalid_query, isRetryable = false)
    SearchError.Unexpected -> RepoSearchContent.Error(R.string.repo_search_error_unexpected, isRetryable = true)
}
