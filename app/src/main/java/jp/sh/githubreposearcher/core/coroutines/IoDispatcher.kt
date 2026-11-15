package jp.sh.githubreposearcher.core.coroutines

import javax.inject.Qualifier

/**
 * I/O 用の CoroutineDispatcher を表す修飾子。
 * infrastructure から参照されるため、コンポジションルートである di パッケージではなくここに置く。
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
