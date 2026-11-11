package jp.sh.githubreposearcher.presentation.search

import androidx.annotation.StringRes

data class RepoSearchUiState(
    /** 検索窓に入力中の文字列。検索が実行されるまでは結果に影響しない。 */
    val queryInput: String = "",
    val content: RepoSearchContent = RepoSearchContent.Idle,
) {
    val canSearch: Boolean get() = queryInput.isNotBlank()
    val canClear: Boolean get() = queryInput.isNotEmpty()
}

sealed interface RepoSearchContent {
    /** まだ検索していない、またはクエリがクリアされた状態。 */
    data object Idle : RepoSearchContent
    data object Loading : RepoSearchContent
    data object Empty : RepoSearchContent
    data class Loaded(val repos: List<RepoItemUiModel>) : RepoSearchContent
    data class Error(@param:StringRes val messageResId: Int, val isRetryable: Boolean) : RepoSearchContent
}

data class RepoItemUiModel(val id: Long, val name: String)

sealed interface RepoSearchEvent {
    data class QueryChanged(val input: String) : RepoSearchEvent

    /** 検索ボタンまたはキーボードの検索アクションで、入力中のクエリを確定して検索する。 */
    data object Search : RepoSearchEvent
    data object ClearQuery : RepoSearchEvent
    data object Retry : RepoSearchEvent
}
