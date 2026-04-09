package com.athena.client.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.ToneGenerator
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.athena.client.audio.AudioPlayer
import com.athena.client.speech.SpeechRecognizerManager
import com.athena.client.ui.components.MicButton
import com.athena.client.ui.components.MimicButton
import com.athena.client.ui.components.ResponseCard
import com.athena.client.ui.components.SpeakButton
import com.athena.client.ui.components.SpeakDialog
import com.athena.client.ui.components.ThinkingIndicator
import com.athena.client.ui.components.VoiceSelector
import com.athena.client.viewmodel.MainViewModel
import com.athena.client.viewmodel.ResponseType

private enum class ListenMode {
    PROMPT,
    MIMIC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current
    val uiState by viewModel.uiState.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    
    // Keep screen on while loading, polling, or playing audio
    val showProgress = uiState.isLoading || uiState.isPolling
    LaunchedEffect(showProgress, uiState.playingResponseId) {
        view.keepScreenOn = showProgress || uiState.playingResponseId != null
    }
    
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }
    
    val audioPlayer = remember { AudioPlayer() }
    
    var speechAvailable by remember { mutableStateOf(true) }
    var showSpeakDialog by remember { mutableStateOf(false) }
    var listenMode by remember { mutableStateOf(ListenMode.PROMPT) }
    var isMimicListening by remember { mutableStateOf(false) }
    
    val speechRecognizer = remember {
        SpeechRecognizerManager(
            context = context,
            onResult = { result ->
                when (listenMode) {
                    ListenMode.PROMPT -> viewModel.sendPrompt(result)
                    ListenMode.MIMIC -> viewModel.speakText(result)
                }
                isMimicListening = false
            },
            onPartialResult = {},
            onError = { error ->
                viewModel.setListening(false)
                isMimicListening = false
            },
            onListeningStateChanged = { listening ->
                viewModel.setListening(listening)
                if (!listening) isMimicListening = false
            }
        ).also { speechAvailable = it.initialize() }
    }
    
    LaunchedEffect(speechAvailable) {
        if (!speechAvailable) {
            snackbarHostState.showSnackbar("Speech recognition unavailable on this device")
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
            audioPlayer.release()
        }
    }
    
    
    // Track previous response count for auto-play
    var previousResponseCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(uiState.responses.size) {
        if (uiState.responses.isNotEmpty()) {
            listState.animateScrollToItem(uiState.responses.size - 1)
            
            // Auto-play audio when a new response arrives
            if (uiState.responses.size > previousResponseCount) {
                val latestResponse = uiState.responses.last()
                latestResponse.audioBase64?.let { audio ->
                    audioPlayer.stop()
                    viewModel.setPlayingResponse(latestResponse.id)
                    audioPlayer.play(
                        base64Audio = audio,
                        onCompletion = {
                            viewModel.setPlayingResponse(null)
                        },
                        onError = {
                            viewModel.setPlayingResponse(null)
                        }
                    )
                }
            }
        }
        previousResponseCount = uiState.responses.size
    }

    if (showSpeakDialog) {
        SpeakDialog(
            onDismiss = { showSpeakDialog = false },
            onConfirm = { text -> viewModel.speakText(text) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Athena",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = uiState.responses,
                    key = { it.id }
                ) { response ->
                    ResponseCard(
                        text = response.text,
                        hasAudio = response.audioBase64 != null,
                        isPlaying = uiState.playingResponseId == response.id,
                        isTranscript = response.type == ResponseType.TRANSCRIPT,
                        onPlayClick = {
                            if (uiState.playingResponseId == response.id) {
                                audioPlayer.stop()
                                viewModel.setPlayingResponse(null)
                            } else {
                                response.audioBase64?.let { audio ->
                                    audioPlayer.stop()
                                    viewModel.setPlayingResponse(response.id)
                                    audioPlayer.play(
                                        base64Audio = audio,
                                        onCompletion = {
                                            viewModel.setPlayingResponse(null)
                                        },
                                        onError = {
                                            viewModel.setPlayingResponse(null)
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
                
                if (showProgress) {
                    item {
                        ThinkingIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Request error banner (fades after 15 seconds, or tap X)
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.error != null,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    uiState.error?.let { error ->
                        LaunchedEffect(error) {
                            // Play error sound
                            try {
                                val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                                toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 200)
                                kotlinx.coroutines.delay(300)
                                toneGenerator.release()
                            } catch (e: Exception) {
                                // Ignore if tone can't be played
                            }
                            kotlinx.coroutines.delay(15000)
                            viewModel.clearError()
                        }
                        Row(
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onError
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { viewModel.clearError() }
                            )
                        }
                    }
                }
                
                // Connection error banner
                if (!isConnected) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = "Disconnected",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connection issue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpeakButton(
                        onClick = { showSpeakDialog = true },
                        enabled = isConnected && !showProgress
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    MimicButton(
                        isListening = isMimicListening,
                        isProcessing = showProgress || !speechAvailable || !isConnected,
                        onClick = {
                            if (!isConnected) return@MimicButton
                            if (!speechAvailable) return@MimicButton
                            
                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@MimicButton
                            }
                            
                            if (uiState.isListening) {
                                speechRecognizer.stopListening()
                                isMimicListening = false
                            } else if (!showProgress) {
                                listenMode = ListenMode.MIMIC
                                isMimicListening = true
                                speechRecognizer.startListening()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    MicButton(
                        isListening = uiState.isListening && !isMimicListening,
                        isProcessing = showProgress || !speechAvailable || !isConnected,
                        onClick = {
                            if (!isConnected) return@MicButton
                            if (!speechAvailable) return@MicButton
                            
                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@MicButton
                            }
                            
                            if (uiState.isListening) {
                                speechRecognizer.stopListening()
                            } else if (!showProgress) {
                                listenMode = ListenMode.PROMPT
                                speechRecognizer.startListening()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    VoiceSelector(
                        selectedVoice = uiState.selectedVoice,
                        voices = uiState.voices,
                        isLoading = uiState.isLoadingVoices,
                        onExpand = { if (isConnected) viewModel.fetchVoices() },
                        onVoiceSelected = { viewModel.setSelectedVoice(it) },
                        enabled = isConnected
                    )
                }
            }
        }
    }
}
