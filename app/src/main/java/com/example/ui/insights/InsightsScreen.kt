package com.example.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TimeRangeFilter
import com.example.ui.common.ErrorBanner
import com.example.ui.common.OnResume
import com.example.ui.common.PermissionGate
import com.example.ui.common.SectionCard
import com.example.ui.theme.LocalGoogleColors
import java.text.DateFormat
import java.util.Date

@Composable
fun InsightsScreen(viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val latest by viewModel.latest.collectAsStateWithLifecycle()
    val colors = LocalGoogleColors.current

    OnResume { viewModel.recheck() }

    PermissionGate(state.hasPermission, onRecheck = viewModel::recheck) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "intro") {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = colors.uiBlue)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "AI usage insights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.darkGray
                        )
                    }
                    Text(
                        "Gemini analyzes your totals and top apps to explain where your data goes and how to save it. " +
                            "Only app names and byte counts are sent — never your browsing or content.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.mediumGray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (!state.isConfigured) {
                        Text(
                            "AI insights aren't configured in this build. Add google-services.json for a Firebase " +
                                "project with Firebase AI Logic enabled, then rebuild.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.red,
                            modifier = Modifier.padding(top = 8.dp).testTag("insights_not_configured")
                        )
                        return@SectionCard
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(TimeRangeFilter.TODAY, TimeRangeFilter.LAST_7_DAYS, TimeRangeFilter.BILLING_CYCLE).forEach { r ->
                            FilterChip(
                                selected = state.range == r,
                                onClick = { viewModel.setRange(r) },
                                label = { Text(r.label) }
                            )
                        }
                    }
                    Button(
                        onClick = viewModel::generate,
                        enabled = !state.isGenerating,
                        modifier = Modifier.padding(top = 8.dp).testTag("generate_insights")
                    ) {
                        if (state.isGenerating) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Analyzing…")
                        } else {
                            Text(if (latest == null) "Generate insights" else "Regenerate")
                        }
                    }
                }
            }

            state.error?.let { item(key = "error") { ErrorBanner(it, onRetry = viewModel::generate) } }

            latest?.let { insight ->
                item(key = "result") {
                    SectionCard {
                        Text(
                            "${insight.rangeLabel} · " +
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(insight.createdAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.mediumGray
                        )
                        MarkdownText(insight.text, Modifier.padding(top = 8.dp))
                        Text(
                            "AI-generated. Check important details in your carrier's app.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.mediumGray,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Minimal Markdown renderer for Gemini output: headings, bullets and **bold** spans. */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val colors = LocalGoogleColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        markdown.lines().forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() -> Spacer(Modifier.size(4.dp))
                line.startsWith("#") -> Text(
                    inlineBold(line.trimStart('#').trim()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.darkGray,
                    modifier = Modifier.padding(top = 6.dp)
                )
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> Row {
                    Text("•  ", style = MaterialTheme.typography.bodyMedium, color = colors.uiBlue)
                    Text(inlineBold(line.trimStart().drop(2)), style = MaterialTheme.typography.bodyMedium, color = colors.darkGray)
                }
                else -> Text(inlineBold(line), style = MaterialTheme.typography.bodyMedium, color = colors.darkGray)
            }
        }
    }
}

fun inlineBold(text: String): AnnotatedString = buildAnnotatedString {
    val parts = text.split("**")
    parts.forEachIndexed { i, part ->
        if (i % 2 == 1 && i < parts.lastIndex) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
        else append(part)
    }
}
