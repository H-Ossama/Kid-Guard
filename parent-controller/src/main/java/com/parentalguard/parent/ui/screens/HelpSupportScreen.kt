package com.parentalguard.parent.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parentalguard.parent.R
import com.parentalguard.parent.ui.aura.auraEnter
import com.parentalguard.parent.ui.neumorphic.NeumorphicBackground
import com.parentalguard.parent.ui.neumorphic.NeumorphicButton
import com.parentalguard.parent.ui.neumorphic.NeumorphicCard
import com.parentalguard.parent.ui.neumorphic.NeumorphicIconTile
import com.parentalguard.parent.ui.neumorphic.Nm
import com.parentalguard.parent.ui.openUrl

private const val GITHUB_ISSUES_URL = "https://github.com/H-Ossama/Family-Guard/issues"
private const val GITHUB_REPO_URL = "https://github.com/H-Ossama/Family-Guard"

@Composable
fun HelpSupportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NeumorphicBackground {
        LazyColumn(
            modifier = modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().auraEnter(0),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeumorphicIconTile(
                        icon = Icons.Default.ArrowBack,
                        onClick = onBack,
                        contentDescription = stringResource(R.string.back)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.help_title),
                            color = Nm.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.help_desc),
                            color = Nm.onSurfaceMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    NeumorphicIconTile(
                        icon = Icons.Outlined.Help,
                        tint = Nm.primary,
                        contentDescription = stringResource(R.string.help_title)
                    )
                }
            }

            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth().auraEnter(1),
                    padding = 18.dp,
                    corner = 24.dp,
                    backgroundColor = Nm.primary.copy(alpha = 0.07f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeumorphicIconTile(
                            icon = Icons.Outlined.Lightbulb,
                            tint = Nm.warning,
                            size = 42.dp,
                            iconSize = 20.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.help_intro),
                            color = Nm.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.help_questions_title).uppercase(),
                    color = Nm.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp)
                )
            }

            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth().auraEnter(2),
                    padding = 0.dp,
                    corner = 24.dp
                ) {
                    Column {
                        faqItems.forEachIndexed { index, faq ->
                            if (index > 0) HelpDivider()
                            FaqItem(
                                question = stringResource(faq.questionRes),
                                answer = stringResource(faq.answerRes)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.help_troubleshooting_title).uppercase(),
                    color = Nm.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp)
                )
            }

            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth().auraEnter(3),
                    padding = 18.dp,
                    corner = 24.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Refresh, null, tint = Nm.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.help_troubleshooting_hint),
                                color = Nm.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        troubleshootingItems.forEach { tipRes ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(5.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Nm.primary)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = stringResource(tipRes),
                                    color = Nm.onSurfaceMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.help_contact_title).uppercase(),
                    color = Nm.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp)
                )
            }

            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth().auraEnter(4),
                    padding = 18.dp,
                    corner = 24.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NeumorphicIconTile(
                                icon = Icons.Outlined.Code,
                                tint = Nm.primary,
                                size = 42.dp,
                                iconSize = 20.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.help_contact_desc),
                                color = Nm.onSurfaceMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        NeumorphicButton(
                            text = stringResource(R.string.help_report_issue),
                            onClick = { openUrl(context, GITHUB_ISSUES_URL) },
                            icon = Icons.Default.BugReport,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NeumorphicButton(
                            text = stringResource(R.string.help_view_source),
                            onClick = { openUrl(context, GITHUB_REPO_URL) },
                            icon = Icons.Outlined.Code,
                            inset = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().auraEnter(5).padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.help_questions_more),
                        color = Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private data class FaqEntry(val questionRes: Int, val answerRes: Int)

private val faqItems = listOf(
    FaqEntry(R.string.faq_pair_title, R.string.faq_pair_answer),
    FaqEntry(R.string.faq_offline_title, R.string.faq_offline_answer),
    FaqEntry(R.string.faq_block_title, R.string.faq_block_answer),
    FaqEntry(R.string.faq_limits_title, R.string.faq_limits_answer),
    FaqEntry(R.string.faq_icon_title, R.string.faq_icon_answer),
    FaqEntry(R.string.faq_do_title, R.string.faq_do_answer),
    FaqEntry(R.string.faq_reports_title, R.string.faq_reports_answer)
)

private val troubleshootingItems = listOf(
    R.string.tip_same_network,
    R.string.tip_child_app_running,
    R.string.tip_bluetooth_pair,
    R.string.tip_reset_pin,
    R.string.tip_refresh_report
)

@Composable
private fun FaqItem(
    question: String,
    answer: String
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = question,
                color = Nm.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = Nm.primary
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = answer,
                    color = Nm.onSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun HelpDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Nm.divider)
    )
}
