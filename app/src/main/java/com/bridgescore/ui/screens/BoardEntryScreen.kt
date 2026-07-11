package com.bridgescore.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgescore.R
import com.bridgescore.data.model.Doubled
import com.bridgescore.data.model.Suit
import com.bridgescore.data.model.Vulnerability
import com.bridgescore.ui.theme.BlackSuit
import com.bridgescore.ui.theme.RedSuit
import com.bridgescore.ui.viewmodel.BridgeViewModel
import com.bridgescore.ui.viewmodel.BoardEntryState

@Composable
fun BoardEntryScreen(
    viewModel: BridgeViewModel,
    onNavigateSummary: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val entry = state.entryState
    val weAreNS = state.pairSchedule
        .firstOrNull { it.boards.contains(entry.boardNumber) }
        ?.sitNS ?: true
    var jumpDialogOpen by remember { mutableStateOf(false) }
    var jumpTarget by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Helper: run action directly if board is new; else queue for confirmation
    fun saveWithConfirm(action: () -> Unit) {
        if (entry.isExistingBoard) pendingAction = action else action()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Board ${entry.boardNumber}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${vulnerabilityLabel(entry.vulnerability)}  •  ${if (weAreNS) "Sitting NS" else "Sitting EW"}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (entry.opponentPair > 0)
                    Text("Pair ${state.pairNumber} vs Pair ${entry.opponentPair}", fontSize = 13.sp)
                if (entry.nextTable != null)
                    Text("Next → Table ${entry.nextTable}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary)
            }
        }

        HorizontalDivider()

        // ── Passed Out / Not Played ──────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = entry.passed, onCheckedChange = { viewModel.togglePassed() })
            Text("Passed Out")
            Spacer(modifier = Modifier.width(24.dp))
            Checkbox(checked = entry.notPlayed, onCheckedChange = { viewModel.toggleNotPlayed() })
            Text("Not Played")
        }

        if (!entry.passed && !entry.notPlayed) {
            // ── Declarer ─────────────────────────────────────────────────────
            Text("Declarer", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("N", "S", "E", "W").forEach { dir ->
                    DirectionButton(dir, selected = entry.declarer == dir) {
                        viewModel.updateDeclarer(dir)
                    }
                }
            }

            // ── Level ─────────────────────────────────────────────────────────
            Text("Level", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..7).forEach { lvl ->
                    LevelButton(lvl, selected = entry.level == lvl) {
                        viewModel.updateLevel(lvl)
                    }
                }
            }

            // ── Suit ──────────────────────────────────────────────────────────
            Text("Suit", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SuitButton(Suit.CLUBS, entry.suit, R.drawable.ic_club, BlackSuit) {
                    viewModel.updateSuit(Suit.CLUBS)
                }
                SuitButton(Suit.DIAMONDS, entry.suit, R.drawable.ic_diamond, RedSuit) {
                    viewModel.updateSuit(Suit.DIAMONDS)
                }
                SuitButton(Suit.HEARTS, entry.suit, R.drawable.ic_heart, RedSuit) {
                    viewModel.updateSuit(Suit.HEARTS)
                }
                SuitButton(Suit.SPADES, entry.suit, R.drawable.ic_spade, BlackSuit) {
                    viewModel.updateSuit(Suit.SPADES)
                }
                val ntSelected = entry.suit == Suit.NOTRUMP
                val ntBgColor by animateColorAsState(
                    if (ntSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    label = "ntBgColor"
                )
                val ntBorderColor by animateColorAsState(
                    if (ntSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    label = "ntBorderColor"
                )
                Surface(
                    onClick = { viewModel.updateSuit(Suit.NOTRUMP) },
                    shape = RoundedCornerShape(8.dp),
                    color = ntBgColor,
                    modifier = Modifier.height(44.dp).padding(0.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (ntSelected) 2.dp else 1.dp,
                        ntBorderColor
                    )
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            if (ntSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                "NT",
                                fontWeight = FontWeight.Bold,
                                color = if (ntSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // ── Double/Redouble ───────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToggleButton(
                    label = "X",
                    selected = entry.doubled == Doubled.DOUBLED,
                    color = Color(0xFFB71C1C)
                ) {
                    viewModel.updateDoubled(
                        if (entry.doubled == Doubled.DOUBLED) Doubled.NONE else Doubled.DOUBLED
                    )
                }
                ToggleButton(
                    label = "XX",
                    selected = entry.doubled == Doubled.REDOUBLED,
                    color = Color(0xFF880E4F)
                ) {
                    viewModel.updateDoubled(
                        if (entry.doubled == Doubled.REDOUBLED) Doubled.NONE else Doubled.REDOUBLED
                    )
                }
            }

            HorizontalDivider()

            // ── Result ────────────────────────────────────────────────────────
            Text("Result (tricks taken)", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            val tricksNeeded = entry.level + 6
            TricksSelector(
                tricksMade = entry.tricksMade,
                tricksNeeded = tricksNeeded,
                onTricksChanged = { viewModel.updateResult(it) }
            )

            // ── Score display ─────────────────────────────────────────────────
            val score = if (weAreNS) entry.computedScore else -entry.computedScore
            val scoreColor = when {
                score > 0 -> Color(0xFF1B5E20)
                score < 0 -> Color(0xFFB71C1C)
                else -> Color.Gray
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scoreColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (score > 0) "+$score" else "$score",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }
        }

        if (state.jumpedFromBoard != null) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Jumped from Board ${state.jumpedFromBoard}", fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { viewModel.returnToJumpedFromBoard() }) { Text("Return") }
                        TextButton(onClick = { viewModel.dismissJumpBanner() }) { Text("✕") }
                    }
                }
            }
        }

        val nextExpected = state.expectedNextBoard
        if (nextExpected != null && nextExpected != state.currentBoardNumber) {
            Surface(
                onClick = { viewModel.navigateToBoard(nextExpected) },
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Next: Board $nextExpected",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Navigation ────────────────────────────────────────────────────────

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.navigatePrevBoard() },
                modifier = Modifier.weight(1f),
                enabled = viewModel.hasPrevBoard()
            ) { Text("◀ Prev") }

            Button(
                onClick = { saveWithConfirm { viewModel.saveAndNextBoard() } },
                modifier = Modifier.weight(1f),
                enabled = viewModel.hasNextBoard()
            ) { Text("Save ▶") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { saveWithConfirm { viewModel.saveCurrentBoard() } },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
            ) { Text("Save", fontSize = 13.sp, maxLines = 1) }

            Button(
                onClick = { jumpDialogOpen = true },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) { Text("Go To #", fontSize = 13.sp, maxLines = 1) }

            Button(
                onClick = {
                    if (entry.isExistingBoard) {
                        // Existing board: go to summary without saving (no extra row)
                        onNavigateSummary()
                    } else {
                        // New board: save it, then go to summary
                        viewModel.saveCurrentBoard()
                        onNavigateSummary()
                    }
                },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
            ) { Text("Summary", fontSize = 13.sp, maxLines = 1) }
        }
    }

    // ── Jump-to-board dialog ──────────────────────────────────────────────────
    if (jumpDialogOpen) {
        AlertDialog(
            onDismissRequest = { jumpDialogOpen = false },
            title = { Text("Go to Board") },
            text = {
                OutlinedTextField(
                    value = jumpTarget,
                    onValueChange = { jumpTarget = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Board number (1–${state.boardCount})") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = jumpTarget.toIntOrNull() ?: return@TextButton
                    if (n in 1..state.boardCount) {
                        saveWithConfirm { viewModel.navigateToBoard(n, isExplicitJump = true) }
                        jumpDialogOpen = false
                        jumpTarget = ""
                    }
                }) { Text("Go") }
            },
            dismissButton = {
                TextButton(onClick = { jumpDialogOpen = false }) { Text("Cancel") }
            }
        )
    }

    // ── Overwrite confirmation dialog ─────────────────────────────────────────
    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text("Overwrite Score?") },
            text = { Text("Board ${entry.boardNumber} already has a saved score. Replace it?") },
            confirmButton = {
                TextButton(onClick = {
                    action()
                    pendingAction = null
                }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) { Text("Cancel") }
            }
        )
    }
}

// ── Small composables ─────────────────────────────────────────────────────────

@Composable
private fun LevelButton(level: Int, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bg,
        modifier = Modifier.size(40.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("$level", color = fg, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SuitButton(suit: Suit, currentSuit: Suit, iconRes: Int, tint: Color, onClick: () -> Unit) {
    val selected = suit == currentSuit
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) tint.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier.size(44.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) tint else MaterialTheme.colorScheme.outline
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = suit.name,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun DirectionButton(dir: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = bg,
        modifier = Modifier.size(44.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(dir, color = fg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ToggleButton(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val bg = if (selected) color else MaterialTheme.colorScheme.surface
    val fg = if (selected) Color.White else color
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = bg,
        modifier = Modifier.height(36.dp).widthIn(min = 48.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun TricksSelector(
    tricksMade: Int,
    tricksNeeded: Int,
    onTricksChanged: (Int) -> Unit
) {
    val delta = tricksMade - tricksNeeded
    val label = when {
        delta > 0 -> "+$delta"
        delta < 0 -> "$delta"
        else -> "="
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { if (tricksMade > 0) onTricksChanged(tricksMade - 1) },
            modifier = Modifier.size(44.dp),
            contentPadding = PaddingValues(0.dp)
        ) { Text("−", fontSize = 20.sp) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$tricksMade", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                text = label,
                fontSize = 14.sp,
                color = when {
                    delta > 0 -> Color(0xFF1B5E20)
                    delta < 0 -> Color(0xFFB71C1C)
                    else -> Color.Gray
                },
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = { if (tricksMade < 13) onTricksChanged(tricksMade + 1) },
            modifier = Modifier.size(44.dp),
            contentPadding = PaddingValues(0.dp)
        ) { Text("+", fontSize = 20.sp) }

        Spacer(modifier = Modifier.width(8.dp))

        // Quick +/- shortcut buttons relative to contract
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(-2, -1, 0, 1, 2).forEach { d ->
                    val tricks = (tricksNeeded + d).coerceIn(0, 13)
                    val isActive = tricksMade == tricks
                    Surface(
                        onClick = { onTricksChanged(tricks) },
                        shape = RoundedCornerShape(4.dp),
                        color = when {
                            isActive && d >= 0 -> Color(0xFF1B5E20)
                            isActive && d < 0 -> Color(0xFFB71C1C)
                            d == 0 -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surface
                        },
                        modifier = Modifier.size(width = 36.dp, height = 30.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (d < 0) Color(0xFFB71C1C) else if (d > 0) Color(0xFF1B5E20)
                            else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                if (d > 0) "+$d" else "$d",
                                fontSize = 11.sp,
                                color = when {
                                    isActive -> Color.White
                                    d < 0 -> Color(0xFFB71C1C)
                                    d > 0 -> Color(0xFF1B5E20)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun vulnerabilityLabel(vuln: Vulnerability): String = when (vuln) {
    Vulnerability.NONE -> "Neither Vul"
    Vulnerability.NS   -> "N-S Vul"
    Vulnerability.EW   -> "E-W Vul"
    Vulnerability.BOTH -> "Both Vul"
}
