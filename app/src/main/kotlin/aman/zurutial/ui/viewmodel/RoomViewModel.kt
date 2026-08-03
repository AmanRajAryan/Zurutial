package aman.zurutial.ui.viewmodel

import android.app.Application
import aman.zurutial.data.RecentRoomsManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import aman.zurutial.data.DeviceIdProvider
import aman.zurutial.data.model.Member
import aman.zurutial.data.model.Room
import aman.zurutial.data.repository.RoomRepository
import aman.zurutial.media.FileFingerprint
import aman.zurutial.media.PickedFile
import aman.zurutial.sync.ClockSync
import aman.zurutial.sync.PlaybackController
import aman.zurutial.sync.PlaybackSyncEngine
import aman.zurutial.sync.PingEngine
import com.google.firebase.Firebase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class RoomUiState {
    object Idle : RoomUiState()
    object Connecting : RoomUiState()
    data class JoinRoomFileSelection(val room: aman.zurutial.data.model.Room) : RoomUiState()
    data class InRoom(val room: aman.zurutial.data.model.Room) : RoomUiState()
    data class Error(val message: String) : RoomUiState()
}

class RoomViewModel(application: Application) : AndroidViewModel(application) {

    private val dbUrl = "https://zurutial-fe726-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private val db = Firebase.database(dbUrl)
    private val roomRepository = RoomRepository(db)
    private val deviceId = DeviceIdProvider.getOrCreate(application)

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _toastMessage = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            roomRepository.cleanupOldRooms(deviceId)
        }
    }

    private val _serverPing = MutableStateFlow(0L)
    val serverPing: StateFlow<Long> = _serverPing.asStateFlow()

    private val _e2ePing = MutableStateFlow<Map<String, Long>>(emptyMap())
    val e2ePing: StateFlow<Map<String, Long>> = _e2ePing.asStateFlow()

    fun log(msg: String) {
        val current = _logs.value.toMutableList()
        current.add(0, "${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())} $msg")
        if (current.size > 100) current.removeLast()
        _logs.value = current
    }

    private val _uiState = MutableStateFlow<RoomUiState>(RoomUiState.Idle)
    val uiState: StateFlow<RoomUiState> = _uiState.asStateFlow()

    private val _canControlPlayback = MutableStateFlow(true)
    val canControlPlayback: StateFlow<Boolean> = _canControlPlayback.asStateFlow()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    private var membersListener: ValueEventListener? = null
    private var heartbeatJob: kotlinx.coroutines.Job? = null
    private var syncJob: kotlinx.coroutines.Job? = null
    private var pingJob: kotlinx.coroutines.Job? = null
    private var roomJob: kotlinx.coroutines.Job? = null
    private var cacheLogJob: kotlinx.coroutines.Job? = null

    var player: ExoPlayer? = null
        private set

    private var clockSync: ClockSync? = null
    private var playbackController: PlaybackController? = null
    private var syncEngine: PlaybackSyncEngine? = null
    private var pingEngine: PingEngine? = null
    private var roomListener: ValueEventListener? = null
    private var playbackStateListener: ValueEventListener? = null
    private var connectionListener: ValueEventListener? = null
    private var currentRoomCode: String? = null
    private var pickedFile: PickedFile? = null
    private var currentActingHostId: String? = null
    private var currentMember: Member? = null

    fun getDeviceId(): String = deviceId

    /** Call once, before create/join, after the user has picked their local file. */
    fun setPickedFile(file: PickedFile) {
        log("File picked: ${file.fileName}")
        pickedFile = file
    }

    fun createRoom() {
        val file = pickedFile ?: run {
            _uiState.value = RoomUiState.Error("Pick a file first")
            return
        }
        _uiState.value = RoomUiState.Connecting
        log("Creating room with file hash: ${file.fingerprint}")
        roomJob = viewModelScope.launch {
            val result = roomRepository.createRoom(deviceId, file.fingerprint, file.fileName)
            result.fold(
                onSuccess = { code -> 
                    log("Room created successfully: $code")
                    RecentRoomsManager.addRoom(getApplication(), code, file.fileName)
                    try {
                        enterRoom(code, file) 
                    } catch (e: Exception) {
                        log("Failed to initialize room: ${e.message}")
                        _uiState.value = RoomUiState.Error("Connection lost while initializing")
                    }
                },
                onFailure = { 
                    log("Failed to create room: ${it.message}")
                    _uiState.value = RoomUiState.Error(it.message ?: "Failed to create room") 
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = RoomUiState.Idle
    }

    fun verifyRoomForJoin(roomCode: String) {
        log("Verifying room code: $roomCode")
        _uiState.value = RoomUiState.Connecting
        viewModelScope.launch {
            val result = roomRepository.joinRoom(roomCode)
            result.fold(
                onSuccess = { room ->
                    log("Room found: ${room.roomCode}, file: ${room.fileName}")
                    if (room.fileHash.startsWith("http://") || room.fileHash.startsWith("https://")) {
                        log("Room uses web stream. Auto-joining.")
                        val urlFile = aman.zurutial.media.PickedFile(
                            uri = android.net.Uri.parse(room.fileHash),
                            fileName = room.fileName,
                            sizeBytes = 0L,
                            durationMs = 0L,
                            customFingerprint = room.fileHash
                        )
                        setPickedFile(urlFile)
                        joinRoom(room)
                    } else {
                        _uiState.value = RoomUiState.JoinRoomFileSelection(room)
                    }
                },
                onFailure = { 
                    log("Room not found: ${it.message}")
                    RecentRoomsManager.removeRoom(getApplication(), roomCode)
                    _uiState.value = RoomUiState.Error("Room not found or expired")
                }
            )
        }
    }

    fun joinRoom(room: aman.zurutial.data.model.Room) {
        val file = pickedFile ?: run {
            _uiState.value = RoomUiState.Error("Pick a file first")
            return
        }
        _uiState.value = RoomUiState.Connecting
        log("Joining room ${room.roomCode} with local file ${file.fileName}")
        roomJob = viewModelScope.launch {
            if (!aman.zurutial.media.FileFingerprint.matches(file, room.fileHash)) {
                log("File mismatch: local hash=${file.fingerprint}, room hash=${room.fileHash}")
                _uiState.value = RoomUiState.Error("This isn't the same file as the room")
                return@launch
            }
            log("File match successful. Entering room.")
            RecentRoomsManager.addRoom(getApplication(), room.roomCode, room.fileName)
            try {
                enterRoom(room.roomCode, file)
            } catch (e: Exception) {
                log("Failed to enter room: ${e.message}")
                _uiState.value = RoomUiState.Error("Connection lost while joining")
            }
        }
    }

    private suspend fun enterRoom(roomCode: String, file: PickedFile) {
        currentRoomCode = roomCode
        val roomRef = db.getReference("rooms/$roomCode")

        // Build ExoPlayer pointed at the local file or URL
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
            
        val cacheInstance = aman.zurutial.media.VideoCache.getInstance(getApplication<android.app.Application>())
        
        val cacheDataSourceFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(cacheInstance)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(
            getApplication<android.app.Application>(),
            cacheDataSourceFactory
        )
            
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)

        val exoPlayer = ExoPlayer.Builder(getApplication<android.app.Application>())
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
            setMediaItem(MediaItem.fromUri(file.uri))
            
            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                .setPreferredAudioLanguage("en")
                .build()
                
            prepare()
        }
        player = exoPlayer

        // Add the persistent error listener for mid-movie failures
        exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val cause = error.cause?.message ?: error.message
                log("❌ Playback Error: $cause")
                viewModelScope.launch {
                    leaveRoom(withError = "Video Error: $cause")
                }
            }
        })

        // Estimate clock offset before anything else touches sync
        val sync = ClockSync(roomRef) { msg -> log(msg) }
        val offset = sync.estimateOffset()
        log("Clock offset est: $offset ms")
        clockSync = sync

        // NEW LAZY REJOIN LOGIC
        val snapshot = roomRef.get().await()
        val membersCount = snapshot.child("members").childrenCount
        if (membersCount == 0L) {
            val lastActiveAt = snapshot.child("lastActiveAt").getValue(Long::class.java) ?: 0L
            val isPlaying = snapshot.child("playbackState").child("isPlaying").getValue(Boolean::class.java) ?: false
            if (isPlaying && lastActiveAt > 0L) {
                val positionMs = snapshot.child("playbackState").child("positionMs").getValue(Long::class.java) ?: 0L
                val hostMeasuredAt = snapshot.child("playbackState").child("hostMeasuredAtServerTime").getValue(Long::class.java) ?: 0L
                val elapsed = lastActiveAt - hostMeasuredAt
                if (elapsed > 0) {
                    val newPosition = positionMs + elapsed
                    log("🧹 Lazy Rejoin: Room abandoned. Pausing at $newPosition ms")
                    
                    roomRef.child("playbackState").updateChildren(mapOf(
                        "isPlaying" to false,
                        "positionMs" to newPosition,
                        "hostMeasuredAtServerTime" to com.google.firebase.database.ServerValue.TIMESTAMP,
                        "lastActionType" to "auto-pause"
                    )).await()
                }
            }
        }

        val engine = PlaybackSyncEngine(exoPlayer, sync, deviceId) { msg -> log(msg) }
        syncEngine = engine

        val controller = PlaybackController(roomRef, sync, deviceId) { msg -> log(msg) }
        playbackController = controller

        val ping = PingEngine(roomRef, deviceId)
        pingEngine = ping
        ping.start()

        viewModelScope.launch {
            ping.serverPing.collect { _serverPing.value = it }
        }
        viewModelScope.launch {
            ping.e2ePing.collect { _e2ePing.value = it }
        }

        // Register as a member on connect/reconnect
        val displayName = aman.zurutial.data.SettingsManager.getDisplayName(getApplication()).takeIf { it.isNotBlank() } ?: "Guest"
        currentMember = Member(
            deviceId = deviceId,
            displayName = displayName,
            joinedAt = System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis()
        )
        connectionListener = roomRepository.observeConnection { isConnected ->
            if (isConnected) {
                log("🔌 Firebase socket connected! Registering presence.")
                viewModelScope.launch {
                    currentMember = currentMember?.copy(lastSeen = System.currentTimeMillis())
                    currentMember?.let { roomRepository.addMember(roomCode, it) }
                }
            } else {
                log("🔌 Firebase socket disconnected!")
            }
        }

        // Observe room for canMembersControlPlayback changes.
        // NOTE: playbackState updates should be observed separately (see below)
        // since Room and PlaybackState are stored as sibling nodes, not nested.
        roomListener = roomRepository.observeRoom(roomCode) { room ->
            if (room == null) return@observeRoom
            _uiState.value = RoomUiState.InRoom(room)
            updatePlaybackControl()
        }

        var lastToastSeekVersion = -1L
        playbackStateListener = roomRepository.observePlaybackState(roomCode) { state ->
            if (state == null) return@observePlaybackState
            
            if (state.seekVersion > lastToastSeekVersion) {
                lastToastSeekVersion = state.seekVersion
                if (state.lastActionBy.isNotEmpty() && state.lastActionBy != deviceId && state.lastActionType != "sync") {
                    val actorName = _members.value.find { it.deviceId == state.lastActionBy }?.displayName ?: "Someone"
                    val actionText = when (state.lastActionType) {
                        "play" -> "played the video"
                        "pause" -> "paused the video"
                        "seek" -> "seeked to ${formatTime(state.positionMs)}"
                        else -> state.lastActionType
                    }
                    viewModelScope.launch {
                        _toastMessage.emit("$actorName $actionText")
                    }
                }
            }

            engine.onBroadcastReceived(
                aman.zurutial.sync.PlaybackBroadcast(
                    positionMs = state.positionMs,
                    isPlaying = state.isPlaying,
                    hostMeasuredAtServerTime = state.hostMeasuredAtServerTime,
                    seekVersion = state.seekVersion,
                    lastActionBy = state.lastActionBy,
                    actionType = state.lastActionType,
                    lastActionId = state.lastActionId
                )
            )
        }

        var previousMembers = emptyMap<String, String>()
        membersListener = roomRepository.observeMembers(roomCode) { memberList ->
            val activeMembers = memberList.filter { System.currentTimeMillis() - it.lastSeen < 30_000 }
            val currentMap = activeMembers.associate { it.deviceId to it.displayName }
            
            if (previousMembers.isNotEmpty()) {
                val newMembers = currentMap.keys - previousMembers.keys
                newMembers.forEach { id ->
                    if (id != deviceId) {
                        val name = currentMap[id] ?: "Someone"
                        viewModelScope.launch {
                            _toastMessage.emit("$name joined")
                        }
                    }
                }
                
                val leftMembers = previousMembers.keys - currentMap.keys
                leftMembers.forEach { id ->
                    if (id != deviceId) {
                        val name = previousMembers[id] ?: "Someone"
                        viewModelScope.launch {
                            _toastMessage.emit("$name left")
                        }
                    }
                }
            }
            previousMembers = currentMap
            _members.value = memberList
            
            log("Members updated: ${activeMembers.size} active out of ${memberList.size} total")
            
            updatePlaybackControl()
        }

        heartbeatJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10_000)
                currentMember = currentMember?.copy(lastSeen = System.currentTimeMillis())
                currentMember?.let { roomRepository.updateHeartbeat(roomCode, it) }
                
                // Silent sync pulse from Host
                if (isActingHost()) {
                    val exoPlayer = player
                    if (exoPlayer != null && exoPlayer.playWhenReady) {
                        val actionId = java.util.UUID.randomUUID().toString()
                        syncEngine?.markSelfInitiated(actionId)
                        playbackController?.sendAction(
                            positionMs = exoPlayer.currentPosition,
                            isPlaying = true,
                            actionType = "sync",
                            actionId = actionId
                        )
                    }
                }
            }
        }

        syncJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(500)
                syncEngine?.evaluateDrift()
            }
        }

        pingJob = viewModelScope.launch {
            while (true) {
                pingEngine?.measureServerPing()
                pingEngine?.measureE2EPing()
                kotlinx.coroutines.delay(3000)
            }
        }
        
        cacheLogJob = viewModelScope.launch {
            while (true) {
                val currentMb = cacheInstance.cacheSpace / 1024 / 1024
                log("Disk Cache Update: $currentMb MB currently stored.")
                kotlinx.coroutines.delay(10_000)
            }
        }
    }

    fun onPlayPause() {
        if (canControlPlayback.value != true) return
        val exoPlayer = player ?: return
        val newPlaying = !exoPlayer.playWhenReady // Use intent, not buffering state
        exoPlayer.playWhenReady = newPlaying
        val actionId = java.util.UUID.randomUUID().toString()
        syncEngine?.markSelfInitiated(actionId)
        playbackController?.sendAction(
            positionMs = exoPlayer.currentPosition,
            isPlaying = newPlaying,
            actionType = if (newPlaying) "play" else "pause",
            actionId = actionId
        )
    }

    fun onSeek(positionMs: Long) {
        if (canControlPlayback.value != true) return
        val exoPlayer = player ?: return
        exoPlayer.seekTo(positionMs)
        val actionId = java.util.UUID.randomUUID().toString()
        syncEngine?.markSelfInitiated(actionId)
        playbackController?.sendAction(
            positionMs = positionMs,
            isPlaying = exoPlayer.playWhenReady, // Use intent
            actionType = "seek",
            actionId = actionId
        )
    }

    // Removed onTransientPause as Audio Focus handling is removed

    fun forceResync() {
        syncEngine?.forceResync()
    }

    fun forceHeartbeat() {
        val code = currentRoomCode ?: return
        viewModelScope.launch {
            currentMember = currentMember?.copy(lastSeen = System.currentTimeMillis())
            currentMember?.let { roomRepository.updateHeartbeat(code, it) }
        }
    }

    /** For non-creator users when canMembersControlPlayback == false */
    fun toggleLocalMute() {
        val exoPlayer = player ?: return
        exoPlayer.volume = if (exoPlayer.volume == 0f) 1f else 0f
    }

    fun setMemberControlAllowed(allowed: Boolean) {
        val code = currentRoomCode ?: return
        viewModelScope.launch {
            roomRepository.setMemberControlAllowed(code, allowed)
        }
    }

    private fun cleanupRoom() {
        currentRoomCode?.let { code ->
            roomListener?.let { roomRepository.removeRoomListener(code, it) }
            playbackStateListener?.let { roomRepository.removePlaybackStateListener(code, it) }
            membersListener?.let { roomRepository.removeMembersListener(code, it) }
            connectionListener?.let { roomRepository.removeConnectionListener(it) }
        }
        pingEngine?.stop()
        roomJob?.cancel()
        heartbeatJob?.cancel()
        syncJob?.cancel()
        pingJob?.cancel()
        cacheLogJob?.cancel()
        player?.release()
        player = null
        roomListener = null
        playbackStateListener = null
        membersListener = null
        connectionListener = null
        currentRoomCode = null
        currentMember = null
        pickedFile = null
    }

    override fun onCleared() {
        super.onCleared()
        cleanupRoom()
    }

    fun leaveRoom(withError: String? = null) {
        val code = currentRoomCode ?: return
        viewModelScope.launch {
            try {
                // Graceful active pause if I am the last person leaving
                val activeMembers = _members.value.filter { System.currentTimeMillis() - it.lastSeen < 30_000 }
                if (activeMembers.size == 1 && activeMembers.first().deviceId == deviceId) {
                    val exoPlayer = player
                    if (exoPlayer != null && exoPlayer.playWhenReady) {
                        log("🚪 Last person leaving gracefully. Pausing video.")
                        playbackController?.sendAction(
                            positionMs = exoPlayer.currentPosition,
                            isPlaying = false,
                            actionType = "auto-pause",
                            actionId = java.util.UUID.randomUUID().toString()
                        )
                        kotlinx.coroutines.delay(100) // Give network 100ms to send before we kill socket
                    }
                }
                roomRepository.removeMember(code, deviceId)
            } catch (e: Exception) { }
        }
        cleanupRoom()
        _uiState.value = if (withError != null) RoomUiState.Error(withError) else RoomUiState.Idle
    }

    fun isActingHost(): Boolean {
        val room = (uiState.value as? RoomUiState.InRoom)?.room ?: return false
        val activeMembers = _members.value.filter { System.currentTimeMillis() - it.lastSeen < 30_000 }
        val isOriginalHostHere = activeMembers.any { it.deviceId == room.roomCreatorId }
        
        val newActingHostId = if (isOriginalHostHere) {
            room.roomCreatorId
        } else {
            val oldestMember = activeMembers.minByOrNull { it.joinedAt }
            oldestMember?.deviceId ?: room.roomCreatorId
        }

        val currentlyActing = newActingHostId == deviceId
        if (currentActingHostId != newActingHostId) {
            currentActingHostId = newActingHostId
            if (currentlyActing) {
                log("👑 Host Migration: You are now the Acting Host!")
            } else {
                val name = _members.value.find { it.deviceId == newActingHostId }?.displayName ?: "Someone"
                log("👑 Host Migration: $name is now the Acting Host!")
            }
        }

        return currentlyActing
    }

    private fun updatePlaybackControl() {
        val room = (uiState.value as? RoomUiState.InRoom)?.room ?: return
        _canControlPlayback.value = room.canMembersControlPlayback || isActingHost()
    }
    
    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
