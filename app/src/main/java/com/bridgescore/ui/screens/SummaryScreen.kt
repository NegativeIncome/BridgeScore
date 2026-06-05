package com.bridgescore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgescore.data.model.BoardResult
import com.bridgescore.data.model.Doubled
import com.bridgescore.data.model.Suit
import com.bridgescore.ui.viewmodel.BridgeViewModel

@Composable
fun SummaryScreen(
    viewModel: BridgeViewModel,
    onBack: () -> Unit,
    onEditBoard: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val boards = state.boards.sortedBy { it.boardNumber }

    val totalScore = boards.sumOf { it.score }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Summary", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${state.date}  •  Partner: ${state.partner}", fontSize = 13.sp)
                Text("Pair ${state.pairNumber}  •  ${state.movementType.name}  •  ${state.numberOfTables} tables",
                    fontSize = 13.sp)
            }
            OutlinedButton(onClick = onBack) { Text("Back") }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Totals bar
        val scoreColor = when {
            totalScore > 0 -> Color(0xFF1B5E20)
            totalScore < 0 -> Color(0xFFB71C1C)
            else -> Color.Gray
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scoreColor.copy(alpha = 0.1f), MaterialTheme.shapes.small)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${boards.size} boards played", fontWeight = FontWeight.Medium)
            Text(
                "Total: ${if (totalScore > 0) "+$totalScore" else "$totalScore"}",
                fontWeight = FontWeight.Bold,
                color = scoreColor,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Header row
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text("#", modifier = Modifier.width(32.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Contract", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Made", modifier = Modifier.width(48.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("Score", modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        HorizontalDivider()

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(boards) { board ->
                BoardSummaryRow(board = board, onClick = { onEditBoard(board.boardNumber) })
            }
            // Placeholder rows for unplayed boards
            val playedNumbers = boards.map { it.boardNumber }.toSet()
            val unplayed = (1..state.boardCount).filter { it !in playedNumbers }
            items(unplayed) { n ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$n", modifier = Modifier.width(32.dp), fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline)
                    Text("—", modifier = Modifier.weight(1f), fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline)
                    Text("", modifier = Modifier.width(48.dp))
                    Text("", modifier = Modifier.width(60.dp))
                }
            }
        }
    }
}

@Composable
private fun BoardSummaryRow(board: BoardResult, onClick: () -> Unit) {
    val scoreColor = when {
        board.score > 0 -> Color(0xFF1B5E20)
        board.score < 0 -> Color(0xFFB71C1C)
        else -> Color.Gray
    }
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${board.boardNumber}",
                modifier = Modifier.width(32.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                contractString(board),
                modifier = Modifier.weight(1f),
                fontSize = 13.sp
            )
            Text(
                when {
                    board.notPlayed -> "NP"
                    board.passed -> "P/O"
                    else -> "${board.tricksMade}"
                },
                modifier = Modifier.width(48.dp),
                fontSize = 13.sp
            )
            Text(
                if (board.score > 0) "+${board.score}" else "${board.score}",
                modifier = Modifier.width(60.dp),
                color = scoreColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

private fun contractString(board: BoardResult): String {
    if (board.notPlayed) return "NP"
    if (board.passed) return "Passed"
    val suitChar = when (board.suit) {
        Suit.CLUBS    -> "♣"
        Suit.DIAMONDS -> "♦"
        Suit.HEARTS   -> "♥"
        Suit.SPADES   -> "♠"
        Suit.NOTRUMP  -> "NT"
    }
    val dbl = when (board.doubled) {
        Doubled.NONE      -> ""
        Doubled.DOUBLED   -> "X"
        Doubled.REDOUBLED -> "XX"
    }
    return "${board.level}$suitChar$dbl by ${board.declarer}"
}
