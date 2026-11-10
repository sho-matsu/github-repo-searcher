# CLAUDE.md

このファイルは、Claude Code (claude.ai/code) や Codex などの AI コーディングエージェントがこのリポジトリで作業する際のガイドです。

## プロジェクト概要

GitHub Search API(`GET /search/repositories`)でリポジトリを検索して一覧表示する、単一モジュールの Android アプリ(`:app`、パッケージ `jp.sh.githubreposearcher`)。

### 主要な技術

- **言語 / UI**: Kotlin、Jetpack Compose(Material 3)。画面は Fragment の `ComposeView` 上に Compose で描画する
- **DI**: Hilt(KSP)
- **ネットワーク**: Retrofit + OkHttp、kotlinx.serialization
- **非同期処理**: Kotlin Coroutines + Flow
- **テスト**: JUnit 4、kotlinx-coroutines-test、手書きの Fake

### アプリケーションフロー

1. `GitHubRepoSearcherApp`: `@HiltAndroidApp` を付けた Application クラス
2. `MainActivity`: `RepoSearchFragment` に初期クエリ(`"Android"`)を渡して表示する
3. `RepoSearchFragment` → `RepoSearchRoute` → `RepoSearchViewModel`: クエリを `SavedStateHandle` から受け取り、`SearchGitHubReposUseCase` 経由で検索する
4. `RetrofitGitHubRepoSearchGateway`: `GitHubApi` を呼び、DTO をドメインモデルに変換する。共通ヘッダーは `GitHubHeadersInterceptor` が付与する

## コマンド

### ビルド

```bash
./gradlew :app:compileDebugKotlin       # コンパイルだけを確認(最速)
./gradlew assembleDebug                 # デバッグ APK をビルド
./gradlew installDebug                  # 接続中の端末/エミュレータにインストール(端末が必要)
```

### テスト

```bash
./gradlew test                          # JVM ユニットテスト (app/src/test)
./gradlew connectedAndroidTest          # インストルメンテッドテスト (app/src/androidTest)、端末が必要

# 単一のテストクラス / メソッドを実行
./gradlew :app:testDebugUnitTest --tests "jp.sh.githubreposearcher.domain.SearchQueryTest"
./gradlew :app:testDebugUnitTest --tests "jp.sh.githubreposearcher.presentation.search.RepoSearchViewModelTest"
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=jp.sh.githubreposearcher.ExampleInstrumentedTest
```

### コード品質

```bash
./gradlew lint                          # Android lint
```

- 変更後は少なくとも `./gradlew test` を通してから完了とする。端末が必要なタスクは、端末が接続されていない場合は実行せずその旨を報告する。

### 動作確認

- 画面の変更は、端末/エミュレータにインストールして確認する。スクリーンショットは `adb exec-out screencap -p > screen.png`、UI 階層は `adb shell uiautomator dump` で取得できる。
- GitHub Search API は未認証だとレート制限が厳しい(検索は 1 分あたり 10 回程度)。動作確認で何度も検索して 403 が返ったら、時間をおいて再試行する。

## Git

- コミットメッセージは英語で書くこと。

## ビルド設定に関する注意

- ツールチェーン: AGP 9.5(Gradle 9.6 wrapper)、Kotlin 2.2、Java 11 source/target。SDK は要件により compileSdk/targetSdk 34、minSdk 26。
- compileSdk 34 のため、AndroidX/Compose は compileSdk 34 で使える最後の系統に固定している(Compose BOM 2024.12.01、activity 1.9、lifecycle 2.8、fragment 1.8、core 1.13)。これより新しい版は compileSdk 35 以上を要求し `checkDebugAarMetadata` で失敗するので、上げるときは compileSdk も一緒に上げること。
- AGP 9 は Kotlin を組み込みでサポートしているため、`org.jetbrains.kotlin.android` プラグインは使っていない。プラグインは `android.application`、`kotlin.compose`、`kotlin.serialization`、`ksp`、`hilt`。Kotlin Android プラグインを再追加しないこと。アノテーション処理は kapt ではなく KSP を使う。
- `compileSdk` は新しい `compileSdk { version = release(34) }` DSL を使用。リリースビルドの最適化は `isMinifyEnabled`/ProGuard ファイルではなく `optimization { enable = true; packageScope = ... }` ブロックで設定する。R8 の keep ルールは `app/src/main/keepRules/` に置く(`proguard-rules.pro` は使わない)。
- 依存関係とプラグインのバージョンはすべて version catalog `gradle/libs.versions.toml` で管理し、`libs.*` エイリアスで参照する。Compose ライブラリのバージョンは Compose BOM で決まる。
- `settings.gradle.kts` で `RepositoriesMode.FAIL_ON_PROJECT_REPOS` を設定している。リポジトリはモジュールのビルドファイルではなく、ここで宣言すること。
- Gradle の configuration cache が有効(`gradle.properties`)。新しく書くビルドロジックは configuration cache に対応させること。

## コーディングガイドライン

ここに書くのは**目指す**設計である。新しいコードは必ず従い、従っていない既存コードは手を入れるタイミングで移行する(移行は小さな単位に分ける)。形式的な作法よりも、シンプルで Kotlin らしいコードを優先する。パターンは実際の問題を解決するときにだけ使い、チェックリストを埋めるために使わない。

### アーキテクチャ(Clean Architecture)

`jp.sh.githubreposearcher` 配下のパッケージ構成(当面は単一モジュール。規模が大きくなったら同じ境界で Gradle モジュールに分割する):

```
domain/          # エンティティ、値オブジェクト、ドメインサービス、ポート(インターフェース)、ドメインエラー。純粋な Kotlin。
application/     # ユースケース(アプリケーションサービス)。ドメインとポートを組み合わせて処理する。純粋な Kotlin。
infrastructure/  # アダプター: GitHub API クライアント、DTO、マッパー、DB、ポートの実装。
presentation/    # Compose の画面、ViewModel、UiState、ナビゲーション(ui/theme もここに置く)。
di/              # コンポジションルート: インターフェースと実装を結び付ける Hilt module。
```

- **依存関係のルール**: 依存は内側への一方向のみ — `presentation → application → domain ← infrastructure`。`domain` と `application` では `android.*`、`androidx.*`、Compose、Retrofit/Ktor、シリアライゼーション、DB のアノテーションを import しない。
- **ポートとアダプター**: domain/application が必要とするインターフェース(検索 Gateway、Repository など)は `domain` に宣言し(Separated Interface)、`infrastructure` で実装する。
- **コンポジションルート**: 具体的な実装を知っているのは `di/`(と `MainActivity`/`Application`)だけ。すべてコンストラクタインジェクションで依存を受け取り、ビジネスロジックでサービスロケーターや static な参照は使わない。DI には Hilt を使う(後述の「依存性注入(Hilt)」を参照)。
- インフラ層の型を境界の外に漏らさない。API の DTO や DB エンティティは `infrastructure` でドメインモデルに変換し、ViewModel はドメインモデルを UI モデルに変換する。

### DDD(戦術的設計)

- **ユビキタス言語**: 型にはドメインの概念に沿った名前を付ける(repository、owner、star、language、query など)。名前の衝突に注意: *GitHub のリポジトリ*はドメインエンティティ `GitHubRepo` とし、型名の接尾辞 `Repository` は DDD/PoEAA の Repository パターン専用とする。読み取り専用のリモート検索ポートは Gateway として扱う(例: `GitHubRepoSearchGateway`)。
- **値オブジェクト**: ルールや意味を持つプリミティブはラップする(`RepoId`、`OwnerName`、`SearchQuery`、`StarCount`、`PageNumber`)。単一の値なら `@JvmInline value class`、複数の値をまとめるなら `data class` を使う。不変条件は `init { require(...) }` か、結果型を返すスマートコンストラクタで検証し、不正なインスタンスを作れないようにする。必ずイミュータブルにする。
- **エンティティ**: 同一性は識別子(`id`)で判断し、等価性も id で比較する。実際のロジックがあるなら、"Manager"/"Helper" クラスではなくエンティティ自身に振る舞いを持たせる(ドメインモデル貧血症を避ける)。
- **集約**: 状態の変更は集約ルートを通してのみ行う。Repository はテーブル単位ではなく集約ルート単位で作る。
- **ドメインサービス**: 単一のエンティティや値オブジェクトに属さない、状態を持たないロジックを置く。
- **ドメインエラー**: 想定内の失敗は、例外や HTTP ステータスコードではなく `sealed interface` で表す(例: `SearchError.RateLimited`、`SearchError.Network`、`SearchError.InvalidQuery`)。

### Kotlin における SOLID

- **SRP(単一責任)**: クラスを変更する理由は一つだけにする。ユースケースは 1 クラスにつき `operator fun invoke(...)` を 1 つだけ持つ。ViewModel が扱うのは UI 状態のロジックのみで、HTTP/JSON/SQL は扱わない。
- **OCP(開放閉鎖)**: `if/else` の連鎖を書き換えるのではなく、`sealed` 階層と網羅的な `when`、Strategy、Decorator で拡張する。クラスは `final` のまま(Kotlin のデフォルト)にし、拡張のために `open` を付けず合成を使う。
- **LSP(リスコフの置換)**: ポートの実装はその契約を守る(エラーの意味を揃える、想定外の例外を投げない、事前条件を厳しくしない)。テスト用の Fake も本物の実装と同じように振る舞わせる。
- **ISP(インターフェース分離)**: 役割ごとの小さなインターフェースにする(メソッドが 1 つなら `fun interface`)。使わないメソッドに依存させない。
- **DIP(依存性逆転)**: 上位のコードは自分が所有する抽象(`domain` のポート)に依存する。`CoroutineDispatcher`、時計(`kotlin.time.Clock`/`TimeSource`)、ID 生成器は static に呼ばず注入する。

### エンタープライズアプリケーションパターン(PoEAA)

- **Service Layer** → `application` のユースケース。トランザクションや整合性の境界を決め、presentation からビジネスロジックへの唯一の入り口となる。
- **Repository** → 集約にコレクションのようにアクセスする仕組み。インターフェースは `domain`、実装は `infrastructure` に置く。
- **Gateway** → 外部システム(GitHub REST API)をドメインに合わせた形のインターフェースで包む。
- **Data Mapper** → `infrastructure` に明示的な変換関数を置く(`fun RepoDto.toDomain(): GitHubRepo`)。ドメインモデルにはシリアライゼーションや永続化のアノテーションを付けない。
- **Data Transfer Object** → `@Serializable` な DTO は `infrastructure` の中だけに置き、通信フォーマットをそのまま表す。上位の層には公開しない。
- **Special Case** → 「結果なし」「不明」は `null` やマジックナンバーではなく、sealed のサブタイプや明示的な空状態で表す。
- **Unit of Work** → ローカル DB を追加した場合、複数の書き込みは 1 つのトランザクション(例: Room の `withTransaction`)でまとめ、ユースケースから呼ぶ Repository がそれを管理する。

### GoF パターン — Kotlin での書き方

| パターン | Kotlin での書き方 |
|---|---|
| Strategy | `fun interface` または関数型をコンストラクタで注入する(例: 並び順、リトライ方針) |
| Factory Method / Static Factory | `companion object { fun of(...) }`、または結果型を返すスマートコンストラクタ。検証が必要なら `private constructor` で隠す |
| Builder | 名前付き引数・デフォルト引数と `copy()` で代用し、手書きの Builder は作らない。DSL ビルダーは本当に入れ子の設定が必要な場合のみ |
| Adapter | ドメインのポートを実装する infrastructure のクラス、および変換用の拡張関数 |
| Decorator | ポートの実装を包んでキャッシュ・ログ・リトライを追加する(`class CachingSearchGateway(private val delegate: GitHubRepoSearchGateway) : GitHubRepoSearchGateway by delegate`) |
| Facade | ユースケースが複数のポートをまとめたシンプルな API を提供する |
| Observer | `Flow` / `StateFlow` / `SharedFlow` を使い、独自のリスナーリストは作らない |
| State | `sealed interface` で UI やドメインの状態を表し、網羅的な `when` で分岐する |
| Command | `sealed interface` で UI イベント/インテントを表し、ViewModel が処理する(`onEvent(event)`) |
| Singleton | `object` は状態を持たないものに限る。状態や依存を持つものはグローバルにせず、Hilt でスコープを管理する(`@Singleton`) |
| Template Method | 継承でバリエーションを作らず、合成と高階関数を使う |

### 依存性注入(Hilt)

- DI フレームワークは Hilt を使う(KSP で処理する。kapt は使わない)。
- エントリポイント: `Application` クラスに `@HiltAndroidApp`、`MainActivity` に `@AndroidEntryPoint`、ViewModel に `@HiltViewModel` + `@Inject constructor` を付ける。ViewModel は `XxxRoute` コンポーザブルの中で `hiltViewModel()` を使って取得し、状態を持たない `XxxScreen` には ViewModel を渡さない。
- `domain` / `application` で使ってよいのは、コンストラクタに付ける JSR-330 の `@Inject`(`javax.inject`)だけ。Hilt/Dagger 独自のアノテーション(`@Module`、`@InstallIn`、`@HiltViewModel`、`@AndroidEntryPoint` など)は、`di/`、`presentation/`、`Application`/`Activity` クラスでのみ使う。
- module は `di/` に置き、関心ごとに分ける(`NetworkModule`、`GatewayModule`、`DispatcherModule` など)。インターフェースと実装は `abstract` な module で `@Binds` を使って結び付ける。`@Provides` は自分でアノテーションを付けられない型(Retrofit/OkHttp/Ktor、`Json`、Room DB、Dispatcher)に限る。
- 適切な範囲で最も狭いコンポーネントに install する(`SingletonComponent`、`ViewModelComponent` など)。基本はスコープなしとし、`@Singleton` は共有が必要なものや生成コストが高いもの(HTTP クライアント、DB、メモリキャッシュ)にだけ付ける。
- 同じ型を区別するときは修飾子を使う: `CoroutineDispatcher` には `@IoDispatcher` / `@DefaultDispatcher`、異なるベース URL には独自の `@Qualifier` アノテーションなど。
- フィールドインジェクション(`@Inject lateinit var`)は Android のエントリポイント以外で使わない。ビジネスロジックから `EntryPointAccessors` を呼ばない。

### Kotlin のスタイル

- Kotlin 公式のコーディング規約に従う(`kotlin.code.style=official`)。
- イミュータブルを優先する: `val`、API には読み取り専用のコレクション型(`List`、`Map`)を使い、状態の変更は `data class` + `copy()` で行う。理由なく公開 API に `var` を置かない。
- Null 安全: 本番コードで `!!` を使わない。`?.`、`?:`、メッセージ付きの `requireNotNull`/`checkNotNull` を使うか、値がないことを型で明示的に表す。
- サブタイプごとに持つデータが異なる場合は、`sealed class` や enum より `sealed interface` を優先する。sealed 型に対する `when` は網羅的に書く(`else` 分岐を置かない)。
- 可視性: 動く範囲で最も狭くする — `private` > `internal` > `public`。`infrastructure` の実装クラスは `internal` にする。
- 拡張関数は変換処理や小さなヘルパーに使い、本来その型に属すべきビジネスロジックには使わない。スコープ関数(`let`、`apply`、`also`、`run`)は控えめに使い、入れ子にしない。
- マジックナンバー/マジックストリングを使わない。名前付き定数か値オブジェクトにする。
- 関数: 小さく保ち、抽象度を揃え、1 行で書けるものは式本体にする。Boolean には述語の名前を付ける(`isPrivate`、`hasNextPage`)。

### コメント

- コードを言い換えるだけの WHAT コメントは書かないか、1 行以内に留める。冗長ならコードで語る(命名や関数抽出で表現する)。
- 意図(WHY)を説明するコメントは複数行でもよい(なぜこの順序で処理するか、なぜこの制約があるか、なぜ一見自然な書き方を避けたか など)。
- 判断基準: 「このコメントを削ったら設計意図が失われるか?」が Yes なら残す。

### エラー処理

- 想定内の失敗 → ポートとユースケースの境界では型付きの結果(`Result` 風の sealed 型、またはドメインエラーに変換した `kotlin.Result`)で返す。例外はプログラミングエラーや本当に想定外の状況にだけ使う。
- 例外は infrastructure の境界で捕まえ、そこでドメインエラーに変換する。例外を黙って握りつぶさない。
- `CancellationException` を捕まえたら必ず再送出する(コルーチン内での `catch (e: Exception)` や `runCatching` に注意)。

### コルーチンと Flow

- 構造化された並行性のみを使う: ViewModel では `viewModelScope`、ユースケースやポートでは `suspend` 関数。`GlobalScope` は使わず、`runBlocking` はテスト以外で使わない。
- ポートは、1 回きりの呼び出しには `suspend fun`、ストリームには `Flow` を公開する。Dispatcher の切り替え(`withContext(ioDispatcher)`)は、注入された Dispatcher を使って infrastructure が行う。呼び出し側からはメインスレッドから安全に呼べるようにする。
- UI 状態: `StateFlow<UiState>`(内部は private な `MutableStateFlow`、外部には読み取り専用で公開)。一度きりのイベントは、状態として表せない場合に限り `Channel`/`SharedFlow` を使う。
- 検索リクエストの間引きやキャンセルは、Job を手動で管理せず Flow のオペレーター(`debounce`、`distinctUntilChanged`、`flatMapLatest`)で行う。

### プレゼンテーション(Compose)

- 単方向データフロー: `UiState` は下へ、`UiEvent` は上へ流す。状態は ViewModel が持ち、コンポーザブルは状態を持たず、状態とラムダを受け取る(状態のホイスティング)。
- 各画面は、ViewModel を取得して `collectAsStateWithLifecycle()` で状態を収集する `XxxRoute` と、状態を持たずプレビュー可能な `XxxScreen(state, onEvent)` に分ける。
- コンポーザブルにビジネスロジックやデータ変換を書かない。`Modifier` は最初の省略可能な引数として受け取る。画面と単純でないコンポーネントには `@Preview` を用意する。
- ユーザーに表示する文字列は `strings.xml` に置き、UI テキストをハードコードしない。

### テスト

- `domain` と `application`: Android に依存しない、高速で決定的な JVM ユニットテスト(`app/src/test`)で確認する。
- モックフレームワークより、ポートの手書き Fake を優先する。コルーチンのコードは `kotlinx-coroutines-test`(`runTest`、注入した `TestDispatcher`)でテストする。
- ViewModel は、Fake のユースケース/ポートを渡し、公開されている `StateFlow` を通してテストする。変換処理や値オブジェクトの検証は、不正な入力も含めて明示的にテストする。
- JVM ユニットテストでは Hilt を使わず、テスト対象のクラスを Fake を使って直接生成する。インストルメンテッドテストでは `@HiltAndroidTest` + `HiltAndroidRule` を使い、`@TestInstallIn`(テストごとなら `@UninstallModules`)で本番の module を Fake に置き換える。
- テスト名は振る舞いを説明する: `` `API が rate-limit ヘッダー付きの 403 を返したとき RateLimited を返す` ``。

## コードレビューの観点

レビュー(人・AI とも)の目的は、不具合になりそうな箇所を減らし、複数人で読み書きしやすいコードにすること。次の観点で確認する。

- 意図が明確か(命名、型、WHY コメント)
- 冗長さがなく読みやすいか
- 既存のコードやこのガイドラインと一貫しているか
- 副作用が局所化されているか(状態の変更、I/O、コルーチンのキャンセル)
- 振る舞いの変更にテストが伴っているか
