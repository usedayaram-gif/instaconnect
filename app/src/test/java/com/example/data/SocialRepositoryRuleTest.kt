package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.ChatMessage
import com.example.model.Post
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SocialRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureUserProfile_andPrivateInfoIsolation_enforced() = runBlocking {
    val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
    val aliceRepo = SocialRepository(firestore, auth)

    val profileResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.ensureUserProfile(
          displayName = "Alice Vance",
          email = "alice@test.com",
          phoneNumber = "+15550101",
        )
      }
    assertTrue(profileResult.isSuccess)

    val observed =
      withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo.observeUserProfile(aliceUid).first { it != null }
      }
    assertEquals(aliceUid, observed?.userId)

    // Bob cannot read Alice's private PII doc
    signInTestUser("bob_${UUID.randomUUID()}@test.com")
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore
          .collection("users")
          .document(aliceUid)
          .collection("private")
          .document("info")
          .get()
          .await()
      }
      fail("Expected PERMISSION_DENIED when reading another user's private/info")
    } catch (e: Exception) {
      val fsErr = checkNotNull(e.unwrapFirestoreException()) { "Expected FirestoreException, got $e" }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr.code)
    }
  }

  @Test
  fun postLifecycle_createLikeCommentAndCrossUserDeleteRejection() = runBlocking {
    signInTestUser("alice_post_${UUID.randomUUID()}@test.com")
    val aliceRepo = SocialRepository(firestore, auth)
    val alice =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.ensureUserProfile("Alice Post", "alicepost@test.com").getOrThrow()
      }

    val postId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo
          .createPost(
            author = alice,
            postKind = Post.KIND_POST,
            mediaType = Post.MEDIA_IMAGE,
            mediaDataUrl = "data:image/jpeg;base64,testimage",
            videoUri = null,
            caption = "Golden hour #photography #instaconnect",
            audioTitle = null,
          )
          .getOrThrow()
      }

    val feed =
      withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo.observePublicPosts().first { list -> list.any { it.id == postId } }
      }
    val createdPost = feed.first { it.id == postId }
    assertTrue(createdPost.hashtags.contains("photography"))

    // Bob likes and comments on Alice's post, but cannot delete it
    signInTestUser("bob_post_${UUID.randomUUID()}@test.com")
    val bobRepo = SocialRepository(firestore, auth)
    val bob =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        bobRepo.ensureUserProfile("Bob Viewer", "bobviewer@test.com").getOrThrow()
      }

    val likeRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        bobRepo.toggleLike(createdPost, bob, currentlyLiked = false)
      }
    assertTrue(likeRes.isSuccess)

    val commentRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        bobRepo.addComment(createdPost, bob, "Stunning shot!")
      }
    assertTrue(commentRes.isSuccess)

    val deleteRes = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.deletePost(createdPost) }
    assertTrue(deleteRes.isFailure)
    val deleteErr = deleteRes.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, deleteErr?.code)
  }

  @Test
  fun storyAndChat_createAndParticipantIsolation() = runBlocking {
    val aliceUid = signInTestUser("alice_chat_${UUID.randomUUID()}@test.com")
    val aliceRepo = SocialRepository(firestore, auth)
    val alice =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.ensureUserProfile("Alice Chat", "alicechat@test.com").getOrThrow()
      }

    val storyId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo
          .createStory(
            author = alice,
            mediaDataUrl = "data:image/jpeg;base64,storyimg",
            caption = "Morning coffee",
            musicTitle = "Lofi Chill - Studio Beats",
            stickerLabel = "☕ Mood",
          )
          .getOrThrow()
      }
    val stories =
      withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo.observeActiveStories().first { list -> list.any { it.id == storyId } }
      }
    assertTrue(stories.any { it.id == storyId })

    val bobUid = signInTestUser("bob_chat_${UUID.randomUUID()}@test.com")
    val bobRepo = SocialRepository(firestore, auth)
    val bob =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        bobRepo.ensureUserProfile("Bob Chat", "bobchat@test.com").getOrThrow()
      }

    val chatId = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getOrCreateChat(bob, alice).getOrThrow() }
    val msgId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        bobRepo
          .sendMessage(
            chatId = chatId,
            currentUser = bob,
            peerUserId = aliceUid,
            messageType = ChatMessage.TYPE_TEXT,
            text = "Hey Alice, loved your story!",
          )
          .getOrThrow()
      }

    val messages =
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeMessages(chatId, bobUid).first { list -> list.any { it.id == msgId } }
      }
    assertEquals("Hey Alice, loved your story!", messages.first { it.id == msgId }.text)

    // Charlie cannot observe Alice & Bob's chat messages
    signInTestUser("charlie_${UUID.randomUUID()}@test.com")
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        SocialRepository(firestore, auth).observeMessages(chatId, bobUid).first()
      }
      fail("Expected PERMISSION_DENIED for non-participant")
    } catch (e: Exception) {
      val fsErr = checkNotNull(e.unwrapFirestoreException()) { "Expected FirestoreException, got $e" }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr.code)
    }
  }

  @Test
  fun observePublicPosts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = SocialRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observePublicPosts().first() }
      fail("Expected PERMISSION_DENIED for unauthenticated user")
    } catch (e: Exception) {
      val fsErr = checkNotNull(e.unwrapFirestoreException()) { "Expected FirestoreException, got $e" }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr.code)
    }
  }

  @Test
  fun adminModeration_nonAdminRejected() = runBlocking {
    val bobUid = signInTestUser("nonadmin_${UUID.randomUUID()}@test.com")
    val repo = SocialRepository(firestore, auth)
    repo.ensureUserProfile("Regular User", "regular@test.com").getOrThrow()

    val reportId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.submitReport("post", "post_missing", "Summary", "Spam content").getOrThrow()
      }

    val suspendRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.adminSetUserSuspended(bobUid, true) }
    assertTrue(suspendRes.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      suspendRes.exceptionOrNull()?.unwrapFirestoreException()?.code,
    )

    val resolveRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.resolveReport(reportId, "resolved") }
    assertTrue(resolveRes.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      resolveRes.exceptionOrNull()?.unwrapFirestoreException()?.code,
    )
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
