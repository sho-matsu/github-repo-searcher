package jp.sh.githubreposearcher.application

import javax.inject.Inject
import jp.sh.githubreposearcher.domain.GitHubRepoSearchGateway
import jp.sh.githubreposearcher.domain.RepoSearchResult
import jp.sh.githubreposearcher.domain.SearchQuery

class SearchGitHubReposUseCase @Inject constructor(
    private val gateway: GitHubRepoSearchGateway,
) {
    suspend operator fun invoke(query: SearchQuery): RepoSearchResult = gateway.search(query)
}
