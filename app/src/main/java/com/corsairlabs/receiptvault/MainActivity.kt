package com.corsairlabs.receiptvault

import android.app.Application
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
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
    private var sharedImports by mutableStateOf<List<SharedReceiptImport>>(emptyList())
    private var oauthCallbackPending by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedImports = extractSharedReceiptImports(intent)
        if (isOAuthCallback(intent)) oauthCallbackPending = true
        setContent {
            ReceiptVaultRoot(
                sharedImports = sharedImports,
                oauthCallbackPending = oauthCallbackPending,
                onOAuthCallbackConsumed = { oauthCallbackPending = false },
                onSharedImportsConsumed = { sharedImports = emptyList() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sharedImports = extractSharedReceiptImports(intent)
        if (isOAuthCallback(intent)) oauthCallbackPending = true
    }

    private fun isOAuthCallback(intent: Intent): Boolean {
        val data = intent.data ?: return false
        return data.scheme == "receiptvault" && data.host == "connectors"
    }
}

@Composable
private fun ReceiptVaultRoot(
    sharedImports: List<SharedReceiptImport>,
    oauthCallbackPending: Boolean = false,
    onOAuthCallbackConsumed: () -> Unit = {},
    onSharedImportsConsumed: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ReceiptVaultViewModel = viewModel(
        factory = ReceiptVaultViewModel.factory(context.applicationContext as Application)
    )

    // When the app returns from the OAuth browser via receiptvault://connectors deep link,
    // refresh the connector account list from the Worker to get the real email + Ready status.
    LaunchedEffect(oauthCallbackPending) {
        if (oauthCallbackPending) {
            viewModel.refreshConnectorAccounts()
            onOAuthCallbackConsumed()
        }
    }
    val authUser by viewModel.authUser.collectAsState()
    val authBusy by viewModel.authBusy.collectAsState()
    val authMessage by viewModel.authMessage.collectAsState()
    var screen by rememberSaveable { mutableStateOf(AppScreen.Home) }
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val googleWebClientId = remember(context) { resolveGoogleSignInWebClientId(context) }
    val googleSignInClient = remember(context, googleWebClientId) {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        if (googleWebClientId.isNotBlank()) {
            builder.requestIdToken(googleWebClientId)
        }
        GoogleSignIn.getClient(context, builder.build())
    }
    val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.signInWithGoogle(result.data)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) {
            cameraUri?.let { uri ->
                // B3: use viewModelScope so import survives rotation; B4: only navigate on success
                viewModel.launchImport(uri, ImportSource.Camera) { screen = AppScreen.Detail }
            }
        }
    }

    // ML Kit Document Scanner result: live edge detection + auto-crop + manual crop UI.
    val docScannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val uri = scanResult?.pages?.firstOrNull()?.imageUri
            if (uri != null) {
                viewModel.launchImport(uri, ImportSource.Camera) { screen = AppScreen.Detail }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.launchImport(uri, ImportSource.Image) { screen = AppScreen.Detail }
        }
    }

    // B1: launchSharedImport deduplicates URIs in the ViewModel so rotation doesn't re-import
    LaunchedEffect(sharedImports, authUser) {
        if (sharedImports.isNotEmpty() && authUser != null) {
            viewModel.launchSharedImport(sharedImports) {
                screen = AppScreen.Detail
                onSharedImportsConsumed()
            }
        }
    }

    var showDeleteAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }

    // Back from any non-Home screen returns to Home; the app never exits from a
    // stray back press/gesture. Only Home asks the user before quitting (Issue 4).
    BackHandler(enabled = screen != AppScreen.Home) { screen = AppScreen.Home }
    BackHandler(enabled = screen == AppScreen.Home) { showExitDialog = true }

    ReceiptVaultTheme {
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("Quit ReceiptVault?") },
                text = { Text("Are you sure you want to quit the app?") },
                confirmButton = {
                    TextButton(onClick = { (context as? Activity)?.finish() }) { Text("Quit") }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
                }
            )
        }
        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAccountDialog = false },
                title = { Text("Delete account?") },
                text = { Text("All receipts and account data will be permanently deleted. This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteAccountDialog = false
                        viewModel.deleteAccount()
                    }) { Text("Delete", color = Coral) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (authUser == null) {
            AuthScreen(
                isBusy = authBusy,
                message = authMessage,
                googleSsoAvailable = googleWebClientId.isNotBlank(),
                onSignIn = viewModel::signInWithEmail,
                onSignUp = viewModel::signUpWithEmail,
                onGoogle = {
                    if (googleWebClientId.isBlank()) {
                        viewModel.showAuthMessage("Google SSO needs the Firebase web client ID in this build.")
                    } else {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                },
                onClearMessage = viewModel::clearAuthMessage
            )
        } else {
            ReceiptVaultApp(
                viewModel = viewModel,
                authUser = authUser,
                currentScreen = screen,
                onScreenChange = { screen = it },
                onScan = {
                    val activity = context.findActivity()
                    if (activity != null) {
                        val options = GmsDocumentScannerOptions.Builder()
                            .setGalleryImportAllowed(false)
                            .setPageLimit(1)
                            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                            .build()
                        GmsDocumentScanning.getClient(options)
                            .getStartScanIntent(activity)
                            .addOnSuccessListener { intentSender ->
                                docScannerLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                            .addOnFailureListener {
                                // Fallback to the plain camera if the doc scanner is unavailable.
                                val uri = createCameraUri(context)
                                cameraUri = uri
                                cameraLauncher.launch(uri)
                            }
                    } else {
                        val uri = createCameraUri(context)
                        cameraUri = uri
                        cameraLauncher.launch(uri)
                    }
                },
                onPickImage = { imagePicker.launch("image/*") },
                onSignOut = {
                    googleSignInClient.signOut()
                    viewModel.signOut()
                    screen = AppScreen.Home
                },
                onDeleteAccount = { showDeleteAccountDialog = true }
            )
        }
    }
}

@Composable
private fun ReceiptVaultApp(
    viewModel: ReceiptVaultViewModel,
    authUser: ReceiptVaultAuthUser?,
    currentScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    onScan: () -> Unit,
    onPickImage: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit = {}
) {
    val receipts by viewModel.receipts.collectAsState()
    val emailAccounts by viewModel.emailAccounts.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val billingState by viewModel.billingState.collectAsState()
    val preferredCurrency by viewModel.preferredCurrency.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val message by viewModel.message.collectAsState()
    val pendingExternalUrl by viewModel.pendingExternalUrl.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val showFreeAds = activePlan == ReceiptVaultPlan.Free && !billingState.loading
    val adController = remember(context) {
        context.findActivity()?.let { ReceiptVaultAdController(it) }
    }

    LaunchedEffect(showFreeAds, adController) {
        if (showFreeAds) {
            adController?.startForFreeUser()
            adController?.recordFreeVisit()
        } else {
            adController?.stopForPaidUser()
        }
    }

    DisposableEffect(showFreeAds, adController, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && showFreeAds) {
                adController?.recordFreeVisit()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // B5: wrap in try/catch — ActivityNotFoundException if no browser is installed
    LaunchedEffect(pendingExternalUrl) {
        val url = pendingExternalUrl
        if (!url.isNullOrBlank()) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) { }
            viewModel.clearPendingExternalUrl()
        }
    }

    Scaffold(
        containerColor = Soft,
        topBar = {
            AppTopBar(
                currentScreen = currentScreen,
                authUser = authUser,
                onBack = { onScreenChange(AppScreen.Home) },
                onSignOut = onSignOut,
                onDeleteAccount = onDeleteAccount
            )
        },
        bottomBar = {
            Surface(color = Color.White) {
                Column {
                    FreeBannerAd(show = showFreeAds)
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
                        NavItem(AppScreen.Analytics, currentScreen, onScreenChange)
                        NavItem(AppScreen.Plus, currentScreen, onScreenChange)
                    }
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
                    preferredCurrency = preferredCurrency,
                    onCurrencyChange = viewModel::setPreferredCurrency,
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
                    },
                    onDeleteMultiple = { ids -> viewModel.deleteReceipts(ids) {} }
                )

                AppScreen.Warranties -> WarrantyScreen(
                    receipts = receipts,
                    onSelect = {
                        viewModel.selectReceipt(it.id)
                        onScreenChange(AppScreen.Detail)
                    }
                )
                AppScreen.Analytics -> AnalyticsScreen(
                    receipts = receipts,
                    activePlan = activePlan,
                    preferredCurrency = preferredCurrency
                )
                AppScreen.Email -> EmailConnectorsScreen(
                    accounts = emailAccounts,
                    plan = activePlan,
                    onConnect = viewModel::connectEmailProvider,
                    onConnectImap = viewModel::connectManualImap,
                    onSync = viewModel::syncEmailAccount,
                    onDisconnect = viewModel::disconnectEmailAccount,
                    onDeleteData = viewModel::deleteEmailAccountData,
                    onPlusScreen = { onScreenChange(AppScreen.Plus) }
                )
                AppScreen.Plus -> PlusScreen(
                    billingState = billingState,
                    onPurchase = viewModel::purchaseBillingProduct
                )
                AppScreen.Detail -> ReceiptDetailScreen(
                    receipt = viewModel.selectedReceipt ?: receipts.firstOrNull(),
                    onBack = { onScreenChange(AppScreen.Home) },
                    onDelete = {
                        val id = viewModel.selectedReceipt?.id
                        if (id != null) {
                            viewModel.deleteReceipt(id) { onScreenChange(AppScreen.Home) }
                        } else {
                            onScreenChange(AppScreen.Home)
                        }
                    },
                    onUpdateText = { text ->
                        (viewModel.selectedReceipt?.id ?: receipts.firstOrNull()?.id)?.let { id ->
                            viewModel.updateReceiptText(id, text)
                        }
                    },
                    onOpenAttachment = viewModel::openEmailAttachment,
                    onSave = viewModel::updateReceipt
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

@Composable
private fun AuthScreen(
    isBusy: Boolean,
    message: String,
    googleSsoAvailable: Boolean,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onGoogle: () -> Unit,
    onClearMessage: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var signUpMode by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Soft)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ReceiptVault", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(if (signUpMode) "Create your account" else "Sign in to continue", color = Muted)
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                enabled = !isBusy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                enabled = !isBusy,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (signUpMode) onSignUp(email, password) else onSignIn(email, password)
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (signUpMode) "Create account" else "Sign in")
            }

            OutlinedButton(
                onClick = onGoogle,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (googleSsoAvailable) "Continue with Google" else "Google SSO setup required")
            }

            TextButton(
                onClick = { signUpMode = !signUpMode },
                enabled = !isBusy,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (signUpMode) "I already have an account" else "Create a new account")
            }
        }

        if (isBusy) {
            BusyOverlay()
        }

        if (message.isNotBlank()) {
            MessageBar(message, onDismiss = onClearMessage)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    currentScreen: AppScreen,
    authUser: ReceiptVaultAuthUser?,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit = {}
) {
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
                    Text(authUser?.email ?: "Signed in", color = Muted, style = MaterialTheme.typography.labelMedium)
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
                // P3: in-app account deletion required by Play Store policy
                TextButton(onClick = onDeleteAccount) {
                    Text("Delete account", color = Coral, style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onSignOut) {
                    Text("Sign out")
                }
            }
        }
    )
}

@Composable
private fun HomeScreen(
    receipts: List<Receipt>,
    preferredCurrency: String,
    onCurrencyChange: (String) -> Unit,
    onScan: () -> Unit,
    onPickImage: () -> Unit,
    onSelect: (Receipt) -> Unit,
    onSearch: () -> Unit,
    onEmail: () -> Unit,
    onWarranties: () -> Unit
) {
    val selectedCurrency = normalizeCurrencyCode(preferredCurrency) ?: defaultCurrencyCode()
    val currencyReceipts = receipts.filter { it.normalizedCurrencyCode == selectedCurrency }
    val totalLabel = formatReceiptTotal(currencyReceipts, selectedCurrency)
    // B16: filter to current calendar month so "This month" is accurate
    val thisMonthReceipts = run {
        val now = LocalDate.now()
        currencyReceipts.filter {
            val d = Instant.ofEpochMilli(it.purchasedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            d.year == now.year && d.monthValue == now.monthValue
        }
    }
    val thisMonthLabel = formatReceiptTotal(thisMonthReceipts, selectedCurrency)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            VaultHero(
                totalLabel = totalLabel,
                count = receipts.size,
                selectedCurrency = selectedCurrency,
                onCurrencyChange = onCurrencyChange,
                onScan = onScan
            )
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
                    accent = Teal,
                    onClick = onWarranties
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Warranties",
                    value = receipts.count { it.warrantyUntilMillis != null }.toString(),
                    accent = Amber,
                    onClick = onWarranties
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "This month",
                    value = thisMonthLabel,
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
            WarrantyMiniRow(receipt, onClick = { onSelect(receipt) })
        }
    }
}

@Composable
private fun VaultHero(
    totalLabel: String,
    count: Int,
    selectedCurrency: String,
    onCurrencyChange: (String) -> Unit,
    onScan: () -> Unit
) {
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
            Column(Modifier.weight(1f)) {
                Text("Total tracked", color = Color.White.copy(alpha = 0.72f))
                Text(
                    totalLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text("$count receipts saved", color = Color.White.copy(alpha = 0.72f))
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CurrencySelector(
                    selectedCurrency = selectedCurrency,
                    onCurrencyChange = onCurrencyChange,
                    light = true
                )
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
}

@Composable
private fun CurrencySelector(
    selectedCurrency: String,
    onCurrencyChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    light: Boolean = false
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (light) Color.White else TealDark
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(selectedCurrency, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            supportedCurrencyCodes().forEach { code ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "$code - ${currencyDisplayName(code)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        expanded = false
                        onCurrencyChange(code)
                    }
                )
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
                "Add receipts, bills, invoices, warranties, and orders from your camera, gallery, files, or email.",
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
                detail = "Share an attachment now, or connect email accounts for automatic purchase-document imports.",
                icon = { Icon(Icons.Default.Email, contentDescription = null) },
                button = "Email accounts",
                onClick = onEmailAccounts
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SearchScreen(
    receipts: List<Receipt>,
    onSelect: (Receipt) -> Unit,
    onDeleteMultiple: (List<String>) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var dateFilterName by rememberSaveable { mutableStateOf(SearchDateFilter.All.name) }
    var customStartDate by rememberSaveable { mutableStateOf("") }
    var customEndDate by rememberSaveable { mutableStateOf("") }
    var selectedIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    val inSelectMode = selectedIds.isNotEmpty()
    val dateFilter = remember(dateFilterName) { SearchDateFilter.fromName(dateFilterName) }
    val filtered = remember(query, receipts, dateFilter, customStartDate, customEndDate) {
        receipts.searchReceipts(query).filterByPurchasedDate(dateFilter, customStartDate, customEndDate)
    }
    val filteredIds = remember(filtered) { filtered.map { it.id }.toSet() }
    val allVisibleSelected = filteredIds.isNotEmpty() && filteredIds.all { it in selectedIds }
    val categories = remember(receipts) {
        receipts.map { it.category }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.US) }
            .sorted()
    }

    // Drop any selected ids that no longer exist (e.g. after a bulk delete).
    LaunchedEffect(receipts) {
        val existing = receipts.map { it.id }.toSet()
        if (selectedIds.any { it !in existing }) {
            selectedIds = selectedIds.intersect(existing)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 18.dp,
                bottom = if (inSelectMode) 148.dp else 18.dp
            ),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(onClick = { query = "" }, label = { Text("All") })
                    AssistChip(onClick = { query = "warranty" }, label = { Text("Warranty") })
                    categories.take(4).forEach { category ->
                        AssistChip(onClick = { query = "category:$category" }, label = { Text(category) })
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchDateFilter.values().forEach { filter ->
                        FilterChip(
                            selected = dateFilter == filter,
                            onClick = { dateFilterName = filter.name },
                            label = { Text(filter.label) }
                        )
                    }
                }
            }
            if (dateFilter == SearchDateFilter.Custom) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateInputField(
                            value = customStartDate,
                            onValueChange = { customStartDate = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = "From"
                        )
                        DateInputField(
                            value = customEndDate,
                            onValueChange = { customEndDate = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = "To"
                        )
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    EmptySearchState()
                }
            } else {
                items(filtered, key = { it.id }) { receipt ->
                    Box(
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (inSelectMode) {
                                    selectedIds = if (receipt.id in selectedIds) {
                                        selectedIds - receipt.id
                                    } else {
                                        selectedIds + receipt.id
                                    }
                                } else {
                                    onSelect(receipt)
                                }
                            },
                            onLongClick = { selectedIds = selectedIds + receipt.id }
                        )
                    ) {
                        // No inner onClick: the wrapping Box's combinedClickable handles
                        // tap and long-press; an inner clickable would swallow both.
                        ReceiptRow(
                            receipt = receipt,
                            trailingContent = if (inSelectMode) {
                                {
                                    Checkbox(
                                        checked = receipt.id in selectedIds,
                                        onCheckedChange = { checked ->
                                            selectedIds = if (checked) {
                                                selectedIds + receipt.id
                                            } else {
                                                selectedIds - receipt.id
                                            }
                                        }
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }

        if (inSelectMode) {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${selectedIds.size} selected", color = Muted, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = {
                                selectedIds = if (allVisibleSelected) {
                                    selectedIds - filteredIds
                                } else {
                                    selectedIds + filteredIds
                                }
                            },
                            enabled = filteredIds.isNotEmpty()
                        ) {
                            Text(
                                if (allVisibleSelected) "Clear visible" else "Select all (${filteredIds.size})",
                                color = TealDark
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedIds = emptySet() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancel") }
                        Button(
                            onClick = {
                                onDeleteMultiple(selectedIds.toList())
                                selectedIds = emptySet()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Coral),
                            modifier = Modifier.weight(1f)
                        ) { Text("Delete (${selectedIds.size})") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WarrantyScreen(receipts: List<Receipt>, onSelect: (Receipt) -> Unit) {
    // Only receipts with an actual warranty date; return-only receipts don't belong here.
    val warranties = receipts.filter { it.warrantyUntilMillis != null }
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
                            formatReceiptTotal(warranties),
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
                        "Warranties will appear here after you set a warranty date on an important purchase.",
                        modifier = Modifier.padding(18.dp),
                        color = Muted
                    )
                }
            }
        } else {
            items(warranties, key = { it.id }) { receipt ->
                WarrantyRow(receipt, onClick = { onSelect(receipt) })
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
    onDeleteData: (String) -> Unit,
    onPlusScreen: () -> Unit = {}
) {
    var imapEmail by rememberSaveable { mutableStateOf("") }
    var imapHost by rememberSaveable { mutableStateOf("") }
    var imapPort by rememberSaveable { mutableStateOf("993") }
    var imapUsername by rememberSaveable { mutableStateOf("") }
    var imapPassword by rememberSaveable { mutableStateOf("") }
    var imapUseTls by rememberSaveable { mutableStateOf(true) }
    var mailboxConsent by rememberSaveable { mutableStateOf(false) }

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
                    Text("Email connectors", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${plan.label}: ${accounts.count { it.status != ConnectorStatus.Disconnected }}/${plan.maxEmailAccounts} connected email connectors",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Only receipt, order, invoice, bill, statement, return, and warranty messages are eligible for import.",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            SectionHeader("Connect email connectors", "Plan", onPlusScreen)
        }

        item {
            MailboxConsentCard(
                checked = mailboxConsent,
                onCheckedChange = { mailboxConsent = it }
            )
        }

        items(EmailProvider.entries.filter { it != EmailProvider.Imap }, key = { it.name }) { provider ->
            ProviderConnectCard(
                provider = provider,
                enabled = mailboxConsent && provider.liveSyncAvailable,
                onConnect = { onConnect(provider) }
            )
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
                },
                enabled = mailboxConsent && EmailProvider.Imap.liveSyncAvailable
            )
        }

        item {
            SectionHeader("Connected accounts", "", {})
        }

        if (accounts.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Text(
                        "No email connectors connected yet. Add Gmail, Outlook, Yahoo, or IMAP to prepare automatic receipt imports.",
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
private fun MailboxConsentCard(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Spacer(Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Mailbox access consent", fontWeight = FontWeight.ExtraBold)
                Text(
                    "ReceiptVault searches connected mailboxes only for receipts, orders, invoices, bills, statements, returns, and warranty records. Non-matching messages are discarded and never stored.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Eligible document text and attachments are saved to the receipt. Extracted text may be sent to an AI service to identify the merchant, amount, and category.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Imported receipt records can be disconnected or deleted from this screen.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ProviderConnectCard(provider: EmailProvider, enabled: Boolean, onConnect: () -> Unit) {
    val disabledMessage = when {
        !provider.liveSyncAvailable -> provider.unavailableMessage
        !enabled -> "Accept mailbox access consent to connect."
        else -> ""
    }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(if (enabled) Color.White else Color(0xFFEFF3F4))
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .alpha(if (enabled) 1f else 0.62f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (enabled) Color(0xFFEAF8F6) else Color(0xFFE1E7E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = if (enabled) TealDark else Muted)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(provider.label, fontWeight = FontWeight.ExtraBold)
                    Text(provider.scopeLabel, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Search: ${provider.receiptQuery}", color = Muted, style = MaterialTheme.typography.bodySmall)
            if (disabledMessage.isNotBlank()) {
                Text(disabledMessage, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = onConnect,
                enabled = enabled,
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
    onConnect: () -> Unit,
    enabled: Boolean
) {
    val disabledMessage = when {
        !EmailProvider.Imap.liveSyncAvailable -> EmailProvider.Imap.unavailableMessage
        !enabled -> "Accept mailbox access consent to save IMAP settings."
        else -> ""
    }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(if (enabled) Color.White else Color(0xFFEFF3F4))
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .alpha(if (enabled) 1f else 0.62f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (enabled) Color(0xFFEAF8F6) else Color(0xFFE1E7E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = if (enabled) TealDark else Muted)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Other IMAP", fontWeight = FontWeight.ExtraBold)
                    Text("Encrypted server settings", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                disabledMessage.ifBlank { "Use an app password when your provider supports one." },
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = emailAddress,
                onValueChange = onEmailChange,
                label = { Text("Email address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = enabled
            )
            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                label = { Text("IMAP host") },
                placeholder = { Text("imap.example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = enabled
            )
            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = enabled
            )
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = enabled
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("App password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = enabled
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Use TLS", fontWeight = FontWeight.Bold)
                Switch(checked = useTls, onCheckedChange = onUseTlsChange, enabled = enabled)
            }
            Button(
                onClick = onConnect,
                enabled = enabled,
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
    val canSync = account.provider.liveSyncAvailable &&
        account.status != ConnectorStatus.Disconnected &&
        account.status != ConnectorStatus.OAuthPending
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(if (account.provider.liveSyncAvailable) Color.White else Color(0xFFEFF3F4))
    ) {
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
            if (!account.provider.liveSyncAvailable) {
                Text(account.provider.unavailableMessage, color = Muted, style = MaterialTheme.typography.bodySmall)
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
                OutlinedButton(
                    onClick = onSync,
                    enabled = canSync,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
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
private fun PlusScreen(
    billingState: ReceiptVaultBillingState,
    onPurchase: (Activity, ReceiptVaultBillingProduct) -> Unit
) {
    val activity = LocalContext.current.findActivity()
    // P5: use live prices from billing; fall back to fallback prices when not yet loaded
    val monthlyPrice = billingState.offers
        .firstOrNull { it.product == ReceiptVaultBillingProduct.PlusMonthly }
        ?.displayPrice ?: ReceiptVaultBillingProduct.PlusMonthly.fallbackPrice
    val yearlyPrice = billingState.offers
        .firstOrNull { it.product == ReceiptVaultBillingProduct.PlusYearly }
        ?.displayPrice ?: ReceiptVaultBillingProduct.PlusYearly.fallbackPrice

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
                    Text(monthlyPrice, color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                    Text("per month", color = Color.White.copy(alpha = 0.78f))
                    Text("$yearlyPrice yearly", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text(
                billingState.message,
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
        items(billingState.offers, key = { it.product.productId }) { offer ->
            BillingOfferCard(
                offer = offer,
                canLaunchPurchase = activity != null && offer.available && !offer.active,
                onPurchase = {
                    val launchActivity = activity ?: return@BillingOfferCard
                    onPurchase(launchActivity, offer.product)
                }
            )
        }
        item { FeatureRow("Unlimited manual receipt uploads", Icons.Default.CheckCircle) }
        item { FeatureRow("Cloud backup", Icons.Default.Shield) }
        item { FeatureRow("No banner or video ads", Icons.Default.CheckCircle) }
        item { FeatureRow("AI receipt categorization", Icons.Default.Star) }
        item { FeatureRow("3 connected email connectors", Icons.Default.Email) }
        item { FeatureRow("Auto email sync every 15 minutes", Icons.Default.Notifications) }
        item { FeatureRow("Unlimited warranty tracking", Icons.Default.Shield) }
        item { FeatureRow("Return and warranty reminders", Icons.Default.DateRange) }
        item { FeatureRow("CSV and PDF exports", Icons.Default.CheckCircle) }
    }
}

@Composable
private fun BillingOfferCard(
    offer: BillingPlanOffer,
    canLaunchPurchase: Boolean,
    onPurchase: () -> Unit
) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(offer.product.title, fontWeight = FontWeight.ExtraBold)
                    Text(offer.product.cadence, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    offer.displayPrice,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (offer.active) TealDark else Ink
                )
            }
            Text(offer.product.description, color = Muted, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = onPurchase,
                enabled = canLaunchPurchase,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    when {
                        offer.active -> "Active"
                        offer.available -> "Subscribe"
                        else -> "Create in Play Console"
                    }
                )
            }
        }
    }
}

@Composable
private fun ReceiptDetailScreen(
    receipt: Receipt?,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onUpdateText: (String) -> Unit = {},
    onOpenAttachment: (ReceiptEmailAttachment) -> Unit = {},
    onSave: (Receipt) -> Unit = {}
) {
    if (receipt == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No receipt selected", fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) { Text("Back home") }
            }
        }
        return
    }

    var editingDetails by remember(receipt.id) { mutableStateOf(false) }
    var editState by remember(receipt.id) { mutableStateOf(ReceiptEditState.from(receipt)) }
    var editError by remember(receipt.id) { mutableStateOf("") }

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
                    Text(formatCurrency(receipt.amountCents, receipt.currencyCode), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                }
                AssistChip(
                    onClick = {},
                    label = { Text(receipt.documentTypeLabel) }
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        editState = ReceiptEditState.from(receipt)
                        editError = ""
                        editingDetails = !editingDetails
                    }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit receipt details", tint = TealDark)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete receipt", tint = Coral)
                }
            }
        }
        if (editingDetails) {
            item {
                ReceiptEditCard(
                    state = editState,
                    error = editError,
                    onStateChange = {
                        editState = it
                        editError = ""
                    },
                    onCancel = {
                        editState = ReceiptEditState.from(receipt)
                        editError = ""
                        editingDetails = false
                    },
                    onSave = {
                        val updated = editState.toReceipt(receipt)
                        if (updated == null) {
                            editError = "Use YYYY-MM-DD for dates, a valid amount, and a valid 3-letter currency."
                        } else {
                            onSave(updated)
                            editError = ""
                            editingDetails = false
                        }
                    }
                )
            }
        }
        item {
            ReceiptImage(receipt.imagePath)
        }
        if (receipt.emailAttachments.isNotEmpty()) {
            item {
                EmailAttachmentsCard(receipt.emailAttachments, onOpenAttachment)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailTile(Modifier.weight(1f), "Type", receipt.documentTypeLabel, Icons.Default.Info)
                DetailTile(Modifier.weight(1f), "Source", receipt.source.label, Icons.Default.Email)
            }
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
            var editingText by rememberSaveable(receipt.id) { mutableStateOf(receipt.rawText) }
            var isEditing by rememberSaveable(receipt.id) { mutableStateOf(false) }
            Card(shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Extracted text", fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                        if (!isEditing) {
                            TextButton(onClick = { editingText = receipt.rawText; isEditing = true }) {
                                Text("Edit")
                            }
                        }
                    }
                    if (isEditing) {
                        OutlinedTextField(
                            value = editingText,
                            onValueChange = { editingText = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 12,
                            placeholder = { Text("Edit extracted text…") }
                        )
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { editingText = receipt.rawText; isEditing = false }) {
                                Text("Cancel")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { onUpdateText(editingText); isEditing = false }) {
                                Text("Save")
                            }
                        }
                    } else {
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
}

@Composable
private fun ReceiptEditCard(
    state: ReceiptEditState,
    error: String,
    onStateChange: (ReceiptEditState) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Edit receipt details", fontWeight = FontWeight.ExtraBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { onStateChange(state.copy(amount = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.currencyCode,
                    onValueChange = { onStateChange(state.copy(currencyCode = it.uppercase(Locale.US).take(3))) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Currency") },
                    singleLine = true
                )
            }
            DateInputField(
                value = state.purchaseDate,
                onValueChange = { onStateChange(state.copy(purchaseDate = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = "Purchase date",
                required = true
            )
            OutlinedTextField(
                value = state.documentType,
                onValueChange = { onStateChange(state.copy(documentType = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Document type") },
                placeholder = { Text("Receipt, bill, invoice, warranty") },
                singleLine = true
            )
            OutlinedTextField(
                value = state.category,
                onValueChange = { onStateChange(state.copy(category = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Category") },
                singleLine = true
            )
            DateInputField(
                value = state.returnByDate,
                onValueChange = { onStateChange(state.copy(returnByDate = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = "Return by"
            )
            DateInputField(
                value = state.warrantyUntilDate,
                onValueChange = { onStateChange(state.copy(warrantyUntilDate = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = "Warranty"
            )
            if (error.isNotBlank()) {
                Text(error, color = Coral, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    required: Boolean = false
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filterDateInput()) },
        modifier = modifier,
        label = { Text(label) },
        placeholder = { Text(if (required) "YYYY-MM-DD" else "YYYY-MM-DD or blank") },
        supportingText = { Text("Format: YYYY-MM-DD") },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Select $label")
            }
        },
        singleLine = true
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toDatePickerMillisOrNull()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onValueChange(millis.toIsoDateFromDatePicker())
                        }
                        showPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun EmailAttachmentsCard(
    attachments: List<ReceiptEmailAttachment>,
    onOpenAttachment: (ReceiptEmailAttachment) -> Unit
) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Email attachments", fontWeight = FontWeight.ExtraBold)
            attachments.forEach { attachment ->
                val canOpen = attachment.canOpen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = canOpen) { onOpenAttachment(attachment) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = if (canOpen) TealDark else Muted
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            attachment.filename,
                            fontWeight = FontWeight.Bold,
                            color = if (canOpen) Ink else Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${attachment.mimeType} - ${formatFileSize(attachment.size)}",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        if (canOpen) "Open" else attachment.statusLabel,
                        color = if (canOpen) TealDark else Muted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (canOpen) FontWeight.Bold else FontWeight.Normal
                    )
                    if (canOpen) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TealDark)
                    }
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
private fun AnalyticsScreen(
    receipts: List<Receipt>,
    activePlan: ReceiptVaultPlan = ReceiptVaultPlan.Free,
    preferredCurrency: String = defaultCurrencyCode()
) {
    val selectedCurrency = normalizeCurrencyCode(preferredCurrency) ?: defaultCurrencyCode()
    val currencyReceipts = receipts.filter { it.normalizedCurrencyCode == selectedCurrency }
    // Build last 6 months spending data
    val monthlyData: List<Pair<String, Long>> = run {
        val calendar = java.util.Calendar.getInstance()
        (5 downTo 0).map { monthsBack ->
            calendar.time = java.util.Date()
            calendar.add(java.util.Calendar.MONTH, -monthsBack)
            val month = calendar.get(java.util.Calendar.MONTH)
            val year = calendar.get(java.util.Calendar.YEAR)
            val label = java.text.SimpleDateFormat("MMM", java.util.Locale.US).format(calendar.time)
            val total = currencyReceipts.filter {
                val cal = java.util.Calendar.getInstance().also { c -> c.timeInMillis = it.purchasedAtMillis }
                cal.get(java.util.Calendar.MONTH) == month && cal.get(java.util.Calendar.YEAR) == year
            }.sumOf { it.amountCents }
            label to total
        }
    }

    // Category totals
    val categoryTotals: List<Pair<String, List<Receipt>>> = currencyReceipts
        .groupBy { it.normalizedCategory }
        .map { (cat, items) -> cat to items }
        .sortedByDescending { (_, items) -> items.sumOf { it.amountCents } }
        .take(5)

    val categoryColors = listOf(Teal, Coral, Amber, VaultBlue, Color(0xFF7C4DFF))
    val maxMonthly = monthlyData.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1L
    val totalSpent = currencyReceipts.sumOf { it.amountCents }
    val categorizedCount = currencyReceipts.count { it.normalizedCategory != "Uncategorized" }
    val returnDateCount = currencyReceipts.count { it.returnByMillis != null }
    val warrantyDateCount = currencyReceipts.count { it.warrantyUntilMillis != null }
    val avgAmountLabel = if (currencyReceipts.isNotEmpty()) {
        formatCurrency(totalSpent / currencyReceipts.size, selectedCurrency)
    } else {
        formatCurrency(0, selectedCurrency)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Stats row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), "Total spent", formatCurrency(totalSpent, selectedCurrency), Teal)
                StatCard(Modifier.weight(1f), "Avg receipt", avgAmountLabel, Coral)
                StatCard(Modifier.weight(1f), "Receipts", currencyReceipts.size.toString(), Amber)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), "Categorized", categorizedCount.toString(), VaultBlue)
                StatCard(Modifier.weight(1f), "Returns", returnDateCount.toString(), Teal)
                StatCard(Modifier.weight(1f), "Warranties", warrantyDateCount.toString(), Amber)
            }
        }
        item {
            // Monthly bar chart
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Monthly spending", fontWeight = FontWeight.ExtraBold)
                    if (monthlyData.all { it.second == 0L }) {
                        Text("No spending data yet.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                            val barCount = monthlyData.size
                            val totalWidth = size.width
                            val barSpacing = totalWidth * 0.05f / barCount
                            val barWidth = (totalWidth - barSpacing * (barCount + 1)) / barCount
                            val maxBarHeight = size.height - 28.dp.toPx()

                            monthlyData.forEachIndexed { i, (label, amount) ->
                                val x = barSpacing * (i + 1) + barWidth * i
                                val barHeight = if (amount > 0) (amount.toFloat() / maxMonthly.toFloat()) * maxBarHeight else 4.dp.toPx()
                                val y = size.height - barHeight - 24.dp.toPx()

                                drawRoundRect(
                                    color = if (i == monthlyData.lastIndex) Teal else Teal.copy(alpha = 0.45f),
                                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                )

                                drawContext.canvas.nativeCanvas.drawText(
                                    label,
                                    x + barWidth / 2,
                                    size.height - 4.dp.toPx(),
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.parseColor("#888999")
                                        textSize = 10.sp.toPx()
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            // Category breakdown
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("By category", fontWeight = FontWeight.ExtraBold)
                    if (categoryTotals.isEmpty()) {
                        Text("No category data yet.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Donut
                            val catTotal = categoryTotals.sumOf { (_, items) -> items.sumOf { it.amountCents } }.takeIf { it > 0 } ?: 1L
                            Canvas(modifier = Modifier.size(120.dp)) {
                                val strokeWidth = 28.dp.toPx()
                                val radius = (size.minDimension - strokeWidth) / 2
                                val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                                var startAngle = -90f
                                categoryTotals.forEachIndexed { i, (_, items) ->
                                    val amount = items.sumOf { it.amountCents }
                                    val sweep = (amount.toFloat() / catTotal.toFloat()) * 360f
                                    drawArc(
                                        color = categoryColors.getOrElse(i) { Muted },
                                        startAngle = startAngle,
                                        sweepAngle = sweep - 1f,
                                        useCenter = false,
                                        topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                    )
                                    startAngle += sweep
                                }
                            }
                            // Legend
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                categoryTotals.forEachIndexed { i, (cat, items) ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(categoryColors.getOrElse(i) { Muted }))
                                        Text(cat, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(formatReceiptTotal(items), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            // Top merchants
            if (currencyReceipts.isNotEmpty()) {
                val topMerchants = currencyReceipts
                    .groupBy { it.merchant }
                    .map { (merchant, items) -> merchant to items }
                    .sortedByDescending { (_, items) -> items.sumOf { it.amountCents } }
                    .take(5)
                Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Top merchants", fontWeight = FontWeight.ExtraBold)
                        topMerchants.forEach { (merchant, items) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                    MerchantMark(merchant)
                                    Text(merchant, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(formatReceiptTotal(items), fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
        // Issue 5: tax-ready CSV export for Business plan users
        if (activePlan == ReceiptVaultPlan.Business) {
            item { TaxExportCard(currencyReceipts) }
        }
    }
}

@Composable
private fun TaxExportCard(receipts: List<Receipt>) {
    val context = LocalContext.current
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tax-ready export", fontWeight = FontWeight.ExtraBold)
            Text(
                "Export all receipts as a CSV file with columns for Date, Merchant, Category, Amount, Tax Amount, and Notes — ready to hand to your accountant or import into tax software.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = {
                    val csv = buildTaxCsv(receipts)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "ReceiptVault Tax Export")
                        putExtra(Intent.EXTRA_TEXT, csv)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share tax export"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export CSV")
            }
        }
    }
}

private fun buildTaxCsv(receipts: List<Receipt>): String {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val header = "Date,Merchant,Document Type,Category,Amount,Currency,Return By,Warranty Until,Email From,Email Subject,Attachment Count,Tax Amount,Notes,Metadata Pattern"
    val rows = receipts.sortedByDescending { it.purchasedAtMillis }.map { r ->
        val date = Instant.ofEpochMilli(r.purchasedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(fmt)
        val amount = "%.2f".format(r.amountCents / 100.0)
        fun csvField(s: String) = "\"${s.replace("\"", "\"\"")}\""
        listOf(
            date,
            csvField(r.merchant),
            csvField(r.documentTypeLabel),
            csvField(r.normalizedCategory),
            amount,
            r.currencyCode,
            r.returnByIso.orEmpty(),
            r.warrantyUntilIso.orEmpty(),
            csvField(r.emailFrom.orEmpty()),
            csvField(r.emailSubject.orEmpty()),
            r.emailAttachments.size.toString(),
            "",
            csvField(r.notes),
            csvField(r.metadataPattern)
        ).joinToString(",")
    }
    return (listOf(header) + rows).joinToString("\n")
}

@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    value: String,
    accent: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
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
        if (action.isNotBlank()) {
            TextButton(onClick = onClick) { Text(action, color = TealDark) }
        }
    }
}

@Composable
private fun ReceiptRow(
    receipt: Receipt,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MerchantMark(receipt.merchant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(receipt.merchant, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${receipt.documentTypeLabel} - ${receipt.category} - ${receipt.purchaseDateLabel}", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text(formatCurrency(receipt.amountCents, receipt.currencyCode), fontWeight = FontWeight.ExtraBold)
            if (trailingContent != null) {
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    trailingContent()
                }
            }
        }
    }
}

@Composable
private fun WarrantyMiniRow(receipt: Receipt, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = TealDark)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(receipt.merchant, fontWeight = FontWeight.ExtraBold)
                Text(receipt.warrantyLabel, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text(formatCurrency(receipt.amountCents, receipt.currencyCode), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WarrantyRow(receipt: Receipt, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MerchantMark(receipt.merchant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(receipt.merchant, fontWeight = FontWeight.ExtraBold)
                Text("Return: ${receipt.returnByLabel}", color = Muted, style = MaterialTheme.typography.bodySmall)
                Text("Warranty: ${receipt.warrantyLabel}", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text(formatCurrency(receipt.amountCents, receipt.currencyCode), fontWeight = FontWeight.ExtraBold)
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
    // X4: decode JPEG on IO thread to avoid main-thread ANR on large images.
    // Also apply EXIF rotation so legacy files saved before the enhance-pipeline
    // fix display correctly (upright) regardless of when they were imported.
    var image by remember(path) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(path) {
        image = withContext(Dispatchers.IO) {
            runCatching {
                val raw = BitmapFactory.decodeFile(path) ?: return@runCatching null
                val orientation = runCatching {
                    android.media.ExifInterface(path).getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL
                    )
                }.getOrDefault(android.media.ExifInterface.ORIENTATION_NORMAL)
                val degrees = when (orientation) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                val afterExif = if (degrees != 0f) {
                    val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
                    val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                    raw.recycle()
                    rotated
                } else raw
                // Fallback: if still landscape after EXIF correction, force portrait
                if (afterExif.width > afterExif.height) {
                    val m = android.graphics.Matrix().apply { postRotate(90f) }
                    val portrait = Bitmap.createBitmap(afterExif, 0, 0, afterExif.width, afterExif.height, m, true)
                    afterExif.recycle()
                    portrait.asImageBitmap()
                } else {
                    afterExif.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(Color.White)) {
        val bmp = image
        if (bmp != null) {
            Image(
                bitmap = bmp,
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
    // X3: pointerInput consumes all touch events so the UI underneath is not interactive
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.78f))
            .pointerInput(Unit) {
                awaitPointerEventScope { while (true) { awaitPointerEvent() } }
            },
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
private fun FreeBannerAd(show: Boolean) {
    if (!show) return
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
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
        AppScreen.Analytics -> Icons.Default.DateRange
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

data class ReceiptVaultAuthUser(
    val uid: String,
    val email: String,
    val displayName: String
)

private class CurrencyPreferenceStore(context: Context) {
    private val prefs = context.getSharedPreferences("receiptvault_preferences", Context.MODE_PRIVATE)

    fun load(): String = normalizeCurrencyCode(prefs.getString(CURRENCY_KEY, null)) ?: defaultCurrencyCode()

    fun save(currencyCode: String) {
        val normalized = normalizeCurrencyCode(currencyCode) ?: defaultCurrencyCode()
        prefs.edit().putString(CURRENCY_KEY, normalized).apply()
    }

    private companion object {
        const val CURRENCY_KEY = "preferred_currency"
    }
}

class ReceiptVaultViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val store = ReceiptStore(application)
    private val currencyStore = CurrencyPreferenceStore(application)
    private val connectorStore = EmailConnectorStore(application)
    private val connectorClient = EmailConnectorClient()
    private val playBillingClient = PlayBillingClient(application)
    private val ocrScanner = OcrScanner(application)
    private val imageEnhancer = ImageEnhancer(application)
    private val aiClient = ReceiptAiClient(application)
    private val parser = ReceiptParser()

    private val _authUser = MutableStateFlow(auth.currentUser?.toReceiptVaultAuthUser())
    val authUser: StateFlow<ReceiptVaultAuthUser?> = _authUser

    private val _authBusy = MutableStateFlow(false)
    val authBusy: StateFlow<Boolean> = _authBusy

    private val _authMessage = MutableStateFlow("")
    val authMessage: StateFlow<String> = _authMessage

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _authUser.value = firebaseAuth.currentUser?.toReceiptVaultAuthUser()
    }

    // B12: initialise with empty lists; load from SharedPreferences on the IO thread in init
    private val _receipts = MutableStateFlow<List<Receipt>>(emptyList())
    val receipts: StateFlow<List<Receipt>> = _receipts

    private val _preferredCurrency = MutableStateFlow(currencyStore.load())
    val preferredCurrency: StateFlow<String> = _preferredCurrency

    private val _emailAccounts = MutableStateFlow<List<EmailConnectorAccount>>(emptyList())
    val emailAccounts: StateFlow<List<EmailConnectorAccount>> = _emailAccounts

    private val _activePlan = MutableStateFlow(connectorStore.currentPlan())
    val activePlan: StateFlow<ReceiptVaultPlan> = _activePlan

    val billingState: StateFlow<ReceiptVaultBillingState> = playBillingClient.state

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    private val _pendingExternalUrl = MutableStateFlow<String?>(null)
    val pendingExternalUrl: StateFlow<String?> = _pendingExternalUrl

    private var selectedReceiptId by mutableStateOf<String?>(_receipts.value.firstOrNull()?.id)
    val selectedReceipt: Receipt?
        get() = _receipts.value.firstOrNull { it.id == selectedReceiptId }

    init {
        auth.addAuthStateListener(authStateListener)
        playBillingClient.start()
        viewModelScope.launch {
            billingState.collect {
                _activePlan.value = connectorStore.currentPlan()
                updateBackgroundSyncSchedule()
            }
        }
        // B12: load persisted data on IO thread to avoid blocking the main thread at startup
        viewModelScope.launch {
            _receipts.value = withContext(Dispatchers.IO) { store.loadReceipts() }
            _emailAccounts.value = withContext(Dispatchers.IO) { connectorStore.loadAccounts() }
        }
    }

    /**
     * Background email sync runs only for paid (Plus/Business) users with at least one
     * connected account. Schedules the periodic WorkManager job when those conditions hold
     * and cancels it on downgrade or when every account is disconnected.
     */
    private fun updateBackgroundSyncSchedule() {
        val context = getApplication<Application>()
        val hasActiveAccount = _emailAccounts.value.any {
            it.status == ConnectorStatus.Ready || it.status == ConnectorStatus.SyncReady
        }
        if (_activePlan.value != ReceiptVaultPlan.Free && hasActiveAccount) {
            EmailSyncScheduler.schedule(context)
        } else {
            EmailSyncScheduler.cancel(context)
        }
    }

    fun signInWithEmail(email: String, password: String) {
        val normalizedEmail = email.trim()
        if (!validateAuthInput(normalizedEmail, password)) return
        _authBusy.value = true
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(normalizedEmail, password).authAwait()
                _authMessage.value = "Signed in."
            } catch (error: Exception) {
                _authMessage.value = authErrorMessage(error)
            } finally {
                _authBusy.value = false
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        val normalizedEmail = email.trim()
        if (!validateAuthInput(normalizedEmail, password)) return
        _authBusy.value = true
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(normalizedEmail, password).authAwait()
                _authMessage.value = "Account created."
            } catch (error: Exception) {
                _authMessage.value = authErrorMessage(error)
            } finally {
                _authBusy.value = false
            }
        }
    }

    fun signInWithGoogle(data: Intent?) {
        if (data == null) {
            _authMessage.value = "Google sign-in was canceled."
            return
        }
        _authBusy.value = true
        viewModelScope.launch {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
                val idToken = account.idToken ?: throw IOException("Google SSO returned no ID token.")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).authAwait()
                _authMessage.value = "Signed in with Google."
            } catch (error: Exception) {
                _authMessage.value = authErrorMessage(error)
            } finally {
                _authBusy.value = false
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _authMessage.value = ""
        _message.value = "Signed out."
    }

    fun showAuthMessage(message: String) {
        _authMessage.value = message
    }

    fun setPreferredCurrency(currencyCode: String) {
        val normalized = normalizeCurrencyCode(currencyCode) ?: return
        currencyStore.save(normalized)
        _preferredCurrency.value = normalized
        _message.value = "Default currency set to $normalized."
    }

    fun clearAuthMessage() {
        _authMessage.value = ""
    }

    private fun validateAuthInput(email: String, password: String): Boolean {
        if (!email.contains("@") || !email.contains(".")) {
            _authMessage.value = "Enter a valid email address."
            return false
        }
        if (password.length < 6) {
            _authMessage.value = "Password must be at least 6 characters."
            return false
        }
        return true
    }

    suspend fun importReceipt(
        uri: Uri,
        source: ImportSource,
        emailUrl: String? = null
    ): Receipt? {
        _isBusy.value = true
        return try {
            val id = UUID.randomUUID().toString()
            // Issue 1: save the enhanced (auto-cropped + high-contrast) version so users see
            // a clean, readable image in the vault instead of the raw un-cropped original.
            val enhanced = imageEnhancer.enhance(uri)

            // Detect content-rotation: ML Kit reports TextLine bounding boxes whose width
            // and height reveal text direction. Properly oriented receipt lines are wide
            // (width >> height). If the camera embedded content sideways in a portrait JPEG
            // (EXIF ORIENTATION_NORMAL but content rotated 90°), lines appear tall/narrow.
            // In that case we rotate 90° CW before saving so the stored image is upright.
            val ocrResult = ocrScanner.readTextResult(enhanced)
            val orientedBitmap = run {
                val lines = ocrResult.textBlocks.flatMap { it.lines }
                val aspects = lines.mapNotNull { line ->
                    val b = line.boundingBox ?: return@mapNotNull null
                    if (b.height() > 0) b.width().toFloat() / b.height() else null
                }
                val avgAspect = if (aspects.isNotEmpty()) aspects.average() else 1.0
                if (avgAspect < 0.85) {
                    // Lines are taller than wide → content is rotated 90° → correct it
                    val matrix = android.graphics.Matrix().apply { postRotate(90f) }
                    val r = Bitmap.createBitmap(enhanced, 0, 0, enhanced.width, enhanced.height, matrix, true)
                    enhanced.recycle()
                    r
                } else enhanced
            }

            val imagePath = store.saveImageBitmap(orientedBitmap, id)
            // Re-run OCR on the corrected image if we rotated; otherwise reuse what we have
            val rawText = if (orientedBitmap !== enhanced) {
                ocrScanner.readText(orientedBitmap)
            } else {
                ocrResult.text
            }
            orientedBitmap.recycle()  // free memory after OCR
            val defaultCurrency = _preferredCurrency.value
            val localDraft = parser.parse(rawText, defaultCurrency)
            val draft = parser.mergeWithAi(
                localDraft,
                aiClient.categorize(rawText, source, defaultCurrency),
                defaultCurrency
            )
            val receipt = Receipt(
                id = id,
                merchant = draft.merchant,
                amountCents = draft.amountCents,
                currencyCode = draft.currencyCode,
                purchasedAtMillis = draft.purchasedAtMillis,
                category = draft.category,
                location = draft.location,
                returnByMillis = draft.returnByMillis,
                warrantyUntilMillis = draft.warrantyUntilMillis,
                notes = draft.notes,
                rawText = rawText,
                imagePath = imagePath,
                source = source,
                emailUrl = emailUrl
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

    // B3/B4: launch import in viewModelScope so it survives rotation;
    // navigate only when the receipt is successfully parsed (non-null result)
    fun launchImport(uri: Uri, source: ImportSource, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = importReceipt(uri, source)
            if (result != null) onSuccess()
        }
    }

    // B1: track which shared URIs were already imported so rotation doesn't re-import them
    private val consumedSharedUris = mutableSetOf<String>()

    fun launchSharedImport(imports: List<SharedReceiptImport>, onComplete: () -> Unit) {
        viewModelScope.launch {
            val newImports = imports.filter { consumedSharedUris.add("${it.uri}|${it.emailUrl.orEmpty()}") }
            if (newImports.isNotEmpty()) {
                newImports.forEach { sharedImport ->
                    importReceipt(
                        uri = sharedImport.uri,
                        source = ImportSource.EmailShare,
                        emailUrl = sharedImport.emailUrl
                    )
                }
                onComplete()
            }
        }
    }

    // P3: in-app account deletion; Firebase AuthStateListener clears _authUser automatically
    fun deleteAccount() {
        val user = auth.currentUser ?: return
        _authBusy.value = true
        viewModelScope.launch {
            try {
                user.delete().authAwait()
                withContext(Dispatchers.IO) { store.clearAll() }
                _receipts.value = emptyList()
                _emailAccounts.value = emptyList()
            } catch (e: Exception) {
                val isReauthRequired = e.message?.contains("requires-recent-login") == true
                _message.value = if (isReauthRequired) {
                    "Please sign out and sign back in, then try deleting your account again."
                } else {
                    "Could not delete account: ${e.message}"
                }
            } finally {
                _authBusy.value = false
            }
        }
    }

    fun selectReceipt(id: String) {
        selectedReceiptId = id
    }

    fun deleteReceipt(id: String, onDeleted: () -> Unit) {
        _receipts.value = store.delete(id)
        if (selectedReceiptId == id) {
            selectedReceiptId = _receipts.value.firstOrNull()?.id
        }
        _message.value = "Receipt deleted."
        onDeleted()
    }

    fun deleteReceipts(ids: List<String>, onDeleted: () -> Unit) {
        if (ids.isEmpty()) {
            onDeleted()
            return
        }
        var current = _receipts.value
        for (id in ids) current = store.delete(id)
        _receipts.value = current
        if (selectedReceiptId in ids) {
            selectedReceiptId = _receipts.value.firstOrNull()?.id
        }
        _message.value = "${ids.size} receipt${if (ids.size == 1) "" else "s"} deleted."
        onDeleted()
    }

    fun updateReceiptText(id: String, newText: String) {
        val updated = _receipts.value.map { r -> if (r.id == id) r.copy(rawText = newText) else r }
        _receipts.value = updated
        store.saveAll(updated)
        _message.value = "Text updated."
    }

    fun updateReceipt(receipt: Receipt) {
        val updated = store.upsert(receipt)
        _receipts.value = updated
        selectedReceiptId = receipt.id
        _message.value = "Receipt updated."
    }

    fun connectEmailProvider(provider: EmailProvider) {
        if (!provider.liveSyncAvailable) {
            _message.value = provider.unavailableMessage
            return
        }
        if (!connectorStore.canAddAccount()) {
            val plan = connectorStore.currentPlan()
            _message.value = "${plan.label} allows ${plan.maxEmailAccounts} connected email connector."
            return
        }
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val authorizationUrl = connectorClient.startOAuth(provider)
                if (authorizationUrl != null) {
                    val result = connectorStore.connect(
                        provider = provider,
                        lastMessage = "Complete OAuth in the browser, then return to ReceiptVault."
                    )
                    _emailAccounts.value = result.accounts
                    _message.value = result.message
                    _pendingExternalUrl.value = authorizationUrl
                } else {
                    _message.value = "${provider.label} OAuth did not return an authorization URL."
                }
            } catch (error: Exception) {
                _message.value = "${provider.label} connector setup failed: ${error.message ?: "unknown error"}"
            } finally {
                _isBusy.value = false
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
        if (!EmailProvider.Imap.liveSyncAvailable) {
            _message.value = EmailProvider.Imap.unavailableMessage
            return
        }
        if (!connectorStore.canAddAccount()) {
            val plan = connectorStore.currentPlan()
            _message.value = "${plan.label} allows ${plan.maxEmailAccounts} connected email connector."
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
                        lastMessage = "IMAP settings saved encrypted. Purchase-document imports will use this mailbox configuration."
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
        val account = _emailAccounts.value.firstOrNull { it.id == id } ?: return
        if (!account.provider.liveSyncAvailable) {
            _message.value = "${account.provider.label} sync is not available in this build."
            return
        }
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val summary = connectorClient.syncProvider(account.provider)
                // Import receipts returned by the Worker, skipping emails imported on a
                // previous sync — a blind upsert would resurrect deleted receipts and
                // overwrite local edits with the server copy on every sync.
                var importedNow = 0
                for (receiptJson in summary.receipts) {
                    runCatching {
                        val jsonCurrency = normalizeCurrencyCode(receiptJson.optString("currencyCode", ""))
                        val receipt = Receipt.fromJson(receiptJson).let { imported ->
                            if (jsonCurrency == null) imported.copy(currencyCode = _preferredCurrency.value) else imported
                        }
                        val emailKey = receipt.emailMessageId ?: receipt.id
                        if (!store.isEmailImported(emailKey)) {
                            _receipts.value = store.upsert(receipt)
                            store.markEmailImported(emailKey)
                            importedNow++
                        }
                    }
                }
                val syncMessage = when {
                    summary.status == "import_limit_reached" -> summary.message
                    summary.imported > 0 && importedNow == 0 -> "No new purchase documents to import."
                    else -> summary.message
                }
                val result = connectorStore.markSyncReady(
                    id = id,
                    scanned = summary.scanned,
                    candidates = summary.candidates,
                    imported = importedNow,
                    monthlyImportUsed = summary.monthlyImportUsed,
                    monthlyImportLimit = summary.monthlyImportLimit,
                    message = syncMessage
                )
                _emailAccounts.value = result.accounts
                _message.value = result.message
                // Paid users get periodic background sync once a manual sync succeeds.
                updateBackgroundSyncSchedule()
            } catch (error: Exception) {
                val detail = error.message?.takeIf { it.isNotBlank() } ?: "unknown backend error"
                val result = connectorStore.markSyncFailed(id, message = "Could not reach connector sync: $detail.")
                _emailAccounts.value = result.accounts
                _message.value = result.message
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun openEmailAttachment(attachment: ReceiptEmailAttachment) {
        val storageKey = attachment.storageKey
        if (!attachment.canOpen || storageKey.isNullOrBlank()) {
            _message.value = "${attachment.filename} is metadata-only and cannot be opened."
            return
        }
        _isBusy.value = true
        viewModelScope.launch {
            try {
                val downloaded = connectorClient.downloadAttachment(storageKey)
                val file = withContext(Dispatchers.IO) {
                    saveAttachmentToCache(getApplication(), attachment.filename, downloaded.bytes)
                }
                openAttachmentFile(getApplication(), file, downloaded.contentType.ifBlank { attachment.mimeType })
                _message.value = "Opening ${attachment.filename}."
            } catch (error: Exception) {
                _message.value = "Could not open ${attachment.filename}: ${error.message ?: "unknown error"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun purchaseBillingProduct(activity: Activity, product: ReceiptVaultBillingProduct) {
        playBillingClient.launchPurchase(activity, product)
    }

    fun disconnectEmailAccount(id: String) {
        val provider = _emailAccounts.value.firstOrNull { it.id == id }?.provider
        val result = connectorStore.disconnect(id)
        _emailAccounts.value = result.accounts
        _message.value = result.message
        updateBackgroundSyncSchedule()
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
        updateBackgroundSyncSchedule()
        if (provider != null) {
            viewModelScope.launch {
                connectorClient.deleteAccount(provider)
            }
        }
    }

    /**
     * Called when the app returns from the OAuth browser flow via the receiptvault://connectors
     * deep link. Fetches the real account list from the Worker and updates local state so the
     * email address and Ready status are shown instead of the placeholder.
     */
    fun refreshConnectorAccounts() {
        viewModelScope.launch {
            val remote = connectorClient.fetchRemoteAccounts()
            if (remote.isNotEmpty()) {
                val updated = connectorStore.syncFromRemote(remote)
                _emailAccounts.value = updated
                _message.value = "Email accounts updated."
                // A paid user just connected an account — start periodic background sync.
                updateBackgroundSyncSchedule()
            }
        }
    }

    fun clearMessage() {
        _message.value = ""
    }

    fun clearPendingExternalUrl() {
        _pendingExternalUrl.value = null
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        playBillingClient.stop()
        super.onCleared()
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

private suspend fun <T> Task<T>.authAwait(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
    }

private fun FirebaseUser.toReceiptVaultAuthUser(): ReceiptVaultAuthUser =
    ReceiptVaultAuthUser(
        uid = uid,
        email = email ?: displayName ?: "Signed in",
        displayName = displayName ?: email ?: "ReceiptVault user"
    )

private fun resolveGoogleSignInWebClientId(context: Context): String {
    val fromBuild = BuildConfig.GOOGLE_SIGN_IN_WEB_CLIENT_ID.trim()
    if (fromBuild.isNotBlank()) return fromBuild

    val resourceId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName
    )
    return if (resourceId == 0) "" else context.getString(resourceId).trim()
}

private fun authErrorMessage(error: Exception): String {
    if (error is ApiException) {
        return when (error.statusCode) {
            GoogleSignInStatusCodes.DEVELOPER_ERROR ->
                "Google SSO is not configured for this app signing key yet. Register the release " +
                    "(upload and Play App Signing) SHA-1 fingerprints in Firebase, then rebuild."
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
                "Google sign-in was canceled."
            GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS ->
                "A Google sign-in is already in progress."
            GoogleSignInStatusCodes.NETWORK_ERROR ->
                "Network error during Google sign-in. Check the connection and try again."
            else ->
                "Google sign-in failed (${GoogleSignInStatusCodes.getStatusCodeString(error.statusCode)})."
        }
    }
    val message = error.message?.trim().orEmpty()
    return when {
        message.contains("network", ignoreCase = true) -> "Network error while signing in."
        message.contains("password", ignoreCase = true) -> "Email or password was not accepted."
        message.contains("no user", ignoreCase = true) -> "No account found for that email."
        message.isNotBlank() -> message
        else -> "Could not complete sign-in."
    }
}

internal class ReceiptStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("receiptvault", Context.MODE_PRIVATE)

    // B6: wrap entire parse in try/catch so a corrupt prefs string doesn't crash at launch
    fun loadReceipts(): List<Receipt> {
        val raw = prefs.getString("receipts", "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    // skip individual entries that fail to parse rather than crashing
                    runCatching { add(Receipt.fromJson(array.getJSONObject(index))) }
                }
            }.sortedByDescending { it.purchasedAtMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // B7: @Synchronized prevents concurrent writes from UI thread and EmailSyncWorker
    @Synchronized
    fun upsert(receipt: Receipt): List<Receipt> {
        val updated = (loadReceipts().filterNot { it.id == receipt.id } + receipt)
            .sortedByDescending { it.purchasedAtMillis }
        prefs.edit().putString("receipts", JSONArray(updated.map { it.toJson() }).toString()).apply()
        return updated
    }

    @Synchronized
    fun delete(id: String): List<Receipt> {
        val updated = loadReceipts().filterNot { it.id == id }
            .sortedByDescending { it.purchasedAtMillis }
        prefs.edit().putString("receipts", JSONArray(updated.map { it.toJson() }).toString()).apply()
        return updated
    }

    // Email messages stay recorded once imported so a receipt the user deleted
    // (or edited) is not re-imported/overwritten by every subsequent connector sync.
    @Synchronized
    fun isEmailImported(emailId: String): Boolean =
        prefs.getStringSet("imported_email_ids", emptySet())?.contains(emailId) == true

    @Synchronized
    fun markEmailImported(emailId: String) {
        val ids = (prefs.getStringSet("imported_email_ids", emptySet()) ?: emptySet()).toMutableSet()
        ids.add(emailId)
        prefs.edit().putStringSet("imported_email_ids", ids).apply()
    }

    @Synchronized
    fun saveAll(receipts: List<Receipt>) {
        val sorted = receipts.sortedByDescending { it.purchasedAtMillis }
        prefs.edit().putString("receipts", JSONArray(sorted.map { it.toJson() }).toString()).apply()
    }

    // P3: wipe all local data when user deletes their account
    fun clearAll() {
        prefs.edit().clear().apply()
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

    suspend fun saveImageBitmap(bitmap: Bitmap, id: String): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "receipts").apply { mkdirs() }
        val file = File(dir, "$id.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
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
                .addOnSuccessListener { text -> continuation.resume(text.text) }
                .addOnFailureListener { error -> continuation.resumeWithException(error) }
        }
    }

    suspend fun readText(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { text -> continuation.resume(text.text) }
                .addOnFailureListener { error -> continuation.resumeWithException(error) }
        }
    }

    /** Returns the full ML Kit Text result (with TextBlocks/TextLines) so callers can
     *  inspect bounding boxes for content-rotation detection. */
    suspend fun readTextResult(bitmap: Bitmap): Text {
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { text -> continuation.resume(text) }
                .addOnFailureListener { error -> continuation.resumeWithException(error) }
        }
    }
}

/**
 * Preprocesses an image before OCR by:
 * 1. Down-sampling oversized images to prevent OOM.
 * 2. Converting to grayscale with boosted contrast (2× factor) so text edges are sharp.
 * 3. Auto-cropping: scans inward from each edge and trims rows/columns that are
 *    almost entirely background (< 3 % non-white pixels). This removes large blank
 *    borders from photos taken with a phone and keeps just the bill area.
 */
private class ImageEnhancer(private val context: Context) {

    suspend fun enhance(uri: Uri): Bitmap = withContext(Dispatchers.Default) {
        val original = decodeUri(uri)
        val gray = toHighContrastGray(original)
        // B9: recycle intermediate bitmaps to avoid OOM on large receipt photos
        original.recycle()
        val cropped = autoCrop(gray)
        if (cropped !== gray) gray.recycle()
        cropped
    }

    private fun decodeUri(uri: Uri): Bitmap {
        // Read EXIF orientation before decoding — camera photos are typically stored
        // rotated 90° and BitmapFactory.decodeStream ignores EXIF, so autoCrop and
        // high-contrast conversion would run on a sideways image without this step.
        val orientation = context.contentResolver.openInputStream(uri)?.use { input ->
            android.media.ExifInterface(input).getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
        } ?: android.media.ExifInterface.ORIENTATION_NORMAL

        // First pass: read dimensions only
        val sizeOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, sizeOpts)
        }
        val maxDim = maxOf(sizeOpts.outWidth, sizeOpts.outHeight)
        val sampleSize = when {
            maxDim > 6000 -> 4
            maxDim > 3000 -> 2
            else -> 1
        }
        // Second pass: decode at reduced size
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: throw IOException("Cannot decode image")

        // Apply EXIF rotation so the bitmap is always upright before enhancement
        val degrees = when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val afterExif = if (degrees != 0f) {
            val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            raw.recycle()
            rotated
        } else raw

        // Fallback: receipts are always portrait (taller than wide). If the image is still
        // landscape after EXIF correction — e.g. EXIF tag was absent or ORIENTATION_NORMAL
        // was embedded incorrectly — rotate 90° CW to produce an upright portrait image.
        return if (afterExif.width > afterExif.height) {
            val m = android.graphics.Matrix().apply { postRotate(90f) }
            val portrait = Bitmap.createBitmap(afterExif, 0, 0, afterExif.width, afterExif.height, m, true)
            afterExif.recycle()
            portrait
        } else afterExif
    }

    /**
     * Converts to grayscale and applies 2× contrast boost in one ColorMatrix pass.
     * Highlights become brighter, shadows become darker — ideal for printed text.
     */
    private fun toHighContrastGray(src: Bitmap): Bitmap {
        val dst = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(dst)

        val gray = ColorMatrix().apply { setSaturation(0f) }

        val factor = 2.0f
        val translate = (-0.5f * factor + 0.5f) * 255f
        val contrast = ColorMatrix(floatArrayOf(
            factor, 0f, 0f, 0f, translate,
            0f, factor, 0f, 0f, translate,
            0f, 0f, factor, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        gray.postConcat(contrast)

        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(gray) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dst
    }

    /**
     * Finds the white-paper receipt region and crops to it.
     *
     * After high-contrast grayscale conversion the receipt paper is almost entirely
     * bright-white (luminance ≥ 210), while fingers, hands, and dark backgrounds
     * land below that level.  We scan each edge row/column and stop as soon as we
     * find one whose brightness profile looks like paper:
     *   • ≥ 35 % of its pixels must be bright-white (the paper background), AND
     *   • ≥ 1 % must be very dark (the printed text)
     *
     * This correctly skips uniformly dark finger/background rows while still
     * including rows that are mostly white with a few dark text pixels.
     */
    private fun autoCrop(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val brightThresh = 210   // luminance ≥ this → white paper pixel
        val darkThresh   = 80    // luminance ≤ this → printed text pixel
        val minPaper     = 0.35f // row/col must be ≥ 35 % bright-white
        val minText      = 0.01f // row/col must have ≥ 1 % dark text

        fun rowIsReceiptArea(y: Int): Boolean {
            var bright = 0; var dark = 0
            for (x in 0 until w) {
                val c = pixels[y * w + x]
                val lum = ((c shr 16 and 0xFF) + (c shr 8 and 0xFF) + (c and 0xFF)) / 3
                if (lum >= brightThresh) bright++ else if (lum <= darkThresh) dark++
            }
            return bright.toFloat() / w >= minPaper && dark.toFloat() / w >= minText
        }

        fun colIsReceiptArea(x: Int): Boolean {
            var bright = 0; var dark = 0
            for (y in 0 until h) {
                val c = pixels[y * w + x]
                val lum = ((c shr 16 and 0xFF) + (c shr 8 and 0xFF) + (c and 0xFF)) / 3
                if (lum >= brightThresh) bright++ else if (lum <= darkThresh) dark++
            }
            return bright.toFloat() / h >= minPaper && dark.toFloat() / h >= minText
        }

        var top    = 0;     while (top    < h - 1 && !rowIsReceiptArea(top))    top++
        var bottom = h - 1; while (bottom > top   && !rowIsReceiptArea(bottom)) bottom--
        var left   = 0;     while (left   < w - 1 && !colIsReceiptArea(left))   left++
        var right  = w - 1; while (right  > left  && !colIsReceiptArea(right))  right--

        // If the detected window is suspiciously small, bail out
        val rawCropW = right - left
        val rawCropH = bottom - top
        if (rawCropW < w * 0.10f || rawCropH < h * 0.10f) return bitmap

        // Add 2 % safety padding so we never clip the document edge
        val padX = (rawCropW * 0.02f).toInt().coerceAtLeast(4)
        val padY = (rawCropH * 0.02f).toInt().coerceAtLeast(4)
        left   = (left   - padX).coerceAtLeast(0)
        top    = (top    - padY).coerceAtLeast(0)
        right  = (right  + padX).coerceAtMost(w - 1)
        bottom = (bottom + padY).coerceAtMost(h - 1)

        val cropW = right - left
        val cropH = bottom - top

        // Only apply crop when it trims > 5 % from at least one dimension
        return if (cropW < w * 0.95f || cropH < h * 0.95f) {
            Bitmap.createBitmap(bitmap, left, top, cropW, cropH)
        } else {
            bitmap
        }
    }
}

private class ReceiptParser {
    private val validCategories = StandardReceiptCategories.toSet()
    private val moneyRegex = Regex("""(?i)(?:\b[A-Z]{3}\s*)?[$€£¥₹₨₺₩₽₫₪₱]?\s*([0-9]{1,6}(?:[,.][0-9]{3})*(?:[,.][0-9]{2}))""")
    private val datePatterns = listOf(
        DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US),
        DateTimeFormatter.ofPattern("M-d-yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MM-dd-yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MMM d yyyy", Locale.US),
        DateTimeFormatter.ofPattern("MMMM d yyyy", Locale.US)
    )

    fun parse(rawText: String, defaultCurrency: String = defaultCurrencyCode()): ReceiptDraft {
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
        val currencyCode = inferCurrencyCode(rawText, defaultCurrency)
        val purchasedAt = parseDate(lines) ?: LocalDate.now()
        val category = inferCategory(rawText, merchant)
        // Issue 3: do NOT auto-set returnByMillis — only set it via explicit user input or AI signal
        return ReceiptDraft(
            merchant = merchant.take(42),
            amountCents = amount,
            currencyCode = currencyCode,
            purchasedAtMillis = purchasedAt.toMillis(),
            category = category,
            location = inferLocation(lines),
            returnByMillis = null,
            warrantyUntilMillis = null,
            notes = if (rawText.isBlank()) "Manual review needed" else "OCR processed"
        )
    }

    fun mergeWithAi(
        local: ReceiptDraft,
        ai: ReceiptAiSuggestion?,
        defaultCurrency: String = defaultCurrencyCode()
    ): ReceiptDraft {
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

        val currencyCode = normalizeCurrencyCode(ai.currencyCode) ?: local.currencyCode.ifBlank { defaultCurrency }

        // Issue 3: only set returnBy when the AI explicitly signals a return window
        val returnBy = ai.returnWindowDays
            ?.takeIf { it in 1..365 }
            ?.let { purchasedAt.plusDays(it.toLong()).toMillis() }

        // A warranty signal alone is not enough to invent a coverage date.
        // Keep warranty empty unless the user sets it explicitly.
        val warrantyUntil = local.warrantyUntilMillis

        val category = ai.category
            ?.let { normalizeReceiptCategory(it) }
            ?.takeIf { it in validCategories }
            ?: local.category

        val merchant = ai.merchant
            ?.takeIf { it.length in 2..42 }
            ?: local.merchant

        val confidence = (ai.confidence * 100).roundToLong()
        return local.copy(
            merchant = merchant.take(42),
            amountCents = amountCents,
            currencyCode = currencyCode,
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
            listOf("restaurant", "café", "cafe", "starbucks", "mcdonald", "pizza", "diner", "coffee").any { text.contains(it) } -> "Food"
            listOf("uber", "lyft", "hotel", "airline", "airways", "delta", "southwest", "flight").any { text.contains(it) } -> "Travel"
            listOf("cvs", "walgreens", "pharmacy", "doctor", "clinic", "hospital", "dental").any { text.contains(it) } -> "Health"
            listOf("auto", "toyota", "ford", "honda", "gas station", "shell", "exxon", "chevron", "tire").any { text.contains(it) } -> "Auto"
            listOf("home depot", "lowe", "furniture", "appliance", "tool").any { text.contains(it) } -> "Home"
            listOf("office", "staples", "invoice", "client", "business").any { text.contains(it) } -> "Business"
            listOf("target", "amazon", "store").any { text.contains(it) } -> "Shopping"
            else -> "Uncategorized"
        }
    }

    private fun inferCurrencyCode(rawText: String, fallbackCurrencyCode: String): String {
        val upper = rawText.uppercase(Locale.US)
        supportedCurrencyCodes()
            .firstOrNull { Regex("""\b$it\b""").containsMatchIn(upper) }
            ?.let { return it }

        val fallback = normalizeCurrencyCode(fallbackCurrencyCode) ?: defaultCurrencyCode()
        return when {
            Regex("""(?i)\bRS\.?\b|\bRUPEES?\b""").containsMatchIn(rawText) -> "PKR"
            rawText.contains("€") -> "EUR"
            rawText.contains("£") -> "GBP"
            rawText.contains("₹") -> "INR"
            rawText.contains("₨") -> "PKR"
            rawText.contains("AED", ignoreCase = true) -> "AED"
            rawText.contains("SAR", ignoreCase = true) -> "SAR"
            rawText.contains("¥") -> "JPY"
            rawText.contains("₩") -> "KRW"
            rawText.contains("₺") -> "TRY"
            rawText.contains("₽") -> "RUB"
            rawText.contains("₫") -> "VND"
            rawText.contains("₪") -> "ILS"
            rawText.contains("₱") -> "PHP"
            rawText.contains("R$") -> "BRL"
            rawText.contains("$") -> fallback
            else -> fallback
        }
    }

    private fun inferLocation(lines: List<String>): String {
        return lines.firstOrNull { line ->
            Regex("""\b[A-Z]{2}\s+[0-9]{5}\b""").containsMatchIn(line)
        } ?: "Location not detected"
    }
}

data class ReceiptEditState(
    val amount: String,
    val currencyCode: String,
    val purchaseDate: String,
    val documentType: String,
    val category: String,
    val returnByDate: String,
    val warrantyUntilDate: String
) {
    fun toReceipt(receipt: Receipt): Receipt? {
        val amountCents = amount.toCentsOrNull() ?: return null
        val purchaseDateMillis = purchaseDate.toLocalDateOrNull()?.toMillis() ?: return null
        val returnMillis = returnByDate.toBlankableDateMillis() ?: return null
        val warrantyMillis = warrantyUntilDate.toBlankableDateMillis() ?: return null
        val normalizedCurrency = normalizeCurrencyCode(currencyCode) ?: return null

        return receipt.copy(
            amountCents = amountCents,
            currencyCode = normalizedCurrency,
            purchasedAtMillis = purchaseDateMillis,
            documentType = normalizeDocumentType(documentType),
            category = normalizeReceiptCategory(category),
            returnByMillis = returnMillis.takeIf { it > 0L },
            warrantyUntilMillis = warrantyMillis.takeIf { it > 0L }
        )
    }

    companion object {
        fun from(receipt: Receipt): ReceiptEditState = ReceiptEditState(
            amount = "%.2f".format(Locale.US, receipt.amountCents / 100.0),
            currencyCode = receipt.currencyCode,
            purchaseDate = receipt.purchasedAtMillis.formatIsoDate(),
            documentType = receipt.documentTypeLabel,
            category = receipt.category,
            returnByDate = receipt.returnByMillis?.formatIsoDate().orEmpty(),
            warrantyUntilDate = receipt.warrantyUntilMillis?.formatIsoDate().orEmpty()
        )
    }
}

data class ReceiptDraft(
    val merchant: String,
    val amountCents: Long,
    val currencyCode: String,
    val purchasedAtMillis: Long,
    val category: String,
    val location: String,
    val returnByMillis: Long?,
    val warrantyUntilMillis: Long?,
    val notes: String
)

data class ReceiptEmailAttachment(
    val filename: String,
    val mimeType: String,
    val size: Long,
    val storageKey: String?,
    val stored: Boolean,
    val skippedReason: String?
) {
    val canOpen: Boolean get() = stored && !storageKey.isNullOrBlank()

    val statusLabel: String
        get() = when {
            stored -> "Attached"
            skippedReason.isNullOrBlank() -> "Metadata only"
            skippedReason == "too_large" -> "Too large"
            skippedReason == "total_limit" -> "Skipped by size limit"
            skippedReason == "inline" -> "Inline"
            else -> "Metadata only"
        }

    fun toJson(): JSONObject = JSONObject()
        .put("filename", filename)
        .put("mimeType", mimeType)
        .put("size", size)
        .put("storageKey", storageKey ?: "")
        .put("stored", stored)
        .put("skippedReason", skippedReason ?: "")

    companion object {
        fun fromJson(json: JSONObject): ReceiptEmailAttachment = ReceiptEmailAttachment(
            filename = json.optString("filename", "Attachment"),
            mimeType = json.optString("mimeType", "application/octet-stream"),
            size = json.optLong("size", 0),
            storageKey = json.optString("storageKey", "").takeIf { it.isNotBlank() },
            stored = json.optBoolean("stored", false),
            skippedReason = json.optString("skippedReason", "").takeIf { it.isNotBlank() }
        )
    }
}

data class Receipt(
    val id: String,
    val merchant: String,
    val amountCents: Long,
    val currencyCode: String = defaultCurrencyCode(),
    val purchasedAtMillis: Long,
    val documentType: String = "receipt",
    val category: String,
    val location: String,
    val returnByMillis: Long?,
    val warrantyUntilMillis: Long?,
    val notes: String,
    val rawText: String,
    val imagePath: String,
    val source: ImportSource,
    val emailUrl: String? = null,
    val emailMessageId: String? = null,
    val emailSubject: String? = null,
    val emailFrom: String? = null,
    val emailDate: String? = null,
    val emailAttachments: List<ReceiptEmailAttachment> = emptyList()
) {
    val purchaseDateLabel: String get() = purchasedAtMillis.formatDate()
    val purchasedIso: String get() = purchasedAtMillis.formatIsoDate()
    val normalizedDocumentType: String get() = normalizeDocumentType(documentType)
    val documentTypeLabel: String get() = normalizedDocumentType.toDocumentTypeLabel()
    val normalizedCategory: String get() = normalizeReceiptCategory(category)
    val normalizedCurrencyCode: String get() = normalizeCurrencyCode(currencyCode) ?: defaultCurrencyCode()
    val returnByIso: String? get() = returnByMillis?.formatIsoDate()
    val warrantyUntilIso: String? get() = warrantyUntilMillis?.formatIsoDate()
    val returnByLabel: String get() = returnByMillis?.formatDate() ?: "Not set"
    val warrantyLabel: String get() = warrantyUntilMillis?.formatDate() ?: "Not set"
    val metadataPattern: String get() = receiptMetadataPattern(this)

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("merchant", merchant)
        .put("amountCents", amountCents)
        .put("currencyCode", currencyCode)
        .put("purchasedAtMillis", purchasedAtMillis)
        .put("documentType", normalizedDocumentType)
        .put("category", normalizedCategory)
        .put("location", location)
        .put("returnByMillis", returnByMillis)
        .put("warrantyUntilMillis", warrantyUntilMillis)
        .put("metadataPattern", metadataPattern)
        .put("notes", notes)
        .put("rawText", rawText)
        .put("imagePath", imagePath)
        .put("source", source.name)
        .put("emailUrl", emailUrl ?: "")
        .put("emailMessageId", emailMessageId ?: "")
        .put("emailSubject", emailSubject ?: "")
        .put("emailFrom", emailFrom ?: "")
        .put("emailDate", emailDate ?: "")
        .put("emailAttachments", JSONArray(emailAttachments.map { it.toJson() }))

    companion object {
        fun fromJson(json: JSONObject): Receipt = Receipt(
            // B6: getString("id") throws if key is missing — use optString with UUID fallback
            id = json.optString("id", "").ifBlank { UUID.randomUUID().toString() },
            merchant = json.optString("merchant", "Unknown store"),
            amountCents = json.optLong("amountCents", 0),
            currencyCode = normalizeCurrencyCode(json.optString("currencyCode", "")) ?: defaultCurrencyCode(),
            purchasedAtMillis = json.optLong("purchasedAtMillis", todayMillis()),
            documentType = normalizeDocumentType(json.optString("documentType", "receipt")),
            category = normalizeReceiptCategory(json.optString("category", "Uncategorized")),
            location = json.optString("location", "Location not detected"),
            returnByMillis = json.nullableLong("returnByMillis"),
            warrantyUntilMillis = json.nullableLong("warrantyUntilMillis"),
            notes = json.optString("notes", ""),
            rawText = json.optString("rawText", ""),
            imagePath = json.optString("imagePath", ""),
            source = runCatching { ImportSource.valueOf(json.optString("source", ImportSource.Image.name)) }.getOrDefault(ImportSource.Image),
            emailUrl = json.optString("emailUrl", "").takeIf { it.isNotBlank() },
            emailMessageId = json.optString("emailMessageId", "").takeIf { it.isNotBlank() },
            emailSubject = json.optString("emailSubject", "").takeIf { it.isNotBlank() },
            emailFrom = json.optString("emailFrom", "").takeIf { it.isNotBlank() },
            emailDate = json.optString("emailDate", "").takeIf { it.isNotBlank() },
            emailAttachments = json.optJSONArray("emailAttachments").toReceiptEmailAttachments()
        )
    }
}

enum class ImportSource(val label: String) {
    Camera("Camera"),
    Image("Image"),
    EmailShare("Email")
}

data class SharedReceiptImport(
    val uri: Uri,
    val emailUrl: String?
)

enum class AppScreen(val title: String, val navLabel: String) {
    Home("ReceiptVault", "Home"),
    Search("Search", "Search"),
    Scan("Add receipt", "Add"),
    Email("Email accounts", "Email"),
    Warranties("Warranties", "Warranty"),
    Analytics("Analytics", "Charts"),
    Plus("ReceiptVault Plus", "Plus"),
    Detail("Receipt", "Receipt")
}

private enum class SearchDateFilter(val label: String) {
    All("Any date"),
    Today("Today"),
    Last7("7 days"),
    Last30("30 days"),
    ThisMonth("This month"),
    ThisYear("This year"),
    Custom("Custom");

    companion object {
        fun fromName(name: String): SearchDateFilter =
            values().firstOrNull { it.name == name } ?: All
    }
}

private fun createCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile("receipt_", ".jpg", dir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun saveAttachmentToCache(context: Context, filename: String, bytes: ByteArray): File {
    val dir = File(context.cacheDir, "attachments").apply { mkdirs() }
    val file = File(dir, safeLocalFilename(filename))
    FileOutputStream(file).use { it.write(bytes) }
    return file
}

private fun openAttachmentFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType.ifBlank { "application/octet-stream" })
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Open attachment").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    } catch (_: Exception) {
        Toast.makeText(context, "No app can open this attachment.", Toast.LENGTH_SHORT).show()
    }
}

private fun safeLocalFilename(filename: String): String {
    val cleaned = filename
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]+"), "-")
        .trim('.', ' ', '-')
        .take(120)
    return cleaned.ifBlank { "attachment" }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun extractSharedReceiptImports(intent: Intent?): List<SharedReceiptImport> {
    if (intent == null) return emptyList()
    val emailUrl = intent.originalEmailUrl()
    val uris = when (intent.action) {
        Intent.ACTION_SEND -> listOfNotNull(intent.streamUri())
        Intent.ACTION_SEND_MULTIPLE -> intent.streamUris()
        else -> emptyList()
    }
    return uris.map { uri -> SharedReceiptImport(uri, emailUrl) }
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

private fun Intent.originalEmailUrl(): String? {
    val text = listOfNotNull(
        getStringExtra(Intent.EXTRA_TEXT),
        getStringExtra(Intent.EXTRA_HTML_TEXT)
    ).joinToString(" ")

    return Regex("""https?://[^\s<>"']+""")
        .findAll(text)
        .map { it.value.trimEnd('.', ',', ')', ']') }
        .firstOrNull { url ->
            listOf("mail.google.com", "outlook.live.com", "outlook.office.com", "outlook.office365.com", "mail.yahoo.com")
                .any { host -> url.contains(host, ignoreCase = true) }
        }
}

private fun Long.formatDate(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
}

private fun Long.formatIsoDate(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
}

private fun LocalDate.toMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun String.filterDateInput(): String =
    filter { it.isDigit() || it == '-' }.take(10)

private fun String.toDatePickerMillisOrNull(): Long? =
    toLocalDateOrNull()
        ?.atStartOfDay(ZoneOffset.UTC)
        ?.toInstant()
        ?.toEpochMilli()

private fun Long.toIsoDateFromDatePicker(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .toString()

private fun todayMillis(): Long = LocalDate.now().toMillis()

private fun formatReceiptTotal(receipts: List<Receipt>, currencyCode: String? = null): String {
    val selectedCurrency = normalizeCurrencyCode(currencyCode)
    if (selectedCurrency != null) {
        return formatCurrency(
            receipts
                .filter { it.normalizedCurrencyCode == selectedCurrency }
                .sumOf { it.amountCents },
            selectedCurrency
        )
    }
    if (receipts.isEmpty()) return formatCurrency(0, defaultCurrencyCode())
    val totals = receipts.groupBy { normalizeCurrencyCode(it.currencyCode) ?: defaultCurrencyCode() }
        .mapValues { (_, values) -> values.sumOf { it.amountCents } }
        .toSortedMap()
    return totals.entries.joinToString("\n") { (currencyCode, cents) ->
        formatCurrency(cents, currencyCode)
    }
}

private fun formatCurrency(cents: Long, currencyCode: String = defaultCurrencyCode()): String {
    val currency = runCatching { Currency.getInstance(normalizeCurrencyCode(currencyCode) ?: defaultCurrencyCode()) }
        .getOrElse { Currency.getInstance(defaultCurrencyCode()) }
    return NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        this.currency = currency
    }.format(cents / 100.0)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "size unknown"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.0f KB".format(Locale.US, kb)
    return "%.1f MB".format(Locale.US, kb / 1024.0)
}

private fun String.toCents(): Long {
    return toCentsOrNull() ?: 0L
}

private fun String.toCentsOrNull(): Long? {
    val clean = replace(",", "").trim()
    if (clean.isBlank()) return null
    val normalized = normalizeDecimalNumber(clean)
    return runCatching { (normalized.toDouble() * 100).roundToLong() }.getOrNull()
}

private fun normalizeDecimalNumber(value: String): String {
    val digits = value.filter { it.isDigit() || it == '.' || it == ',' }
    val lastDot = digits.lastIndexOf('.')
    val lastComma = digits.lastIndexOf(',')
    val decimalSeparator = when {
        lastDot >= 0 && lastComma >= 0 -> if (lastDot > lastComma) '.' else ','
        lastComma >= 0 && digits.length - lastComma == 3 -> ','
        lastDot >= 0 -> '.'
        else -> null
    }
    return buildString {
        digits.forEach { char ->
            when {
                char.isDigit() -> append(char)
                decimalSeparator != null && char == decimalSeparator -> append('.')
            }
        }
    }
}

private fun defaultCurrencyCode(): String {
    return runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("USD")
}

private fun normalizeCurrencyCode(value: String?): String? {
    val normalized = value?.trim()?.uppercase(Locale.US).orEmpty()
    if (normalized.length != 3) return null
    return runCatching { Currency.getInstance(normalized).currencyCode }.getOrNull()
}

private fun supportedCurrencyCodes(): List<String> =
    Currency.getAvailableCurrencies()
        .map { it.currencyCode }
        .distinct()
        .sorted()

private fun currencyDisplayName(currencyCode: String): String =
    runCatching { Currency.getInstance(currencyCode).getDisplayName(Locale.getDefault()) }
        .getOrDefault(currencyCode)

private val StandardReceiptCategories = listOf(
    "Groceries",
    "Electronics",
    "Home",
    "Business",
    "Shopping",
    "Food",
    "Travel",
    "Health",
    "Auto",
    "Other",
    "Uncategorized"
)

private fun normalizeReceiptCategory(value: String?): String {
    val normalized = value.orEmpty()
        .lowercase(Locale.US)
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .trim()
    return when {
        normalized.isBlank() -> "Uncategorized"
        normalized in setOf("grocery", "groceries", "supermarket", "market") -> "Groceries"
        normalized in setOf("electronic", "electronics", "tech", "computer", "phone") -> "Electronics"
        normalized in setOf("home", "house", "household", "utilities", "utility") -> "Home"
        normalized in setOf("business", "office", "work", "professional") -> "Business"
        normalized in setOf("shopping", "retail", "store", "purchase") -> "Shopping"
        normalized in setOf("food", "restaurant", "restaurants", "dining", "coffee", "cafe") -> "Food"
        normalized in setOf("travel", "trip", "hotel", "flight", "airline", "rideshare") -> "Travel"
        normalized in setOf("health", "healthcare", "medical", "pharmacy", "doctor", "clinic") -> "Health"
        normalized in setOf("auto", "car", "vehicle", "gas", "fuel", "automotive") -> "Auto"
        normalized == "other" -> "Other"
        normalized == "uncategorized" -> "Uncategorized"
        else -> StandardReceiptCategories.firstOrNull { it.normalizeSearchText() == normalized } ?: "Other"
    }
}

private fun normalizeDocumentType(value: String?): String {
    val normalized = value.orEmpty()
        .lowercase(Locale.US)
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .trim()
    return when (normalized) {
        "", "receipt", "sales receipt" -> "receipt"
        "order", "order confirmation", "purchase order" -> "order"
        "invoice" -> "invoice"
        "bill", "utility bill", "payment due" -> "bill"
        "statement", "account statement" -> "statement"
        "warranty", "protection plan", "coverage" -> "warranty"
        "return", "return label", "return confirmation" -> "return"
        "subscription", "recurring charge" -> "subscription"
        "other" -> "other"
        else -> normalized.split(" ").firstOrNull().takeIf {
            it in setOf("receipt", "order", "invoice", "bill", "statement", "warranty", "return", "subscription")
        } ?: "other"
    }
}

private fun String.toDocumentTypeLabel(): String =
    split("-", " ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char -> char.uppercase(Locale.US) }
        }
        .ifBlank { "Receipt" }

private fun receiptMetadataPattern(receipt: Receipt): String {
    val purchasedMonth = receipt.purchasedIso.take(7)
    val returnValue = receipt.returnByIso ?: "not-set"
    val warrantyValue = receipt.warrantyUntilIso ?: "not-set"
    return listOf(
        "purchased:${receipt.purchasedIso}",
        "purchase:${receipt.purchasedIso}",
        "date:${receipt.purchasedIso}",
        "month:$purchasedMonth",
        "type:${receipt.normalizedDocumentType.toPatternToken()}",
        "document:${receipt.normalizedDocumentType.toPatternToken()}",
        "category:${receipt.normalizedCategory.toPatternToken()}",
        "return:${if (receipt.returnByIso == null) "not-set" else "set"}",
        "return:$returnValue",
        "warranty:${if (receipt.warrantyUntilIso == null) "not-set" else "set"}",
        "warranty:$warrantyValue"
    ).joinToString(" ")
}

private fun String.toPatternToken(): String =
    lowercase(Locale.US)
        .replace(Regex("""[^a-z0-9]+"""), "-")
        .trim('-')
        .ifBlank { "uncategorized" }

private fun String.toLocalDateOrNull(): LocalDate? {
    return runCatching { LocalDate.parse(trim()) }.getOrNull()
}

private fun String.toBlankableDateMillis(): Long? {
    val trimmed = trim()
    if (trimmed.isBlank()) return 0L
    return trimmed.toLocalDateOrNull()?.toMillis()
}

private fun List<Receipt>.searchReceipts(query: String): List<Receipt> {
    val normalized = query.normalizeSearchText()
    if (normalized.isBlank()) return this

    val explicitCategory = normalized
        .substringAfter("category:", "")
        .substringBefore(" ")
        .takeIf { it.isNotBlank() }
    val terms = normalized
        .replace("category:", " ")
        .split(Regex("""\s+"""))
        .filter { it.isNotBlank() && it !in SearchStopWords }
    val knownCategories = map { it.normalizedCategory.normalizeSearchText() }.toSet()
    val categoryTerm = explicitCategory ?: terms.firstOrNull { it in knownCategories }

    if (categoryTerm != null) {
        return filter { it.normalizedCategory.normalizeSearchText() == categoryTerm }
    }

    return filter { receipt ->
        terms.all { term -> receipt.matchesSearchTerm(term) }
    }
}

private fun List<Receipt>.filterByPurchasedDate(
    filter: SearchDateFilter,
    customStartDate: String,
    customEndDate: String
): List<Receipt> {
    val today = LocalDate.now()
    val range = when (filter) {
        SearchDateFilter.All -> return this
        SearchDateFilter.Today -> today to today
        SearchDateFilter.Last7 -> today.minusDays(6) to today
        SearchDateFilter.Last30 -> today.minusDays(29) to today
        SearchDateFilter.ThisMonth -> today.withDayOfMonth(1) to today
        SearchDateFilter.ThisYear -> today.withDayOfYear(1) to today
        SearchDateFilter.Custom -> {
            val start = customStartDate.toLocalDateOrNull()
            val end = customEndDate.toLocalDateOrNull()
            if (start == null && end == null) return this
            (start ?: LocalDate.MIN) to (end ?: LocalDate.MAX)
        }
    }
    val start = if (range.first.isAfter(range.second)) range.second else range.first
    val end = if (range.first.isAfter(range.second)) range.first else range.second
    return filter { receipt ->
        val purchasedAt = Instant.ofEpochMilli(receipt.purchasedAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        !purchasedAt.isBefore(start) && !purchasedAt.isAfter(end)
    }
}

private val SearchStopWords = setOf("category", "categories", "receipt", "receipts")

private fun Receipt.matchesSearchTerm(term: String): Boolean {
    if (term == "warranty") return warrantyUntilMillis != null
    if (term == "return") return returnByMillis != null
    if (term == "purchased" || term == "purchase" || term == "date") return true
    val attachmentText = emailAttachments.joinToString(" ") {
        "${it.filename} ${it.mimeType} ${it.statusLabel}"
    }
    return listOf(
        merchant,
        normalizedDocumentType,
        documentTypeLabel,
        normalizedCategory,
        location,
        notes,
        emailSubject.orEmpty(),
        emailFrom.orEmpty(),
        emailDate.orEmpty(),
        attachmentText,
        purchaseDateLabel,
        purchasedIso,
        returnByLabel,
        returnByIso.orEmpty(),
        warrantyLabel,
        warrantyUntilIso.orEmpty(),
        metadataPattern,
        formatCurrency(amountCents, currencyCode),
        "%.2f".format(Locale.US, amountCents / 100.0),
        rawText
    ).any { field -> field.normalizeSearchText().containsWholeSearchTerm(term) }
}

private fun String.normalizeSearchText(): String {
    return lowercase(Locale.getDefault())
        .replace(Regex("""[^a-z0-9:-]+"""), " ")
        .trim()
}

private fun String.containsWholeSearchTerm(term: String): Boolean {
    return split(Regex("""\s+""")).any { token ->
        token == term || token.startsWith("$term-")
    }
}

private fun JSONObject.nullableLong(key: String): Long? {
    return if (isNull(key) || !has(key)) null else optLong(key)
}

private fun JSONArray?.toReceiptEmailAttachments(): List<ReceiptEmailAttachment> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(ReceiptEmailAttachment.fromJson(item))
        }
    }
}
