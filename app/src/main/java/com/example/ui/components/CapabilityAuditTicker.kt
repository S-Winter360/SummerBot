package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.actions.ActionAuditEntry
import com.example.ui.theme.CoreCharcoalBorder
import com.example.ui.theme.CoreCharcoalElevated
import com.example.ui.theme.CyanLuminous
import com.example.ui.theme.SlateLight
import com.example.ui.theme.StateErrorRose
import com.example.ui.theme.StateIdleCyan

/**
 * Understated, organic capability verification pill.
 * Informs the user of system capability events without technical dashboard clutter.
 */
@Composable
fun CapabilityAuditTicker(
    latestAudit: ActionAuditEntry?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = latestAudit != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        modifier = modifier
    ) {
        if (latestAudit != null) {
            val pillShape = RoundedCornerShape(20.dp)
            val statusColor = if (latestAudit.isAuthorized) StateIdleCyan else StateErrorRose

            Row(
                modifier = Modifier
                    .clip(pillShape)
                    .background(CoreCharcoalElevated.copy(alpha = 0.70f))
                    .border(1.dp, CoreCharcoalBorder.copy(alpha = 0.40f), pillShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("capability_audit_ticker"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Capability Guard",
                    tint = statusColor,
                    modifier = Modifier.size(14.dp)
                )

                Text(
                    text = "${latestAudit.actionName} · ${if (latestAudit.isAuthorized) "Permitted" else "Restricted"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateLight
                )
            }
        }
    }
}
