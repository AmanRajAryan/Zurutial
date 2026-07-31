package aman.zurutial.ui.viewmodel

import android.app.Application
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
import kotlinx.coroutines.launch

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

    var player: ExoPlayer? = null
        private set

    private var clockSync: ClockSync? = null
    private var playbackController: PlaybackController? = null
    private var syncEngine: PlaybackSyncEngine? = null
    private var pingEngine: PingEngine? = null
    private var roomListener: ValueEventListener? = null
    private var playbackStateListener: ValueEventListener? = null
    private var currentRoomCode: String? = null
    private var pickedFile: PickedFile? = null

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
        viewModelScope.launch {
            val result = roomRepository.createRoom(deviceId, file.fingerprint, file.fileName)
            result.fold(
                onSuccess = { code -> 
                    log("Room created successfully: $code")
                    enterRoom(code, file) 
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
                    _uiState.value = RoomUiState.JoinRoomFileSelection(room)
                },
                onFailure = { 
                    log("Room not found: ${it.message}")
                    _uiState.value = RoomUiState.Error(it.message ?: "Room not found") 
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
        viewModelScope.launch {
            if (!aman.zurutial.media.FileFingerprint.matches(file, room.fileHash)) {
                log("File mismatch: local hash=${file.fingerprint}, room hash=${room.fileHash}")
                _uiState.value = RoomUiState.Error("This isn't the same file as the room")
                return@launch
            }
            log("File match successful. Entering room.")
            enterRoom(room.roomCode, file)
        }
    }

    private suspend fun enterRoom(roomCode: String, file: PickedFile) {
        currentRoomCode = roomCode
        val roomRef = db.getReference("rooms/$roomCode")

        // Build ExoPlayer pointed at the local file
        val exoPlayer = ExoPlayer.Builder(getApplication()).build().apply {
            setMediaItem(MediaItem.fromUri(file.uri))
            prepare()
        }
        player = exoPlayer

        // Estimate clock offset before anything else touches sync
        val sync = ClockSync(roomRef)
        val offset = sync.estimateOffset()
        log("Clock offset est: $offset ms")
        clockSync = sync

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

        // Register as a member
        roomRepository.addMember(
            roomCode,
            Member(
                deviceId = deviceId,
                displayName = "Guest",
                joinedAt = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis()
            )
        )

        // Observe room for canMembersControlPlayback changes.
        // NOTE: playbackState updates should be observed separately (see below)
        // since Room and PlaybackState are stored as sibling nodes, not nested.
        roomListener = roomRepository.observeRoom(roomCode) { room ->
            if (room == null) return@observeRoom
            _canControlPlayback.value = room.canMembersControlPlayback || room.roomCreatorId == deviceId
            _uiState.value = RoomUiState.InRoom(room)
        }

        playbackStateListener = roomRepository.observePlaybackState(roomCode) { state ->
            if (state == null) return@observePlaybackState
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

        membersListener = roomRepository.observeMembers(roomCode) { memberList ->
            _members.value = memberList
        }

        heartbeatJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10_000)
                roomRepository.updateHeartbeat(roomCode, deviceId)
            }
        }

        viewModelScope.launch {
            while (true) {
                pingEngine?.measureServerPing()
                pingEngine?.measureE2EPing()
                kotlinx.coroutines.delay(3000)
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

    fun forceResync() {
        syncEngine?.forceResync()
    }

    fun forceHeartbeat() {
        val code = currentRoomCode ?: return
        viewModelScope.launch {
            roomRepository.updateHeartbeat(code, deviceId)
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

    override fun onCleared() {
        super.onCleared()
        currentRoomCode?.let { code ->
            roomListener?.let { roomRepository.removeRoomListener(code, it) }
            playbackStateListener?.let { roomRepository.removePlaybackStateListener(code, it) }
            membersListener?.let { roomRepository.removeMembersListener(code, it) }
        }
        pingEngine?.stop()
        heartbeatJob?.cancel()
        player?.release()
    }
}
