package com.corsairlabs.receiptvault

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Teal = Color(0xFF17A99A)
private val TealDark = Color(0xFF0F776D)
private val Ink = Color(0xFF13202B)
private val Muted = Color(0xFF697683)
private val Soft = Color(0xFFF4F7F8)
private val Amber = Color(0xFFF4A62A)
private val Coral = Color(0xFFEF6959)
private val VaultBlue = Color(0xFF4367DC)

class MainActivity : ComponentActivity() {
    private var sharedUris by mutableStateOf<List<Uri>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedUris = extractSharedImageUris(intent)
        setContent {
            ReceiptVaultRoot(sharedUris) {
                sharedUris = emptyList()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sharedUris = extractSharedImageUris(intent)
    }
}

@Composable
private fun ReceiptVaultRoot(
    sharedUris: List<Uri>,
    onSharedUrisConsumed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: ReceiptVaultViewModel = viewModel(
        factory = ReceiptVaultViewModel.factory(context.applicationContext as Application)
    )
    var screen by rememberSaveable { mutableStateOf(AppScreen.Home) }
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) {
            cameraUri?.let { uri ->
                scope.launch {
                    viewModel.importReceipt(uri, ImportSource.Camera)
                    screen = AppScreen.Detail
                }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.importReceipt(uri, ImportSource.Image)
                screen = AppScreen.Detail
            }
        }
    }

    LaunchedEffect(sharedUris) {
        if (sharedUris.isNotEmpty()) {
            sharedUris.forEach { uri ->
                viewModel.importReceipt(uri, ImportSource.EmailShare)
            }
            screen = AppScreen.Detail
            onSharedUrisConsumed()
        }
    }

    ReceiptVaultTheme {
        ReceiptVaultApp(
            viewModel = viewModel,
            currentScreen = screen,
            onScreenChange = { screen = it },
            onScan = {
                val uri = createCameraUri(context)
                cameraUri = uri
                cameraLauncher.launch(uri)
            },
            onPickImage = { imagePicker.launch("image/*") }
        )
    }
}

@Composable
private fun ReceiptVaultApp(
    viewModel: ReceiptVaultViewModel,
    currentScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    onScan: () -> Unit,
    onPickImage: () -> Unit
) {
    val receipts by viewModel.receipts.collectAsState()
    val emailAccounts by viewModel.emailAccounts.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val message by viewModel.message.collectAsState()
    val pendingExternalUrl by viewModel.pendingExternalUrl.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(pendingExternalUrl) {
        val url = pendingExternalUrl
        if (!url.isNullOrBlank()) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            viewModel.clearPendingExternalUrl()
        }
    }

    Scaffold(
        containerColor = Soft,
        topBar = {
            AppTopBar(
                currentScreen = currentScreen,
                onBack = { onScreenChange(AppScreen.Home) }
            )
        },
        bottomBar = {
            Surface(color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem(AppScreen.Home, currentScreen, onScreenChange)
                    NavItem(AppScreen.Search, currentScreen, onScreenChange)
                    NavItem(AppScreen.Scan, currentScreen, onScreenChange)
                    NavItem(AppScreen.Email, currentScreen, onScreenChange)
                    NavItem(AppScreen.Warranties, currentScreen, onScreenChange)
                    NavItem(AppScreen.Plus, currentScreen, onScreenChange)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentScreen) {
                AppScreen.Home -> HomeScreen(
                    receipts = receipts,
                    onScan = onScan,
                    onPickImage = onPickImage,
                    onSelect = {
                        viewModel.selectReceipt(it.id)
                        onScreenChange(AppScreen.Detail)
                    },
                    onSearch = { onScreenChange(AppScreen.Search) },
                    onEmail = { onScreenChange(AppScreen.Email) },
                    onWarranties = { onScreenChange(AppScreen.Warranties) }
                )

                AppScreen.Scan -> ImportScreen(
                    onScan = onScan,
                    onPickImage = onPickImage,
                    onEmailAccounts = { onScreenChange(AppScreen.Email) }
                )

                AppScreen.Search -> SearchScreen(
                    receipts = receipts,
                    onSelect = {
                        viewModel.selectReceipt(it.id)
                        onScreenChange(AppScreen.Detail)
                    }
                )

                AppScreen.Warranties -> WarrantyScreen(receipts)
                AppScreen.Email -> EmailConnectorsScreen(
                    accounts = emailAccounts,
                    plan = viewModel.activePlan,
                    onConnect = viewModel::connectEmailProvider,
                    onConnectImap = viewModel::connectManualImap,
                    onSync = viewModel::syncEmailAccount,
                    onDisconnect = viewModel::disconnectEmailAccount,
                    onDeleteData = viewModel::deleteEmailAccountData
                )
                AppScreen.Plus -> PlusScreen()
                AppScreen.Detail -> ReceiptDetailScreen(
                    receipt = viewModel.selectedReceipt ?: receipts.firstOrNull(),
                    onBack = { onScreenChange(AppScreen.Home) }
                )
            }

            if (isBusy) {
                BusyOverlay()
            }

            if (message.isNotBlank()) {
                MessageBar(message) {
                    viewModel.clearMessage()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(currentScreen: AppScreen, onBack: () -> Unit) {
    TopAppBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Soft,
            titleContentColor = Ink
        ),
        title = {
            Column {
                if (currentScreen == AppScreen.Home) {
                    Text("ReceiptVault", fontWeight = FontWeight.ExtraBold)
                    Text("Scan once. Search forever.", color = Muted, style = MaterialTheme.typography.labelMedium)
                } else {
                    Text(currentScreen.title, fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        navigationIcon = {
            if (currentScreen != AppScreen.Home) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            if (currentScreen == AppScreen.Home) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
            }
        }
    )
}

@Composable
private fun HomeScreen(
    receipts: List<Receipt>,
    onScan: () -> Unit,
    onPickImage: () -> Unit,
    onSelect: (Receipt) -> Unit,
    onSearch: () -> Unit,
    onEmail: () -> Unit,
    onWarranties: () -> Unit
) {
    val total = receipts.sumOf { it.amountCents }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            VaultHero(total, receipts.size, onScan)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(
                    modifier = Modifier.weight(1f),
                    title = "Upload image",
                    detail = "Gallery or files",
                    icon = { Icon(Icons.Default.Image, contentDescription = null) },
                    onClick = onPickImage
                )
                QuickAction(
                    modifier = Modifier.weight(1f),
                    title = "Email import",
                    detail = "Auto or share",
                    icon = { Icon(Icons.Default.Email, contentDescription = null) },
                    onClick = onEmail
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Returns open",
                    value = receipts.count { it.returnByMillis != null && it.returnByMillis >= todayMillis() }.toString(),
                    accent = Teal
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Warranties",
                    value = receipts.count { it.warrantyUntilMillis != null }.toString(),
                    accent = Amber
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "This month",
                    value = formatCurrency(total),
                    accent = Coral
                )
            }
        }
        item {
            SectionHeader("Recent receipts", "Search", onSearch)
        }
        if (receipts.isEmpty()) {
            item {
                EmptyState(onScan, onPickImage)
            }
        } else {
            items(receipts.take(8), key = { it.id }) { receipt ->
                ReceiptRow(receipt, onClick = { onSelect(receipt) })
            }
        }
        item {
            SectionHeader("Protected purchases", "View", onWarranties)
        }
        items(receipts.filter { it.warrantyUntilMillis != null }.take(3), key = { "warranty-${it.id}" }) { receipt ->
            WarrantyMiniRow(receipt)
        }
    }
}

@Composable
private fun VaultHero(total: Long, count: Int, onScan: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF173943)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Total tracked", color = Color.White.copy(alpha = 0.72f))
                Text(
                    formatCurrency(total),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text("$count receipts saved", color = Color.White.copy(alpha = 0.72f))
            }
            Button(
                onClick = onScan,
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(68.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Scan receipt")
            }
        }
    }
}

@Composable
private fun ImportScreen(onScan: () -> Unit, onPickImage: () -> Unit, onEmailAccounts: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Add receipts from your camera, gallery, files, or an email attachment shared from Gmail or Outlook.",
                color = Muted
            )
        }
        item {
            ImportCard(
                title = "Scan a receipt",
                detail = "Take a photo and let ReceiptVault read store, date, and total.",
                icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                button = "Open camera",
                onClick = onScan
            )
        }
        item {
            ImportCard(
                title = "Upload image",
                detail = "Choose a receipt photo or screenshot from your phone.",
                icon = { Icon(Icons.Default.Image, contentDescription = null) },
                button = "Choose image",
                onClick = onPickImage
            )
        }
        item {
            ImportCard(
                title = "Import from email",
                detail = "Share an attachment now, or connect email accounts for receipt-only automatic imports.",
                icon = { Icon(Icons.Default.Email, contentDescription = null) },
                button = "Email accounts",
                onClick = onEmailAccounts
            )
        }
    }
}

@Composable
private fun SearchScreen(receipts: List<Receipt>, onSelect: (Receipt) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, receipts) {
        receipts.filter { receipt ->
            val haystack = listOf(
                receipt.merchant,
                receipt.category,
                receipt.location,
                receipt.notes,
                receipt.rawText
            ).joinToString(" ").lowercase(Locale.US)
            haystack.contains(query.lowercase(Locale.US))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search store, item, date, category") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { query = "" }, label = { Text("All") })
                AssistChip(onClick = { query = "warranty" }, label = { Text("Warranty") })
                AssistChip(onClick = { query = "business" }, label = { Text("Business") })
            }
        }
        if (filtered.isEmpty()) {
            item {
                EmptySearchState()
            }
        } else {
            items(filtered, key = { it.id }) { receipt ->
                ReceiptRow(receipt, onClick = { onSelect(receipt) })
            }
        }
    }
}

@Composable
private fun WarrantyScreen(receipts: List<Receipt>) {
    val warranties = receipts.filter { it.warrantyUntilMillis != null || it.returnByMillis != null }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4A5F33)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Coverage value", color = Color.White.copy(alpha = 0.72f))
                        Text(
                            formatCurrency(warranties.sumOf { it.amountCents }),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFD8F6AF), modifier = Modifier.size(42.dp))
                }
            }
        }
        if (warranties.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "Warranties and return dates will appear here after you scan important purchases.",
                        modifier = Modifier.padding(18.dp),
                        color = Muted
                    )
                }
            }
        } else {
            items(warranties, key = { it.id }) { receipt ->
                WarrantyRow(receipt)
            }
        }
    }
}

@Composable
private fun EmailConnectorsScreen(
    accounts: List<EmailConnectorAccount>,
    plan: ReceiptVaultPlan,
    onConnect: (EmailProvider) -> Unit,
    onConnectImap: (String, String, String, String, String, Boolean) -> Unit,
    onSync: (String) -> Unit,
    onDisconnect: (String) -> Unit,
    onDeleteData: (String) -> Unit
) {
    var imapEmail by rememberSaveable { mutableStateOf("") }
    var imapHost by rememberSaveable { mutableStateOf("") }
    var imapPort by rememberSaveable { mutableStateOf("993") }
    var imapUsername by rememberSaveable { mutableStateOf("") }
    var imapPassword by rememberSaveable { mutableStateOf("") }
    var imapUseTls by rememberSaveable { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF244653)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Email receipt import", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${plan.label}: ${accounts.count { it.status != ConnectorStatus.Disconnected }}/${plan.maxEmailAccounts} accounts, ${plan.monthlyEmailImports} imports monthly",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Only receipt, order, invoice, return, and warranty messages are eligible for import.",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            SectionHeader("Connect accounts", "Plan limits", {})
        }

        items(EmailProvider.entries.filter { it != EmailProvider.Imap }, key = { it.name }) { provider ->
            ProviderConnectCard(provider, onConnect = { onConnect(provider) })
        }

        item {
            ManualImapConnectCard(
                emailAddress = imapEmail,
                host = imapHost,
                port = imapPort,
                username = imapUsername,
                password = imapPassword,
                useTls = imapUseTls,
                onEmailChange = { imapEmail = it },
                onHostChange = { imapHost = it },
                onPortChange = { imapPort = it.filter(Char::isDigit).take(5) },
                onUsernameChange = { imapUsername = it },
                onPasswordChange = { imapPassword = it },
                onUseTlsChange = { imapUseTls = it },
                onConnect = {
                    onConnectImap(imapEmail, imapHost, imapPort, imapUsername, imapPassword, imapUseTls)
                }
            )
        }

        item {
            SectionHeader("Connected accounts", "Delete data", {})
        }

        if (accounts.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Text(
                        "No email accounts connected yet. Add Gmail, Outlook, Yahoo, or IMAP to prepare automatic receipt imports.",
                        modifier = Modifier.padding(18.dp),
                        color = Muted
                    )
                }
            }
        } else {
            items(accounts, key = { it.id }) { account ->
                EmailAccountCard(
                    account = account,
                    onSync = { onSync(account.id) },
                    onDisconnect = { onDisconnect(account.id) },
                    onDeleteData = { onDeleteData(account.id) }
                )
            }
        }
    }
}

@Composable
private fun ProviderConnectCard(provider: EmailProvider, onConnect: () -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEAF8F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = TealDark)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(provider.label, fontWeight = FontWeight.ExtraBold)
                    Text(provider.scopeLabel, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Search: ${provider.receiptQuery}", color = Muted, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Connect ${provider.label}")
            }
        }
    }
}

@Composable
private fun ManualImapConnectCard(
    emailAddress: String,
    host: String,
    port: String,
    username: String,
    password: String,
    useTls: Boolean,
    onEmailChange: (String) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onUseTlsChange: (Boolean) -> Unit,
    onConnect: () -> Unit
) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEAF8F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = TealDark)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Other IMAP", fontWeight = FontWeight.ExtraBold)
                    Text("Encrypted server settings", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "Use an app password when your provider supports one.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = emailAddress,
                onValueChange = onEmailChange,
                label = { Text("Email address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                label = { Text("IMAP host") },
                placeholder = { Text("imap.example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("App password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Use TLS", fontWeight = FontWeight.Bold)
                Switch(checked = useTls, onCheckedChange = onUseTlsChange)
            }
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save IMAP settings")
            }
        }
    }
}

@Composable
private fun EmailAccountCard(
    account: EmailConnectorAccount,
    onSync: () -> Unit,
    onDisconnect: () -> Unit,
    onDeleteData: () -> Unit
) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MerchantMark(account.provider.label)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(account.emailAddress, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(account.provider.label, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    account.status.label,
                    color = connectorStatusColor(account.status),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "${account.monthlyImportCount}/${account.monthlyImportLimit} imports used this month",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
            Text(account.lastMessage, color = Muted, style = MaterialTheme.typography.bodySmall)
            Text(
                "Last sync: ${account.lastSyncMillis?.formatDate() ?: "Never"}",
                color = Muted,
                style = MaterialTheme.typography.labelSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSync, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text("Sync")
                }
                OutlinedButton(onClick = onDisconnect, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text("Disconnect")
                }
            }
            TextButton(onClick = onDeleteData) {
                Text("Delete connector data", color = Coral)
            }
        }
    }
}

private fun connectorStatusColor(status: ConnectorStatus): Color {
    return when (status) {
        ConnectorStatus.OAuthPending -> Amber
        ConnectorStatus.Ready -> TealDark
        ConnectorStatus.SyncReady -> TealDark
        ConnectorStatus.Disconnected -> Muted
    }
}

@Composable
private fun PlusScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF26374E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ReceiptVault Plus", color = Color.White.copy(alpha = 0.78f))
                    Text("${'$'}4.99", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                    Text("per month", color = Color.White.copy(alpha = 0.78f))
                    Text("${'$'}47.99 yearly", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { FeatureRow("1,000 stored receipts", Icons.Default.CheckCircle) }
        item { FeatureRow("Cloud backup with R2", Icons.Default.Shield) }
        item { FeatureRow("Gemini receipt categorization", Icons.Default.Star) }
        item { FeatureRow("3 connected email accounts", Icons.Default.Email) }
        item { FeatureRow("250 receipt email imports monthly", Icons.Default.Email) }
        item { FeatureRow("Unlimited warranty tracking", Icons.Default.Shield) }
        item { FeatureRow("Return and warranty reminders", Icons.Default.DateRange) }
        item { FeatureRow("CSV and PDF exports", Icons.Default.CheckCircle) }
        item {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Connect Google Play Billing")
            }
        }
        item {
            Text(
                "Billing is intentionally stubbed for the first app build. The next step is adding Google Play Billing product IDs after the Play Console app exists.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ReceiptDetailScreen(receipt: Receipt?, onBack: () -> Unit) {
    if (receipt == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No receipt selected", fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) { Text("Back home") }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MerchantMark(receipt.merchant)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(receipt.merchant, color = Muted, fontWeight = FontWeight.Bold)
                    Text(formatCurrency(receipt.amountCents), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                }
                AssistChip(onClick = {}, label = { Text(receipt.source.label) })
            }
        }
        item {
            ReceiptImage(receipt.imagePath)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailTile(Modifier.weight(1f), "Purchased", receipt.purchaseDateLabel, Icons.Default.DateRange)
                DetailTile(Modifier.weight(1f), "Category", receipt.category, Icons.Default.Info)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailTile(Modifier.weight(1f), "Return by", receipt.returnByLabel, Icons.Default.CheckCircle)
                DetailTile(Modifier.weight(1f), "Warranty", receipt.warrantyLabel, Icons.Default.Shield)
            }
        }
        item {
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("OCR text", fontWeight = FontWeight.ExtraBold)
                    Text(
                        receipt.rawText.ifBlank { "No text detected. The receipt image is still saved." },
                        color = Muted,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier = Modifier,
    title: String,
    detail: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEAF8F6)),
                contentAlignment = Alignment.Center
            ) {
                Surface(color = Color.Transparent, contentColor = TealDark) {
                    icon()
                }
            }
            Text(title, fontWeight = FontWeight.ExtraBold)
            Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ImportCard(
    title: String,
    detail: String,
    icon: @Composable () -> Unit,
    button: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEAF8F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(color = Color.Transparent, contentColor = TealDark) {
                        icon()
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.ExtraBold)
                    Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) {
                Text(button)
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, accent: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.padding(12.dp)) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.height(10.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.ExtraBold)
        TextButton(onClick = onClick) { Text(action, color = TealDark) }
    }
}

@Composable
private fun ReceiptRow(receipt: Receipt, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MerchantMark(receipt.merchant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(receipt.merchant, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${receipt.category} - ${receipt.purchaseDateLabel}", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text(formatCurrency(receipt.amountCents), fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun WarrantyMiniRow(receipt: Receipt) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = TealDark)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(receipt.merchant, fontWeight = FontWeight.ExtraBold)
                Text(receipt.warrantyLabel, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text(formatCurrency(receipt.amountCents), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WarrantyRow(receipt: Receipt) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MerchantMark(receipt.merchant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(receipt.merchant, fontWeight = FontWeight.ExtraBold)
                Text("Return: ${receipt.returnByLabel}", color = Muted, style = MaterialTheme.typography.bodySmall)
                Text("Warranty: ${receipt.warrantyLabel}", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text(formatCurrency(receipt.amountCents), fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun DetailTile(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier = modifier, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, contentDescription = null, tint = TealDark)
            Text(label, color = Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(value, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FeatureRow(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TealDark)
            Spacer(Modifier.width(12.dp))
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MerchantMark(merchant: String) {
    val initial = merchant.firstOrNull()?.uppercaseChar()?.toString() ?: "R"
    val color = when (initial.first()) {
        'A', 'B', 'C', 'D', 'E', 'F' -> VaultBlue
        'G', 'H', 'I', 'J', 'K', 'L' -> Teal
        'M', 'N', 'O', 'P', 'Q', 'R' -> Amber
        else -> Coral
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(initial, color = Color.White, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ReceiptImage(path: String) {
    val image = remember(path) {
        runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
    }
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Saved receipt image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.74f),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color(0xFFF7F8FB)),
                contentAlignment = Alignment.Center
            ) {
                Text("Receipt image saved", color = Muted)
            }
        }
    }
}

@Composable
private fun EmptyState(onScan: () -> Unit, onPickImage: () -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Start your vault", fontWeight = FontWeight.ExtraBold)
            Text("Scan a receipt or upload an image to store proof of purchase, return dates, and warranties.", color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onScan, colors = ButtonDefaults.buttonColors(Teal), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan")
                }
                OutlinedButton(onClick = onPickImage, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload")
                }
            }
        }
    }
}

@Composable
private fun EmptySearchState() {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Text(
            "No receipts found. Try searching by store name, item text, category, or warranty.",
            modifier = Modifier.padding(18.dp),
            color = Muted
        )
    }
}

@Composable
private fun BusyOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = Teal)
                Text("Reading receipt")
            }
        }
    }
}

@Composable
private fun MessageBar(message: String, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Card(
            modifier = Modifier.padding(18.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(Color(0xFFFFF7EC))
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(message, modifier = Modifier.weight(1f), color = Ink)
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(screen: AppScreen, current: AppScreen, onChange: (AppScreen) -> Unit) {
    val selected = current == screen
    val color = if (selected) TealDark else Muted
    val icon = when (screen) {
        AppScreen.Home -> Icons.Default.Home
        AppScreen.Search -> Icons.Default.Search
        AppScreen.Scan -> Icons.Default.Add
        AppScreen.Email -> Icons.Default.Email
        AppScreen.Warranties -> Icons.Default.Shield
        AppScreen.Plus -> Icons.Default.Star
        AppScreen.Detail -> Icons.Default.Info
    }
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onChange(screen) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = screen.title, tint = color)
        Text(
            screen.navLabel,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReceiptVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Teal,
            secondary = VaultBlue,
            tertiary = Amber,
            background = Soft,
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Ink,
            onSurface = Ink
        ),
        content = content
    )
}

class ReceiptVaultViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ReceiptStore(application)
    private val connectorStore = EmailConnectorStore(application)
    private val connectorClient = EmailConnectorClient()
    private val ocrScanner = OcrScanner(application)
    private val aiClient = ReceiptAiClient(application)
    private val parser = ReceiptParser()

    private val _receipts = MutableStateFlow(store.loadReceipts())
    val receipts: StateFlow<List<Receipt>> = _receipts

    private val _emailAccounts = MutableStateFlow(connectorStore.loadAccounts())
    val emailAccounts: StateFlow<List<EmailConnectorAccount>> = _emailAccounts

    val activePlan: ReceiptVaultPlan = connectorStore.currentPlan()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    private val _pendingExternalUrl = MutableStateFlow<String?>(null)
    val pendingExternalUrl: StateFlow<String?> = _pendingExternalUrl

    private var selectedReceiptId by mutableStateOf<String?>(_receipts.value.firstOrNull()?.id)
    val selectedReceipt: Receipt?
        get() = _receipts.value.firstOrNull { it.id == selectedReceiptId }

    suspend fun importReceipt(uri: Uri, source: ImportSource): Receipt? {
        _isBusy.value = true
        return try {
            val id = UUID.randomUUID().toString()
            val rawText = ocrScanner.readText(uri)
            val localDraft = parser.parse(rawText)
            val draft = parser.mergeWithAi(localDraft, aiClient.categorize(rawText, source))
            val imagePath = store.saveImage(uri, id)
            val receipt = Receipt(
                id = id,
                merchant = draft.merchant,
                amountCents = draft.amountCents,
                purchasedAtMillis = draft.purchasedAtMillis,
                category = draft.category,
                location = draft.location,
                returnByMillis = draft.returnByMillis,
                warrantyUntilMillis = draft.warrantyUntilMillis,
                notes = draft.notes,
                rawText = rawText,
                imagePath = imagePath,
                source = source
            )
            val updated = store.upsert(receipt)
            _receipts.value = updated
            selectedReceiptId = id
            if (rawText.isBlank()) {
                _message.value = "Image saved, but OCR did not find text."
            }
            receipt
        } catch (error: Exception) {
            _message.value = "Could not import receipt: ${error.message ?: "unknown error"}"
            null
        } finally {
            _isBusy.value = false
        }
    }

    fun selectReceipt(id: String) {
        selectedReceiptId = id
    }

    fun connectEmailProvider(provider: EmailProvider) {
        val result = connectorStore.connect(provider)
        _emailAccounts.value = result.accounts
        _message.value = result.message
        viewModelScope.launch {
            val authorizationUrl = connectorClient.startOAuth(provider)
            if (authorizationUrl != null) {
                _pendingExternalUrl.value = authorizationUrl
            } else {
                _message.value = "${provider.label} OAuth credentials are not configured yet."
            }
        }
    }

    fun connectManualImap(
        emailAddress: String,
        host: String,
        port: String,
        username: String,
        password: String,
        useTls: Boolean
    ) {
        if (!connectorStore.canAddAccount()) {
            val plan = connectorStore.currentPlan()
            _message.value = "${plan.label} allows ${plan.maxEmailAccounts} connected email account."
            return
        }

        val parsedPort = port.toIntOrNull()
        if (emailAddress.isBlank() || host.isBlank() || username.isBlank() || password.isBlank() || parsedPort == null) {
            _message.value = "Enter email, host, port, username, and app password for IMAP."
            return
        }

        _isBusy.value = true
        viewModelScope.launch {
            try {
                val config = ImapManualConfig(
                    emailAddress = emailAddress.trim(),
                    host = host.trim(),
                    port = parsedPort,
                    username = username.trim(),
                    password = password,
                    useTls = useTls
                )
                val saved = connectorClient.registerManualImap(config)
                if (saved) {
                    val result = connectorStore.connect(
                        provider = EmailProvider.Imap,
                        emailAddress = config.emailAddress,
                        status = ConnectorStatus.Ready,
                        lastMessage = "IMAP settings saved encrypted. Receipt-only imports will use this mailbox configuration."
                    )
                    _emailAccounts.value = result.accounts
                    _message.value = "IMAP connector saved."
                } else {
                    _message.value = "Could not save IMAP settings."
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun syncEmailAccount(id: String) {
        val result = connectorStore.markSyncReady(id)
        _emailAccounts.value = result.accounts
        _message.value = result.message
    }

    fun disconnectEmailAccount(id: String) {
        val provider = _emailAccounts.value.firstOrNull { it.id == id }?.provider
        val result = connectorStore.disconnect(id)
        _emailAccounts.value = result.accounts
        _message.value = result.message
        if (provider != null) {
            viewModelScope.launch {
                connectorClient.deleteAccount(provider)
            }
        }
    }

    fun deleteEmailAccountData(id: String) {
        val provider = _emailAccounts.value.firstOrNull { it.id == id }?.provider
        val result = connectorStore.deleteAccountData(id)
        _emailAccounts.value = result.accounts
        _message.value = result.message
        if (provider != null) {
            viewModelScope.launch {
                connectorClient.deleteAccount(provider)
            }
        }
    }

    fun clearMessage() {
        _message.value = ""
    }

    fun clearPendingExternalUrl() {
        _pendingExternalUrl.value = null
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReceiptVaultViewModel(application) as T
                }
            }
    }
}

private class ReceiptStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("receiptvault", Context.MODE_PRIVATE)

    fun loadReceipts(): List<Receipt> {
        val raw = prefs.getString("receipts", "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                add(Receipt.fromJson(array.getJSONObject(index)))
            }
        }.sortedByDescending { it.purchasedAtMillis }
    }

    fun upsert(receipt: Receipt): List<Receipt> {
        val updated = (loadReceipts().filterNot { it.id == receipt.id } + receipt)
            .sortedByDescending { it.purchasedAtMillis }
        prefs.edit().putString("receipts", JSONArray(updated.map { it.toJson() }).toString()).apply()
        return updated
    }

    suspend fun saveImage(uri: Uri, id: String): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "receipts").apply { mkdirs() }
        val file = File(dir, "$id.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("image file could not be opened")
        file.absolutePath
    }
}

private class OcrScanner(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun readText(uri: Uri): String {
        val image = withContext(Dispatchers.IO) {
            InputImage.fromFilePath(context, uri)
        }
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { text ->
                    continuation.resume(text.text)
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }
    }
}

private class ReceiptParser {
    private val moneyRegex = Regex("""\$?\s*([0-9]{1,4}(?:,[0-9]{3})*\.[0-9]{2})""")
    private val datePatterns = listOf(
        DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US),
        DateTimeFormatter.ofPattern("M-d-yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MM-dd-yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MMM d yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.US)
    )

    fun parse(rawText: String): ReceiptDraft {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val merchant = lines.firstOrNull { line ->
            val lower = line.lowercase(Locale.US)
            lower.length in 3..42 &&
                !lower.contains("receipt") &&
                !lower.contains("total") &&
                !lower.contains("cashier")
        } ?: "Unknown store"

        val amount = parseAmount(lines)
        val purchasedAt = parseDate(lines) ?: LocalDate.now()
        val category = inferCategory(rawText, merchant)
        val returnBy = purchasedAt.plusDays(defaultReturnWindow(category))
        val warrantyUntil = if (category in setOf("Electronics", "Home", "Business")) {
            purchasedAt.plusYears(1)
        } else {
            null
        }

        return ReceiptDraft(
            merchant = merchant.take(42),
            amountCents = amount,
            purchasedAtMillis = purchasedAt.toMillis(),
            category = category,
            location = inferLocation(lines),
            returnByMillis = returnBy.toMillis(),
            warrantyUntilMillis = warrantyUntil?.toMillis(),
            notes = if (rawText.isBlank()) "Manual review needed" else "OCR processed"
        )
    }

    fun mergeWithAi(local: ReceiptDraft, ai: ReceiptAiSuggestion?): ReceiptDraft {
        if (ai == null || !ai.isReceipt || ai.confidence < 0.55) return local

        val purchasedAt = ai.purchaseDate
            ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
            ?: Instant.ofEpochMilli(local.purchasedAtMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

        val amountCents = ai.total
            ?.takeIf { it >= 0.0 }
            ?.let { (it * 100).roundToLong() }
            ?: local.amountCents

        val returnBy = ai.returnWindowDays
            ?.takeIf { it in 0..365 }
            ?.let { purchasedAt.plusDays(it.toLong()).toMillis() }
            ?: local.returnByMillis

        val warrantyUntil = if (ai.warrantyCandidate) {
            purchasedAt.plusYears(1).toMillis()
        } else {
            local.warrantyUntilMillis
        }

        val category = ai.category
            ?.takeIf { it.length in 2..24 }
            ?: local.category

        val merchant = ai.merchant
            ?.takeIf { it.length in 2..42 }
            ?: local.merchant

        val confidence = (ai.confidence * 100).roundToLong()
        return local.copy(
            merchant = merchant.take(42),
            amountCents = amountCents,
            purchasedAtMillis = purchasedAt.toMillis(),
            category = category,
            returnByMillis = returnBy,
            warrantyUntilMillis = warrantyUntil,
            notes = "Gemini categorized ${confidence}% confidence${ai.notes?.let { ": $it" } ?: ""}"
        )
    }

    private fun parseAmount(lines: List<String>): Long {
        val totalLineAmount = lines.firstNotNullOfOrNull { line ->
            val lower = line.lowercase(Locale.US)
            if (lower.contains("total") || lower.contains("amount")) {
                moneyRegex.findAll(line).lastOrNull()?.groupValues?.get(1)?.toCents()
            } else {
                null
            }
        }
        if (totalLineAmount != null) return totalLineAmount

        return lines
            .flatMap { line -> moneyRegex.findAll(line).map { it.groupValues[1].toCents() } }
            .maxOrNull() ?: 0L
    }

    private fun parseDate(lines: List<String>): LocalDate? {
        for (line in lines) {
            val normalized = line.replace(",", " ").replace(Regex("""\s+"""), " ")
            val candidates = Regex("""\b([0-9]{1,2}[/-][0-9]{1,2}[/-][0-9]{2,4}|[A-Za-z]{3,9}\s+[0-9]{1,2}\s+[0-9]{4})\b""")
                .findAll(normalized)
                .map { it.value }
            for (candidate in candidates) {
                datePatterns.forEach { formatter ->
                    val fixed = if (candidate.count { it == '/' || it == '-' } == 2 && candidate.takeLast(2).all(Char::isDigit) && candidate.length <= 8) {
                        candidate.dropLast(2) + "20" + candidate.takeLast(2)
                    } else {
                        candidate
                    }
                    try {
                        return LocalDate.parse(fixed, formatter)
                    } catch (_: DateTimeParseException) {
                    }
                }
            }
        }
        return null
    }

    private fun inferCategory(rawText: String, merchant: String): String {
        val text = "$rawText $merchant".lowercase(Locale.US)
        return when {
            listOf("best buy", "apple", "phone", "laptop", "headphone", "charger", "electronics").any { text.contains(it) } -> "Electronics"
            listOf("costco", "kroger", "walmart", "grocery", "market", "food").any { text.contains(it) } -> "Groceries"
            listOf("home depot", "lowe", "furniture", "appliance", "tool").any { text.contains(it) } -> "Home"
            listOf("office", "staples", "invoice", "client", "business").any { text.contains(it) } -> "Business"
            listOf("target", "amazon", "store").any { text.contains(it) } -> "Shopping"
            else -> "Uncategorized"
        }
    }

    private fun defaultReturnWindow(category: String): Long {
        return when (category) {
            "Electronics" -> 30
            "Home" -> 90
            "Business" -> 30
            else -> 30
        }
    }

    private fun inferLocation(lines: List<String>): String {
        return lines.firstOrNull { line ->
            Regex("""\b[A-Z]{2}\s+[0-9]{5}\b""").containsMatchIn(line)
        } ?: "Location not detected"
    }
}

data class ReceiptDraft(
    val merchant: String,
    val amountCents: Long,
    val purchasedAtMillis: Long,
    val category: String,
    val location: String,
    val returnByMillis: Long?,
    val warrantyUntilMillis: Long?,
    val notes: String
)

data class Receipt(
    val id: String,
    val merchant: String,
    val amountCents: Long,
    val purchasedAtMillis: Long,
    val category: String,
    val location: String,
    val returnByMillis: Long?,
    val warrantyUntilMillis: Long?,
    val notes: String,
    val rawText: String,
    val imagePath: String,
    val source: ImportSource
) {
    val purchaseDateLabel: String get() = purchasedAtMillis.formatDate()
    val returnByLabel: String get() = returnByMillis?.formatDate() ?: "Not set"
    val warrantyLabel: String get() = warrantyUntilMillis?.formatDate() ?: "Not set"

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("merchant", merchant)
        .put("amountCents", amountCents)
        .put("purchasedAtMillis", purchasedAtMillis)
        .put("category", category)
        .put("location", location)
        .put("returnByMillis", returnByMillis)
        .put("warrantyUntilMillis", warrantyUntilMillis)
        .put("notes", notes)
        .put("rawText", rawText)
        .put("imagePath", imagePath)
        .put("source", source.name)

    companion object {
        fun fromJson(json: JSONObject): Receipt = Receipt(
            id = json.getString("id"),
            merchant = json.optString("merchant", "Unknown store"),
            amountCents = json.optLong("amountCents", 0),
            purchasedAtMillis = json.optLong("purchasedAtMillis", todayMillis()),
            category = json.optString("category", "Uncategorized"),
            location = json.optString("location", "Location not detected"),
            returnByMillis = json.nullableLong("returnByMillis"),
            warrantyUntilMillis = json.nullableLong("warrantyUntilMillis"),
            notes = json.optString("notes", ""),
            rawText = json.optString("rawText", ""),
            imagePath = json.optString("imagePath", ""),
            source = runCatching { ImportSource.valueOf(json.optString("source", ImportSource.Image.name)) }.getOrDefault(ImportSource.Image)
        )
    }
}

enum class ImportSource(val label: String) {
    Camera("Camera"),
    Image("Image"),
    EmailShare("Email")
}

enum class AppScreen(val title: String, val navLabel: String) {
    Home("ReceiptVault", "Home"),
    Search("Search", "Search"),
    Scan("Add receipt", "Add"),
    Email("Email accounts", "Email"),
    Warranties("Warranties", "Warranty"),
    Plus("ReceiptVault Plus", "Plus"),
    Detail("Receipt", "Receipt")
}

private fun createCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile("receipt_", ".jpg", dir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun extractSharedImageUris(intent: Intent?): List<Uri> {
    if (intent == null) return emptyList()
    return when (intent.action) {
        Intent.ACTION_SEND -> listOfNotNull(intent.streamUri())
        Intent.ACTION_SEND_MULTIPLE -> intent.streamUris()
        else -> emptyList()
    }
}

private fun Intent.streamUri(): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(Intent.EXTRA_STREAM)
    }
}

private fun Intent.streamUris(): List<Uri> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: emptyList()
    } else {
        @Suppress("DEPRECATION")
        getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
    }
}

private fun Long.formatDate(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
}

private fun LocalDate.toMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun todayMillis(): Long = LocalDate.now().toMillis()

private fun formatCurrency(cents: Long): String {
    return NumberFormat.getCurrencyInstance(Locale.US).format(cents / 100.0)
}

private fun String.toCents(): Long {
    val clean = replace(",", "").trim()
    return runCatching { (clean.toDouble() * 100).toLong() }.getOrDefault(0L)
}

private fun JSONObject.nullableLong(key: String): Long? {
    return if (isNull(key) || !has(key)) null else optLong(key)
}
