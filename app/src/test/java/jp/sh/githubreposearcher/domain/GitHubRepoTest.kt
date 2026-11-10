package jp.sh.githubreposearcher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GitHubRepoTest {
    @Test
    fun `id が同じなら名前が違っても等しい`() {
        assertEquals(GitHubRepo(RepoId(1), RepoName("a")), GitHubRepo(RepoId(1), RepoName("b")))
    }

    @Test
    fun `id が違えば等しくない`() {
        assertNotEquals(GitHubRepo(RepoId(1), RepoName("a")), GitHubRepo(RepoId(2), RepoName("a")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `0 以下の RepoId は生成できない`() {
        RepoId(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `空白の RepoName は生成できない`() {
        RepoName(" ")
    }
}
