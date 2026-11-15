package jp.sh.githubreposearcher.core.network

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/** GitHub REST API が推奨するヘッダーを付与する。 */
internal class GitHubHeadersInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(
        chain.request().newBuilder()
            .header(HEADER_ACCEPT, MEDIA_TYPE_GITHUB_JSON)
            .header(HEADER_API_VERSION, API_VERSION)
            .build(),
    )

    private companion object {
        const val HEADER_ACCEPT = "Accept"
        const val MEDIA_TYPE_GITHUB_JSON = "application/vnd.github+json"
        const val HEADER_API_VERSION = "X-GitHub-Api-Version"
        const val API_VERSION = "2022-11-28"
    }
}
