package com.example.healthlog.ui.medications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthlog.R
import com.example.healthlog.ui.theme.HealthLogTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderAlertsSheet(
    followUpEnabled: Boolean,
    keepRingingEnabled: Boolean,
    onFollowUpChanged: (Boolean) -> Unit,
    onKeepRingingChanged: (Boolean) -> Unit,
    onOpenSoundSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        ReminderAlertsContent(
            followUpEnabled = followUpEnabled,
            keepRingingEnabled = keepRingingEnabled,
            onFollowUpChanged = onFollowUpChanged,
            onKeepRingingChanged = onKeepRingingChanged,
            onOpenSoundSettings = onOpenSoundSettings
        )
    }
}

@Composable
internal fun ReminderAlertsContent(
    followUpEnabled: Boolean,
    keepRingingEnabled: Boolean,
    onFollowUpChanged: (Boolean) -> Unit,
    onKeepRingingChanged: (Boolean) -> Unit,
    onOpenSoundSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp)
    ) {
        Text(
            text = stringResource(R.string.reminder_alerts_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.reminder_alerts_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Follow-up & Keep Ringing toggles
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ReminderAlertToggleRow(
                    title = stringResource(R.string.reminder_alerts_follow_up_title),
                    description = stringResource(R.string.reminder_alerts_follow_up_desc),
                    checked = followUpEnabled,
                    onCheckedChange = onFollowUpChanged
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                ReminderAlertToggleRow(
                    title = stringResource(R.string.reminder_alerts_keep_ringing_title),
                    description = stringResource(R.string.reminder_alerts_keep_ringing_desc),
                    checked = keepRingingEnabled,
                    onCheckedChange = onKeepRingingChanged
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Deep link to Android's system channel settings (sound, vibration, DND override)
        Surface(
            onClick = onOpenSoundSettings,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_notifications),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.reminder_alerts_system_sound_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.reminder_alerts_system_sound_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ReminderAlertToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderAlertsContentPreview() {
    HealthLogTheme {
        ReminderAlertsContent(
            followUpEnabled = true,
            keepRingingEnabled = false,
            onFollowUpChanged = {},
            onKeepRingingChanged = {},
            onOpenSoundSettings = {}
        )
    }
}
