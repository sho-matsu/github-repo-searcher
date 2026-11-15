package jp.sh.githubreposearcher.reposearch.domain

sealed interface RepoSearchResult {
    data class Success(val repos: List<GitHubRepo>) : RepoSearchResult
    data class Failure(val error: SearchError) : RepoSearchResult
}

/** リポジトリ検索における想定内の失敗。 */
sealed interface SearchError {
    /** 通信できなかった。 */
    data object Network : SearchError

    /** API のレート制限に達した。 */
    data object RateLimited : SearchError

    /** クエリが API に受け付けられなかった。 */
    data object InvalidQuery : SearchError

    /** 上記以外のサーバーエラーや不正なレスポンス。 */
    data object Unexpected : SearchError
}
