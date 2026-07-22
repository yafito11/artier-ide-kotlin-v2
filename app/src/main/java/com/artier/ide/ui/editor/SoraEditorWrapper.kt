package com.artier.ide.ui.editor

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.artier.ide.data.remote.LspClient
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.delay

@SuppressLint("ClickableViewAccessibility")
@Suppress("FunctionParameterNaming")
@Composable
fun SoraEditorWrapper(
    content: String,
    language: String = "textmate",
    filePath: String? = null,
    modifier: Modifier = Modifier,
    onContentChanged: (String) -> Unit = {},
    onCursorChanged: (p1: Int, p2: Int) -> Unit = { _, _ -> },
    onCompletionRequested: (line: Int, column: Int) -> Unit = { _, _ -> },
    completionItems: List<LspClient.CompletionItem> = emptyList(),
    onCompletionSelected: (LspClient.CompletionItem) -> Unit = {},
    onClearCompletions: () -> Unit = {},
    readOnly: Boolean = false
) {
    val context = LocalContext.current

    val lastCursorState = remember { mutableStateOf(0 to 0) }
    val contentCb = remember { mutableStateOf(onContentChanged) }
    contentCb.value = onContentChanged
    val cursorCb = remember { mutableStateOf(onCursorChanged) }
    cursorCb.value = onCursorChanged

    val editor = remember {
        CodeEditor(context).apply {
            isEditable = !readOnly
            // Configure editor settings
            setLineSpacing(0f, 1.1f)
            setTextSize(14f)
            // Use default color scheme
            
            // Subscribe to events
            subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                contentCb.value(text.toString())
            }
            subscribeEvent(SelectionChangeEvent::class.java) { event, _ ->
                val line = event.left.line
                val column = event.left.column
                lastCursorState.value = line to column
                cursorCb.value(line, column)
            }
        }
    }

    DisposableEffect(language) {
        val scopeName = LanguageMapper.getLanguageForFile("file.$language")
        try {
            val textMateLanguage = TextMateLanguage.create(scopeName, true)
            editor.setEditorLanguage(textMateLanguage)
        } catch (_: Exception) {
            // Grammar may be missing
        }
        onDispose { }
    }

    LaunchedEffect(lastCursorState.value, content, filePath) {
        delay(500)
        val (line, col) = lastCursorState.value
        if (content.isNotEmpty() && filePath != null) {
            onCompletionRequested(line, col)
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { editor },
            modifier = Modifier.fillMaxSize(),
            update = { ed ->
                val currentText = ed.text.toString()
                if (currentText != content) {
                    ed.setText(content)
                }
            }
        )

        if (completionItems.isNotEmpty()) {
            CompletionPopup(
                items = completionItems,
                onSelect = { item ->
                    onCompletionSelected(item)
                    onClearCompletions()
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .fillMaxWidth(0.6f)
                    .heightIn(max = 200.dp)
            )
        }
    }
}

@Composable
private fun CompletionPopup(
    items: List<LspClient.CompletionItem>,
    onSelect: (LspClient.CompletionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .padding(4.dp)
    ) {
        LazyColumn {
            items(items.take(20)) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val detail = item.detail
                    if (!detail.isNullOrBlank()) {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

object LanguageMapper {
    fun getLanguageForFile(fileName: String): String {
        val extension = fileName.substringAfterLast(".").lowercase()
        return when (extension) {
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "py" -> "python"
            "js" -> "javascript"
            "ts" -> "typescript"
            "tsx" -> "typescriptreact"
            "jsx" -> "javascriptreact"
            "html" -> "html"
            "css" -> "css"
            "scss" -> "scss"
            "json" -> "json"
            "xml" -> "xml"
            "yaml", "yml" -> "yaml"
            "md" -> "markdown"
            "sql" -> "sql"
            "sh", "bash" -> "shellscript"
            "rs" -> "rust"
            "go" -> "go"
            "c", "h" -> "c"
            "cpp", "hpp" -> "cpp"
            "cs" -> "csharp"
            "rb" -> "ruby"
            "php" -> "php"
            "swift" -> "swift"
            "dart" -> "dart"
            else -> "text"
        }
    }

    fun getLanguageDisplayName(language: String): String {
        return when (language) {
            "kotlin" -> "Kotlin"
            "java" -> "Java"
            "python" -> "Python"
            "javascript" -> "JavaScript"
            "typescript" -> "TypeScript"
            "typescriptreact" -> "TypeScript React"
            "javascriptreact" -> "JavaScript React"
            "html" -> "HTML"
            "css" -> "CSS"
            "scss" -> "SCSS"
            "json" -> "JSON"
            "xml" -> "XML"
            "yaml" -> "YAML"
            "markdown" -> "Markdown"
            "sql" -> "SQL"
            "shellscript" -> "Shell"
            "rust" -> "Rust"
            "go" -> "Go"
            "c" -> "C"
            "cpp" -> "C++"
            "csharp" -> "C#"
            "ruby" -> "Ruby"
            "php" -> "PHP"
            "swift" -> "Swift"
            "dart" -> "Dart"
            else -> "Plain Text"
        }
    }
}
