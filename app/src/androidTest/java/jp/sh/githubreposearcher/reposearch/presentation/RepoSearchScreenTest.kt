package jp.sh.githubreposearcher.reposearch.presentation

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.sh.githubreposearcher.R
import jp.sh.githubreposearcher.core.ui.theme.GitHubRepoSearcherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// minSdk 26 の DEX ではメソッド名に空白を使えないため、テスト名は空白なしで書く。
@RunWith(AndroidJUnit4::class)
class RepoSearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<RepoSearchEvent>()
    private val clickedRepos = mutableListOf<RepoItemUiModel>()

    @Test
    fun 入力が空白だけのとき検索ボタンは押せない() {
        setScreen(RepoSearchUiState(queryInput = "   "))

        composeRule.onNodeWithText(string(R.string.repo_search_submit)).assertIsNotEnabled()
    }

    @Test
    fun 検索ボタンを押すとSearchイベントを送る() {
        setScreen(RepoSearchUiState(queryInput = "Kotlin"))

        composeRule.onNodeWithText(string(R.string.repo_search_submit)).performClick()

        assertEquals(listOf(RepoSearchEvent.Search), events)
    }

    @Test
    fun クリアボタンを押すとClearQueryイベントを送る() {
        setScreen(RepoSearchUiState(queryInput = "Kotlin"))

        composeRule.onNodeWithContentDescription(string(R.string.repo_search_clear)).performClick()

        assertEquals(listOf(RepoSearchEvent.ClearQuery), events)
    }

    @Test
    fun 再試行できるエラーで再試行ボタンを押すとRetryイベントを送る() {
        setScreen(errorState(R.string.repo_search_error_network, isRetryable = true))

        composeRule.onNodeWithText(string(R.string.repo_search_retry)).performClick()

        assertEquals(listOf(RepoSearchEvent.Retry), events)
    }

    @Test
    fun 再試行できないエラーでは再試行ボタンを表示しない() {
        setScreen(errorState(R.string.repo_search_error_invalid_query, isRetryable = false))

        composeRule.onNodeWithText(string(R.string.repo_search_error_invalid_query)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.repo_search_retry)).assertDoesNotExist()
    }

    @Test
    fun リポジトリをタップするとそのリポジトリをonRepoClickに渡す() {
        val tapped = RepoItemUiModel(2, "nowinandroid", "https://github.com/android/nowinandroid")
        val repos = listOf(RepoItemUiModel(1, "architecture-samples", "https://github.com/android/architecture-samples"), tapped)
        setScreen(RepoSearchUiState(queryInput = "Android", content = RepoSearchContent.Loaded(repos)))

        composeRule.onNodeWithText(tapped.name).performClick()

        assertEquals(listOf(tapped), clickedRepos)
    }

    private fun setScreen(state: RepoSearchUiState) {
        composeRule.setContent {
            GitHubRepoSearcherTheme {
                RepoSearchScreen(state = state, onEvent = { events += it }, onRepoClick = { clickedRepos += it })
            }
        }
    }

    private fun errorState(@StringRes messageResId: Int, isRetryable: Boolean) = RepoSearchUiState(
        queryInput = "Android",
        content = RepoSearchContent.Error(messageResId, isRetryable),
    )

    private fun string(@StringRes resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)
}
