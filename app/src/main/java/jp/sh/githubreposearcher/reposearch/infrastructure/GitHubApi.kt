package jp.sh.githubreposearcher.reposearch.infrastructure

import retrofit2.http.GET
import retrofit2.http.Query

internal fun interface GitHubApi {
    @GET("search/repositories")
    suspend fun searchRepositories(@Query("q") query: String): SearchRepositoriesResponseDto
}
