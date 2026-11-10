package jp.sh.githubreposearcher.domain

/** GitHub 上のリポジトリ。同一性は [id] で判断する。 */
class GitHubRepo(
    val id: RepoId,
    val name: RepoName,
) {
    override fun equals(other: Any?): Boolean = other is GitHubRepo && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "GitHubRepo(id=$id, name=$name)"
}

@JvmInline
value class RepoId(val value: Long) {
    init {
        require(value > 0) { "RepoId must be positive: $value" }
    }
}

@JvmInline
value class RepoName(val value: String) {
    init {
        require(value.isNotBlank()) { "RepoName must not be blank" }
    }
}
