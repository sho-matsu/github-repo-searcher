package jp.sh.githubreposearcher.reposearch.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import jp.sh.githubreposearcher.core.ui.theme.GitHubRepoSearcherTheme

/** GitHub のリポジトリを検索し、結果を表示する Fragment。引数のクエリは初期表示時の検索に使う。 */
@AndroidEntryPoint
class RepoSearchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            GitHubRepoSearcherTheme {
                RepoSearchRoute()
            }
        }
    }

    companion object {
        fun newInstance(query: String): RepoSearchFragment = RepoSearchFragment().apply {
            arguments = bundleOf(RepoSearchViewModel.ARG_QUERY to query)
        }
    }
}
