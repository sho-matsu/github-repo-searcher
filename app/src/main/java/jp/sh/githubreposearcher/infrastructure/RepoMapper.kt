package jp.sh.githubreposearcher.infrastructure

import jp.sh.githubreposearcher.domain.GitHubRepo
import jp.sh.githubreposearcher.domain.RepoId
import jp.sh.githubreposearcher.domain.RepoName
import jp.sh.githubreposearcher.domain.RepoPageUrl

internal fun RepoDto.toDomain(): GitHubRepo =
    GitHubRepo(id = RepoId(id), name = RepoName(name), pageUrl = RepoPageUrl(htmlUrl))
