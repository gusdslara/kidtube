package com.kidstv.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kidstv.app.data.db.PlaylistWithVideos
import com.kidstv.app.data.db.VideoEntity
import com.kidstv.app.data.db.VideoSourceType
import com.kidstv.app.data.storage.StorageSnapshot
import com.kidstv.app.ui.theme.KidsColors
import com.kidstv.app.util.Formatters
import java.io.File

@Composable
fun VideoThumbnail(
    video: VideoEntity,
    modifier: Modifier = Modifier
) {
    val model: Any? = remember(video.thumbnailPathOrUrl) {
        val raw = video.thumbnailPathOrUrl
        when {
            raw.isNullOrBlank() -> null
            raw.startsWith("http") -> raw
            else -> File(raw)
        }
    }

    Box(
        modifier = modifier
            .background(KidsColors.SurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Movie,
                contentDescription = null,
                tint = KidsColors.TextSecondary,
                modifier = Modifier.size(36.dp)
            )
        }

        // Source badge
        Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
        ) {
            val label = if (video.sourceType == VideoSourceType.YOUTUBE) {
                "YouTube"
            } else if (video.durationMs > 0L) {
                Formatters.formatDuration(video.durationMs)
            } else {
                "Local"
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun HorizontalVideoCard(
    video: VideoEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val border = when {
        focused -> BorderStroke(2.5.dp, KidsColors.Primary)
        isSelected -> BorderStroke(2.dp, KidsColors.Secondary)
        else -> BorderStroke(1.dp, KidsColors.Border)
    }

    Card(
        modifier = modifier
            .width(204.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = border,
        colors = CardDefaults.cardColors(containerColor = KidsColors.Surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                VideoThumbnail(video = video, modifier = Modifier.fillMaxSize())
                if (isSelected) {
                    Surface(
                        color = KidsColors.Secondary.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "Tocando",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = video.channelOrAuthor ?: if (video.sourceType == VideoSourceType.YOUTUBE) "YouTube" else "Dispositivo",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FeedVideoCard(
    video: VideoEntity,
    playlists: List<PlaylistWithVideos>,
    canManage: Boolean,
    onPlay: () -> Unit,
    onAddToPlaylist: ((Long) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    var showPlMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (focused) 2.5.dp else 1.dp,
            color = if (focused) KidsColors.Primary else KidsColors.Border
        ),
        colors = CardDefaults.cardColors(containerColor = KidsColors.Surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                VideoThumbnail(video = video, modifier = Modifier.fillMaxSize())
                Surface(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = "Reproduzir",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(44.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val detail = buildString {
                        append(if (video.sourceType == VideoSourceType.YOUTUBE) "YouTube" else "Arquivo local")
                        if (video.fileSizeBytes > 0L) {
                            append(" • ")
                            append(Formatters.formatBytes(video.fileSizeBytes))
                        }
                        if (!video.channelOrAuthor.isNullOrBlank()) {
                            append(" • ")
                            append(video.channelOrAuthor)
                        }
                    }
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (canManage) {
                    Row {
                        if (onAddToPlaylist != null && playlists.isNotEmpty()) {
                            Box {
                                IconButton(onClick = { showPlMenu = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                        contentDescription = "Adicionar à playlist",
                                        tint = KidsColors.TextSecondary
                                    )
                                }
                                DropdownMenu(
                                    expanded = showPlMenu,
                                    onDismissRequest = { showPlMenu = false }
                                ) {
                                    playlists.forEach { pwv ->
                                        DropdownMenuItem(
                                            text = { Text(pwv.playlist.name) },
                                            onClick = {
                                                showPlMenu = false
                                                onAddToPlaylist(pwv.playlist.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (onDelete != null) {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Excluir vídeo",
                                    tint = KidsColors.Danger
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StorageStatusCard(
    snapshot: StorageSnapshot,
    modifier: Modifier = Modifier,
    onAdjustQuotaMb: ((Int) -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, KidsColors.Border),
        colors = CardDefaults.cardColors(containerColor = KidsColors.Surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = null,
                        tint = KidsColors.Primary
                    )
                    Text("Espaço do dispositivo", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = "${(snapshot.usedRatioOfQuota * 100).toInt()}% da cota",
                    style = MaterialTheme.typography.labelMedium,
                    color = KidsColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { snapshot.usedRatioOfQuota },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = if (snapshot.usedRatioOfQuota > 0.85f) KidsColors.Warning else KidsColors.Primary,
                trackColor = KidsColors.SurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Vídeos salvos: ${Formatters.formatBytes(snapshot.appVideosBytes)} / Cota: ${Formatters.formatBytes(snapshot.quotaBytes)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Livre no aparelho: ${Formatters.formatBytes(snapshot.freeDeviceBytes)} • Disponível p/ novos vídeos: ${Formatters.formatBytes(snapshot.remainingAllowedBytes)}",
                style = MaterialTheme.typography.bodySmall
            )

            if (onAdjustQuotaMb != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2048 to "2 GB", 4096 to "4 GB", 8192 to "8 GB", 16384 to "16 GB").forEach { (mb, label) ->
                        val active = (snapshot.quotaBytes / (1024L * 1024L)).toInt() == mb
                        OutlinedButton(
                            onClick = { onAdjustQuotaMb(mb) },
                            border = BorderStroke(1.dp, if (active) KidsColors.Primary else KidsColors.Border),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinDialog(
    isFirstTimeSetup: Boolean,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onConfirmSetup: (String) -> Boolean = { false },
    onVerifyPin: (String) -> Boolean = { false },
    requireCurrentPin: Boolean = false
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var currentPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = KidsColors.Primary) },
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
                if (requireCurrentPin) {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { currentPin = it; error = null },
                        label = { Text("Senha atual") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it; error = null },
                    label = { Text("Senha / PIN (mín. 4 dígitos)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isFirstTimeSetup || requireCurrentPin) {
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it; error = null },
                        label = { Text("Confirmar senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (error != null) {
                    Text(error!!, color = KidsColors.Danger, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = pin.trim()
                    if (requireCurrentPin && !onVerifyPin(currentPin.trim())) {
                        error = "Senha atual incorreta."
                        return@Button
                    }
                    if (clean.length < 4) {
                        error = "Use pelo menos 4 dígitos."
                        return@Button
                    }
                    if (isFirstTimeSetup || requireCurrentPin) {
                        if (clean != confirmPin.trim()) {
                            error = "As senhas não conferem."
                            return@Button
                        }
                        if (!onConfirmSetup(clean)) {
                            error = "Não foi possível salvar a senha."
                        }
                    } else {
                        if (!onVerifyPin(clean)) {
                            error = "Senha incorreta."
                        }
                    }
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AddVideoDialog(
    playlists: List<PlaylistWithVideos>,
    storageSnapshot: StorageSnapshot,
    onDismiss: () -> Unit,
    onAddYouTube: (url: String, title: String?, playlistId: Long?) -> Unit,
    onPickLocalVideos: (title: String?, playlistId: Long?) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var ytUrl by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var showPlDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Vídeo") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("URL YouTube") },
                        icon = { Icon(Icons.Rounded.Link, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Do aparelho") },
                        icon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) }
                    )
                }

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = ytUrl,
                        onValueChange = { ytUrl = it },
                        label = { Text("Link ou ID do YouTube") },
                        placeholder = { Text("https://youtube.com/watch?v=...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "Disponível p/ vídeos locais: ${Formatters.formatBytes(storageSnapshot.remainingAllowedBytes)} (livre no aparelho: ${Formatters.formatBytes(storageSnapshot.freeDeviceBytes)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = KidsColors.Primary
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (playlists.isNotEmpty()) {
                    Box {
                        val chosenName = playlists.firstOrNull { it.playlist.id == selectedPlaylistId }?.playlist?.name
                            ?: "Nenhuma playlist (só no Feed)"
                        OutlinedButton(
                            onClick = { showPlDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Playlist: $chosenName")
                        }
                        DropdownMenu(
                            expanded = showPlDropdown,
                            onDismissRequest = { showPlDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nenhuma (só no Feed)") },
                                onClick = { selectedPlaylistId = null; showPlDropdown = false }
                            )
                            playlists.forEach { pwv ->
                                DropdownMenuItem(
                                    text = { Text(pwv.playlist.name) },
                                    onClick = { selectedPlaylistId = pwv.playlist.id; showPlDropdown = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                Button(
                    onClick = {
                        if (ytUrl.isNotBlank()) {
                            onAddYouTube(ytUrl.trim(), title.trim().ifBlank { null }, selectedPlaylistId)
                            onDismiss()
                        }
                    }
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Adicionar")
                }
            } else {
                Button(
                    onClick = {
                        onPickLocalVideos(title.trim().ifBlank { null }, selectedPlaylistId)
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Escolher vídeo(s)")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome da playlist") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim())
                        onDismiss()
                    }
                }
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
