package jp.sh.githubreposearcher.reposearch.infrastructure

import jp.sh.githubreposearcher.reposearch.domain.GitHubRepo
import jp.sh.githubreposearcher.reposearch.domain.RepoId
import jp.sh.githubreposearcher.reposearch.domain.RepoName
import jp.sh.githubreposearcher.reposearch.domain.RepoPageUrl

internal fun RepoDto.toDomain(): GitHubRepo =
    GitHubRepo(id = RepoId(id), name = RepoName(name), pageUrl = RepoPageUrl(htmlUrl))
