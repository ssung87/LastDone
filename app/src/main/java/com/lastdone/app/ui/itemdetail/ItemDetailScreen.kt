package com.lastdone.app.ui.itemdetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lastdone.app.core.format.formatLongDate
import com.lastdone.app.domain.ItemStatus
import com.lastdone.app.ui.ads.AdBannerSlot
import com.lastdone.app.ui.theme.LastDoneTheme
import com.lastdone.app.ui.theme.statusColorFor
import java.time.LocalDate

@Composable
fun ItemDetailRoute(
    itemId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: ItemDetailViewModel = viewModel(factory = ItemDetailViewModel.factory(itemId))
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    when {
        state.isLoading -> ItemDetailLoading(onBack = onBack)
        state.ui != null -> ItemDetailScreen(
            ui = state.ui!!,
            onBack = onBack,
            onMarkDone = viewModel::markDoneToday,
            onEdit = onEdit,
            onDelete = { viewModel.delete(onDeleted = onBack) },
            onDeleteHistory = viewModel::deleteHistory,
            onSaveAsTemplate = viewModel::saveAsTemplate,
            snackbarHostState = snackbarHostState
        )
        else -> ItemDetailNotFound(onBack = onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    ui: ItemDetailUi,
    onBack: () -> Unit,
    onMarkDone: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onDeleteHistory: (Long) -> Unit = {},
    onSaveAsTemplate: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, "메뉴")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("수정") },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("템플릿으로 저장") },
                            onClick = {
                                menuOpen = false
                                onSaveAsTemplate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("삭제") },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { AdBannerSlot() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            HeroSection(ui)
            Spacer(Modifier.height(8.dp))
            InfoSection(ui)
            Spacer(Modifier.height(32.dp))
            DoneTodayButton(onClick = onMarkDone)
            Spacer(Modifier.height(40.dp))
            HistorySection(history = ui.history, onDeleteHistory = onDeleteHistory)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailLoading(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailNotFound(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "항목을 찾을 수 없어요",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeroSection(ui: ItemDetailUi) {
    val statusColor = statusColorFor(ui.status.kind)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        Text(text = ui.icon, fontSize = 40.sp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = ui.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${ui.status.daysElapsed}일째",
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = bigStatusText(ui.status),
            style = MaterialTheme.typography.bodyLarge,
            color = statusColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoSection(ui: ItemDetailUi) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        InfoRow("마지막 수행일", formatLongDate(ui.lastDoneDate))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        InfoRow("권장 주기", "${ui.intervalDays}일")
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        InfoRow("카테고리", ui.categoryName)
        ui.memo?.let {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow("메모", it)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DoneTodayButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "오늘 했어요",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HistorySection(
    history: List<HistoryEntry>,
    onDeleteHistory: (Long) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<HistoryEntry?>(null) }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "수행 히스토리",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        if (history.isEmpty()) {
            Text(
                text = "아직 기록이 없어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            history.forEach { entry ->
                HistoryRow(
                    entry = entry,
                    onLongPress = { pendingDelete = entry }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Text(
                text = "기록을 길게 누르면 삭제할 수 있어요.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("기록 삭제") },
            text = {
                Text("${formatLongDate(entry.doneDate)} 기록을 삭제할까요?")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteHistory(entry.id)
                    pendingDelete = null
                }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("취소") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onLongPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            )
            .padding(vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatLongDate(entry.doneDate),
                style = MaterialTheme.typography.bodyMedium
            )
            entry.gapDays?.let {
                Text(
                    text = "이전보다 ${it}일 후",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        entry.memo?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun bigStatusText(status: ItemStatus): String = when (status.kind) {
    ItemStatus.Kind.OVERDUE -> "권장일에서 ${status.daysOver}일 초과"
    ItemStatus.Kind.DUE_TODAY -> "오늘이 권장일"
    ItemStatus.Kind.IMPENDING -> "다음 권장일까지 ${status.daysRemaining}일"
    ItemStatus.Kind.RELAXED -> "다음 권장일까지 ${status.daysRemaining}일"
}

internal val sampleItemDetailUi: ItemDetailUi
    get() {
        val today = LocalDate.now()
        return ItemDetailUi(
            name = "침구 세탁",
            icon = "🛏️",
            categoryName = "집안관리",
            lastDoneDate = today.minusDays(12),
            intervalDays = 14,
            memo = "이불, 베개커버 포함",
            status = ItemStatus(
                daysElapsed = 12,
                daysRemaining = 2,
                kind = ItemStatus.Kind.IMPENDING
            ),
            history = listOf(
                HistoryEntry(1L, today.minusDays(12), 14, "이불, 베개커버 포함"),
                HistoryEntry(2L, today.minusDays(26), 13, null),
                HistoryEntry(3L, today.minusDays(39), null, null)
            )
        )
    }

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ItemDetailScreenPreview() {
    LastDoneTheme {
        ItemDetailScreen(
            ui = sampleItemDetailUi,
            onBack = {},
            onMarkDone = {}
        )
    }
}
