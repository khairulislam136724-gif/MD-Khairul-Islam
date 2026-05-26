package com.example

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.example.data.EncryptionUtil
import com.example.data.database.AppDatabase
import com.example.data.database.CallLogEntity
import com.example.data.database.FileEntity
import com.example.data.database.MessageEntity
import com.example.data.repository.AppRepository
import com.example.ui.theme.*
import com.example.ui.viewmodel.ProbashiViewModel
import com.example.ui.viewmodel.ProbashiViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Local SQLite Room initialization
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "probashi_alap_db"
        ).fallbackToDestructiveMigration().build()

        val repository = AppRepository(db.dao())
        val factory = ProbashiViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[ProbashiViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNavigation(viewModel)
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MainContentScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MainContentScreen(viewModel: ProbashiViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // App Header with switchable languages and Bandwidth optimizer
        AppHeader(viewModel)

        // Switch screens based on active tab
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> ChatScreen(viewModel)
                1 -> VideoCallScreen(viewModel)
                2 -> FileShareScreen(viewModel)
                3 -> ExpatHubScreen(viewModel)
            }
        }
    }
}

@Composable
fun AppHeader(viewModel: ProbashiViewModel) {
    val currentLang by viewModel.language.collectAsStateWithLifecycle()
    val lowBandwidth by viewModel.lowBandwidthMode.collectAsStateWithLifecycle()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo & Name with Saudi + BD colors representation
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(ProbashiGreenLight, ProbashiRedLight)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security App Icon",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (currentLang == "BN") "প্রবাসী আলাপ" else "Probashi Alap",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (currentLang == "BN") "অপ্টিমাইজড সুরক্ষিত সংযোগ" else "Optimized VoIP Portal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Quick language converter Button
                Button(
                    onClick = { viewModel.toggleLanguage() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("lang_toggle")
                ) {
                    Text(
                        text = if (currentLang == "BN") "English" else "বাংলা",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(6.dp))

            // Low-bandwidth Speed Optimizer Bar (For slow internet inside BD/Saudi)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (lowBandwidth) Icons.Default.Speed else Icons.Default.SignalCellularAlt,
                        contentDescription = "Speed Optimization Status",
                        modifier = Modifier.size(18.dp),
                        tint = if (lowBandwidth) ProbashiGoldDark else ProbashiGreenLight
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = viewModel.t("data_saver"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = viewModel.t("data_saver_desc"),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Switch(
                    checked = lowBandwidth,
                    onCheckedChange = { viewModel.toggleLowBandwidthMode() },
                    modifier = Modifier
                        .scale(0.7f)
                        .testTag("bandwidth_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ProbashiGreenDark,
                        checkedTrackColor = ProbashiGreenLight.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}

@Composable
fun BottomNavigation(viewModel: ProbashiViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isBn = viewModel.language.collectAsStateWithLifecycle().value == "BN"

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { viewModel.selectTab(0) },
            icon = { Icon(Icons.Default.Chat, contentDescription = "Tab Chat") },
            label = { Text(text = viewModel.t("chat"), fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ProbashiGreenDark,
                selectedTextColor = ProbashiGreenDark,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { viewModel.selectTab(1) },
            icon = { Icon(Icons.Default.VideoCall, contentDescription = "Tab Video Call") },
            label = { Text(text = viewModel.t("call"), fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ProbashiGreenDark,
                selectedTextColor = ProbashiGreenDark,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { viewModel.selectTab(2) },
            icon = { Icon(Icons.Default.FolderShared, contentDescription = "Tab Files") },
            label = { Text(text = if (isBn) "ফাইল" else "Files", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ProbashiGreenDark,
                selectedTextColor = ProbashiGreenDark,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { viewModel.selectTab(3) },
            icon = { Icon(Icons.Default.Language, contentDescription = "Tab Expat Guide") },
            label = { Text(text = viewModel.t("hub"), fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ProbashiGreenDark,
                selectedTextColor = ProbashiGreenDark,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        )
    }
}

// TAB 0: ENCRYPTED CHAT LAYER
@Composable
fun ChatScreen(viewModel: ProbashiViewModel) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val chatInput by viewModel.chatInput.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Encryption Key Fingerprint Banner
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Shield E2EE",
                        tint = ProbashiGreenDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = viewModel.t("encryption_info"),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${viewModel.t("encryption_key")} ${EncryptionUtil.getFingerprint()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Chat List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Secure Empty State Logo",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.t("empty_chat"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        ChatMessageBubble(message = message)
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        // TextInput Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { viewModel.onChatInputChanged(it) },
                placeholder = { Text(viewModel.t("write_msg"), fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure Lock Indicator",
                        tint = ProbashiGreenDark,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ProbashiGreenLight,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.sendMessage() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ProbashiGreenLight)
                    .testTag("send_button"),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send secure text",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: MessageEntity) {
    var showRawCipher by remember { mutableStateOf(false) }
    val decryptedText = remember(message.encryptedText) {
        EncryptionUtil.decrypt(message.encryptedText)
    }

    val bubbleBg = if (message.isFromMe) {
        ProbashiGreenLight.copy(alpha = 0.15f)
    } else {
        CardSlateDark
    }

    val alignAlignment = if (message.isFromMe) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignAlignment
    ) {
        // Sender name + lock
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message.senderName,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "E2EE Verified",
                tint = ProbashiGreenDark,
                modifier = Modifier.size(11.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Message body bubble
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                        bottomEnd = if (message.isFromMe) 4.dp else 16.dp
                    )
                )
                .background(bubbleBg)
                .border(
                    1.dp,
                    if (message.isFromMe) ProbashiGreenLight.copy(alpha = 0.3f) else BordersSlate,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                        bottomEnd = if (message.isFromMe) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Column {
                if (showRawCipher) {
                    Text(
                        text = "CIPHERTEXT (AES-256):\n${message.encryptedText}",
                        fontSize = 10.sp,
                        color = ProbashiRedDark,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = decryptedText,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Toggle Raw Cipher view to visually show encryption
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMsgTime(message.timestamp),
                        fontSize = 8.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )

                    Row(
                        modifier = Modifier.clickable { showRawCipher = !showRawCipher },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showRawCipher) Icons.Default.VisibilityOff else Icons.Default.EnhancedEncryption,
                            contentDescription = "Encryption Toggle",
                            modifier = Modifier.size(10.dp),
                            tint = if (showRawCipher) ProbashiRedDark else ProbashiGreenDark
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (showRawCipher) "DECRYPT" else "VERIFY E2EE",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showRawCipher) ProbashiRedDark else ProbashiGreenDark
                        )
                    }
                }
            }
        }
    }
}

private fun formatMsgTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// TAB 1: VIDEO CALL SIMULATOR (optimized for Saudi-BD expats with Low-bandwidth metrics)
@Composable
fun VideoCallScreen(viewModel: ProbashiViewModel) {
    val callState by viewModel.callState.collectAsStateWithLifecycle()
    val duration by viewModel.callDuration.collectAsStateWithLifecycle()
    val latency by viewModel.latency.collectAsStateWithLifecycle()
    val cameraEnabled by viewModel.cameraEnabled.collectAsStateWithLifecycle()
    val micEnabled by viewModel.micEnabled.collectAsStateWithLifecycle()
    val lowBandwidth by viewModel.lowBandwidthMode.collectAsStateWithLifecycle()
    val callsFromDb by viewModel.callLogs.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Launcher for permissions request as required
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (cameraGranted && micGranted) {
            viewModel.startCall("VIDEO")
        } else {
            Toast.makeText(context, "ডায়াল করতে অনুগ্রহ করে ক্যামেরা ও মাইকের অনুমতি দিন।", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (callState == "IDLE" || callState == "ENDED") {
            // Idle main control screen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Call Status panel
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardSlateDark),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, BordersSlate)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(ProbashiGreenDark)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = viewModel.t("active_now"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (viewModel.language.value == "BN") {
                                    "আপনার প্রিয়জনের সাথে বিনামূল্যে এনক্রিপ্টেড সুরক্ষিত ভিডিও/অডিও আলাপ পরিচালনা করুন।"
                                } else {
                                    "Securely stream data-optimized calls through high-density servers."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons to place calls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.CAMERA,
                                                Manifest.permission.RECORD_AUDIO
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("video_call_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ProbashiGreenLight),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VideoCall,
                                        contentDescription = "Video Call Icon"
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = viewModel.t("call_peer"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.CAMERA,
                                                Manifest.permission.RECORD_AUDIO
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ProbashiGoldLight),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Voice Call Icon"
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = viewModel.t("voice_call"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Call logs inside database
                    Column(modifier = Modifier.weight(1f)) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (viewModel.language.value == "BN") "সাম্প্রতিক কলসমূহ (Room Database)" else "Call History (Persisted Room Log)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (callsFromDb.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = "No Logs Symbol",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (viewModel.language.value == "BN") "কোন পূর্ববর্তী কল পাওয়া যায়নি।" else "No historical calls stored.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                items(callsFromDb) { log ->
                                    CallLogItem(log, viewModel)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Calling console overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SlateDarkBackground)
            ) {
                // Interactive Sine Soundwave custom drawn
                LiveWaveformCanvas(
                    callState = callState,
                    lowBandwidth = lowBandwidth,
                    modifier = Modifier.fillMaxSize()
                )

                // Console Header Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "মুশফিক (রিয়াদ)",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (callState == "CALLING") viewModel.t("calling_mesh") else viewModel.t("connected_mesh"),
                        color = ProbashiGreenDark,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Ping and bandwidth status indicators
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                        modifier = Modifier.padding(6.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BordersSlate)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Ping Icon",
                                tint = if (lowBandwidth) ProbashiGoldDark else ProbashiGreenDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${viewModel.t("latency_label")} $latency ms",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (lowBandwidth) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "3G COMPRESSED (12-FPS)",
                                    fontSize = 9.sp,
                                    color = ProbashiGoldDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            ProbashiGoldDark.copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (callState == "CONNECTED") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = formatDuration(duration),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                // Interactive layout of local pip video or camera preview simulator
                if (cameraEnabled) {
                    Box(
                        modifier = Modifier
                            .padding(24.dp)
                            .size(width = 100.dp, height = 150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Gray.copy(alpha = 0.2f))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .align(Alignment.CenterEnd)
                    ) {
                        // Drawing static silhouette to resemble a dynamic video capture simulator
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = "Simulated self camera feedback",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (viewModel.language.value == "BN") "আপনি" else "You",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Call Controls Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Audio / Mic toggle
                    IconButton(
                        onClick = { viewModel.toggleMic() },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (micEnabled) CardSlateDark else ProbashiRedLight)
                    ) {
                        Icon(
                            imageVector = if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Mute Mic button",
                            tint = Color.White
                        )
                    }

                    // Hangup red button
                    IconButton(
                        onClick = { viewModel.endCall() },
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(ProbashiRedLight)
                            .testTag("end_call_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Hang Up button",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Enable/disable self camera preview representation
                    IconButton(
                        onClick = { viewModel.toggleCamera() },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (cameraEnabled) CardSlateDark else ProbashiRedLight)
                    ) {
                        Icon(
                            imageVector = if (cameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Camera Toggle button",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallLogItem(log: CallLogEntity, viewModel: ProbashiViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BordersSlate.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (log.callType == "VIDEO") Icons.Default.VideoCall else Icons.Default.Call,
                    contentDescription = "Call Type Symbol",
                    tint = if (log.callType == "VIDEO") ProbashiGreenLight else ProbashiGoldLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = log.callerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = formatMsgTime(log.timestamp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Text(
                text = if (log.durationSeconds > 0) formatDuration(log.durationSeconds) else "Connected",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ProbashiGreenLight
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

// Draw soundwaves in canvas as a mock visual audio activity trigger
@Composable
fun LiveWaveformCanvas(callState: String, lowBandwidth: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PhaseTranslation"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val waveColor = ProbashiGreenDark.copy(alpha = 0.15f)

        // Plot simulated E2E secure encrypted waveform channels
        val amplitude = if (callState == "CALLING") 40f else 90f
        val freq = if (lowBandwidth) 2.5f else 4f

        val path = androidx.compose.ui.graphics.Path()
        path.moveTo(0f, centerY)

        for (x in 0..width.toInt() step 5) {
            val relativeX = x / width
            val sine = sin(relativeX * freq * Math.PI * 2f + phase)
            val y = centerY + sine * amplitude * (1f - relativeX) * relativeX * 4f
            path.lineTo(x.toFloat(), y.toFloat())
        }

        drawPath(
            path = path,
            color = waveColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        )
    }
}

// TAB 2: SECURE ENCRYPTED FILE TRANSMITTER
@Composable
fun FileShareScreen(viewModel: ProbashiViewModel) {
    val files by viewModel.files.collectAsStateWithLifecycle()
    var decryptingFileId by remember { mutableStateOf<Int?>(null) }
    var decryptedContent by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = viewModel.t("file_share_title"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = viewModel.t("file_share_desc"),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons to add mock secure documents
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.sharePresetFile("Passport_Mushfiq.pdf", "420 KB") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_visa_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ProbashiGreenLight),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.InsertDriveFile, contentDescription = "Doc Icon", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = viewModel.t("share_visa"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.sharePresetFile("Iqama_Receipt_Image.png", "1.2 MB") },
                        modifier = Modifier
                            .weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ProbashiGoldLight),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = "Pic Icon", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = viewModel.t("share_pic"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List of shared local files
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            if (files.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = "No Files Symbol",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = viewModel.t("no_files"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files) { file ->
                        SharedFileListItem(
                            file = file,
                            viewModel = viewModel,
                            onDecryptTrigger = { id, content ->
                                decryptingFileId = id
                                decryptedContent = content
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Decryption Visual Proof dialog
    if (decryptingFileId != null) {
        AlertDialog(
            onDismissRequest = {
                decryptingFileId = null
                decryptedContent = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        tint = ProbashiGreenDark,
                        contentDescription = "Decrypted Lock Screen"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (viewModel.language.value == "BN") "সুরক্ষিত ফাইল ডিক্রিপশন" else "File Decryption Terminal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = if (viewModel.language.value == "BN") {
                            "মিলিটারি-গ্রেড AES-256 বিট জটিল প্রাইভেট সিকিউরিটি কি ভেরিফাই করা হচ্ছে..."
                        } else {
                            "Applying ephemeral private key locally to decrypt Base64 hex streams..."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulated Code Output Visualizing AES decrypt
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateDarkBackground),
                        border = BorderStroke(1.dp, BordersSlate),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "RAW CIPHER: SECURE_AES256_PAYLOAD...",
                                fontSize = 9.sp,
                                color = ProbashiRedDark,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "DECRYPT SHA-256 STATE: SUCCESS",
                                fontSize = 9.sp,
                                color = ProbashiGreenDark,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "FILE HEADER DECRYPTED IN 4ms",
                                fontSize = 9.sp,
                                color = ProbashiGreenDark,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (viewModel.language.value == "BN") {
                            "আসন্ন ডিক্রিপ্ট ডেমো সম্পূর্ণ! আপনার ফোনে ফাইলটি মেমোরি ফোল্ডারে সুরক্ষিতভাবে সংরক্ষণ করা রয়েছে।"
                        } else {
                            "Verification Complete! The file has been fully decrypted and can now be safely accessed."
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        decryptingFileId = null
                        decryptedContent = null
                    }
                ) {
                    Text(text = "OK / বন্ধ করুন", color = ProbashiGreenLight)
                }
            }
        )
    }
}

@Composable
fun SharedFileListItem(
    file: FileEntity,
    viewModel: ProbashiViewModel,
    onDecryptTrigger: (Int, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSlateDark),
        border = BorderStroke(1.dp, BordersSlate)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Lock badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ProbashiGreenLight.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted Block State Indicator",
                            tint = ProbashiGreenDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = file.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = file.fileSize,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• By ${file.senderName}",
                                fontSize = 10.5.sp,
                                color = ProbashiGreenDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { viewModel.deleteFile(file.id) }
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Secure Delete from cloud db",
                        tint = ProbashiRedDark.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action to decrypt on flight
            Button(
                onClick = { onDecryptTrigger(file.id, file.encryptedContent) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("decrypt_btn_${file.id}"),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = "Lock Decrypt Symbol",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = viewModel.t("decrypt_and_view"),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// TAB 3: EXPAT HUB SCREEN ( বাংলাদেশ - সৌদি আরব স্পেশাল জোন)
@Composable
fun ExpatHubScreen(viewModel: ProbashiViewModel) {
    val dhakaTime by viewModel.dhakaTime.collectAsStateWithLifecycle()
    val riyadhTime by viewModel.riyadhTime.collectAsStateWithLifecycle()
    val isBn = viewModel.language.collectAsStateWithLifecycle().value == "BN"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Clock widgets for Expat connection
        Text(
            text = if (isBn) "দ্বিমুখী তাৎক্ষণিক ঘড়ি (BD vs KSA Clocks)" else "Direct Inter-Country Time Offset Clocks",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bangladesh Clock Screen
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardSlateDark),
                border = BorderStroke(1.dp, BordersSlate)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Symbol depicting BD
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF006A4E)), // BD green
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF42A41)) // BD Red Circle
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isBn) "বাংলাদেশ টাইম" else "Dhaka Time (BD)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dhakaTime.ifEmpty { "--:--:--" },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 1,
                        fontSize = 14.sp
                    )
                }
            }

            // Riyadh Saudi-Arabia Clock Screen
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardSlateDark),
                border = BorderStroke(1.dp, BordersSlate)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Symbol depicting Saudi Arabia Flag
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF006C35)), // KSA Green
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "KSA Emblem",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isBn) "রিয়াদ টাইম" else "Riyadh Time (KSA)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = riyadhTime.ifEmpty { "--:--:--" },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 1,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Riyal BDT Rate conversion block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, ProbashiGreenLight.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ProbashiGoldDark.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyExchange,
                        contentDescription = "Money exchange",
                        tint = ProbashiGoldDark
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = viewModel.t("bdt_rate"),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = viewModel.t("bdt_amount"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Labor rights, guidelines and emergency helplines
        Text(
            text = if (isBn) "প্রবাসীদের জন্য সরকারি ও আইনি ও দূতাবাসের গাইড" else "Official Labor, Embassy & Emergency Helplines",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSlateDark),
            border = BorderStroke(1.dp, BordersSlate)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = "Emergency Help icon",
                        tint = ProbashiGoldDark
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = viewModel.t("saudi_embassy_helpline"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "টোল ফ্রি: ৮০০ ২০০ ৩০০০ (বাংলাদেশ রিয়াদ মিশন)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ProbashiGreenDark
                )
                Text(
                    text = "কল সেন্টার: +৯৬৬-১১-৪৬১০৭১৮ (যেকোনো কুফিল সমস্যা সমাধান ও দুর্ঘটনা সাপোর্ট)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSlateDark),
            border = BorderStroke(1.dp, BordersSlate)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HelpCenter,
                        contentDescription = "Law guide icon",
                        tint = ProbashiGreenDark
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = viewModel.t("expat_rights"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = viewModel.t("expat_rights_desc"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ProbashiGreenDark.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isBn) "আকামা আইন প্রটেকশন" else "Iqama Law Protection",
                            fontSize = 10.sp,
                            color = ProbashiGreenDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ProbashiGoldDark.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isBn) "ভিসা নবায়ন গাইড" else "Visa Guide",
                            fontSize = 10.sp,
                            color = ProbashiGoldDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
