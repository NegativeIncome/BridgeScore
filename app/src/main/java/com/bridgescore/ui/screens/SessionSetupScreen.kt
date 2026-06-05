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
import java.time.LocalDate

@Composable
fun SessionSetupScreen(
    viewModel: BridgeViewModel,
    onSessionStarted: () -> Unit,
    onOpenSession: (Long) -> Unit
) {
    val sessions by viewModel.sessions.collectAsState()
    val today = LocalDate.now().toString()
    val todaySessions = sessions.filter { it.date == today }

    var partner by remember { mutableStateOf("") }
    var pairNumber by remember { mutableStateOf("1") }
    var movementType by remember { mutableStateOf(MovementType.HOWELL) }
    var tables by remember { mutableStateOf("4") }
    var boardCount by remember { mutableStateOf("28") }
    var boardsPerRound by remember { mutableStateOf(4) }
    LaunchedEffect(tables) {
        val tbl = tables.toIntOrNull() ?: 4
        if (movementType == MovementType.HOWELL) {
            boardsPerRound = HowellMovement.defaultBoardsPerRound(tbl)
        }
    }
    var showPastSessions by remember { mutableStateOf(false) }
    var exportDialogOpen by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<com.bridgescore.data.model.Session?>(null) }

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

        if (todaySessions.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Today already has ${if (todaySessions.size == 1) "1 session" else "${todaySessions.size} sessions"}:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    todaySessions.forEach { session ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Partner: ${session.partner}  •  ${session.movementType.name} ${session.numberOfTables}T",
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { onOpenSession(session.id) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) { Text("Open", fontSize = 13.sp) }
                        }
                    }
                    Text(
                        "You can still start a new session below.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

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

        val tblForChips = tables.toIntOrNull() ?: 0
        if (movementType == MovementType.HOWELL && tblForChips in listOf(3, 4)) {
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
                if (tblForChips == 3) {
                    FilterChip(
                        selected = boardsPerRound == 5,
                        onClick = { boardsPerRound = 5 },
                        label = { Text("5") }
                    )
                }
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
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(session.date, fontWeight = FontWeight.Bold)
                                Text("Partner: ${session.partner}  Pair: ${session.pairNumber}")
                                Text("${session.movementType.name} — ${session.numberOfTables} tables — ${session.boardCount} boards")
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = { onOpenSession(session.id) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) { Text("Open") }
                                OutlinedButton(
                                    onClick = { sessionToDelete = session },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp, MaterialTheme.colorScheme.error
                                    )
                                ) { Text("Delete") }
                            }
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

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Session?") },
            text = { Text("${session.date} — Partner: ${session.partner}\nThis cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(session)
                    sessionToDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
