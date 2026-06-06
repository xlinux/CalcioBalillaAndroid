package com.biliardino.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.biliardino.model.CreateSeasonRequest
import com.biliardino.model.LeagueMemberResponse
import com.biliardino.model.LeagueResponse
import com.biliardino.model.SeasonResponse
import com.biliardino.util.DateUtils
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState
import java.time.LocalDate

@Composable
fun LeagueSeasonsScreen(league: LeagueResponse, s: UiState, vm: AppViewModel) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showCloseLeagueDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val userRole = s.currentUserRoleInLeague
    val isAdmin = userRole == "ADMIN"
    val isOwner = userRole == "OWNER"
    val isAdminOrOwner = isAdmin || isOwner
    val isLeagueActive = league.status == "ACTIVE" || league.status == null
    
    val isClub = league.leagueType == "CLUB"
    val typeName = if (isClub) "Circolo" else "Lega"

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { vm.uploadLeagueCover(league.id, it) }
    }

    val currentLeague = s.currentLeague ?: league

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            vm.loadLeagueComments(league.id)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            divider = {}
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Stagioni", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Chat", fontWeight = FontWeight.Bold) })
            if (isAdminOrOwner) {
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Membri", fontWeight = FontWeight.Bold) })
            }
            Tab(selected = selectedTab == (if (isAdminOrOwner) 3 else 2), onClick = { selectedTab = if (isAdminOrOwner) 3 else 2 }, text = { Text("Copertina", fontWeight = FontWeight.Bold) })
        }

        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> SeasonsTab(currentLeague, s, vm)
                1 -> LeagueChatTab(league, s, vm)
                2 -> if (isAdminOrOwner) MembersTab(league, s, vm) else CoverTab(currentLeague, isOwner, onUpload = { launcher.launch("image/*") }, onDelete = { vm.deleteLeagueCover(league.id) })
                3 -> if (isAdminOrOwner) CoverTab(currentLeague, isOwner, onUpload = { launcher.launch("image/*") }, onDelete = { vm.deleteLeagueCover(league.id) })
            }
        }

        if (isAdminOrOwner && isLeagueActive && selectedTab != 1) {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showCloseLeagueDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Chiudi $typeName")
                    }

                    if (selectedTab == 0) {
                        FloatingActionButton(
                            onClick = { showCreateDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) { Icon(Icons.Default.Add, "Crea Stagione") }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateSeasonDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { request ->
                vm.createSeason(league.id, request)
                showCreateDialog = false
            }
        )
    }

    if (showCloseLeagueDialog) {
        AlertDialog(
            onDismissRequest = { showCloseLeagueDialog = false },
            title = { Text("Chiudi $typeName") },
            text = { Text("Sei sicuro di voler chiudere definitivamente questo $typeName? Questa operazione non può essere annullata.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.closeLeague(league.id)
                        showCloseLeagueDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Chiudi") }
            },
            dismissButton = {
                TextButton(onClick = { showCloseLeagueDialog = false }) { Text("Annulla") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueChatTab(league: LeagueResponse, s: UiState, vm: AppViewModel) {
    var messageText by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(s.leagueComments.size) {
        if (s.leagueComments.isNotEmpty()) {
            listState.animateScrollToItem(s.leagueComments.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = s.loading,
            onRefresh = { vm.loadLeagueComments(league.id) },
            modifier = Modifier.weight(1f)
        ) {
            if (s.leagueComments.isEmpty() && !s.loading) {
                val typeName = if (league.leagueType == "CLUB") "circolo" else "lega"
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun messaggio in questo $typeName. Inizia tu!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val sortedComments = remember(s.leagueComments) {
                    s.leagueComments.sortedBy { it.createdAt }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(sortedComments, key = { _, c -> c.id }) { _, comment ->
                        val isMe = comment.userId == s.currentUser?.userId
                        CommentBubble(comment.username, comment.message, comment.createdAt, isMe)
                    }
                }
            }
        }

        Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp).imePadding(), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { if (it.length <= 1000) messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Scrivi un messaggio...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            vm.addLeagueComment(league.id, messageText)
                            messageText = ""
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Invia", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun SeasonsTab(league: LeagueResponse, s: UiState, vm: AppViewModel) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // League Cover Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = androidx.compose.ui.graphics.RectangleShape
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (!league.coverImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = league.coverImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient overlay for better contrast
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                    )
                                )
                        )
                        Text(
                            text = league.name,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text(league.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (s.seasons.isEmpty() && !s.loading) {
            item {
                Box(Modifier.padding(16.dp)) {
                    EmptyStateBox(league.name, s.currentUserRoleInLeague)
                }
            }
        } else {
            items(s.seasons) { season ->
                val userRole = s.currentUserRoleInLeague
                Box(Modifier.padding(horizontal = 16.dp)) {
                    SeasonCard(
                        season = season,
                        isAdmin = userRole == "ADMIN" || userRole == "OWNER",
                        onSelect = { vm.selectSeason(league, season) },
                        onClose = { vm.closeSeason(league, season) }
                    )
                }
            }
        }
    }
}

@Composable
fun CoverTab(league: LeagueResponse, isOwner: Boolean, onUpload: () -> Unit, onDelete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Immagine di Copertina",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(Modifier.fillMaxSize()) {
                if (!league.coverImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = league.coverImageUrl,
                        contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Image, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        Text("Nessuna immagine impostata", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (isOwner) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Gestione Immagine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "In qualità di proprietario, puoi caricare una nuova immagine che verrà usata come copertina.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onUpload,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Cambia Foto")
                        }
                        
                        if (!league.coverImageUrl.isNullOrBlank()) {
                            Button(
                                onClick = onDelete,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Delete, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Rimuovi")
                            }
                        }
                    }
                }
            }
        } else {
            val typeName = if (league.leagueType == "CLUB") "circolo" else "lega"
            Text(
                "Questa immagine viene visualizzata come copertina da tutti i membri del $typeName. Solo il proprietario può modificarla.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun EmptyStateBox(leagueName: String, userRole: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp).fillMaxSize(),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Benvenuto in $leagueName!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Qui iniziano le tue sfide. Crea la tua prima stagione per gestire tornei, classifiche e statistiche.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val userRoleActual = userRole
            if (userRoleActual == "ADMIN" || userRoleActual == "OWNER") {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Tocca il pulsante '+' in basso per iniziare!",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SeasonCard(season: SeasonResponse, isAdmin: Boolean, onSelect: () -> Unit, onClose: () -> Unit) {
    var showCloseConfirm by remember { mutableStateOf(false) }
    val isActive = season.active == true

    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = season.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${DateUtils.formatDate(season.startDate)} - ${DateUtils.formatDate(season.endDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                StatusBadge(isActive)
            }

            if (isAdmin && isActive) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showCloseConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Concludi Stagione")
                }
            }
        }
    }

    if (showCloseConfirm) {
        AlertDialog(
            onDismissRequest = { showCloseConfirm = false },
            title = { Text("Concludere la Stagione?") },
            text = { Text("L'archiviazione della stagione bloccherà l'inserimento di nuove partite. Questa azione è definitiva.") },
            confirmButton = {
                Button(
                    onClick = { 
                        onClose()
                        showCloseConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sì, Concludi") }
            },
            dismissButton = {
                TextButton(onClick = { showCloseConfirm = false }) { Text("Annulla") }
            }
        )
    }
}

@Composable
fun StatusBadge(isActive: Boolean) {
    val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    
    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = if (isActive) "ATTIVA" else "CONCLUSA",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun MembersTab(league: LeagueResponse, s: UiState, vm: AppViewModel) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(s.leagueMembers) { member ->
            val userRole = s.currentUserRoleInLeague
            MemberItem(member, userRole, onUpdateRole = { role ->
                vm.updateMemberRole(league.id, member.userId, role)
            })
        }
    }
}

@Composable
fun MemberItem(member: LeagueMemberResponse, currentUserRole: String?, onUpdateRole: (String) -> Unit) {
    var showUpdateConfirm by remember { mutableStateOf<String?>(null) }
    val canManage = currentUserRole == "ADMIN" || currentUserRole == "OWNER"

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(member.username, fontWeight = FontWeight.Bold) },
            supportingContent = { Text(member.email ?: "") },
            leadingContent = { 
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Person, 
                        contentDescription = null, 
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (member.role) {
                        "OWNER" -> {
                            AssistChip(
                                onClick = {},
                                label = { Text("OWNER") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                        "ADMIN" -> {
                            AssistChip(
                                onClick = {},
                                label = { Text("ADMIN") },
                                leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.secondary)
                            )
                            if (canManage) {
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = { showUpdateConfirm = "PLAYER" }) {
                                    Text("Rendi PLAYER")
                                }
                            }
                        }
                        else -> { // PLAYER
                            if (canManage) {
                                TextButton(onClick = { showUpdateConfirm = "ADMIN" }) {
                                    Text("Rendi ADMIN")
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    if (showUpdateConfirm != null) {
        val targetRole = showUpdateConfirm!!
        AlertDialog(
            onDismissRequest = { showUpdateConfirm = null },
            title = { Text(if (targetRole == "ADMIN") "Promuovi ad Admin" else "Rendi Player") },
            text = { 
                Text(
                    if (targetRole == "ADMIN") 
                        "Vuoi rendere l'utente ${member.username} un amministratore della lega? Potrà creare stagioni, competizioni e gestire altri membri."
                    else 
                        "Vuoi rimuovere i privilegi di amministratore a ${member.username}? Tornerà ad essere un semplice giocatore."
                ) 
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateRole(targetRole)
                    showUpdateConfirm = null
                }) { Text("Conferma") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateConfirm = null }) { Text("Annulla") }
            }
        )
    }
}

@Composable
fun CreateSeasonDialog(onDismiss: () -> Unit, onConfirm: (CreateSeasonRequest) -> Unit) {
    var name by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(DateUtils.now()) }
    var endDate by remember { mutableStateOf(DateUtils.monthsFromNow(12)) }
    var copyTeams by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova Stagione", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Stagione") },
                    placeholder = { Text("es: Stagione 2025/2026") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Data Inizio (GG/MM/AAAA)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("Data Fine (GG/MM/AAAA)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = copyTeams, onCheckedChange = { copyTeams = it }, modifier = Modifier.scale(0.8f))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Copia Squadre", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Importa le squadre della stagione precedente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        CreateSeasonRequest(
                            name = name,
                            startDate = DateUtils.toIsoDate(startDate),
                            endDate = DateUtils.toIsoDate(endDate),
                            copyTeamsFromPreviousSeason = copyTeams
                        )
                    )
                },
                enabled = name.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank(),
                shape = MaterialTheme.shapes.large
            ) { Text("Crea Stagione") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
