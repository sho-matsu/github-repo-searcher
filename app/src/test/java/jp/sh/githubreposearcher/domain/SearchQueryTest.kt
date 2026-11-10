package jp.sh.githubreposearcher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchQueryTest {
    @Test
    fun `前後の空白を除いた文字列で生成される`() {
        assertEquals("Android", SearchQuery.ofOrNull("  Android \n")?.value)
    }

    @Test
    fun `空文字のとき null を返す`() {
        assertNull(SearchQuery.ofOrNull(""))
    }

    @Test
    fun `空白のみのとき null を返す`() {
        assertNull(SearchQuery.ofOrNull("   "))
    }
}
