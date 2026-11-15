package jp.sh.githubreposearcher

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import dagger.hilt.android.AndroidEntryPoint
import jp.sh.githubreposearcher.reposearch.presentation.RepoSearchFragment

@AndroidEntryPoint
class MainActivity : FragmentActivity(R.layout.activity_main) {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, RepoSearchFragment.newInstance(INITIAL_QUERY))
            }
        }
    }

    private companion object {
        const val INITIAL_QUERY = "Android"
    }
}
