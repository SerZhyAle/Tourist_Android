package com.sza.tourist.wear.ui.apps.tourist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ChipDefaults
import com.sza.tourist.wear.ui.common.WearCommandChip as CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.sza.tourist.wear.R

private val ATHLETE_H_PADDING = 14.dp
private val ATHLETE_V_PADDING = 6.dp
private val ROW_SPACING = 6.dp
private val COLUMN_SPACING = 4.dp

/**
 * S3007 / S3015: Quick action buttons for resetting metrics and entering athlete mode. S3115 moved the
 * lock control onto the hero panel itself, so this row no longer carries it.
 *
 * S0023, a local addition to the port: each chip carries its label as its own content description. At a
 * large font a chip cut by the list's edge loses its clipped text child from the accessibility tree and
 * would otherwise be announced as an unlabelled button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TouristActionsRow(
    onResetTrip: () -> Unit,
    onResetSteps: () -> Unit,
    onToggleAthleteMode: () -> Unit,
    onLaunchSos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(COLUMN_SPACING),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ROW_SPACING, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val resetTripLabel = stringResource(R.string.wear_tourist_action_reset_trip)
            CompactChip(
                onClick = onResetTrip,
                modifier = Modifier.semantics { contentDescription = resetTripLabel },
                label = {
                    Text(
                        text = resetTripLabel,
                        style = MaterialTheme.typography.caption3,
                    )
                },
                colors = ChipDefaults.secondaryChipColors(),
            )
            val resetStepsLabel = stringResource(R.string.wear_tourist_action_reset_steps)
            CompactChip(
                onClick = onResetSteps,
                modifier = Modifier.semantics { contentDescription = resetStepsLabel },
                label = {
                    Text(
                        text = resetStepsLabel,
                        style = MaterialTheme.typography.caption3,
                    )
                },
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ROW_SPACING, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val athleteLabel = stringResource(R.string.wear_tourist_athlete_mode)
            CompactChip(
                onClick = onToggleAthleteMode,
                modifier = Modifier.semantics { contentDescription = athleteLabel },
                label = {
                    Text(
                        text = athleteLabel,
                        style = MaterialTheme.typography.caption2,
                    )
                },
                colors = ChipDefaults.primaryChipColors(),
                contentPadding = PaddingValues(
                    horizontal = ATHLETE_H_PADDING,
                    vertical = ATHLETE_V_PADDING,
                ),
            )
        }
        // S3216: a row of its own rather than a fourth chip beside the two resets. The dashboard is
        // where the owner already is when something goes wrong outdoors, so the distress signal has to
        // be found without reading - and it must not be the neighbour of a button that zeroes a metric.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ROW_SPACING, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val sosLabel = stringResource(R.string.wear_tourist_action_sos)
            CompactChip(
                onClick = onLaunchSos,
                modifier = Modifier.semantics { contentDescription = sosLabel },
                label = {
                    Text(
                        text = sosLabel,
                        style = MaterialTheme.typography.caption2,
                    )
                },
                colors = ChipDefaults.primaryChipColors(
                    backgroundColor = colorResource(R.color.color_program_accent_scarlet),
                ),
                contentPadding = PaddingValues(
                    horizontal = ATHLETE_H_PADDING,
                    vertical = ATHLETE_V_PADDING,
                ),
            )
        }
    }
}
