package com.kidstv.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kidstv.app.data.db.ChannelPresets
import com.kidstv.app.data.db.PlaylistEntity
import com.kidstv.app.data.db.PlaylistWithVideos
import com.kidstv.app.data.db.VideoEntity
import com.kidstv.app.ui.theme.KidsColors
import com.kidstv.app.util.Formatters

fun channelIconVector(iconKey: String): ImageVector = when (iconKey) {
    "star" -> Icons.Rounded.Star
    "rocket" -> Icons.Rounded.RocketLaunch
    "music" -> Icons.Rounded.MusicNote
    "pets" -> Icons.Rounded.Pets
    "book" -> Icons.AutoMirrored.Rounded.MenuBook
    "palette" -> Icons.Rounded.Palette
    "science" -> Icons.Rounded.Science
    "sports" -> Icons.Rounded.SportsSoccer
    "bedtime" -> Icons.Rounded.Bedtime
    else -> Icons.Rounded.Tv
}

fun formatChannelNumber(num: Int): String = num.toString().padStart(2, '0')

@Composable
fun ChannelLogoBadge(
    channelNumber: Int,
    iconKey: String,
    colorHex: Long,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val accent = Color(colorHex)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = channelIconVector(iconKey),
            contentDescription = "Canal $channelNumber",
            tint = accent,
            modifier = Modifier.size(size * 0.56f)
        )
        Surface(
            color = accent,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
        ) {
            Text(
                text = formatChannelNumber(channelNumber),
                style = MaterialTheme.typography.labelMedium,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChannelEditorDialog(
    existing: PlaylistEntity? = null,
    suggestedChannelNumber: Int = 1,
    onDismiss: () -> Unit,
    onSave: (name: String, channelNumber: Int, iconKey: String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var channelText by remember {
        mutableStateOf((existing?.channelNumber ?: suggestedChannelNumber).toString())
    }
    var selectedIconKey by remember { mutableStateOf(existing?.iconKey ?: "tv") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Novo Canal / Playlist" else "Editar Canal") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Nome do canal") },
                    placeholder = { Text("Ex.: Desenhos, Músicas, Inglês") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = channelText,
                    onValueChange = { input ->
                        channelText = input.filter { it.isDigit() }.take(3)
                        error = null
                    },
                    label = { Text("Número do canal (ID)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Ícone / Logo do canal", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChannelPresets.ICON_OPTIONS.forEach { opt ->
                        val selected = selectedIconKey == opt.key
                        val accent = Color(opt.defaultColorHex)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) accent.copy(alpha = 0.25f) else KidsColors.SurfaceVariant,
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) accent else KidsColors.Border
                            ),
                            modifier = Modifier.clickable { selectedIconKey = opt.key }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = channelIconVector(opt.key),
                                    contentDescription = opt.label,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(opt.label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                if (error != null) {
                    Text(error!!, color = KidsColors.Danger, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val num = channelText.toIntOrNull()
                    if (cleanName.isEmpty()) {
                        error = "Informe o nome do canal."
                        return@Button
                    }
                    if (num == null || num <= 0) {
                        error = "Informe um número de canal válido (maior que 0)."
                        return@Button
                    }
                    onSave(cleanName, num, selectedIconKey)
                    onDismiss()
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun ChannelNumberJumpDialog(
    channels: List<PlaylistWithVideos>,
    onDismiss: () -> Unit,
    onTuneNumber: (Int) -> Unit
) {
    var typed by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Dialpad, contentDescription = null, tint = KidsColors.Primary) },
        title = { Text("Ir para Canal (ID)") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Número do canal") },
                    placeholder = { Text("Ex.: 1, 2, 10") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (channels.isNotEmpty()) {
                    Text("Canais disponíveis:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(channels) { _, ch ->
                            OutlinedButton(
                                onClick = {
                                    onTuneNumber(ch.playlist.channelNumber)
                                    onDismiss()
                                }
                            ) {
                                Text("CH ${formatChannelNumber(ch.playlist.channelNumber)} • ${ch.playlist.name}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = typed.toIntOrNull()
                    if (num != null && num > 0) {
                        onTuneNumber(num)
                        onDismiss()
                    }
                }
            ) {
                Text("Sintonizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun ChannelZapperBar(
    activeChannel: PlaylistWithVideos?,
    resumedFromMs: Long,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onOpenDialpad: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = KidsColors.SurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, KidsColors.Border),
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            val isCompact = maxWidth < 400.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (activeChannel != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        ChannelLogoBadge(
                            channelNumber = activeChannel.playlist.channelNumber,
                            iconKey = activeChannel.playlist.iconKey,
                            colorHex = activeChannel.playlist.colorHex,
                            size = if (isCompact) 36.dp else 40.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CH ${formatChannelNumber(activeChannel.playlist.channelNumber)} • ${activeChannel.playlist.name}",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (resumedFromMs > 1000L) {
                                    "De ${Formatters.formatDuration(resumedFromMs)}"
                                } else {
                                    "${activeChannel.videos.size} na fila"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = KidsColors.Primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Modo Canais TV",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.width(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPreviousChannel,
                        contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Canal anterior", modifier = Modifier.size(16.dp))
                        if (!isCompact) Text("CH-", maxLines = 1, softWrap = false)
                    }
                    IconButton(onClick = onOpenDialpad, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Rounded.Dialpad, contentDescription = "Digitar canal", tint = KidsColors.Primary)
                    }
                    OutlinedButton(
                        onClick = onNextChannel,
                        contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 10.dp, vertical = 6.dp)
                    ) {
                        if (!isCompact) Text("CH+", maxLines = 1, softWrap = false)
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Próximo canal", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CableTvGuideContent(
    channels: List<PlaylistWithVideos>,
    activeChannelId: Long?,
    canManage: Boolean,
    onTuneChannel: (PlaylistWithVideos) -> Unit,
    onTuneProgram: (PlaylistWithVideos, VideoEntity) -> Unit,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onOpenDialpad: () -> Unit,
    onCreateChannel: () -> Unit,
    onEditChannel: (PlaylistEntity) -> Unit,
    onMoveProgram: (playlistId: Long, videoId: Long, moveUp: Boolean) -> Unit,
    onRemoveProgram: (playlistId: Long, videoId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeChannel = channels.firstOrNull { it.playlist.id == activeChannelId }

    Column(modifier = modifier) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text("Guia de Canais (TV a Cabo)", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Cada canal retoma exatamente do ponto onde você parou.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenDialpad) {
                    Icon(Icons.Rounded.Dialpad, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nº Canal", maxLines = 1, softWrap = false)
                }
                if (canManage) {
                    Button(onClick = onCreateChannel) {
                        Text("+ Canal", maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        if (channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Tv, contentDescription = null, modifier = Modifier.size(52.dp), tint = KidsColors.Primary)
                    Text("Nenhum canal criado", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Crie playlists com número de canal e logo para assistir no estilo TV a cabo.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (canManage) {
                        Button(onClick = onCreateChannel) { Text("Criar primeiro canal") }
                    }
                }
            }
            return
        }

        if (activeChannel != null) {
            ChannelZapperBar(
                activeChannel = activeChannel,
                resumedFromMs = activeChannel.playlist.lastPositionMs,
                onPreviousChannel = onPreviousChannel,
                onNextChannel = onNextChannel,
                onOpenDialpad = onOpenDialpad,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(channels, key = { _, item -> item.playlist.id }) { _, pwv ->
                val isCurrentChannel = pwv.playlist.id == activeChannelId
                val pausedVideo = pwv.playlist.lastVideoId?.let { id ->
                    pwv.videos.firstOrNull { it.id == id }
                } ?: pwv.videos.firstOrNull()
                val pausedAtMs = if (pwv.playlist.lastVideoId == pausedVideo?.id) {
                    pwv.playlist.lastPositionMs
                } else {
                    0L
                }
                var focused by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused }
                        .focusable(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = when {
                            focused -> 2.5.dp
                            isCurrentChannel -> 2.dp
                            else -> 1.dp
                        },
                        color = when {
                            focused -> KidsColors.Primary
                            isCurrentChannel -> KidsColors.Secondary
                            else -> KidsColors.Border
                        }
                    ),
                    colors = CardDefaults.cardColors(containerColor = KidsColors.Surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val wideCard = maxWidth >= 420.dp
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onTuneChannel(pwv) }
                                    ) {
                                        ChannelLogoBadge(
                                            channelNumber = pwv.playlist.channelNumber,
                                            iconKey = pwv.playlist.iconKey,
                                            colorHex = pwv.playlist.colorHex,
                                            size = 46.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "CH ${formatChannelNumber(pwv.playlist.channelNumber)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color(pwv.playlist.colorHex),
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                Text(
                                                    text = pwv.playlist.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            val statusText = when {
                                                pwv.videos.isEmpty() -> "Sem programação ainda"
                                                isCurrentChannel -> "Sintonizado: ${pausedVideo?.title.orEmpty()}"
                                                pausedAtMs > 1000L -> "Parado em: ${pausedVideo?.title.orEmpty()} (${Formatters.formatDuration(pausedAtMs)})"
                                                else -> "Próximo: ${pausedVideo?.title.orEmpty()}"
                                            }
                                            Text(
                                                text = statusText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isCurrentChannel) KidsColors.Secondary else KidsColors.TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (canManage) {
                                            IconButton(onClick = { onEditChannel(pwv.playlist) }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Edit,
                                                    contentDescription = "Editar canal",
                                                    tint = KidsColors.TextSecondary
                                                )
                                            }
                                        }
                                        if (wideCard) {
                                            Button(
                                                onClick = { onTuneChannel(pwv) },
                                                enabled = pwv.videos.isNotEmpty(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isCurrentChannel) KidsColors.Secondary else KidsColors.Primary,
                                                    contentColor = Color(0xFF041E2E)
                                                )
                                            ) {
                                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(if (pausedAtMs > 1000L) "Continuar" else "Assistir", maxLines = 1, softWrap = false)
                                            }
                                        }
                                    }
                                }

                                if (!wideCard) {
                                    Button(
                                        onClick = { onTuneChannel(pwv) },
                                        enabled = pwv.videos.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isCurrentChannel) KidsColors.Secondary else KidsColors.Primary,
                                            contentColor = Color(0xFF041E2E)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (pausedAtMs > 1000L) {
                                                "Continuar de ${Formatters.formatDuration(pausedAtMs)}"
                                            } else {
                                                "Assistir canal"
                                            },
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // Programming Schedule in exact order
                        if (pwv.videos.isNotEmpty()) {
                            Text(
                                text = "Programação do canal (${pwv.videos.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                color = KidsColors.TextSecondary
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(pwv.videos, key = { _, v -> "${pwv.playlist.id}_${v.id}" }) { index, program ->
                                    val isCurrentProgram = pausedVideo?.id == program.id
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCurrentProgram) KidsColors.PrimaryContainer else KidsColors.SurfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isCurrentProgram) KidsColors.Primary else KidsColors.Border
                                        ),
                                        modifier = Modifier
                                            .width(210.dp)
                                            .clickable { onTuneProgram(pwv, program) }
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isCurrentProgram && pausedAtMs > 1000L) {
                                                        "#${index + 1} • ${Formatters.formatDuration(pausedAtMs)}"
                                                    } else {
                                                        "#${index + 1} na fila"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (isCurrentProgram) KidsColors.Primary else KidsColors.TextSecondary
                                                )
                                                if (canManage) {
                                                    Row {
                                                        if (index > 0) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.ArrowUpward,
                                                                contentDescription = "Subir na ordem",
                                                                modifier = Modifier
                                                                    .size(16.dp)
                                                                    .clickable { onMoveProgram(pwv.playlist.id, program.id, true) },
                                                                tint = KidsColors.TextSecondary
                                                            )
                                                            Spacer(Modifier.width(4.dp))
                                                        }
                                                        if (index < pwv.videos.lastIndex) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.ArrowDownward,
                                                                contentDescription = "Descer na ordem",
                                                                modifier = Modifier
                                                                    .size(16.dp)
                                                                    .clickable { onMoveProgram(pwv.playlist.id, program.id, false) },
                                                                tint = KidsColors.TextSecondary
                                                            )
                                                            Spacer(Modifier.width(4.dp))
                                                        }
                                                        Icon(
                                                            imageVector = Icons.Rounded.Close,
                                                            contentDescription = "Remover do canal",
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clickable { onRemoveProgram(pwv.playlist.id, program.id) },
                                                            tint = KidsColors.Danger
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = program.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
