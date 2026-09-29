@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.parentalguard.parent.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parentalguard.parent.R
import com.parentalguard.parent.ui.neumorphic.NeumorphicBackground
import com.parentalguard.parent.ui.neumorphic.NeumorphicButton
import com.parentalguard.parent.ui.neumorphic.NeumorphicIconTile
import com.parentalguard.parent.ui.neumorphic.Nm
import com.parentalguard.parent.ui.neumorphic.neumorphic
import com.parentalguard.parent.ui.theme.DisplayFontFamily
import com.parentalguard.parent.ui.theme.MonoFontFamily
import com.parentalguard.parent.ui.theme.trackingFor
import kotlinx.coroutines.launch

// ============================================================================
// ONBOARDING · short first-run walkthrough for new parents.
// ============================================================================

private data class OnboardingPage(
    val icon: ImageVector,
    val tint: Color,
    val titleRes: Int,
    val bodyRes: Int,
    val pathRes: Int? = null
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Filled.FamilyRestroom,
        tint = Nm.primary,
        titleRes = R.string.onboarding_welcome_title,
        bodyRes = R.string.onboarding_welcome_body
    ),
    OnboardingPage(
        icon = Icons.Outlined.Search,
        tint = Nm.cyan,
        titleRes = R.string.onboarding_discover_title,
        bodyRes = R.string.onboarding_discover_body,
        pathRes = R.string.onboarding_discover_path
    ),
    OnboardingPage(
        icon = Icons.Outlined.Share,
        tint = Nm.violet,
        titleRes = R.string.onboarding_install_title,
        bodyRes = R.string.onboarding_install_body,
        pathRes = R.string.onboarding_install_path
    ),
    OnboardingPage(
        icon = Icons.Outlined.QrCode,
        tint = Nm.cyan,
        titleRes = R.string.onboarding_pair_title,
        bodyRes = R.string.onboarding_pair_body,
        pathRes = R.string.onboarding_pair_path
    ),
    OnboardingPage(
        icon = Icons.Outlined.Shield,
        tint = Nm.primary,
        titleRes = R.string.onboarding_control_title,
        bodyRes = R.string.onboarding_control_body,
        pathRes = R.string.onboarding_control_path
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    NeumorphicBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .neumorphic(shape = CircleShape, backgroundColor = Nm.primary, elevation = 3.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    color = Nm.onSurface,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = trackingFor(2.2.sp)
                )
                Spacer(Modifier.weight(1f))
                if (!isLastPage) {
                    TextButton(onClick = onFinish) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = Nm.onSurfaceMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { index ->
                OnboardingPageContent(page = pages[index])
            }

            Spacer(Modifier.height(10.dp))

            OnboardingDots(currentPage = pagerState.currentPage)

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pagerState.currentPage > 0) {
                    NeumorphicButton(
                        text = stringResource(R.string.back),
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        inset = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                NeumorphicButton(
                    text = stringResource(
                        if (isLastPage) R.string.onboarding_get_started else R.string.onboarding_next
                    ),
                    onClick = {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        NeumorphicIconTile(
            icon = page.icon,
            tint = page.tint,
            size = 108.dp,
            iconSize = 48.dp
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(page.titleRes),
            color = Nm.onSurface,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(page.bodyRes),
            color = Nm.onSurfaceMuted,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp
        )
        if (page.pathRes != null) {
            Spacer(Modifier.height(24.dp))
            PathPill(path = stringResource(page.pathRes))
        }
    }
}

/** Concave pill showing where in the app the action lives. */
@Composable
private fun PathPill(path: String) {
    Box(
        modifier = Modifier
            .neumorphic(
                shape = RoundedCornerShape(14.dp),
                backgroundColor = Nm.inset,
                elevation = 2.dp,
                pressed = true
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = path,
            color = Nm.primary,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = trackingFor(0.2.sp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardingDots(currentPage: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pages.size) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .height(9.dp)
                    .width(if (selected) 26.dp else 9.dp)
                    .neumorphic(
                        shape = RoundedCornerShape(8.dp),
                        backgroundColor = if (selected) Nm.primary else Nm.surface,
                        elevation = if (selected) 4.dp else 2.dp
                    )
            )
        }
    }
}