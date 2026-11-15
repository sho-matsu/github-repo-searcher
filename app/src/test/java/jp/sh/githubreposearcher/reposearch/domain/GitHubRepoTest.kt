package jp.sh.githubreposearcher.reposearch.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GitHubRepoTest {
    private fun url(name: String) = RepoPageUrl("https://github.com/owner/$name")

    @Test
    fun `id が同じなら名前が違っても等しい`() {
        assertEquals(GitHubRepo(RepoId(1), RepoName("a"), url("a")), GitHubRepo(RepoId(1), RepoName("b"), url("b")))
    }

    @Test
    fun `id が違えば等しくない`() {
        assertNotEquals(GitHubRepo(RepoId(1), RepoName("a"), url("a")), GitHubRepo(RepoId(2), RepoName("a"), url("a")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `0 以下の RepoId は生成できない`() {
        RepoId(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `空白の RepoName は生成できない`() {
        RepoName(" ")
    }

    @Test
    fun `https の URL から RepoPageUrl を生成できる`() {
        assertEquals("https://github.com/android/nowinandroid", RepoPageUrl("https://github.com/android/nowinandroid").value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `http の URL から RepoPageUrl は生成できない`() {
        RepoPageUrl("http://github.com/android/nowinandroid")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `スキームだけの RepoPageUrl は生成できない`() {
        RepoPageUrl("https://")
    }
}
