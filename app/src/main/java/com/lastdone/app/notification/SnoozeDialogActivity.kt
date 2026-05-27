package com.lastdone.app.notification

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.lastdone.app.ui.theme.LastDoneTheme
import java.time.LocalDateTime
import java.time.ZoneId

class SnoozeDialogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent?.getLongExtra(EXTRA_ITEM_ID, -1L) ?: -1L
        if (itemId < 0) {
            finish()
            return
        }

        setContent {
            LastDoneTheme {
                SnoozeDialog(
                    onConfirm = { minutes ->
                        applySnooze(itemId, minutes)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun applySnooze(itemId: Long, minutes: Int) {
        val fireAtMillis = LocalDateTime.now()
            .plusMinutes(minutes.toLong())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        RepeatAlarmScheduler.cancel(this, itemId)
        RepeatAlarmScheduler.schedule(this, itemId, fireAtMillis)
        NotificationManagerCompat.from(this).cancel(itemId.toInt())
    }

    companion object {
        const val ACTION_SHOW = "com.lastdone.app.action.SNOOZE_SHOW"
        const val EXTRA_ITEM_ID = "itemId"
    }
}

@Composable
private fun SnoozeDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var customMinutesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("나중에 다시 알림") },
        text = {
            Box(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "얼마 뒤에 다시 알릴까요?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SnoozePresetChips.forEach { (minutes, label) ->
                            AssistChip(
                                onClick = { onConfirm(minutes) },
                                label = { Text(label) },
                                colors = AssistChipDefaults.assistChipColors()
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customMinutesText,
                        onValueChange = { text ->
                            customMinutesText = text.filter { it.isDigit() }.take(4)
                        },
                        label = { Text("직접 입력 (분)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minutes = customMinutesText.toIntOrNull()?.coerceAtLeast(1)
                    if (minutes != null) onConfirm(minutes)
                },
                enabled = customMinutesText.toIntOrNull()?.let { it >= 1 } == true
            ) { Text("확인") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
        modifier = Modifier.padding(8.dp)
    )
}

private val SnoozePresetChips: List<Pair<Int, String>> = listOf(
    5 to "5분",
    15 to "15분",
    30 to "30분",
    60 to "1시간",
    180 to "3시간"
)
