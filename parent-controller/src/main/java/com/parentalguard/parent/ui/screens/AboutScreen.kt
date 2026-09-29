package com.parentalguard.parent.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parentalguard.parent.R
import com.parentalguard.parent.ui.aura.auraEnter
import com.parentalguard.parent.ui.currentAppVersion
import com.parentalguard.parent.ui.neumorphic.NeumorphicBackground
import com.parentalguard.parent.ui.neumorphic.NeumorphicButton
import com.parentalguard.parent.ui.neumorphic.NeumorphicCard
import com.parentalguard.parent.ui.neumorphic.NeumorphicIconTile
import com.parentalguard.parent.ui.neumorphic.Nm
import com.parentalguard.parent.ui.openUrl
import com.parentalguard.parent.ui.theme.MonoFontFamily
import com.parentalguard.parent.ui.theme.trackingFor

private const val GITHUB_PROFILE_URL = "https://github.com/H-Ossama"
private const val GITHUB_REPO_URL = "https://github.com/H-Ossama/Family-Guard"

@Composable
fun AboutScreen(
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
                            text = stringResource(R.string.about_title),
                            color = Nm.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.about_desc),
                            color = Nm.onSurfaceMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    NeumorphicIconTile(
                        icon = Icons.Outlined.Info,
                        tint = Nm.primary,
                        contentDescription = stringResource(R.string.about_title)
                    )
                }
            }

            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth().auraEnter(1),
                    padding = 24.dp,
                    corner = 28.dp,
                    backgroundColor = Nm.primary.copy(alpha = 0.07f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NeumorphicIconTile(
                            icon = Icons.Outlined.Shield,
                            tint = Nm.primary,
                            size = 76.dp,
                            iconSize = 34.dp
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            color = Nm.onSurface,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.padding(vertical = 2.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.app_version, currentAppVersion(context)).uppercase(),
                                color = Nm.primary,
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.labelMedium.fontSize
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.about_tagline),
                            color = Nm.onSurfaceMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth().auraEnter(2),
                    padding = 20.dp,
                    corner = 24.dp
                ) {
                    SectionLabel(R.string.about_screen_title)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.about_screen_desc),
                        color = Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth().auraEnter(3),
                    padding = 20.dp,
                    corner = 24.dp
                ) {
                    SectionLabel(R.string.label_features)
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        listOf(
                            R.string.feature_view_apps,
                            R.string.feature_monitor_usage,
                            R.string.feature_time_limits,
                            R.string.feature_block_apps,
                            R.string.daily_report
                        ).forEach { featureRes ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = Nm.success,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    stringResource(featureRes),
                                    color = Nm.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth().auraEnter(4),
                    padding = 20.dp,
                    corner = 24.dp
                ) {
                    SectionLabel(R.string.about_developer_title)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeumorphicIconTile(
                            icon = Icons.Outlined.AccountCircle,
                            tint = Nm.primary,
                            size = 48.dp,
                            iconSize = 24.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.about_developer_name),
                                color = Nm.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.about_developer_role),
                                color = Nm.onSurfaceMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeumorphicButton(
                            text = stringResource(R.string.about_github_profile),
                            onClick = { openUrl(context, GITHUB_PROFILE_URL) },
                            icon = Icons.Outlined.Link,
                            modifier = Modifier.fillMaxWidth()
                        )
                        NeumorphicButton(
                            text = stringResource(R.string.about_github_repo),
                            onClick = { openUrl(context, GITHUB_REPO_URL) },
                            icon = Icons.Outlined.Code,
                            inset = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth().auraEnter(5),
                    padding = 20.dp,
                    corner = 24.dp
                ) {
                    SectionLabel(R.string.about_legal_title)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.about_legal_desc),
                        color = Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(14.dp))
                    NeumorphicButton(
                        text = stringResource(R.string.about_github_repo),
                        onClick = { openUrl(context, GITHUB_REPO_URL) },
                        icon = Icons.Default.Code,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().auraEnter(6).padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.about_made_with),
                        color = Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(resId: Int) {
    Text(
        text = stringResource(resId).uppercase(),
        color = Nm.primary,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = trackingFor(MaterialTheme.typography.labelSmall.letterSpacing)
    )
}
