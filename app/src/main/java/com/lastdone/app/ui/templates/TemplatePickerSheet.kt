package com.lastdone.app.ui.templates

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lastdone.app.data.local.entity.CategoryEntity
import com.lastdone.app.data.local.entity.TemplateEntity

data class TemplateSelection(
    val name: String,
    val intervalDays: Int,
    val icon: String?,
    val categoryId: Long?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePickerSheet(
    userTemplates: List<TemplateEntity>,
    categories: List<CategoryEntity>,
    onSelect: (TemplateSelection) -> Unit,
    onDeleteUserTemplate: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pendingDelete by remember { mutableStateOf<TemplateEntity?>(null) }

    val categoryNameToId: Map<String, Long> = remember(categories) {
        categories.associate { it.name to it.id }
    }
    val categoryIdToName: Map<Long, String> = remember(categories) {
        categories.associate { it.id to it.name }
    }

    val groupedBuiltIn = remember {
        builtInTemplates.groupBy { it.categoryName }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    text = "템플릿에서 선택",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (userTemplates.isNotEmpty()) {
                item { SectionHeader("내 템플릿") }
                items(
                    items = userTemplates,
                    key = { "user-${it.id}" }
                ) { template ->
                    UserTemplateRow(
                        template = template,
                        categoryName = categoryIdToName[template.categoryId].orEmpty(),
                        onClick = {
                            onSelect(
                                TemplateSelection(
                                    name = template.name,
                                    intervalDays = template.intervalDays,
                                    icon = template.icon,
                                    categoryId = template.categoryId
                                )
                            )
                        },
                        onLongPress = { pendingDelete = template }
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }

            item { SectionHeader("추천 템플릿") }
            groupedBuiltIn.forEach { (categoryName, list) ->
                item(key = "header-$categoryName") {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
                items(
                    items = list,
                    key = { "builtin-${it.name}" }
                ) { template ->
                    BuiltInTemplateRow(
                        template = template,
                        onClick = {
                            onSelect(
                                TemplateSelection(
                                    name = template.name,
                                    intervalDays = template.intervalDays,
                                    icon = template.icon,
                                    categoryId = categoryNameToId[template.categoryName]
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("템플릿 삭제") },
            text = { Text("\"${entry.name}\" 템플릿을 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteUserTemplate(entry.id)
                    pendingDelete = null
                }) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("취소") }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserTemplateRow(
    template: TemplateEntity,
    categoryName: String,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongPress
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = template.icon.orEmpty().ifBlank { "·" },
                style = MaterialTheme.typography.titleLarge
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (categoryName.isNotEmpty()) {
                        "$categoryName · ${template.intervalDays}일마다"
                    } else {
                        "${template.intervalDays}일마다"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BuiltInTemplateRow(
    template: BuiltInTemplate,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = template.icon,
                style = MaterialTheme.typography.titleLarge
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${template.intervalDays}일마다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
