package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "",
  val displayName: String = "",
  val bio: String = "",
  val photoDataUrl: String? = null,
  val isPublic: Boolean = true,
  val isSuspended: Boolean = false,
  val followersCount: Long = 0L,
  val followingCount: Long = 0L,
  val postsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class UserPrivateInfo(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String? = null,
  val phoneNumber: String? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FollowRelation(
  @DocumentId val id: String = "",
  val userId: String = "",
  val targetUserId: String = "",
  val username: String = "",
  val photoDataUrl: String? = null,
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Post(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val authorUsername: String = "",
  val authorPhotoDataUrl: String? = null,
  val postKind: String = KIND_POST,
  val mediaType: String = MEDIA_IMAGE,
  val mediaDataUrl: String = "",
  val videoUri: String? = null,
  val caption: String = "",
  val hashtags: List<String> = emptyList(),
  val audioTitle: String? = null,
  val visibility: String = VISIBILITY_PUBLIC,
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  val sharesCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  companion object {
    const val KIND_POST = "post"
    const val KIND_REEL = "reel"
    const val MEDIA_IMAGE = "image"
    const val MEDIA_VIDEO = "video"
    const val VISIBILITY_PUBLIC = "public"
    const val VISIBILITY_REMOVED = "removed"
  }
}

@IgnoreExtraProperties
data class PostLike(
  @DocumentId val id: String = "",
  val postId: String = "",
  val userId: String = "",
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Comment(
  @DocumentId val id: String = "",
  val postId: String = "",
  val authorId: String = "",
  val authorUsername: String = "",
  val authorPhotoDataUrl: String? = null,
  val text: String = "",
  val isVisible: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Story(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val authorUsername: String = "",
  val authorPhotoDataUrl: String? = null,
  val mediaDataUrl: String = "",
  val caption: String? = null,
  val musicTitle: String? = null,
  val stickerLabel: String? = null,
  val visibility: String = "active",
  val expiresAtEpochMs: Long = 0L,
  val viewsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatThread(
  @DocumentId val id: String = "",
  val participantIds: List<String> = emptyList(),
  val participantUsernames: Map<String, String> = emptyMap(),
  val participantPhotos: Map<String, String> = emptyMap(),
  val lastMessageText: String = "",
  val lastMessageType: String = "text",
  val lastSenderId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val chatId: String = "",
  val senderId: String = "",
  val participantIds: List<String> = emptyList(),
  val messageType: String = TYPE_TEXT,
  val text: String = "",
  val mediaDataUrl: String? = null,
  val videoUri: String? = null,
  val audioDurationSec: Long? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  companion object {
    const val TYPE_TEXT = "text"
    const val TYPE_IMAGE = "image"
    const val TYPE_VIDEO = "video"
    const val TYPE_VOICE = "voice"
  }
}

@IgnoreExtraProperties
data class AppNotification(
  @DocumentId val id: String = "",
  val recipientId: String = "",
  val actorId: String = "",
  val actorUsername: String = "",
  val actorPhotoDataUrl: String? = null,
  val type: String = TYPE_LIKE,
  val targetId: String = "",
  val previewText: String = "",
  val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  companion object {
    const val TYPE_FOLLOW = "follow"
    const val TYPE_LIKE = "like"
    const val TYPE_COMMENT = "comment"
    const val TYPE_MESSAGE = "message"
  }
}

@IgnoreExtraProperties
data class ModerationReport(
  @DocumentId val id: String = "",
  val reporterId: String = "",
  val targetType: String = "post",
  val targetId: String = "",
  val targetSummary: String = "",
  val reason: String = "",
  val status: String = STATUS_OPEN,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  companion object {
    const val STATUS_OPEN = "open"
    const val STATUS_RESOLVED = "resolved"
    const val STATUS_DISMISSED = "dismissed"
  }
}
