package jp.sh.githubreposearcher.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import jp.sh.githubreposearcher.core.network.GitHubHeadersInterceptor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    private const val GITHUB_API_BASE_URL = "https://api.github.com/"
    private const val MEDIA_TYPE_JSON = "application/json"

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(headersInterceptor: GitHubHeadersInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(headersInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(GITHUB_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(MEDIA_TYPE_JSON.toMediaType()))
            .build()
}
