package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.StateErrorRose
import com.example.ui.theme.StateIdleCyan

@Composable
fun CapabilityAuditTicker(
    latestAudit: ActionAuditEntry?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = latestAudit != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (latestAudit != null) {
            val shape = RoundedCornerShape(12.dp)
            val statusColor = if (latestAudit.isAuthorized) StateIdleCyan else StateErrorRose

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(CoreCharcoalElevated.copy(alpha = 0.7f))
                    .border(1.dp, CoreCharcoalBorder.copy(alpha = 0.5f), shape)
                    .padding(12.dp)
                    .testTag("capability_audit_ticker"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Capability Guard",
                            tint = CyanLuminous,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                        Text(
                            text = "SECURITY GATEWAY AUDIT",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateLight
                        )
                    }

                    Text(
                        text = latestAudit.formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateMuted
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = latestAudit.actionName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (latestAudit.isAuthorized) "AUTHORIZED" else "DENIED",
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor
                    )
                }

                Text(
                    text = "Pipeline: AI Decision → Request [${latestAudit.capabilityName}] → Policy Check [${latestAudit.authorizationSummary}] → ${latestAudit.outcomeSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateLight
                )
            }
        }
    }
}
