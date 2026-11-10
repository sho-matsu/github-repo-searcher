package jp.sh.githubreposearcher.presentation.search

import androidx.annotation.StringRes

data class RepoSearchUiState(
    val query: String,
    val content: RepoSearchContent = RepoSearchContent.Loading,
)

sealed interface RepoSearchContent {
    data object Loading : RepoSearchContent
    data object Empty : RepoSearchContent
    data class Loaded(val repos: List<RepoItemUiModel>) : RepoSearchContent
    data class Error(@param:StringRes val messageResId: Int, val isRetryable: Boolean) : RepoSearchContent
}

data class RepoItemUiModel(val id: Long, val name: String)

sealed interface RepoSearchEvent {
    data object Retry : RepoSearchEvent
}
