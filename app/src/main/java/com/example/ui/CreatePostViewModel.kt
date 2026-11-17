package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SocialRepository
import com.example.model.Post
import com.example.model.UserProfile
import com.example.util.MediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CreatePostUiState(
  val postKind: String = Post.KIND_POST,
  val mediaType: String = Post.MEDIA_IMAGE,
  val mediaDataUrl: String? = null,
  val videoUri: String? = null,
  val caption: String = "",
  val audioTitle: String = "",
  val isProcessingMedia: Boolean = false,
  val isSaving: Boolean = false,
  val errorMessage: String? = null,
  val savedPostId: String? = null,
) {
  val extractedHashtags: List<String>
    get() =
      Regex("#([a-zA-Z0-9_]+)")
        .findAll(caption)
        .map { it.groupValues[1].lowercase() }
        .distinct()
        .take(10)
        .toList()
}

class CreatePostViewModel(
  private val repository: SocialRepository,
  private val currentUserId: String,
  initialKind: String = Post.KIND_POST,
) : ViewModel() {

  private val _uiState = MutableStateFlow(CreatePostUiState(postKind = initialKind))
  val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

  fun setPostKind(kind: String) {
    _uiState.update { it.copy(postKind = kind) }
  }

  fun setCaption(caption: String) {
    _uiState.update { it.copy(caption = caption.take(500), errorMessage = null) }
  }

  fun setAudioTitle(audioTitle: String) {
    _uiState.update { it.copy(audioTitle = audioTitle.take(100)) }
  }

  fun setPermissionOrMediaError(message: String) {
    _uiState.update { it.copy(errorMessage = message) }
  }

  fun clearMedia() {
    _uiState.update {
      it.copy(mediaType = Post.MEDIA_IMAGE, mediaDataUrl = null, videoUri = null)
    }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun resetDraft(initialKind: String = Post.KIND_POST) {
    _uiState.value = CreatePostUiState(postKind = initialKind)
  }

  fun onImageSelectedOrCaptured(context: Context, uri: Uri) {
    val appContext = context.applicationContext
    _uiState.update { it.copy(isProcessingMedia = true, errorMessage = null) }
    viewModelScope.launch {
      val encoded =
        withContext(Dispatchers.IO) { MediaUtils.encodeImageUriToDataUrl(appContext, uri) }
      if (encoded != null) {
        _uiState.update {
          it.copy(
            mediaType = Post.MEDIA_IMAGE,
            mediaDataUrl = encoded,
            videoUri = null,
            isProcessingMedia = false,
          )
        }
      } else {
        _uiState.update {
          it.copy(
            isProcessingMedia = false,
            errorMessage = "Could not load the selected image. Please try another photo.",
          )
        }
      }
    }
  }

  fun onVideoSelected(context: Context, uri: Uri) {
    val appContext = context.applicationContext
    _uiState.update { it.copy(isProcessingMedia = true, errorMessage = null) }
    viewModelScope.launch {
      val (thumb, localUri) =
        withContext(Dispatchers.IO) {
          MediaUtils.extractVideoThumbnailDataUrl(appContext, uri) to
            MediaUtils.copyVideoToInternalStorage(appContext, uri)
        }
      _uiState.update {
        it.copy(
          mediaType = Post.MEDIA_VIDEO,
          mediaDataUrl = thumb,
          videoUri = localUri,
          isProcessingMedia = false,
        )
      }
    }
  }

  fun savePostToFirestore(
    author: UserProfile?,
    onSuccess: (postId: String, postKind: String) -> Unit,
  ) {
    val snapshot = _uiState.value
    if (snapshot.isSaving || snapshot.isProcessingMedia) return

    if (snapshot.mediaDataUrl == null && snapshot.caption.isBlank()) {
      _uiState.update {
        it.copy(errorMessage = "Capture or select an image, or write a caption before sharing.")
      }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSaving = true, errorMessage = null) }
      val resolvedAuthor =
        author ?: repository.fetchUserProfileOnce(currentUserId).getOrNull()
      if (resolvedAuthor == null) {
        _uiState.update {
          it.copy(
            isSaving = false,
            errorMessage = "Profile not ready yet. Please try again in a moment.",
          )
        }
        return@launch
      }
      if (resolvedAuthor.isSuspended) {
        _uiState.update {
          it.copy(
            isSaving = false,
            errorMessage = "Your account is currently suspended from posting.",
          )
        }
        return@launch
      }

      val finalMedia =
        snapshot.mediaDataUrl
          ?: MediaUtils.generateGradientCoverDataUrl(
            snapshot.caption.ifBlank { "InstaConnect ${System.currentTimeMillis()}" }
          )

      repository
        .createPost(
          author = resolvedAuthor,
          postKind = snapshot.postKind,
          mediaType = snapshot.mediaType,
          mediaDataUrl = finalMedia,
          videoUri = snapshot.videoUri,
          caption = snapshot.caption,
          audioTitle = snapshot.audioTitle.takeIf { it.isNotBlank() },
        )
        .onSuccess { postId ->
          _uiState.update { it.copy(isSaving = false, savedPostId = postId) }
          onSuccess(postId, snapshot.postKind)
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(
              isSaving = false,
              errorMessage = error.localizedMessage ?: "Failed to save post to Firestore.",
            )
          }
        }
    }
  }
}
