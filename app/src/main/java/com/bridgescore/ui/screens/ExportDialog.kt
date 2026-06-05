package com.bridgescore.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bridgescore.data.model.Session
import com.bridgescore.ui.viewmodel.BridgeViewModel
import kotlinx.coroutines.launch

@Composable
fun ExportDialog(
    sessions: List<Session>,
    viewModel: BridgeViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selected = remember { mutableStateMapOf<Long, Boolean>() }

    // File picker — opens after we have the CSV content ready
    var pendingCsv by remember { mutableStateOf<String?>(null) }
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null && pendingCsv != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(pendingCsv!!.toByteArray())
            }
            pendingCsv = null
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Sessions") },
        text = {
            if (sessions.isEmpty()) {
                Text("No sessions to export.")
            } else {
                LazyColumn {
                    items(sessions) { session ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selected[session.id] == true,
                                onCheckedChange = { checked ->
                                    if (checked) selected[session.id] = true
                                    else selected.remove(session.id)
                                }
                            )
                            Text("${session.date}  (${session.boardCount} boards)")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected.isNotEmpty(),
                onClick = {
                    scope.launch {
                        val ids = selected.keys.sorted()
                        pendingCsv = viewModel.buildExportCsv(ids)
                        createDocument.launch("bridgescore_export.csv")
                    }
                }
            ) { Text("Export") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
