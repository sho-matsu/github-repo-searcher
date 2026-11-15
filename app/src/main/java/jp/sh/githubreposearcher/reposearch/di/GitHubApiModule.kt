package jp.sh.githubreposearcher.reposearch.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import jp.sh.githubreposearcher.reposearch.infrastructure.GitHubApi
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object GitHubApiModule {
    @Provides
    @Singleton
    fun provideGitHubApi(retrofit: Retrofit): GitHubApi = retrofit.create(GitHubApi::class.java)
}
