package jp.sh.githubreposearcher.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import jp.sh.githubreposearcher.R
import jp.sh.githubreposearcher.application.SearchGitHubReposUseCase
import jp.sh.githubreposearcher.domain.GitHubRepo
import jp.sh.githubreposearcher.domain.RepoSearchResult
import jp.sh.githubreposearcher.domain.SearchError
import jp.sh.githubreposearcher.domain.SearchQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RepoSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchGitHubRepos: SearchGitHubReposUseCase,
) : ViewModel() {

    private val rawQuery: String = savedStateHandle[RepoSearchFragment.ARG_QUERY] ?: ""
    private val query: SearchQuery? = SearchQuery.ofOrNull(rawQuery)

    private val _uiState = MutableStateFlow(RepoSearchUiState(query = rawQuery))
    val uiState: StateFlow<RepoSearchUiState> = _uiState.asStateFlow()

    init {
        search()
    }

    fun onEvent(event: RepoSearchEvent) {
        when (event) {
            RepoSearchEvent.Retry -> search()
        }
    }

    private fun search() {
        val validQuery = query ?: return updateContent(SearchError.InvalidQuery.toContent())
        updateContent(RepoSearchContent.Loading)
        viewModelScope.launch {
            updateContent(searchGitHubRepos(validQuery).toContent())
        }
    }

    private fun updateContent(content: RepoSearchContent) {
        _uiState.update { it.copy(content = content) }
    }
}

private fun RepoSearchResult.toContent(): RepoSearchContent = when (this) {
    is RepoSearchResult.Success ->
        if (repos.isEmpty()) RepoSearchContent.Empty else RepoSearchContent.Loaded(repos.map { it.toUiModel() })

    is RepoSearchResult.Failure -> error.toContent()
}

private fun GitHubRepo.toUiModel() = RepoItemUiModel(id = id.value, name = name.value)

private fun SearchError.toContent(): RepoSearchContent.Error = when (this) {
    SearchError.Network -> RepoSearchContent.Error(R.string.repo_search_error_network, isRetryable = true)
    SearchError.RateLimited -> RepoSearchContent.Error(R.string.repo_search_error_rate_limited, isRetryable = true)
    SearchError.InvalidQuery -> RepoSearchContent.Error(R.string.repo_search_error_invalid_query, isRetryable = false)
    SearchError.Unexpected -> RepoSearchContent.Error(R.string.repo_search_error_unexpected, isRetryable = true)
}
