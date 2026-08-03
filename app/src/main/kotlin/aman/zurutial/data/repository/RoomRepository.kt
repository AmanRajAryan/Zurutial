package aman.zurutial.data.repository

import aman.zurutial.data.model.Member
import aman.zurutial.data.model.Room
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

class RoomRepository(private val db: FirebaseDatabase) {
    private val roomsRef = db.getReference("rooms")

    fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no O/0/I/1, avoids ambiguity
        return (1..6).map { chars.random() }.joinToString("")
    }

    suspend fun createRoom(
        creatorId: String,
        fileHash: String,
        fileName: String
    ): Result<String> = suspendCancellableCoroutine { cont ->
        val code = generateRoomCode()
        val room = Room(
            roomCode = code,
            roomCreatorId = creatorId,
            fileHash = fileHash,
            fileName = fileName,
            canMembersControlPlayback = true,
            createdAt = System.currentTimeMillis()
        )

        roomsRef.child(code).setValue(room)
            .addOnSuccessListener { cont.resume(Result.success(code), onCancellation = null) }
            .addOnFailureListener { cont.resume(Result.failure(it), onCancellation = null) }
    }

    suspend fun joinRoom(roomCode: String): Result<Room> = suspendCancellableCoroutine { cont ->
        roomsRef.child(roomCode).get()
            .addOnSuccessListener { snapshot ->
                val room = snapshot.getValue(Room::class.java)
                if (room == null) {
                    cont.resume(Result.failure(Exception("Room not found")), onCancellation = null)
                } else {
                    cont.resume(Result.success(room), onCancellation = null)
                }
            }
            .addOnFailureListener { cont.resume(Result.failure(it), onCancellation = null) }
    }

    suspend fun cleanupOldRooms(creatorId: String) {
        try {
            val snapshot = roomsRef.orderByChild("roomCreatorId").equalTo(creatorId).get().await()
            val now = System.currentTimeMillis()
            for (child in snapshot.children) {
                val room = child.getValue(Room::class.java) ?: continue
                // Delete if older than 12 hours
                if (now - room.createdAt > 12 * 60 * 60 * 1000L) {
                    child.ref.removeValue().await()
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    suspend fun addMember(roomCode: String, member: Member) {
        val memberRef = db.getReference("rooms/$roomCode/members/${member.deviceId}")
        memberRef.onDisconnect().removeValue()
        db.getReference("rooms/$roomCode/lastActiveAt").onDisconnect().setValue(com.google.firebase.database.ServerValue.TIMESTAMP)
        memberRef.setValue(member).await()
    }

    suspend fun updateHeartbeat(roomCode: String, member: Member) {
        db.getReference("rooms/$roomCode/members/${member.deviceId}")
            .setValue(member)
            .await()
    }

    suspend fun setMemberControlAllowed(roomCode: String, allowed: Boolean) {
        db.getReference("rooms/$roomCode/canMembersControlPlayback").setValue(allowed).await()
    }

    fun observeRoom(roomCode: String, onUpdate: (Room?) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onUpdate(snapshot.getValue(Room::class.java))
            }
            override fun onCancelled(error: DatabaseError) { onUpdate(null) }
        }
        roomsRef.child(roomCode).addValueEventListener(listener)
        return listener // caller must removeEventListener on cleanup
    }

    fun removeRoomListener(roomCode: String, listener: ValueEventListener) {
        roomsRef.child(roomCode).removeEventListener(listener)
    }

    fun observePlaybackState(
        roomCode: String,
        onUpdate: (aman.zurutial.data.model.PlaybackState?) -> Unit
    ): ValueEventListener {
        val stateRef = roomsRef.child(roomCode).child("playbackState")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    onUpdate(null)
                    return
                }
                val state = aman.zurutial.data.model.PlaybackState(
                    positionMs = snapshot.child("positionMs").getValue(Long::class.java) ?: 0L,
                    isPlaying = snapshot.child("isPlaying").getValue(Boolean::class.java) ?: false,
                    playbackSpeed = snapshot.child("playbackSpeed").getValue(Float::class.java) ?: 1f,
                    hostMeasuredAtServerTime = snapshot.child("hostMeasuredAtServerTime").getValue(Long::class.java) ?: 0L,
                    seekVersion = snapshot.child("seekVersion").getValue(Long::class.java) ?: 0L,
                    lastActionBy = snapshot.child("lastActionBy").getValue(String::class.java) ?: "",
                    lastActionType = snapshot.child("lastActionType").getValue(String::class.java) ?: "",
                    lastActionId = snapshot.child("lastActionId").getValue(String::class.java) ?: ""
                )
                onUpdate(state)
            }
            override fun onCancelled(error: DatabaseError) { onUpdate(null) }
        }
        stateRef.addValueEventListener(listener)
        return listener
    }

    fun removePlaybackStateListener(roomCode: String, listener: ValueEventListener) {
        roomsRef.child(roomCode).child("playbackState").removeEventListener(listener)
    }

    fun observeMembers(
        roomCode: String,
        onUpdate: (List<Member>) -> Unit
    ): ValueEventListener {
        val membersRef = roomsRef.child(roomCode).child("members")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val members = snapshot.children.mapNotNull { it.getValue(Member::class.java) }
                onUpdate(members)
            }
            override fun onCancelled(error: DatabaseError) { onUpdate(emptyList()) }
        }
        membersRef.addValueEventListener(listener)
        return listener
    }

    fun removeMembersListener(roomCode: String, listener: ValueEventListener) {
        roomsRef.child(roomCode).child("members").removeEventListener(listener)
    }

    suspend fun removeMember(roomCode: String, deviceId: String) {
        val memberRef = db.getReference("rooms/$roomCode/members/$deviceId")
        memberRef.onDisconnect().cancel()
        db.getReference("rooms/$roomCode/lastActiveAt").onDisconnect().cancel()
        memberRef.removeValue().await()
    }

    fun observeConnection(onUpdate: (Boolean) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isConnected = snapshot.getValue(Boolean::class.java) ?: false
                onUpdate(isConnected)
            }
            override fun onCancelled(error: DatabaseError) { onUpdate(false) }
        }
        db.getReference(".info/connected").addValueEventListener(listener)
        return listener
    }

    fun removeConnectionListener(listener: ValueEventListener) {
        db.getReference(".info/connected").removeEventListener(listener)
    }
}
