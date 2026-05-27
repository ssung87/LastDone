package com.lastdone.app.ui.settings

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lastdone.app.BuildConfig
import com.lastdone.app.core.format.formatTime
import com.lastdone.app.data.settings.AppSettings
import com.lastdone.app.data.settings.SortMode
import com.lastdone.app.data.settings.ThemeMode
import com.lastdone.app.feedback.buildFeedbackMailIntent
import com.lastdone.app.feedback.openPlayStoreListing
import com.lastdone.app.feedback.requestInAppReview
import com.lastdone.app.notification.NotificationScheduler
import com.lastdone.app.ui.theme.LastDoneTheme
import kotlinx.coroutines.launch
import java.time.LocalTime

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    SettingsScreen(
        state = state,
        onImpendingChange = viewModel::setImpendingThreshold,
        onThemeChange = viewModel::setThemeMode,
        onSortChange = viewModel::setSortMode,
        onNotifyTimeChange = viewModel::setNotifyTime,
        onQuietHoursEnabledChange = viewModel::setQuietHoursEnabled,
        onQuietHoursStartChange = viewModel::setQuietHoursStart,
        onQuietHoursEndChange = viewModel::setQuietHoursEnd,
        onGlobalRepeatIntervalChange = viewModel::setGlobalRepeatIntervalMinutes,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: AppSettings,
    onImpendingChange: (Int) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onSortChange: (SortMode) -> Unit,
    onNotifyTimeChange: (LocalTime) -> Unit,
    onQuietHoursEnabledChange: (Boolean) -> Unit,
    onQuietHoursStartChange: (LocalTime) -> Unit,
    onQuietHoursEndChange: (LocalTime) -> Unit,
    onGlobalRepeatIntervalChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    var showQuietStartPicker by remember { mutableStateOf(false) }
    var showQuietEndPicker by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val activity = context as? Activity

    val onRateClick: () -> Unit = {
        val act = activity
        if (act != null) {
            coroutineScope.launch { requestInAppReview(act) }
        } else {
            openPlayStoreListing(context)
        }
    }

    val onFeedbackClick: () -> Unit = {
        try {
            context.startActivity(buildFeedbackMailIntent(context))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "메일 앱이 설치돼 있지 않습니다", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* result ignored — system handles UI */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("설정", fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader("표시")
            ImpendingThresholdRow(
                currentDays = state.impendingThresholdDays,
                onChangeFinished = onImpendingChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SelectRow(
                label = "다크 모드",
                value = state.themeMode,
                options = themeModeOptions,
                onSelect = onThemeChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SelectRow(
                label = "시작 화면 정렬",
                value = state.sortMode,
                options = sortModeOptions,
                onSelect = onSortChange
            )

            Spacer(Modifier.height(24.dp))
            SectionHeader("알림")
            ClickableRow(
                label = "기본 알림 시간",
                value = formatTime(state.notifyTime),
                onClick = { showTimePicker = true }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SelectRow(
                label = "재알림 간격",
                value = state.globalRepeatIntervalMinutes,
                options = globalRepeatIntervalOptions,
                onSelect = onGlobalRepeatIntervalChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                label = "방해금지 시간대",
                checked = state.quietHoursEnabled,
                onCheckedChange = onQuietHoursEnabledChange
            )
            if (state.quietHoursEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ClickableRow(
                    label = "방해금지 시작",
                    value = formatTime(state.quietHoursStart),
                    onClick = { showQuietStartPicker = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ClickableRow(
                    label = "방해금지 종료",
                    value = formatTime(state.quietHoursEnd),
                    onClick = { showQuietEndPicker = true }
                )
            }
            if (BuildConfig.DEBUG) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    OutlinedButton(
                        onClick = { NotificationScheduler.runNow(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("[디버그] 지금 알림 체크")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader("도움말")
            ClickableRow(label = "별점 남기기", value = "", onClick = onRateClick)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ClickableRow(label = "문의/제안하기", value = "", onClick = onFeedbackClick)

            Spacer(Modifier.height(24.dp))
            SectionHeader("앱 정보")
            ReadonlyRow(label = "버전", value = BuildConfig.VERSION_NAME)

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            title = "기본 알림 시간",
            initial = state.notifyTime,
            onConfirm = {
                onNotifyTimeChange(it)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showQuietStartPicker) {
        TimePickerDialog(
            title = "방해금지 시작",
            initial = state.quietHoursStart,
            onConfirm = {
                onQuietHoursStartChange(it)
                showQuietStartPicker = false
            },
            onDismiss = { showQuietStartPicker = false }
        )
    }

    if (showQuietEndPicker) {
        TimePickerDialog(
            title = "방해금지 종료",
            initial = state.quietHoursEnd,
            onConfirm = {
                onQuietHoursEndChange(it)
                showQuietEndPicker = false
            },
            onDismiss = { showQuietEndPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(LocalTime.of(timePickerState.hour, timePickerState.minute))
            }) { Text("확인") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun ImpendingThresholdRow(
    currentDays: Int,
    onChangeFinished: (Int) -> Unit
) {
    var localValue by remember(currentDays) { mutableFloatStateOf(currentDays.toFloat()) }
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("임박 기준일", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${localValue.toInt()}일",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = localValue,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onChangeFinished(localValue.toInt()) },
            valueRange = AppSettings.MIN_IMPENDING_THRESHOLD.toFloat()..AppSettings.MAX_IMPENDING_THRESHOLD.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
private fun <T> SelectRow(
    label: String,
    value: T,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = options.firstOrNull { it.first == value }?.second.orEmpty()

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (optionValue, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        onSelect(optionValue)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ClickableRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ReadonlyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlusBlock() {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "광고 제거",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "한 번 결제로 광고가 영구 제거됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = MaterialTheme.shapes.small
        ) {
            Text("Plus 구매하기", fontWeight = FontWeight.SemiBold)
        }
    }
}

private val themeModeOptions = listOf(
    ThemeMode.SYSTEM to "시스템 설정 따름",
    ThemeMode.LIGHT to "라이트",
    ThemeMode.DARK to "다크"
)

private val sortModeOptions = listOf(
    SortMode.STATUS to "상태 우선",
    SortMode.CATEGORY to "카테고리",
    SortMode.LAST_DONE_DATE to "마지막 수행일",
    SortMode.CREATED_AT to "등록 순"
)

private val globalRepeatIntervalOptions = listOf(
    0 to "끔",
    10 to "10분",
    30 to "30분",
    60 to "1시간"
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun SettingsScreenPreview() {
    LastDoneTheme {
        SettingsScreen(
            state = AppSettings(),
            onImpendingChange = {},
            onThemeChange = {},
            onSortChange = {},
            onNotifyTimeChange = {},
            onQuietHoursEnabledChange = {},
            onQuietHoursStartChange = {},
            onQuietHoursEndChange = {},
            onGlobalRepeatIntervalChange = {},
            onBack = {}
        )
    }
}
