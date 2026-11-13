package com.sza.tourist.wear.ui.common

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipColors
import androidx.wear.compose.material.ChipDefaults

/** Content-sized commands share the touch baseline and modest corner shape. */
@Composable
fun WearCommandChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable RowScope.() -> Unit,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    colors: ChipColors = ChipDefaults.secondaryChipColors(),
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    secondaryLabel: (@Composable RowScope.() -> Unit)? = null,
) {
    Chip(
        onClick = onClick,
        modifier = modifier.width(IntrinsicSize.Max).heightIn(min = 48.dp),
        label = label,
        icon = icon,
        secondaryLabel = secondaryLabel,
        colors = colors,
        enabled = enabled,
        contentPadding = contentPadding,
        shape = WearCellShape,
    )
}
