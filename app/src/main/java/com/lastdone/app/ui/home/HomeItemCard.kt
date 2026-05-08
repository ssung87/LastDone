package com.lastdone.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lastdone.app.core.format.formatShortDate
import com.lastdone.app.domain.ItemStatus
import com.lastdone.app.ui.theme.statusColorFor

@Composable
fun HomeItemCard(
    item: HomeItemUi,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = item.icon ?: "·", fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${item.categoryName} · 마지막 ${formatShortDate(item.lastDoneDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = mainText(item.status),
            color = statusColorFor(item.status.kind),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

private fun mainText(status: ItemStatus): String = when (status.kind) {
    ItemStatus.Kind.OVERDUE -> "+${status.daysOver}"
    ItemStatus.Kind.DUE_TODAY -> "오늘"
    ItemStatus.Kind.IMPENDING,
    ItemStatus.Kind.RELAXED -> "D-${status.daysRemaining}"
}
