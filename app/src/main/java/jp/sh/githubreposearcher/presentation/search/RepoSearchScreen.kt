package jp.sh.githubreposearcher.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.sh.githubreposearcher.R
import jp.sh.githubreposearcher.presentation.theme.GitHubRepoSearcherTheme

@Composable
fun RepoSearchRoute(
    modifier: Modifier = Modifier,
    viewModel: RepoSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RepoSearchScreen(state = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoSearchScreen(
    state: RepoSearchUiState,
    onEvent: (RepoSearchEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.repo_search_title)) }) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Text(
                text = stringResource(R.string.repo_search_query_label, state.query),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            HorizontalDivider()
            RepoSearchContentView(
                content = state.content,
                onRetry = { onEvent(RepoSearchEvent.Retry) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RepoSearchContentView(
    content: RepoSearchContent,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (content) {
        RepoSearchContent.Loading -> CenteredBox(modifier) { CircularProgressIndicator() }
        RepoSearchContent.Empty -> CenteredBox(modifier) { Text(stringResource(R.string.repo_search_empty)) }
        is RepoSearchContent.Loaded -> RepoList(repos = content.repos, modifier = modifier)
        is RepoSearchContent.Error -> ErrorView(error = content, onRetry = onRetry, modifier = modifier)
    }
}

@Composable
private fun RepoList(repos: List<RepoItemUiModel>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = repos, key = { it.id }) { repo ->
            Text(
                text = repo.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
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
                RepoItemUiModel(1, "architecture-samples"),
                RepoItemUiModel(2, "nowinandroid"),
                RepoItemUiModel(3, "compose-samples"),
            ),
        ),
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
        RepoSearchScreen(state = RepoSearchUiState(query = "Android", content = content), onEvent = {})
    }
}
