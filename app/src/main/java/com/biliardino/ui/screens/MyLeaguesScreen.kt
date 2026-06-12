package com.biliardino.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.biliardino.model.LeagueResponse
import com.biliardino.model.MatchResponse
import com.biliardino.model.MyCompetitionResponse
import com.biliardino.ui.Screen
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState
import it.gestionecampionati.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLeaguesScreen(s: UiState, vm: AppViewModel) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showTypeSelection by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var selectedLeagueForJoin by remember { mutableStateOf<LeagueResponse?>(null) }
    
    LaunchedEffect(Unit) {
        vm.loadDashboardData()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = s.loading,
            onRefresh = { vm.loadDashboardData() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // 1. Header superiore
                item { DashboardHeader(vm) }

                // 2. Card principale di benvenuto
                item { WelcomeCard(s.currentUser?.name ?: "Utente") }

                // 3. Due card riepilogo affiancate
                item { SummaryRow(s.myUpcomingMatches.size, s.myCompetitions.size) }

                // 4. Sezione “Partite da giocare”
                if (s.myUpcomingMatches.isNotEmpty()) {
                    item { SectionHeader("Partite da giocare") }
                    items(s.myUpcomingMatches.take(3)) { match ->
                        UpcomingMatchCard(match) { vm.navigateTo(Screen.MatchDetail(match)) }
                    }
                }

                // 5. Sezione “I miei campionati”
                if (s.myCompetitions.isNotEmpty()) {
                    item { SectionHeader("I miei campionati") }
                    items(s.myCompetitions.take(3)) { competition ->
                        DashboardCompetitionCard(competition) { vm.selectMyCompetition(competition) }
                    }
                }

                // 6. Sezione “Circoli”
                item { SectionHeader("Circoli") }
                val myLeagueIds = s.myLeagues.map { it.id }.toSet()
                val otherLeagues = s.publicLeagues.filter { it.id !in myLeagueIds }
                
                if (s.myLeagues.isEmpty() && otherLeagues.isEmpty()) {
                    item { EmptyStateCard("Nessun circolo disponibile al momento.") }
                } else {
                    items(s.myLeagues) { league ->
                        ClubCard(league, context, isMember = true) { vm.selectLeague(league) }
                    }
                    items(otherLeagues) { league ->
                        ClubCard(league, context, isMember = false) { 
                            selectedLeagueForJoin = league
                            showJoinDialog = true
                        }
                    }
                }
            }
        }

        // Floating Action Button per la creazione
        Box(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd)
        ) {
            FloatingActionButton(
                onClick = { showMenu = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, "Opzioni")
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Crea Nuova") },
                    leadingIcon = { Icon(Icons.Default.Create, null) },
                    onClick = {
                        showMenu = false
                        showTypeSelection = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Unisciti con Codice") },
                    leadingIcon = { Icon(Icons.Default.VpnKey, null) },
                    onClick = {
                        showMenu = false
                        showJoinDialog = true
                    }
                )
            }
        }
    }

    // Dialogs
    if (showTypeSelection) {
        AlertDialog(
            onDismissRequest = { showTypeSelection = false },
            title = { Text("Cosa vuoi creare?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardCreationOptionCard(
                        title = "Lega privata",
                        description = "Ideale per gruppi di amici o competizioni private.",
                        icon = Icons.Default.Security,
                        onClick = {
                            vm.onNewLeagueTypeChange("PRIVATE_LEAGUE")
                            showTypeSelection = false
                            showCreateDialog = true
                        }
                    )
                    DashboardCreationOptionCard(
                        title = "Circolo / Club",
                        description = "Ideale per circoli sportivi e attività organizzate.",
                        icon = Icons.Default.Groups,
                        onClick = {
                            vm.onNewLeagueTypeChange("CLUB")
                            showTypeSelection = false
                            showCreateDialog = true
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTypeSelection = false }) { Text("Annulla") }
            }
        )
    }

    if (showCreateDialog) {
        val isClub = s.newLeagueType == "CLUB"
        val typeName = if (isClub) "Circolo" else "Lega"

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nuovo $typeName") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = s.newLeagueName,
                        onValueChange = vm::onNewLeagueNameChange,
                        label = { Text("Nome $typeName") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = s.newLeagueDescription,
                        onValueChange = vm::onNewLeagueDescriptionChange,
                        label = { Text("Descrizione") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (isClub) {
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Text("Localizzazione", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        
                        OutlinedTextField(
                            value = s.newLeagueAddress,
                            onValueChange = vm::onNewLeagueAddressChange,
                            label = { Text("Indirizzo") },
                            placeholder = { Text("es: Via Roma 1, Milano") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.createLeague()
                    showCreateDialog = false
                }) {
                    Text("Crea")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = {
                showJoinDialog = false
                selectedLeagueForJoin = null
                vm.onInviteCodeChange("")
            },
            title = { Text(selectedLeagueForJoin?.let { "Unisciti a ${it.name}" } ?: "Unisciti a una Lega") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Inserisci il codice di invito per unirti a questa lega.")
                    OutlinedTextField(
                        value = s.inviteCode,
                        onValueChange = vm::onInviteCodeChange,
                        label = { Text("Codice Invito") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.joinLeague()
                    showJoinDialog = false
                    selectedLeagueForJoin = null
                }) {
                    Text("Unisciti")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showJoinDialog = false
                    selectedLeagueForJoin = null
                    vm.onInviteCodeChange("")
                }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
fun DashboardHeader(vm: AppViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_app_logo),
            contentDescription = "Logo App",
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        
        Spacer(Modifier.width(12.dp))
        
        Text(
            text = "CampionatoCoppe",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.weight(1f))
        
        IconButton(onClick = { /* In futuro chat */ }) {
            Icon(Icons.Outlined.ChatBubbleOutline, "Messaggi")
        }
        
        Box {
            IconButton(onClick = { /* In futuro notifiche */ }) {
                Icon(Icons.Outlined.Notifications, "Notifiche")
            }
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
                color = MaterialTheme.colorScheme.error,
                shape = CircleShape
            ) {
                Text(
                    "3",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(Modifier.width(8.dp))
        
        Surface(
            modifier = Modifier
                .size(40.dp)
                .clickable { vm.navigateTo(Screen.Profile) },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Profilo",
                modifier = Modifier.padding(8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun WelcomeCard(name: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(140.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    "Ciao, $name!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Gestisci le tue competizioni",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun SummaryRow(matchesCount: Int, competitionsCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard(
            title = "Le tue partite\nda giocare",
            count = matchesCount.toString(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "I tuoi\ncampionati",
            count = competitionsCount.toString(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryCard(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(count, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, lineHeight = 16.sp)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
fun UpcomingMatchCard(match: MatchResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${match.teamAName ?: "TBD"} vs ${match.teamBName ?: "TBD"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    match.competitionName ?: "Competizione",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (match.leagueName != null) {
                    Text(
                        match.leagueName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "Stato: Da giocare",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun DashboardCompetitionCard(competition: MyCompetitionResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(competition.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${competition.leagueName} • ${competition.sportName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (competition.active) Color(0xFF4CAF50) else Color.Gray
                    Box(Modifier.size(8.dp).background(statusColor, CircleShape))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (competition.active) "Attiva" else "Chiusa",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun ClubCard(league: LeagueResponse, context: Context, isMember: Boolean, onClick: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column {
            if (!league.coverImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = league.coverImageUrl,
                    contentDescription = "Cover Circolo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (league.coverImageUrl.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                    }
                    
                    Column(Modifier.weight(1f)) {
                        Text(league.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (isMember) {
                            Text("Sei membro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("Tocca per unirti", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                
                if (!league.description.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        league.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isMember && !league.inviteCode.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Codice Invito",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    league.inviteCode,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp
                                )
                            }
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(league.inviteCode))
                            }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copia Codice",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                if (!league.address.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(4.dp))
                        Text(league.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { openDashboardNavigator(context, league.address) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Navigation, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("APRI NAVIGATORE")
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCreationOptionCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                Icon(icon, null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            message,
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun openDashboardNavigator(context: Context, address: String) {
    val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
