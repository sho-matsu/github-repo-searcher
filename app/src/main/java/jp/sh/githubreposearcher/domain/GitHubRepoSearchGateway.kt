package jp.sh.githubreposearcher.domain

/** GitHub のリポジトリ検索を行うポート。メインスレッドから安全に呼び出せること。 */
fun interface GitHubRepoSearchGateway {
    suspend fun search(query: SearchQuery): RepoSearchResult
}
