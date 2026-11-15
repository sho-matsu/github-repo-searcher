package jp.sh.githubreposearcher.reposearch.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.sh.githubreposearcher.reposearch.domain.GitHubRepoSearchGateway
import jp.sh.githubreposearcher.reposearch.infrastructure.RetrofitGitHubRepoSearchGateway

@Module
@InstallIn(SingletonComponent::class)
internal abstract class GatewayModule {
    @Binds
    abstract fun bindGitHubRepoSearchGateway(impl: RetrofitGitHubRepoSearchGateway): GitHubRepoSearchGateway
}
