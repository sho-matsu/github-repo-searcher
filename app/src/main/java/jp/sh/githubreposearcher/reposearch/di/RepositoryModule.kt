package jp.sh.githubreposearcher.reposearch.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.sh.githubreposearcher.reposearch.domain.GitHubRepoRepository
import jp.sh.githubreposearcher.reposearch.infrastructure.RetrofitGitHubRepoRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    abstract fun bindGitHubRepoRepository(impl: RetrofitGitHubRepoRepository): GitHubRepoRepository
}
