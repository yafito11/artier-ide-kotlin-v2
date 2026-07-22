package com.artier.ide.ui.tunnel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artier.ide.data.model.DetectedPort
import com.artier.ide.data.model.TunnelSession
import com.artier.ide.data.model.TunnelStatus

@Composable
fun TunnelPanel(
    modifier: Modifier = Modifier,
    viewModel: TunnelViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    var showPortDialog by remember { mutableStateOf(false) }
    var selectedPort by remember { mutableIntStateOf(3000) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PUBLIC TUNNEL",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row {
                IconButton(
                    onClick = { viewModel.detectPorts() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Detect Ports",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Active tunnel display
        val activeSession = state.activeSession
        if (activeSession != null) {
            ActiveTunnelCard(
                session = activeSession,
                onCopyUrl = { url ->
                    copyToClipboard(context, url)
                },
                onOpenInChrome = { url ->
                    openInChrome(context, url)
                },
                onCloseTunnel = {
                    viewModel.closeActiveTunnel()
                }
            )
        } else {
            // Create tunnel section
            CreateTunnelSection(
                detectedPorts = state.detectedPorts,
                onCreateTunnel = { port ->
                    selectedPort = port
                    viewModel.createTunnel(port)
                },
                onCreateCustomTunnel = {
                    showPortDialog = true
                }
            )
        }

        // Error display
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Loading indicator
        if (state.isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ActiveTunnelCard(
    session: TunnelSession,
    onCopyUrl: (String) -> Unit,
    onOpenInChrome: (String) -> Unit,
    onCloseTunnel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Tunnel Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // URL display
            if (session.url != null) {
                Text(
                    text = session.url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy button
                    OutlinedButton(
                        onClick = { onCopyUrl(session.url) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }

                    // Open in Chrome button
                    Button(
                        onClick = { onOpenInChrome(session.url) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Close button
                OutlinedButton(
                    onClick = onCloseTunnel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Close Tunnel")
                }
            }
        }
    }
}

@Composable
fun CreateTunnelSection(
    detectedPorts: List<DetectedPort>,
    onCreateTunnel: (Int) -> Unit,
    onCreateCustomTunnel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Quick create buttons for detected ports
        if (detectedPorts.isNotEmpty()) {
            Text(
                text = "Detected Services",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(detectedPorts) { port ->
                    DetectedPortCard(
                        port = port,
                        onClick = { onCreateTunnel(port.port) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Custom port input
        OutlinedButton(
            onClick = onCreateCustomTunnel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Create Custom Tunnel")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectedPortCard(
    port: DetectedPort,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Port ${port.port}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = port.service,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var portText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Tunnel") },
        text = {
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it.filter { c -> c.isDigit() } },
                label = { Text("Port Number") },
                placeholder = { Text("e.g., 3000") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val port = portText.toIntOrNull()
                    if (port != null && port in 1..65535) {
                        onConfirm(port)
                        onDismiss()
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Tunnel URL", text)
    clipboard.setPrimaryClip(clip)
}

private fun openInChrome(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.setPackage("com.android.chrome")
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to default browser
        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(fallbackIntent)
    }
}