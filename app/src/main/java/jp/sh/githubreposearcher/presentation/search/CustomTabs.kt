package jp.sh.githubreposearcher.presentation.search

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import jp.sh.githubreposearcher.R

/**
 * [url] を Chrome Custom Tabs で開く。Custom Tabs 非対応のブラウザしかない場合は CustomTabsIntent が
 * 通常の ACTION_VIEW として起動してくれるが、ブラウザ自体が無い端末では起動できないので、その旨を通知する。
 */
internal fun Context.openInCustomTab(url: String) {
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(this, Uri.parse(url))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.repo_open_page_failed, Toast.LENGTH_SHORT).show()
    }
}
