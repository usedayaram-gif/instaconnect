package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AppNotification
import com.example.model.ChatMessage
import com.example.model.ChatThread
import com.example.model.Comment
import com.example.model.FollowRelation
import com.example.model.ModerationReport
import com.example.model.Post
import com.example.model.PostLike
import com.example.model.Story
import com.example.model.UserPrivateInfo
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SocialRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db =
      FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
      ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before accessing Firestore.")

  private fun <T> wrapFlowError(
    flow: Flow<T>,
    operationType: OperationType,
    path: String,
    expectedUid: String? = auth.currentUser?.uid,
  ): Flow<T> =
    flow.catch { error ->
      val firestoreError = error.unwrapFirestoreException()
      if (firestoreError == null && error is CancellationException) throw error
      val exceptionToLog = firestoreError ?: (error as? Exception)
      if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
        handleFirestoreError(exceptionToLog, operationType, path)
      }
      throw error
    }

  // --- Users & Profiles ---

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val path = "users/$userId"
    emitAll(
      wrapFlowError(
        db.collection("users")
          .document(userId)
          .snapshots()
          .map { snapshot ->
            if (snapshot.exists()) {
              snapshot.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              )
            } else {
              null
            }
          },
        OperationType.GET,
        path,
      )
    )
  }

  fun observePublicUsers(): Flow<List<UserProfile>> = flow {
    val path = "users"
    emitAll(
      wrapFlowError(
        db.collection("users")
          .whereEqualTo("isPublic", true)
          .snapshots()
          .map { snapshot ->
            snapshot
              .toObjects(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              )
              .sortedByDescending { it.followersCount }
          },
        OperationType.LIST,
        path,
      )
    )
  }

  fun observeAllUsersForAdmin(): Flow<List<UserProfile>> = flow {
    val path = "users"
    emitAll(
      wrapFlowError(
        db.collection("users")
          .snapshots()
          .map { snapshot ->
            snapshot
              .toObjects(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              )
              .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
          },
        OperationType.LIST,
        path,
      )
    )
  }

  fun observePrivateInfo(userId: String): Flow<UserPrivateInfo?> = flow {
    val path = "users/$userId/private/info"
    emitAll(
      wrapFlowError(
        db.collection("users")
          .document(userId)
          .collection("private")
          .document("info")
          .snapshots()
          .map { snapshot ->
            if (snapshot.exists()) {
              snapshot.toObject(
                UserPrivateInfo::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              )
            } else {
              null
            }
          },
        OperationType.GET,
        path,
        expectedUid = userId,
      )
    )
  }

  suspend fun ensureUserProfile(
    displayName: String?,
    email: String?,
    phoneNumber: String? = null,
    photoDataUrl: String? = null,
  ): Result<UserProfile> = runCatching {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    val existingSnap = userRef.get().await()
    if (existingSnap.exists()) {
      val existing =
        existingSnap.toObject(
          UserProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      if (existing != null) return@runCatching existing
    }

    val safeDisplay = displayName?.trim()?.takeIf { it.isNotEmpty() }?.take(50) ?: "InstaConnector"
    val rawHandle =
      (email?.substringBefore("@") ?: safeDisplay)
        .lowercase()
        .replace(Regex("[^a-z0-9_.]"), "")
        .take(22)
    val safeUsername =
      if (rawHandle.length >= 3) rawHandle else "user_${uid.take(6).lowercase()}"

    val payload =
      mutableMapOf<String, Any>(
        "userId" to uid,
        "username" to safeUsername,
        "displayName" to safeDisplay,
        "bio" to "Creating moments on InstaConnect",
        "isPublic" to true,
        "isSuspended" to false,
        "followersCount" to 0L,
        "followingCount" to 0L,
        "postsCount" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    if (!photoDataUrl.isNullOrBlank()) {
      payload["photoDataUrl"] = photoDataUrl
    }

    try {
      userRef.set(payload).await()
      val privatePayload =
        mapOf(
            "userId" to uid,
            "email" to email?.take(120),
            "phoneNumber" to phoneNumber?.take(30),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
          .filterValues { it != null } as Map<String, Any>
      userRef.collection("private").document("info").set(privatePayload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, userRef.path)
      throw e
    }

    UserProfile(
      id = uid,
      userId = uid,
      username = safeUsername,
      displayName = safeDisplay,
      bio = "Creating moments on InstaConnect",
      photoDataUrl = photoDataUrl,
      isPublic = true,
    )
  }

  suspend fun updateProfile(
    username: String,
    displayName: String,
    bio: String,
    photoDataUrl: String?,
    isPublic: Boolean,
    email: String?,
    phoneNumber: String?,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val cleanHandle =
      username.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "").take(30).let {
        if (it.length >= 3) it else "user_${uid.take(6).lowercase()}"
      }
    val cleanName = displayName.trim().ifEmpty { cleanHandle }.take(50)
    val cleanBio = bio.trim().take(200)

    val userRef = db.collection("users").document(uid)
    val updateMap =
      mutableMapOf<String, Any>(
        "username" to cleanHandle,
        "displayName" to cleanName,
        "bio" to cleanBio,
        "isPublic" to isPublic,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    if (!photoDataUrl.isNullOrBlank()) {
      updateMap["photoDataUrl"] = photoDataUrl
    }

    val privateMap =
      mapOf(
          "userId" to uid,
          "email" to email?.trim()?.takeIf { it.isNotEmpty() }?.take(120),
          "phoneNumber" to phoneNumber?.trim()?.takeIf { it.isNotEmpty() }?.take(30),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        .filterValues { it != null } as Map<String, Any>

    try {
      userRef.update(updateMap).await()
      userRef
        .collection("private")
        .document("info")
        .set(privateMap, SetOptions.merge())
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }
  }

  // --- Follow / Unfollow ---

  fun observeFollowingIds(userId: String): Flow<Set<String>> = flow {
    val path = "users/$userId/following"
    emitAll(
      wrapFlowError(
        db.collection("users")
          .document(userId)
          .collection("following")
          .snapshots()
          .map { snapshot -> snapshot.documents.map { it.id }.toSet() },
        OperationType.LIST,
        path,
        expectedUid = userId,
      )
    )
  }

  fun observeFollowers(userId: String): Flow<List<FollowRelation>> = flow {
    val path = "users/$userId/followers"
    emitAll(
      wrapFlowError(
        db.collection("users")
          .document(userId)
          .collection("followers")
          .whereEqualTo("isActive", true)
          .snapshots()
          .map {
            it.toObjects(
              FollowRelation::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          },
        OperationType.LIST,
        path,
      )
    )
  }

  fun observeFollowing(userId: String): Flow<List<FollowRelation>> = flow {
    val path = "users/$userId/following"
    emitAll(
      wrapFlowError(
        db.collection("users")
          .document(userId)
          .collection("following")
          .whereEqualTo("isActive", true)
          .snapshots()
          .map {
            it.toObjects(
              FollowRelation::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          },
        OperationType.LIST,
        path,
      )
    )
  }

  suspend fun toggleFollow(
    currentUser: UserProfile,
    targetUser: UserProfile,
    isCurrentlyFollowing: Boolean,
  ): Result<Boolean> = runCatching {
    val uid = requireUserId()
    val targetId = targetUser.userId
    require(uid != targetId) { "Cannot follow yourself" }

    val followerDocRef =
      db.collection("users").document(targetId).collection("followers").document(uid)
    val followingDocRef =
      db.collection("users").document(uid).collection("following").document(targetId)
    val currentUserDoc = db.collection("users").document(uid)
    val targetUserDoc = db.collection("users").document(targetId)

    val batch = db.batch()
    if (isCurrentlyFollowing) {
      batch.delete(followerDocRef)
      batch.delete(followingDocRef)
      if (targetUser.followersCount > 0) {
        batch.update(
          targetUserDoc,
          mapOf(
            "followersCount" to FieldValue.increment(-1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      if (currentUser.followingCount > 0) {
        batch.update(
          currentUserDoc,
          mapOf(
            "followingCount" to FieldValue.increment(-1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
    } else {
      val followerPayload =
        mapOf(
            "userId" to uid,
            "targetUserId" to targetId,
            "username" to currentUser.username,
            "photoDataUrl" to currentUser.photoDataUrl,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
          )
          .filterValues { it != null } as Map<String, Any>

      val followingPayload =
        mapOf(
            "userId" to uid,
            "targetUserId" to targetId,
            "username" to targetUser.username,
            "photoDataUrl" to targetUser.photoDataUrl,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
          )
          .filterValues { it != null } as Map<String, Any>

      batch.set(followerDocRef, followerPayload)
      batch.set(followingDocRef, followingPayload)
      batch.update(
        targetUserDoc,
        mapOf(
          "followersCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.update(
        currentUserDoc,
        mapOf(
          "followingCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      val notifRef =
        db.collection("users").document(targetId).collection("notifications").document()
      val notifPayload =
        mapOf(
            "recipientId" to targetId,
            "actorId" to uid,
            "actorUsername" to currentUser.username,
            "actorPhotoDataUrl" to currentUser.photoDataUrl,
            "type" to AppNotification.TYPE_FOLLOW,
            "targetId" to uid,
            "previewText" to "started following you",
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
          )
          .filterValues { it != null } as Map<String, Any>
      batch.set(notifRef, notifPayload)
    }

    try {
      batch.commit().await()
      !isCurrentlyFollowing
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "users/$targetId/followers/$uid")
      throw e
    }
  }

  // --- Posts & Reels ---

  fun observePublicPosts(): Flow<List<Post>> = flow {
    val path = "posts"
    emitAll(
      wrapFlowError(
        db.collection("posts")
          .whereEqualTo("visibility", Post.VISIBILITY_PUBLIC)
          .snapshots()
          .map { snapshot ->
            snapshot
              .toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
          },
        OperationType.LIST,
        path,
      )
    )
  }

  fun observeAllPostsForAdmin(): Flow<List<Post>> = flow {
    val path = "posts"
    emitAll(
      wrapFlowError(
        db.collection("posts")
          .snapshots()
          .map { snapshot ->
            snapshot
              .toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
          },
        OperationType.LIST,
        path,
      )
    )
  }

  fun observeUserPosts(userId: String): Flow<List<Post>> = flow {
    val path = "posts"
    emitAll(
      wrapFlowError(
        db.collection("posts")
          .whereEqualTo("authorId", userId)
          .snapshots()
          .map { snapshot ->
            snapshot
              .toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              .filter { it.visibility == Post.VISIBILITY_PUBLIC }
              .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
          },
        OperationType.LIST,
        path,
      )
    )
  }

  suspend fun createPost(
    author: UserProfile,
    postKind: String,
    mediaType: String,
    mediaDataUrl: String,
    videoUri: String?,
    caption: String,
    audioTitle: String?,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("posts").document()
    val extractedTags =
      Regex("#([a-zA-Z0-9_]+)")
        .findAll(caption)
        .map { it.groupValues[1].lowercase() }
        .distinct()
        .take(10)
        .toList()

    val payload =
      mapOf(
          "authorId" to uid,
          "authorUsername" to author.username,
          "authorPhotoDataUrl" to author.photoDataUrl,
          "postKind" to postKind,
          "mediaType" to mediaType,
          "mediaDataUrl" to mediaDataUrl,
          "videoUri" to videoUri?.take(2000),
          "caption" to caption.trim().take(500),
          "hashtags" to extractedTags,
          "audioTitle" to audioTitle?.trim()?.takeIf { it.isNotEmpty() }?.take(100),
          "visibility" to Post.VISIBILITY_PUBLIC,
          "likesCount" to 0L,
          "commentsCount" to 0L,
          "sharesCount" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        .filterValues { it != null } as Map<String, Any>

    val batch = db.batch()
    batch.set(docRef, payload)
    batch.update(
      db.collection("users").document(uid),
      mapOf(
        "postsCount" to FieldValue.increment(1L),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )

    try {
      batch.commit().await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun deletePost(post: Post): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("posts").document(post.id)
    try {
      docRef.delete().await()
      if (post.authorId == uid && post.visibility == Post.VISIBILITY_PUBLIC) {
        db.collection("users")
          .document(uid)
          .update(
            mapOf(
              "postsCount" to FieldValue.increment(-1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  fun observePostLikeState(postId: String, userId: String): Flow<Boolean> = flow {
    val path = "posts/$postId/likes/$userId"
    emitAll(
      wrapFlowError(
        db.collection("posts")
          .document(postId)
          .collection("likes")
          .document(userId)
          .snapshots()
          .map { it.exists() },
        OperationType.GET,
        path,
        expectedUid = userId,
      )
    )
  }

  suspend fun toggleLike(
    post: Post,
    currentUser: UserProfile,
    currentlyLiked: Boolean,
  ): Result<Boolean> = runCatching {
    val uid = requireUserId()
    val postRef = db.collection("posts").document(post.id)
    val likeRef = postRef.collection("likes").document(uid)
    val batch = db.batch()

    if (currentlyLiked) {
      batch.delete(likeRef)
      if (post.likesCount > 0) {
        batch.update(
          postRef,
          mapOf(
            "likesCount" to FieldValue.increment(-1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
    } else {
      batch.set(
        likeRef,
        mapOf(
          "postId" to post.id,
          "userId" to uid,
          "isActive" to true,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.update(
        postRef,
        mapOf(
          "likesCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      if (post.authorId != uid) {
        val notifRef =
          db.collection("users").document(post.authorId).collection("notifications").document()
        val notifPayload =
          mapOf(
              "recipientId" to post.authorId,
              "actorId" to uid,
              "actorUsername" to currentUser.username,
              "actorPhotoDataUrl" to currentUser.photoDataUrl,
              "type" to AppNotification.TYPE_LIKE,
              "targetId" to post.id,
              "previewText" to "liked your ${post.postKind}",
              "isRead" to false,
              "createdAt" to FieldValue.serverTimestamp(),
            )
            .filterValues { it != null } as Map<String, Any>
        batch.set(notifRef, notifPayload)
      }
    }

    try {
      batch.commit().await()
      !currentlyLiked
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, likeRef.path)
      throw e
    }
  }

  fun observeComments(postId: String): Flow<List<Comment>> = flow {
    val path = "posts/$postId/comments"
    emitAll(
      wrapFlowError(
        db.collection("posts")
          .document(postId)
          .collection("comments")
          .whereEqualTo("isVisible", true)
          .snapshots()
          .map { snapshot ->
            snapshot
              .toObjects(Comment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              .sortedBy { it.createdAt ?: Timestamp(0, 0) }
          },
        OperationType.LIST,
        path,
      )
    )
  }

  suspend fun addComment(post: Post, author: UserProfile, text: String): Result<String> =
    runCatching {
      val uid = requireUserId()
      val cleanText = text.trim().take(300)
      require(cleanText.isNotEmpty()) { "Comment cannot be empty" }

      val postRef = db.collection("posts").document(post.id)
      val commentRef = postRef.collection("comments").document()
      val commentPayload =
        mapOf(
            "postId" to post.id,
            "authorId" to uid,
            "authorUsername" to author.username,
            "authorPhotoDataUrl" to author.photoDataUrl,
            "text" to cleanText,
            "isVisible" to true,
            "createdAt" to FieldValue.serverTimestamp(),
          )
          .filterValues { it != null } as Map<String, Any>

      val batch = db.batch()
      batch.set(commentRef, commentPayload)
      batch.update(
        postRef,
        mapOf(
          "commentsCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )

      if (post.authorId != uid) {
        val notifRef =
          db.collection("users").document(post.authorId).collection("notifications").document()
        val notifPayload =
          mapOf(
              "recipientId" to post.authorId,
              "actorId" to uid,
              "actorUsername" to author.username,
              "actorPhotoDataUrl" to author.photoDataUrl,
              "type" to AppNotification.TYPE_COMMENT,
              "targetId" to post.id,
              "previewText" to "commented: ${cleanText.take(80)}",
              "isRead" to false,
              "createdAt" to FieldValue.serverTimestamp(),
            )
            .filterValues { it != null } as Map<String, Any>
        batch.set(notifRef, notifPayload)
      }

      try {
        batch.commit().await()
        commentRef.id
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, commentRef.path)
        throw e
      }
    }

  suspend fun incrementShareCount(postId: String): Result<Unit> = runCatching {
    requireUserId()
    val postRef = db.collection("posts").document(postId)
    try {
      postRef
        .update(
          mapOf(
            "sharesCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, postRef.path)
      throw e
    }
  }

  // --- Stories (24-Hour) ---

  fun observeActiveStories(): Flow<List<Story>> = flow {
    val path = "stories"
    emitAll(
      wrapFlowError(
        db.collection("stories")
          .whereEqualTo("visibility", "active")
          .snapshots()
          .map { snapshot ->
            val nowMs = System.currentTimeMillis()
            snapshot
              .toObjects(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              .filter { it.expiresAtEpochMs > nowMs }
              .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
          },
        OperationType.LIST,
        path,
      )
    )
  }

  suspend fun createStory(
    author: UserProfile,
    mediaDataUrl: String,
    caption: String?,
    musicTitle: String?,
    stickerLabel: String?,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val storyRef = db.collection("stories").document()
    val expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L

    val payload =
      mapOf(
          "authorId" to uid,
          "authorUsername" to author.username,
          "authorPhotoDataUrl" to author.photoDataUrl,
          "mediaDataUrl" to mediaDataUrl,
          "caption" to caption?.trim()?.takeIf { it.isNotEmpty() }?.take(160),
          "musicTitle" to musicTitle?.trim()?.takeIf { it.isNotEmpty() }?.take(80),
          "stickerLabel" to stickerLabel?.trim()?.takeIf { it.isNotEmpty() }?.take(40),
          "visibility" to "active",
          "expiresAtEpochMs" to expiresAt,
          "viewsCount" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        .filterValues { it != null } as Map<String, Any>

    try {
      storyRef.set(payload).await()
      storyRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, storyRef.path)
      throw e
    }
  }

  suspend fun markStoryViewed(storyId: String): Result<Unit> = runCatching {
    requireUserId()
    val storyRef = db.collection("stories").document(storyId)
    try {
      storyRef
        .update(
          mapOf(
            "viewsCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, storyRef.path)
      throw e
    }
  }

  // --- Private Real-Time Chat ---

  fun buildDeterministicChatId(uidA: String, uidB: String): String {
    val sorted = listOf(uidA, uidB).sorted()
    return "${sorted[0]}_${sorted[1]}"
  }

  fun observeChats(userId: String): Flow<List<ChatThread>> = flow {
    val path = "chats"
    emitAll(
      wrapFlowError(
        db.collection("chats")
          .whereArrayContains("participantIds", userId)
          .snapshots()
          .map { snapshot ->
            snapshot
              .toObjects(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
          },
        OperationType.LIST,
        path,
        expectedUid = userId,
      )
    )
  }

  suspend fun getOrCreateChat(currentUser: UserProfile, peerUser: UserProfile): Result<String> =
    runCatching {
      val uid = requireUserId()
      val peerId = peerUser.userId
      val chatId = buildDeterministicChatId(uid, peerId)
      val chatRef = db.collection("chats").document(chatId)
      val existing = chatRef.get().await()
      if (!existing.exists()) {
        val participants = listOf(uid, peerId).sorted()
        val usernames = mapOf(uid to currentUser.username, peerId to peerUser.username)
        val photos =
          buildMap {
            currentUser.photoDataUrl?.let { put(uid, it) }
            peerUser.photoDataUrl?.let { put(peerId, it) }
          }
        val payload =
          mapOf(
            "participantIds" to participants,
            "participantUsernames" to usernames,
            "participantPhotos" to photos,
            "lastMessageText" to "Say hello!",
            "lastMessageType" to ChatMessage.TYPE_TEXT,
            "lastSenderId" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        try {
          chatRef.set(payload).await()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.CREATE, chatRef.path)
          throw e
        }
      }
      chatId
    }

  fun observeMessages(chatId: String, currentUserId: String): Flow<List<ChatMessage>> = flow {
    val path = "chats/$chatId/messages"
    emitAll(
      wrapFlowError(
        db.collection("chats")
          .document(chatId)
          .collection("messages")
          .whereArrayContains("participantIds", currentUserId)
          .snapshots()
          .map { snapshot ->
            snapshot
              .toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              .sortedBy { it.createdAt ?: Timestamp(0, 0) }
          },
        OperationType.LIST,
        path,
        expectedUid = currentUserId,
      )
    )
  }

  suspend fun sendMessage(
    chatId: String,
    currentUser: UserProfile,
    peerUserId: String,
    messageType: String,
    text: String,
    mediaDataUrl: String? = null,
    videoUri: String? = null,
    audioDurationSec: Int? = null,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val participants = listOf(uid, peerUserId).sorted()
    val chatRef = db.collection("chats").document(chatId)
    val msgRef = chatRef.collection("messages").document()
    val cleanText = text.trim().take(1000)

    val previewSummary =
      when (messageType) {
        ChatMessage.TYPE_IMAGE -> "Sent a photo"
        ChatMessage.TYPE_VIDEO -> "Sent a video"
        ChatMessage.TYPE_VOICE -> "Voice message (${audioDurationSec ?: 0}s)"
        else -> cleanText.take(200)
      }

    val msgPayload =
      mapOf(
          "chatId" to chatId,
          "senderId" to uid,
          "participantIds" to participants,
          "messageType" to messageType,
          "text" to cleanText,
          "mediaDataUrl" to mediaDataUrl,
          "videoUri" to videoUri?.take(2000),
          "audioDurationSec" to audioDurationSec?.coerceIn(0, 60)?.toLong(),
          "createdAt" to FieldValue.serverTimestamp(),
        )
        .filterValues { it != null } as Map<String, Any>

    val batch = db.batch()
    batch.set(msgRef, msgPayload)
    batch.update(
      chatRef,
      mapOf(
        "lastMessageText" to previewSummary,
        "lastMessageType" to messageType,
        "lastSenderId" to uid,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )

    val notifRef =
      db.collection("users").document(peerUserId).collection("notifications").document()
    val notifPayload =
      mapOf(
          "recipientId" to peerUserId,
          "actorId" to uid,
          "actorUsername" to currentUser.username,
          "actorPhotoDataUrl" to currentUser.photoDataUrl,
          "type" to AppNotification.TYPE_MESSAGE,
          "targetId" to chatId,
          "previewText" to previewSummary.take(160),
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
        )
        .filterValues { it != null } as Map<String, Any>
    batch.set(notifRef, notifPayload)

    try {
      batch.commit().await()
      msgRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgRef.path)
      throw e
    }
  }

  // --- Notifications ---

  fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
    val path = "users/$userId/notifications"
    emitAll(
      wrapFlowError(
        db.collection("users")
          .document(userId)
          .collection("notifications")
          .snapshots()
          .map { snapshot ->
            snapshot
              .toObjects(
                AppNotification::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              )
              .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
          },
        OperationType.LIST,
        path,
        expectedUid = userId,
      )
    )
  }

  suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> =
    runCatching {
      requireUserId()
      val ref =
        db.collection("users").document(userId).collection("notifications").document(notificationId)
      try {
        ref.update(mapOf("isRead" to true)).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, ref.path)
        throw e
      }
    }

  // --- Reports & Admin Moderation ---

  suspend fun submitReport(
    targetType: String,
    targetId: String,
    targetSummary: String,
    reason: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val ref = db.collection("reports").document()
    val payload =
      mapOf(
        "reporterId" to uid,
        "targetType" to targetType,
        "targetId" to targetId,
        "targetSummary" to targetSummary.trim().take(200),
        "reason" to reason.trim().take(300).ifEmpty { "Inappropriate content" },
        "status" to ModerationReport.STATUS_OPEN,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      ref.set(payload).await()
      ref.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, ref.path)
      throw e
    }
  }

  fun observeReportsForAdmin(): Flow<List<ModerationReport>> = flow {
    val path = "reports"
    emitAll(
      wrapFlowError(
        db.collection("reports")
          .snapshots()
          .map { snapshot ->
            snapshot
              .toObjects(
                ModerationReport::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              )
              .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
          },
        OperationType.LIST,
        path,
      )
    )
  }

  suspend fun resolveReport(reportId: String, status: String): Result<Unit> = runCatching {
    requireUserId()
    val ref = db.collection("reports").document(reportId)
    try {
      ref
        .update(
          mapOf(
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      throw e
    }
  }

  suspend fun adminDeletePost(postId: String): Result<Unit> = runCatching {
    requireUserId()
    val ref = db.collection("posts").document(postId)
    try {
      ref.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, ref.path)
      throw e
    }
  }

  suspend fun adminSetUserSuspended(userId: String, suspended: Boolean): Result<Unit> =
    runCatching {
      requireUserId()
      val ref = db.collection("users").document(userId)
      try {
        ref
          .update(
            mapOf(
              "isSuspended" to suspended,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, ref.path)
        throw e
      }
    }

  suspend fun checkIsAdmin(userId: String, email: String?): Boolean {
    if (email.equals(ADMIN_EMAIL, ignoreCase = true)) return true
    return runCatching {
        db.collection("admins").document(userId).get().await().exists()
      }
      .getOrDefault(false)
  }

  companion object {
    const val ADMIN_EMAIL = "dayaramasuthara5@gmail.com"
  }
}
