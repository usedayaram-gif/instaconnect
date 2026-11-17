package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SocialRepository
import com.example.data.unwrapFirestoreException
import com.example.model.AppNotification
import com.example.model.ChatMessage
import com.example.model.ChatThread
import com.example.model.Comment
import com.example.model.FollowRelation
import com.example.model.ModerationReport
import com.example.model.Post
import com.example.model.Story
import com.example.model.UiState
import com.example.model.UserPrivateInfo
import com.example.model.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class InstaConnectViewModel(
  private val repository: SocialRepository,
  val currentUserId: String,
  val currentUserEmail: String?,
  val currentUserDisplayName: String?,
) : ViewModel() {

  private val _isAdmin = MutableStateFlow(false)
  val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

  private val _statusBanner = MutableStateFlow<String?>(null)
  val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

  private val _isDarkModeOverride = MutableStateFlow<Boolean?>(null)
  val isDarkModeOverride: StateFlow<Boolean?> = _isDarkModeOverride.asStateFlow()

  init {
    viewModelScope.launch {
      repository.ensureUserProfile(
        displayName = currentUserDisplayName,
        email = currentUserEmail,
      )
      _isAdmin.value = repository.checkIsAdmin(currentUserId, currentUserEmail)
    }
  }

  val currentProfile: StateFlow<UiState<UserProfile?>> =
    repository
      .observeUserProfile(currentUserId)
      .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        val root = fsError ?: error
        Log.w(TAG, "Error observing profile", root)
        emit(UiState.Error(root.message ?: "Failed to load profile"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  val privateInfo: StateFlow<UserPrivateInfo?> =
    repository
      .observePrivateInfo(currentUserId)
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing private info", fsError ?: error)
        emit(null)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

  val feedPosts: StateFlow<UiState<List<Post>>> =
    repository
      .observePublicPosts()
      .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        val root = fsError ?: error
        Log.w(TAG, "Error observing feed posts", root)
        emit(UiState.Error(root.message ?: "Failed to load feed"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  val stories: StateFlow<List<Story>> =
    repository
      .observeActiveStories()
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing stories", fsError ?: error)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val publicUsers: StateFlow<List<UserProfile>> =
    repository
      .observePublicUsers()
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing users", fsError ?: error)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val followingIds: StateFlow<Set<String>> =
    repository
      .observeFollowingIds(currentUserId)
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing following IDs", fsError ?: error)
        emit(emptySet())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

  val chats: StateFlow<List<ChatThread>> =
    repository
      .observeChats(currentUserId)
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing chats", fsError ?: error)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val notifications: StateFlow<List<AppNotification>> =
    repository
      .observeNotifications(currentUserId)
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing notifications", fsError ?: error)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  // Admin streams are strictly gated behind _isAdmin so non-admins never issue admin list queries
  val adminUsers: StateFlow<List<UserProfile>> =
    _isAdmin
      .flatMapLatest { admin ->
        if (admin) repository.observeAllUsersForAdmin() else flowOf(emptyList())
      }
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        Log.w(TAG, "Admin users stream error", fsError ?: error)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val adminPosts: StateFlow<List<Post>> =
    _isAdmin
      .flatMapLatest { admin ->
        if (admin) repository.observeAllPostsForAdmin() else flowOf(emptyList())
      }
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        Log.w(TAG, "Admin posts stream error", fsError ?: error)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  val adminReports: StateFlow<List<ModerationReport>> =
    _isAdmin
      .flatMapLatest { admin ->
        if (admin) repository.observeReportsForAdmin() else flowOf(emptyList())
      }
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        Log.w(TAG, "Admin reports stream error", fsError ?: error)
        emit(emptyList())
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  fun observeUserPosts(userId: String): Flow<List<Post>> =
    repository.observeUserPosts(userId).catch { emit(emptyList()) }

  fun observeUserProfile(userId: String): Flow<UserProfile?> =
    repository.observeUserProfile(userId).catch { emit(null) }

  fun observeFollowers(userId: String): Flow<List<FollowRelation>> =
    repository.observeFollowers(userId).catch { emit(emptyList()) }

  fun observeFollowing(userId: String): Flow<List<FollowRelation>> =
    repository.observeFollowing(userId).catch { emit(emptyList()) }

  fun observePostLikeState(postId: String): Flow<Boolean> =
    repository.observePostLikeState(postId, currentUserId).catch { emit(false) }

  fun observeComments(postId: String): Flow<List<Comment>> =
    repository.observeComments(postId).catch { emit(emptyList()) }

  fun observeMessages(chatId: String): Flow<List<ChatMessage>> =
    repository.observeMessages(chatId, currentUserId).catch { emit(emptyList()) }

  fun clearStatusBanner() {
    _statusBanner.value = null
  }

  fun postStatusBanner(message: String) {
    _statusBanner.value = message
  }

  fun toggleDarkTheme(currentlyDark: Boolean) {
    _isDarkModeOverride.value = !currentlyDark
  }

  fun updateProfile(
    username: String,
    displayName: String,
    bio: String,
    photoDataUrl: String?,
    isPublic: Boolean,
    email: String?,
    phoneNumber: String?,
    onComplete: () -> Unit = {},
  ) {
    viewModelScope.launch {
      repository
        .updateProfile(username, displayName, bio, photoDataUrl, isPublic, email, phoneNumber)
        .onSuccess {
          _statusBanner.value = "Profile updated"
          onComplete()
        }
        .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to update profile" }
    }
  }

  fun toggleFollow(targetUser: UserProfile, isCurrentlyFollowing: Boolean) {
    val me = (currentProfile.value as? UiState.Success)?.data ?: return
    if (me.userId == targetUser.userId) return
    viewModelScope.launch {
      repository.toggleFollow(me, targetUser, isCurrentlyFollowing).onFailure {
        _statusBanner.value = it.localizedMessage ?: "Could not update follow status"
      }
    }
  }

  fun createPost(
    postKind: String,
    mediaType: String,
    mediaDataUrl: String,
    videoUri: String?,
    caption: String,
    audioTitle: String?,
    onCreated: () -> Unit = {},
  ) {
    val me = (currentProfile.value as? UiState.Success)?.data ?: return
    if (me.isSuspended) {
      _statusBanner.value = "Your account is currently suspended from posting."
      return
    }
    viewModelScope.launch {
      repository
        .createPost(me, postKind, mediaType, mediaDataUrl, videoUri, caption, audioTitle)
        .onSuccess {
          _statusBanner.value = if (postKind == Post.KIND_REEL) "Reel published!" else "Post shared!"
          onCreated()
        }
        .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to publish" }
    }
  }

  fun deletePost(post: Post) {
    viewModelScope.launch {
      repository
        .deletePost(post)
        .onSuccess { _statusBanner.value = "Post deleted" }
        .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to delete post" }
    }
  }

  fun toggleLike(post: Post, currentlyLiked: Boolean) {
    val me = (currentProfile.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository.toggleLike(post, me, currentlyLiked).onFailure {
        _statusBanner.value = it.localizedMessage ?: "Failed to update like"
      }
    }
  }

  fun addComment(post: Post, text: String) {
    val me = (currentProfile.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository.addComment(post, me, text).onFailure {
        _statusBanner.value = it.localizedMessage ?: "Failed to post comment"
      }
    }
  }

  fun sharePost(postId: String) {
    viewModelScope.launch {
      repository
        .incrementShareCount(postId)
        .onSuccess { _statusBanner.value = "Share link copied & shared!" }
    }
  }

  fun createStory(
    mediaDataUrl: String,
    caption: String?,
    musicTitle: String?,
    stickerLabel: String?,
    onCreated: () -> Unit = {},
  ) {
    val me = (currentProfile.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository
        .createStory(me, mediaDataUrl, caption, musicTitle, stickerLabel)
        .onSuccess {
          _statusBanner.value = "Story live for 24 hours!"
          onCreated()
        }
        .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to upload story" }
    }
  }

  fun markStoryViewed(storyId: String) {
    viewModelScope.launch { repository.markStoryViewed(storyId) }
  }

  fun openOrCreateChat(peerUser: UserProfile, onChatReady: (String) -> Unit) {
    val me = (currentProfile.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository
        .getOrCreateChat(me, peerUser)
        .onSuccess { chatId -> onChatReady(chatId) }
        .onFailure { _statusBanner.value = it.localizedMessage ?: "Unable to start chat" }
    }
  }

  fun sendMessage(
    chatId: String,
    peerUserId: String,
    messageType: String,
    text: String,
    mediaDataUrl: String? = null,
    videoUri: String? = null,
    audioDurationSec: Int? = null,
  ) {
    val me = (currentProfile.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository
        .sendMessage(
          chatId = chatId,
          currentUser = me,
          peerUserId = peerUserId,
          messageType = messageType,
          text = text,
          mediaDataUrl = mediaDataUrl,
          videoUri = videoUri,
          audioDurationSec = audioDurationSec,
        )
        .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to send message" }
    }
  }

  fun markNotificationRead(notificationId: String) {
    viewModelScope.launch { repository.markNotificationRead(currentUserId, notificationId) }
  }

  fun submitReport(targetType: String, targetId: String, targetSummary: String, reason: String) {
    viewModelScope.launch {
      repository
        .submitReport(targetType, targetId, targetSummary, reason)
        .onSuccess { _statusBanner.value = "Report submitted for review" }
        .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to submit report" }
    }
  }

  fun resolveReport(reportId: String, status: String) {
    viewModelScope.launch {
      repository
        .resolveReport(reportId, status)
        .onSuccess { _statusBanner.value = "Report marked $status" }
        .onFailure { _statusBanner.value = it.localizedMessage ?: "Action failed" }
    }
  }

  fun adminDeletePost(postId: String) {
    viewModelScope.launch {
      repository
        .adminDeletePost(postId)
        .onSuccess { _statusBanner.value = "Post removed by admin" }
        .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to delete post" }
    }
  }

  fun adminToggleSuspend(user: UserProfile) {
    viewModelScope.launch {
      val next = !user.isSuspended
      repository
        .adminSetUserSuspended(user.userId, next)
        .onSuccess {
          _statusBanner.value =
            if (next) "@${user.username} suspended" else "@${user.username} reinstated"
        }
        .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to update user status" }
    }
  }

  private companion object {
    const val TAG = "InstaConnectVM"
    const val STOP_TIMEOUT_MS = 5000L
  }
}
