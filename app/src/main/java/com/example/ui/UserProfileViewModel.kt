package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SocialRepository
import com.example.data.unwrapFirestoreException
import com.example.model.FollowRelation
import com.example.model.Post
import com.example.model.UiState
import com.example.model.UserPrivateInfo
import com.example.model.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UserProfileUiState(
  val profileState: UiState<UserProfile?> = UiState.Loading,
  val isOwnProfile: Boolean = false,
  val avatarDataUrl: String? = null,
  val username: String = "",
  val displayName: String = "",
  val bio: String = "",
  val followersCount: Long = 0L,
  val followingCount: Long = 0L,
  val postsCount: Long = 0L,
  val posts: List<Post> = emptyList(),
  val reels: List<Post> = emptyList(),
  val followers: List<FollowRelation> = emptyList(),
  val following: List<FollowRelation> = emptyList(),
  val isFollowing: Boolean = false,
  val isFollowUpdating: Boolean = false,
  val isRefreshing: Boolean = false,
  val selectedTab: Int = 0,
  val privateInfo: UserPrivateInfo? = null,
  val statusMessage: String? = null,
) {
  val profile: UserProfile?
    get() = (profileState as? UiState.Success)?.data
}

class UserProfileViewModel(
  private val repository: SocialRepository,
  val currentUserId: String,
  val viewedUserId: String,
) : ViewModel() {

  private val isOwnProfile = currentUserId == viewedUserId

  private val _selectedTab = MutableStateFlow(0)
  private val _isFollowUpdating = MutableStateFlow(false)
  private val _isRefreshing = MutableStateFlow(false)
  private val _statusMessage = MutableStateFlow<String?>(null)
  private val _refreshedFollowerCount = MutableStateFlow<Long?>(null)
  private val _refreshedProfileOverride = MutableStateFlow<UserProfile?>(null)

  private val currentUserProfileFlow =
    repository
      .observeUserProfile(currentUserId)
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing current user profile", fsError ?: error)
        emit(null)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

  private val targetProfileStateFlow =
    repository
      .observeUserProfile(viewedUserId)
      .onEach { _refreshedFollowerCount.value = null }
      .combine(_refreshedProfileOverride) { streamed, refreshed ->
        streamed ?: refreshed
      }
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        val root = fsError ?: error
        Log.w(TAG, "Error observing viewed user profile", root)
        _statusMessage.value = root.localizedMessage ?: "Failed to load profile"
        emit(null)
      }

  private val userPostsFlow =
    repository
      .observeUserPosts(viewedUserId)
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing user posts", fsError ?: error)
        emit(emptyList())
      }

  private val followersFlow =
    repository
      .observeFollowers(viewedUserId)
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing followers", fsError ?: error)
        emit(emptyList())
      }

  private val followingFlow =
    repository
      .observeFollowing(viewedUserId)
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing following", fsError ?: error)
        emit(emptyList())
      }

  private val isFollowingFlow =
    if (isOwnProfile) {
      flowOf(false)
    } else {
      repository
        .observeIsFollowing(currentUserId, viewedUserId)
        .catch { error ->
          val fsError = error.unwrapFirestoreException()
          if (fsError == null && error is CancellationException) throw error
          Log.w(TAG, "Error observing follow relationship", fsError ?: error)
          emit(false)
        }
    }

  private val privateInfoFlow =
    if (isOwnProfile) {
      repository
        .observePrivateInfo(currentUserId)
        .catch { error ->
          val fsError = error.unwrapFirestoreException()
          if (fsError == null && error is CancellationException) throw error
          Log.w(TAG, "Error observing private info", fsError ?: error)
          emit(null)
        }
    } else {
      flowOf(null)
    }

  private val countsAndFollowBundleFlow =
    combine(
      followersFlow,
      followingFlow,
      isFollowingFlow,
      _refreshedFollowerCount,
    ) { followers, following, isFollowing, refreshedFollowers ->
      FollowBundle(followers, following, isFollowing, refreshedFollowers)
    }

  private val uiFlagsBundleFlow =
    combine(
      _selectedTab,
      _isFollowUpdating,
      _isRefreshing,
      _statusMessage,
    ) { tab, updating, refreshing, message ->
      UiFlagsBundle(tab, updating, refreshing, message)
    }

  val uiState: StateFlow<UserProfileUiState> =
    combine(
      targetProfileStateFlow,
      userPostsFlow,
      privateInfoFlow,
      countsAndFollowBundleFlow,
      uiFlagsBundleFlow,
    ) { profile, allUserPosts, privateInfo, followBundle, flags ->
      val gridPosts = allUserPosts.filter { it.postKind == Post.KIND_POST }
      val reels = allUserPosts.filter { it.postKind == Post.KIND_REEL }
      val effectiveFollowersCount =
        followBundle.refreshedFollowerCount
          ?: profile?.followersCount
          ?: followBundle.followers.size.toLong()
      val effectiveFollowingCount =
        profile?.followingCount ?: followBundle.following.size.toLong()

      UserProfileUiState(
        profileState =
          if (profile != null) UiState.Success(profile) else UiState.Loading,
        isOwnProfile = isOwnProfile,
        avatarDataUrl = profile?.photoDataUrl,
        username = profile?.username.orEmpty(),
        displayName = profile?.displayName.orEmpty(),
        bio = profile?.bio.orEmpty(),
        followersCount = effectiveFollowersCount.coerceAtLeast(0L),
        followingCount = effectiveFollowingCount.coerceAtLeast(0L),
        postsCount = allUserPosts.size.toLong(),
        posts = gridPosts,
        reels = reels,
        followers = followBundle.followers,
        following = followBundle.following,
        isFollowing = followBundle.isFollowing,
        isFollowUpdating = flags.isFollowUpdating,
        isRefreshing = flags.isRefreshing,
        selectedTab = flags.selectedTab,
        privateInfo = privateInfo,
        statusMessage = flags.statusMessage,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
      initialValue = UserProfileUiState(isOwnProfile = isOwnProfile),
    )

  fun selectTab(tabIndex: Int) {
    _selectedTab.value = tabIndex.coerceIn(0, 1)
  }

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  fun toggleFollow() {
    if (isOwnProfile || _isFollowUpdating.value) return
    val currentState = uiState.value
    val targetProfile = currentState.profile ?: return
    val currentlyFollowing = currentState.isFollowing

    viewModelScope.launch {
      _isFollowUpdating.value = true
      try {
        val currentProfile =
          currentUserProfileFlow.value
            ?: repository.fetchUserProfileOnce(currentUserId).getOrNull()
        if (currentProfile == null) {
          _statusMessage.value = "Please finish setting up your profile first."
          return@launch
        }

        repository
          .toggleFollow(
            currentUser = currentProfile,
            targetUser = targetProfile,
            isCurrentlyFollowing = currentlyFollowing,
          )
          .onSuccess { nowFollowing ->
            refreshFollowerStateFromFirestore()
            _statusMessage.value =
              if (nowFollowing) "Following @${targetProfile.username}"
              else "Unfollowed @${targetProfile.username}"
          }
          .onFailure { error ->
            _statusMessage.value =
              error.localizedMessage ?: "Failed to update follow status"
          }
      } finally {
        _isFollowUpdating.value = false
      }
    }
  }

  fun refreshProfile() {
    if (_isRefreshing.value) return
    viewModelScope.launch {
      _isRefreshing.value = true
      try {
        refreshFollowerStateFromFirestore()
      } finally {
        _isRefreshing.value = false
      }
    }
  }

  private suspend fun refreshFollowerStateFromFirestore() {
    val latestProfile = repository.fetchUserProfileOnce(viewedUserId).getOrNull()
    if (latestProfile != null) {
      _refreshedProfileOverride.value = latestProfile
      _refreshedFollowerCount.value = latestProfile.followersCount
    } else {
      val counted = repository.countActiveFollowers(viewedUserId).getOrNull()
      if (counted != null) {
        _refreshedFollowerCount.value = counted
      }
    }
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
    if (!isOwnProfile) return
    viewModelScope.launch {
      repository
        .updateProfile(username, displayName, bio, photoDataUrl, isPublic, email, phoneNumber)
        .onSuccess {
          refreshFollowerStateFromFirestore()
          _statusMessage.value = "Profile updated"
          onComplete()
        }
        .onFailure { error ->
          _statusMessage.value = error.localizedMessage ?: "Failed to update profile"
        }
    }
  }

  private data class FollowBundle(
    val followers: List<FollowRelation>,
    val following: List<FollowRelation>,
    val isFollowing: Boolean,
    val refreshedFollowerCount: Long?,
  )

  private data class UiFlagsBundle(
    val selectedTab: Int,
    val isFollowUpdating: Boolean,
    val isRefreshing: Boolean,
    val statusMessage: String?,
  )

  private companion object {
    const val TAG = "UserProfileVM"
    const val STOP_TIMEOUT_MS = 5000L
  }
}
