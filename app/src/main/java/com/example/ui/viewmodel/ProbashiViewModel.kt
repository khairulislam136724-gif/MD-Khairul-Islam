package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.EncryptionUtil
import com.example.data.database.CallLogEntity
import com.example.data.database.FileEntity
import com.example.data.database.MessageEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ProbashiViewModel(private val repository: AppRepository) : ViewModel() {

    // Language Toggle: "BN" (Bangla) or "EN" (English)
    private val _language = MutableStateFlow("BN")
    val language: StateFlow<String> = _language.asStateFlow()

    // Low Bandwidth Mode Toggle
    private val _lowBandwidthMode = MutableStateFlow(false)
    val lowBandwidthMode: StateFlow<Boolean> = _lowBandwidthMode.asStateFlow()

    // Selected Tab: 0 = Chat, 1 = Video Call, 2 = File Share, 3 = Expat Hub
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Observables directly from database
    val messages: StateFlow<List<MessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val files: StateFlow<List<FileEntity>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallLogEntity>> = repository.allCallLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Direct Inputs
    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    // Video Call Interactive States
    private val _callState = MutableStateFlow("IDLE") // IDLE, CALLING, CONNECTED, ENDED
    val callState: StateFlow<String> = _callState.asStateFlow()

    private val _callDuration = MutableStateFlow(0)
    val callDuration: StateFlow<Int> = _callDuration.asStateFlow()

    private val _latency = MutableStateFlow(85) // ms
    val latency: StateFlow<Int> = _latency.asStateFlow()

    private val _cameraEnabled = MutableStateFlow(true)
    val cameraEnabled: StateFlow<Boolean> = _cameraEnabled.asStateFlow()

    private val _micEnabled = MutableStateFlow(true)
    val micEnabled: StateFlow<Boolean> = _micEnabled.asStateFlow()

    // Local / Expat Clock Updates
    private val _dhakaTime = MutableStateFlow("")
    val dhakaTime: StateFlow<String> = _dhakaTime.asStateFlow()

    private val _riyadhTime = MutableStateFlow("")
    val riyadhTime: StateFlow<String> = _riyadhTime.asStateFlow()

    init {
        // Start clocks
        viewModelScope.launch {
            while (true) {
                _dhakaTime.value = formatTimeForZone("Asia/Dhaka")
                _riyadhTime.value = formatTimeForZone("Asia/Riyadh")
                delay(1000)
            }
        }

        // Simulating latency deviations based on network/low bandwidth
        viewModelScope.launch {
            while (true) {
                val base = if (_lowBandwidthMode.value) 220 else 75
                _latency.value = base + (-15..15).random()
                delay(3000)
            }
        }

        // Initialize with default helpful messages if database is empty
        viewModelScope.launch {
            repository.allMessages.collect { list ->
                if (list.isEmpty()) {
                    // Seed initial encrypted welcome messages
                    repository.insertMessage(
                        MessageEntity(
                            senderName = "মুশফিক (রিয়াদ)",
                            encryptedText = EncryptionUtil.encrypt("আসসালামু আলাইকুম ভাই, কেমন আছেন? আমি মাত্র রিয়াদে রুমে পৌঁছালাম। অডিও/ভিডিও কোয়ালিটি খুব দারুণ এই অ্যাপে!"),
                            isFromMe = false,
                            timestamp = System.currentTimeMillis() - 600000
                        )
                    )
                    repository.insertMessage(
                        MessageEntity(
                            senderName = "System (E2EE)",
                            encryptedText = EncryptionUtil.encrypt("প্রবাসী আলাপ গোপনীয়তা সুরক্ষা: সকল চ্যাট এবং ফাইল মিলিটারী-গ্রেড AES-256 এনক্রিপশন দ্বারা সর্বদাই সুরক্ষিত থাকে।"),
                            isFromMe = false,
                            timestamp = System.currentTimeMillis() - 500000
                        )
                    )
                }
            }
        }
    }

    private fun formatTimeForZone(zoneId: String): String {
        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone(zoneId)
        return sdf.format(Date())
    }

    fun toggleLanguage() {
        _language.value = if (_language.value == "BN") "EN" else "BN"
    }

    fun toggleLowBandwidthMode() {
        _lowBandwidthMode.value = !_lowBandwidthMode.value
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun onChatInputChanged(text: String) {
        _chatInput.value = text
    }

    fun sendMessage() {
        val text = _chatInput.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            // Store encrypted in Room
            val encrypted = EncryptionUtil.encrypt(text)
            repository.insertMessage(
                MessageEntity(
                    senderName = "আপনি (মেম্বার)",
                    encryptedText = encrypted,
                    isFromMe = true,
                    timestamp = System.currentTimeMillis()
                )
            )
            _chatInput.value = ""

            // Simulate automatic friendly reply representing Saudi Expat or Family
            delay(1500)
            simulateReply(text)
        }
    }

    private suspend fun simulateReply(userMessage: String) {
        val replyText = when {
            userMessage.contains("কেমন", true) || userMessage.contains("kemon", true) -> {
                "আলহামদুলিল্লাহ ভাই, আপনাদের দোয়ায় ভালো আছি। এখানকার নেটওয়ার্ক ধীরগতির হলেও এনক্রিপটেড চ্যাট খুব ফাস্ট কাজ করছে!"
            }
            userMessage.contains("ফাইল", true) || userMessage.contains("file", true) -> {
                "হ্যাঁ ভাই, ফাইল শেয়ার অপশনে গিয়ে প্রয়োজনীয় পাসপোর্ট, ভিসা বা ভোটার আইডি এনক্রিপ্ট করে সিকিউরলি পাঠিয়ে দিন।"
            }
            userMessage.contains("আজকে", true) || userMessage.contains("টাকা", true) || userMessage.contains("রেট", true) -> {
                "আজকে রিয়ালের রেট বেশ ভালো দিচ্ছে, প্রবাসী হাব ট্যাব থেকে আজকের রিয়াল রেট দেখে নিতে পারেন।"
            }
            else -> {
                "ইনশাআল্লাহ্‌ ভাই, আমি রুমে ঢুকেই একটু পর ডিরেক্ট কল দিচ্ছি বা নতুন ফাইল শেয়ার করছি। আমাদের এই আলাপন এনক্রিপশনে খুব নিরাপদ।"
            }
        }

        val encryptedReply = EncryptionUtil.encrypt(replyText)
        repository.insertMessage(
            MessageEntity(
                senderName = "মুশফিক (রিয়াদ)",
                encryptedText = encryptedReply,
                isFromMe = false,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // Call Simulation functions
    fun startCall(type: String) {
        _callState.value = "CALLING"
        _callDuration.value = 0
        viewModelScope.launch {
            // Simulate dialing for 3 seconds then connect
            delay(3000)
            if (_callState.value == "CALLING") {
                _callState.value = "CONNECTED"
                // Track call log insertion
                repository.insertCallLog(
                    CallLogEntity(
                        callerName = "মুশফিক (রিয়াদ)",
                        durationSeconds = 0,
                        isIncoming = false,
                        callType = type
                    )
                )
                // Start duration counter
                while (_callState.value == "CONNECTED") {
                    delay(1000)
                    _callDuration.value += 1
                }
            }
        }
    }

    fun endCall() {
        val duration = _callDuration.value
        _callState.value = "ENDED"
        viewModelScope.launch {
            delay(2000)
            _callState.value = "IDLE"
        }
    }

    fun toggleCamera() {
        _cameraEnabled.value = !_cameraEnabled.value
    }

    fun toggleMic() {
        _micEnabled.value = !_micEnabled.value
    }

    // File simulation
    fun sharePresetFile(fileName: String, fileSize: String) {
        viewModelScope.launch {
            // Simulated encryption process
            val token = "SECURE_AES256_PAYLOAD_FOR_${fileName.uppercase()}_ENCRYPTED"
            repository.insertFile(
                FileEntity(
                    fileName = fileName,
                    fileSize = fileSize,
                    encryptedContent = token,
                    isIncoming = false,
                    senderName = "আপনি"
                )
            )

            // Auto reply file acknowledgement
            delay(2000)
            repository.insertMessage(
                MessageEntity(
                    senderName = "মুশফিক (রিয়াদ)",
                    encryptedText = EncryptionUtil.encrypt("আমি আপনার পাঠানো encrypted ফাইল '$fileName' পেয়েছি। এটি ডিক্রিপ্ট করে সেভ করে নিয়েছি। ধন্যবাদ!"),
                    isFromMe = false,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteFile(id: Int) {
        viewModelScope.launch {
            repository.deleteFile(id)
        }
    }

    // Translation helper in code
    fun t(key: String): String {
        val isBn = _language.value == "BN"
        return when (key) {
            "title" -> if (isBn) "প্রবাসী আলাপ" else "Probashi Alap"
            "chat" -> if (isBn) "চ্যাট" else "Encrypted Chat"
            "call" -> if (isBn) "ভিডিও কল" else "Video Call"
            "files" -> if (isBn) "ফাইল আদান-প্রদান" else "File Exchange"
            "hub" -> if (isBn) "প্রবাসী হাব" else "Expat Guide"
            "active_now" -> if (isBn) "মুশফিক (রিয়াদ) • অনলাইনে আছেন" else "Mushfiq (Riyadh) • Online"
            "encryption_info" -> if (isBn) "মিলিটারি-গ্রেড AES-256 সুরক্ষায় গোপন চ্যাট" else "AES-256 Militant-grade End-to-End Encryption Enabled"
            "write_msg" -> if (isBn) "নিরাপদ বার্তা লিখুন..." else "Write a secure message..."
            "send" -> if (isBn) "পাঠান" else "Send"
            "empty_chat" -> if (isBn) "কোন বার্তা নেই। নিচে লিখে পাঠান।" else "No messages. Send a message to start securely."
            "data_saver" -> if (isBn) "ডাটা সাশ্রয় (লো স্পিড নেটওয়ার্ক)" else "Data Saver (Slow Connection Mode)"
            "data_saver_desc" -> if (isBn) "ধীরগতির ৩জি ও সৌদিয়া-বিডি ভিওআইপি লাইনের ডাটা কমাবে" else "Optimizes audio/video renders & payloads for weak 3G/VoIP signals"
            "encryption_key" -> if (isBn) "এনক্রিপশন কী ভেরিফিকেশন ফিঙ্গারপ্রিন্ট:" else "Encryption Key Fingerprint Verification:"
            "dhaka_time" -> if (isBn) "বাংলাদেশ (ঢাকা) সময়:" else "Bangladesh (Dhaka) Time:"
            "riyadh_time" -> if (isBn) "সৌদি আরব (রিয়াদ) সময়:" else "Saudi Arabia (Riyadh) Time:"
            "bdt_rate" -> if (isBn) "আজকের আনুমানিক রিয়াল রেট:" else "Approx SAR to BDT Remittance Rate:"
            "bdt_amount" -> if (isBn) "১ সৌদি রিয়াল = ৩০.৪২ বাংলাদেশী টাকা (৳)" else "1 SAR = 30.42 Bangladeshi Taka (৳)"
            "fast_transfer" -> if (isBn) "অপ্টিমাইজড ইনস্ট্যান্ট শেয়ারিং" else "Expedited VoIP Fast Connection"
            "fast_transfer_desc" -> if (isBn) "বাংলাদেশি টেলিকম ব্যান্ডউইথে দ্রুত ফাইল আপলোডের টানেল" else "Bypass throttling with fast MTU segments"
            "file_share_title" -> if (isBn) "এনক্রিপ্টেড ফাইল ম্যানেজার" else "Secure Vault & File Sharing"
            "file_share_desc" -> if (isBn) "ভিসা কপি, পাসপোর্ট বা ছবি এনক্রিপ্ট করে সিকিউর চ্যালেনে আপলোড করুন।" else "E2EE encryption before uploading PDF, passports or document snaps."
            "share_visa" -> if (isBn) "পাসপোর্ট ফাইল শেয়ার করুন" else "Share Passport/Visa PDF"
            "share_pic" -> if (isBn) "নতুন ডকুমেন্ট ছবি পাঠান" else "Share Document/ID Photo"
            "encrypted_state" -> if (isBn) "সুরক্ষিত লক্ড ফাইলের অবস্থা" else "E2E AES Encrypted Block"
            "decrypt_and_view" -> if (isBn) "ডিক্রিপ্ট ও ফাইল দেখুন" else "Decrypt & Open Safely"
            "no_files" -> if (isBn) "কোন ফাইল শেয়ার করা হয়নি।" else "No decrypted or encrypted files yet."
            "calling_mesh" -> if (isBn) "কল সংযোগ করা হচ্ছে..." else "Starting E2EE VoIP Connection..."
            "connected_mesh" -> if (isBn) "কলটি এন্ড-টু-এন্ড এনক্রিপশনের মাধ্যমে সংযুক্ত" else "Call Connected Safely (AES-E2EE secured)"
            "ringing" -> if (isBn) "বেজে উঠছে..." else "Ringing Riyadh link..."
            "mute" -> if (isBn) "মিউট" else "Mute"
            "camera" -> if (isBn) "ক্যামেরা" else "Camera"
            "end" -> if (isBn) "মেয়াদ শেষ / কাটুন" else "Hang Up"
            "latency_label" -> if (isBn) "নেটওয়ার্ক রেসপন্স গতি (লেটেন্সি):" else "VoIP Frame Latency:"
            "call_mushfiq" -> if (isBn) "ভিডিও কল করুন (মুশফিক)" else "Start Secure Video Call (Mushfiq)"
            "voice_call" -> if (isBn) "অডিও কল করুন" else "Audio Call"
            "saudi_embassy_helpline" -> if (isBn) "সৌদি রিয়াদস্থ বাংলাদেশ দূতাবাস হেল্পলাইন:" else "BD Riyadh Embassy Emergency Helpline:"
            "expat_rights" -> if (isBn) "প্রবাসী অধিকার সহায়িকা ও রুলস" else "Expat Safety Regulations & Labor laws"
            "expat_rights_desc" -> if (isBn) "সৌদি আরবে আকামা, ভিসা নবায়ন এবং কর্মসংস্থান আইনের নির্দেশনাসমূহ" else "KSA labor contract laws, Iqama renewal and health insurance guide"
            else -> key
        }
    }
}

// Factory
class ProbashiViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProbashiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProbashiViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
