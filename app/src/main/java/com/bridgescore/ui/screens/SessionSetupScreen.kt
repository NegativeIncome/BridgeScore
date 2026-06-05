package com.bridgescore.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgescore.data.model.MovementType
import com.bridgescore.scoring.movement.HowellMovement
import com.bridgescore.ui.viewmodel.BridgeViewModel

@Composable
fun SessionSetupScreen(
    viewModel: BridgeViewModel,
    onSessionStarted: () -> Unit,
    onOpenSession: (Long) -> Unit
) {
    val sessions by viewModel.sessions.collectAsState()

    var partner by remember { mutableStateOf("") }
    var pairNumber by remember { mutableStateOf("1") }
    var movementType by remember { mutableStateOf(MovementType.HOWELL) }
    var tables by remember { mutableStateOf("4") }
    var boardCount by remember { mutableStateOf("28") }
    var boardsPerRound by remember { mutableStateOf(4) }
    var showPastSessions by remember { mutableStateOf(false) }
    var exportDialogOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "BridgeScore",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text("New Session", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

        OutlinedTextField(
            value = partner,
            onValueChange = { partner = it },
            label = { Text("Partner Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = pairNumber,
                onValueChange = { pairNumber = it.filter { c -> c.isDigit() } },
                label = { Text("Pair #") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = tables,
                onValueChange = { tables = it.filter { c -> c.isDigit() } },
                label = { Text("Tables") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            if (movementType == MovementType.MITCHELL) {
                OutlinedTextField(
                    value = boardCount,
                    onValueChange = { boardCount = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Boards") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            } else {
                val tbl = tables.toIntOrNull()?.coerceIn(3, 10) ?: 4
                val computedBoardCount = HowellMovement.boardCountForMovement(tbl, boardsPerRound)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Boards: $computedBoardCount", fontWeight = FontWeight.Medium)
                }
            }
        }

        if (movementType == MovementType.HOWELL && (tables.toIntOrNull() ?: 0) == 4) {
            Text("Boards per round", fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = boardsPerRound == 3,
                    onClick = { boardsPerRound = 3 },
                    label = { Text("3") }
                )
                FilterChip(
                    selected = boardsPerRound == 4,
                    onClick = { boardsPerRound = 4 },
                    label = { Text("4") }
                )
            }
        }

        Text("Movement", fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = movementType == MovementType.HOWELL,
                onClick = { movementType = MovementType.HOWELL },
                label = { Text("Howell") }
            )
            FilterChip(
                selected = movementType == MovementType.MITCHELL,
                onClick = { movementType = MovementType.MITCHELL },
                label = { Text("Mitchell") }
            )
        }

        Button(
            onClick = {
                val pair = pairNumber.toIntOrNull() ?: 1
                val tbl = tables.toIntOrNull()?.coerceIn(3, 10) ?: 4
                viewModel.startNewSession(
                    partner.trim(), pair, movementType, tbl,
                    boardsPerRound = if (movementType == MovementType.HOWELL) boardsPerRound
                        else (boardCount.toIntOrNull()?.coerceIn(1, 36) ?: tbl * 2 * 2)
                )
                onSessionStarted()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = partner.isNotBlank()
        ) {
            Text("Start Session", fontSize = 16.sp)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = { showPastSessions = !showPastSessions },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (showPastSessions) "Hide Past Sessions" else "Resume Past Session")
            }
            OutlinedButton(
                onClick = { exportDialogOpen = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Export CSV")
            }
        }

        if (showPastSessions) {
            if (sessions.isEmpty()) {
                Text("No past sessions found.", color = MaterialTheme.colorScheme.outline)
            } else {
                sessions.forEach { session ->
                    Card(
                        onClick = { onOpenSession(session.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(session.date, fontWeight = FontWeight.Bold)
                            Text("Partner: ${session.partner}  Pair: ${session.pairNumber}")
                            Text("${session.movementType.name} — ${session.numberOfTables} tables — ${session.boardCount} boards")
                        }
                    }
                }
            }
        }
    }

    if (exportDialogOpen) {
        ExportDialog(
            sessions = sessions,
            viewModel = viewModel,
            onDismiss = { exportDialogOpen = false }
        )
    }
}
