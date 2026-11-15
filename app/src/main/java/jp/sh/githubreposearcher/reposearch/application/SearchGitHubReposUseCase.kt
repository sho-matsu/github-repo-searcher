package jp.sh.githubreposearcher.reposearch.application

import javax.inject.Inject
import jp.sh.githubreposearcher.reposearch.domain.GitHubRepoSearchGateway
import jp.sh.githubreposearcher.reposearch.domain.RepoSearchResult
import jp.sh.githubreposearcher.reposearch.domain.SearchQuery

class SearchGitHubReposUseCase @Inject constructor(
    private val gateway: GitHubRepoSearchGateway,
) {
    suspend operator fun invoke(query: SearchQuery): RepoSearchResult = gateway.search(query)
}
