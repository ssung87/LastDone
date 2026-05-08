package com.lastdone.app.ui.additem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lastdone.app.core.format.formatIsoDate
import com.lastdone.app.notification.NotifyPreset
import com.lastdone.app.notification.RepeatPreset
import com.lastdone.app.ui.templates.TemplatePickerSheet
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    itemId: Long? = null,
    prefill: AddItemPrefill? = null,
    viewModel: AddItemViewModel = viewModel(
        key = "addItem-${itemId ?: "new"}-${prefill?.name ?: ""}",
        factory = AddItemViewModel.factory(itemId, prefill)
    )
) {
    val state by viewModel.state.collectAsState()
    val userTemplates by viewModel.userTemplates.collectAsState()
    var showTemplateSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) onSaved()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isEditMode) "항목 수정" else "항목 추가",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "취소")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = !state.isSaving
                    ) {
                        Text("저장", fontWeight = FontWeight.SemiBold)
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!state.isEditMode) {
                OutlinedButton(
                    onClick = { showTemplateSheet = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📋  템플릿에서 선택")
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("항목명") },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            CategoryDropdown(state = state, onSelect = viewModel::setCategory)

            DateField(date = state.lastDoneDate, onDateChange = viewModel::setDate)

            OutlinedTextField(
                value = state.intervalDaysText,
                onValueChange = viewModel::setInterval,
                label = { Text("권장 주기 (일)") },
                isError = state.intervalDaysError != null,
                supportingText = state.intervalDaysError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            EmojiQuickPicker(
                selected = state.icon,
                onSelect = viewModel::setIcon
            )

            OutlinedTextField(
                value = state.icon,
                onValueChange = viewModel::setIcon,
                label = { Text("아이콘 / 이모지 (선택)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.memo,
                onValueChange = viewModel::setMemo,
                label = { Text("메모 (선택)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            NotifyToggleRow(
                enabled = state.notifyEnabled,
                onEnabledChange = viewModel::setNotifyEnabled
            )

            if (state.notifyEnabled) {
                NotifyPresetDropdown(
                    selected = state.notifyPreset,
                    onSelect = viewModel::setNotifyPreset
                )
                RepeatPresetDropdown(
                    selected = state.repeatPreset,
                    onSelect = viewModel::setRepeatPreset
                )
            }
        }
    }

    if (showTemplateSheet) {
        TemplatePickerSheet(
            userTemplates = userTemplates,
            categories = state.categories,
            onSelect = { selection ->
                viewModel.applyTemplate(selection)
                showTemplateSheet = false
            },
            onDeleteUserTemplate = viewModel::deleteUserTemplate,
            onDismiss = { showTemplateSheet = false }
        )
    }
}

@Composable
private fun EmojiQuickPicker(
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(emojiPalette, key = { it }) { emoji ->
            val isSelected = selected == emoji
            Surface(
                onClick = { onSelect(emoji) },
                shape = CircleShape,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 20.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotifyPresetDropdown(
    selected: NotifyPreset,
    onSelect: (NotifyPreset) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.labelKo,
            onValueChange = {},
            readOnly = true,
            label = { Text("알림 시점") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            NotifyPreset.entries.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.labelKo) },
                    onClick = {
                        onSelect(preset)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatPresetDropdown(
    selected: RepeatPreset,
    onSelect: (RepeatPreset) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.labelKo,
            onValueChange = {},
            readOnly = true,
            label = { Text("추가 알림") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RepeatPreset.entries.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.labelKo) },
                    onClick = {
                        onSelect(preset)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun NotifyToggleRow(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text("알림 사용", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "권장일 기준으로 정해진 시점에 알림을 받아요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    state: AddItemUiState,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name
        .orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("카테고리") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            state.categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = formatIsoDate(date),
            onValueChange = {},
            readOnly = true,
            label = { Text("마지막 수행일") },
            trailingIcon = {
                Icon(Icons.Filled.DateRange, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.toEpochUtcMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val today = LocalDate.now().toEpochUtcMillis()
                    return utcTimeMillis <= today
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateChange(LocalDate.ofEpochDay(millis / DAY_MILLIS))
                    }
                    showDialog = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private const val DAY_MILLIS = 86_400_000L

private fun LocalDate.toEpochUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
