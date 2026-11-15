package jp.sh.githubreposearcher.reposearch.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.sh.githubreposearcher.R
import jp.sh.githubreposearcher.core.ui.theme.GitHubRepoSearcherTheme

@Composable
fun RepoSearchRoute(
    modifier: Modifier = Modifier,
    viewModel: RepoSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    RepoSearchScreen(
        state = uiState,
        onEvent = viewModel::onEvent,
        onRepoClick = { repo -> context.openInCustomTab(repo.pageUrl) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoSearchScreen(
    state: RepoSearchUiState,
    onEvent: (RepoSearchEvent) -> Unit,
    onRepoClick: (RepoItemUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.repo_search_title)) }) },
    ) { innerPadding ->
        // edge-to-edge ではキーボード表示時に adjustResize でウィンドウが縮まないため、IME の insets を自分で反映する。
        // Scaffold が付けたシステムバー分の padding を consume してから imePadding を付け、下端の余白が二重にならないようにする。
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            QueryInputBar(
                queryInput = state.queryInput,
                canSearch = state.canSearch,
                canClear = state.canClear,
                onQueryChange = { onEvent(RepoSearchEvent.QueryChanged(it)) },
                onSearch = { onEvent(RepoSearchEvent.Search) },
                onClear = { onEvent(RepoSearchEvent.ClearQuery) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()
            RepoSearchContentView(
                content = state.content,
                onRetry = { onEvent(RepoSearchEvent.Retry) },
                onRepoClick = onRepoClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QueryInputBar(
    queryInput: String,
    canSearch: Boolean,
    canClear: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val submit = {
        if (canSearch) {
            keyboardController?.hide()
            onSearch()
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = queryInput,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.repo_search_query_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (canClear) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.repo_search_clear))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            modifier = Modifier.weight(1f),
        )
        Button(onClick = submit, enabled = canSearch) {
            Text(stringResource(R.string.repo_search_submit))
        }
    }
}

@Composable
private fun RepoSearchContentView(
    content: RepoSearchContent,
    onRetry: () -> Unit,
    onRepoClick: (RepoItemUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (content) {
        RepoSearchContent.Idle -> CenteredBox(modifier) { Text(stringResource(R.string.repo_search_idle)) }
        RepoSearchContent.Loading -> CenteredBox(modifier) { CircularProgressIndicator() }
        RepoSearchContent.Empty -> CenteredBox(modifier) { Text(stringResource(R.string.repo_search_empty)) }
        is RepoSearchContent.Loaded -> RepoList(repos = content.repos, onRepoClick = onRepoClick, modifier = modifier)
        is RepoSearchContent.Error -> ErrorView(error = content, onRetry = onRetry, modifier = modifier)
    }
}

@Composable
private fun RepoList(
    repos: List<RepoItemUiModel>,
    onRepoClick: (RepoItemUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = repos, key = { it.id }) { repo ->
            Text(
                text = repo.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRepoClick(repo) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun ErrorView(error: RepoSearchContent.Error, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CenteredBox(modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(text = stringResource(error.messageResId), textAlign = TextAlign.Center)
            if (error.isRetryable) {
                Button(onClick = onRetry) { Text(stringResource(R.string.repo_search_retry)) }
            }
        }
    }
}

@Composable
private fun CenteredBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

private class RepoSearchContentPreviewProvider : PreviewParameterProvider<RepoSearchContent> {
    override val values = sequenceOf(
        RepoSearchContent.Loaded(
            listOf(
                RepoItemUiModel(1, "architecture-samples", "https://github.com/android/architecture-samples"),
                RepoItemUiModel(2, "nowinandroid", "https://github.com/android/nowinandroid"),
                RepoItemUiModel(3, "compose-samples", "https://github.com/android/compose-samples"),
            ),
        ),
        RepoSearchContent.Idle,
        RepoSearchContent.Loading,
        RepoSearchContent.Empty,
        RepoSearchContent.Error(R.string.repo_search_error_network, isRetryable = true),
    )
}

@Preview(showBackground = true)
@Composable
private fun RepoSearchScreenPreview(
    @PreviewParameter(RepoSearchContentPreviewProvider::class) content: RepoSearchContent,
) {
    GitHubRepoSearcherTheme {
        RepoSearchScreen(
            state = RepoSearchUiState(queryInput = "Android", content = content),
            onEvent = {},
            onRepoClick = {},
        )
    }
}
