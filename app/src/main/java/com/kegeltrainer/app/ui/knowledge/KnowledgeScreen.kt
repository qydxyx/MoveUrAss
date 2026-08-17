package com.kegeltrainer.app.ui.knowledge

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.kegeltrainer.app.domain.knowledge.KnowledgeArticles
import com.kegeltrainer.app.domain.model.KnowledgeArticle
import com.kegeltrainer.app.ui.components.AppCard
import com.kegeltrainer.app.ui.components.ScreenColumn

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Composable
fun KnowledgeScreen(onOpen: (String) -> Unit, onBack: () -> Unit) {
    ScreenColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("知识库", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("帮助你练对，而不是练猛。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        KnowledgeArticles.all.forEach { article ->
            AppCard(onClick = { onOpen(article.id) }, modifier = Modifier.padding(bottom = 10.dp)) {
                Text(article.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(article.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
        TextButton(onClick = onBack) { Text("返回", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@HiltViewModel
class ArticleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val article: KnowledgeArticle = KnowledgeArticles.byId(checkNotNull(savedStateHandle["articleId"]))
}

@Composable
fun ArticleScreen(
    onBack: () -> Unit,
    vm: ArticleViewModel = hiltViewModel(),
) {
    val article = vm.article
    ScreenColumn(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(article.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(article.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Text(article.body, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onBack) { Text("返回", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
