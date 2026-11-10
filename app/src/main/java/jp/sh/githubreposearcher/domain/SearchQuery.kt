package jp.sh.githubreposearcher.domain

/** リポジトリ検索のクエリ文字列。前後の空白を除いた空でない文字列のみ許可する。 */
@JvmInline
value class SearchQuery private constructor(val value: String) {
    companion object {
        /** 不正な入力(空文字・空白のみ)の場合は null を返す。 */
        fun ofOrNull(raw: String): SearchQuery? = raw.trim().takeIf { it.isNotEmpty() }?.let(::SearchQuery)
    }
}
