package jp.sh.githubreposearcher.reposearch.infrastructure

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SearchRepositoriesResponseDto(
    @SerialName("items") val items: List<RepoDto>,
)

@Serializable
internal data class RepoDto(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("html_url") val htmlUrl: String,
)
