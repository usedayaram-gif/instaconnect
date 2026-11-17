package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ChatMessage
import com.example.model.ChatThread
import com.example.model.UserProfile
import com.example.ui.InstaConnectViewModel
import com.example.ui.components.AvatarWithStoryRing
import com.example.ui.components.DataImage
import com.example.ui.components.VideoPlayerSurface
import com.example.ui.theme.BrandMagenta
import com.example.ui.theme.ButtonGradient
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.util.MediaUtils
import com.example.util.VoicePlayerHelper
import com.example.util.VoiceRecorderHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChatsHubScreen(
  viewModel: InstaConnectViewModel,
  chats: List<ChatThread>,
  publicUsers: List<UserProfile>,
  activeChatId: String?,
  onSelectChat: (String?) -> Unit,
  onBackFromChats: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showNewChatPicker by remember { mutableStateOf(false) }

  if (activeChatId != null) {
    val activeThread = chats.firstOrNull { it.id == activeChatId }
    val peerId =
      activeThread?.participantIds?.firstOrNull { it != viewModel.currentUserId }
        ?: activeChatId.split("_").firstOrNull { it != viewModel.currentUserId }.orEmpty()
    val peerUser = publicUsers.firstOrNull { it.userId == peerId }
    val peerName =
      activeThread?.participantUsernames?.get(peerId)
        ?: peerUser?.username
        ?: "Chat"
    val peerPhoto =
      activeThread?.participantPhotos?.get(peerId) ?: peerUser?.photoDataUrl

    ChatDetailScreen(
      viewModel = viewModel,
      chatId = activeChatId,
      peerUserId = peerId,
      peerUsername = peerName,
      peerPhotoDataUrl = peerPhoto,
      onBack = { onSelectChat(null) },
      modifier = modifier,
    )
    return
  }

  BackHandler { onBackFromChats() }

  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBackFromChats) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to feed")
      }
      Text(
        text = "Messages",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f),
      )
      IconButton(
        onClick = { showNewChatPicker = true },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("new_chat_button"),
      ) {
        Icon(Icons.Outlined.AddComment, contentDescription = "Start new message")
      }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

    if (chats.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(ButtonGradient),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.Forum,
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(34.dp),
            )
          }
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "Private Messages",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Send real-time text, photos, videos, and voice notes to friends.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(14.dp))
          TextButton(onClick = { showNewChatPicker = true }) {
            Text("Start a Conversation", color = BrandMagenta, fontWeight = FontWeight.Bold)
          }
        }
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(chats, key = { it.id }) { thread ->
          val peerId =
            thread.participantIds.firstOrNull { it != viewModel.currentUserId }.orEmpty()
          val peerUsername = thread.participantUsernames[peerId] ?: "User"
          val peerPhoto = thread.participantPhotos[peerId]
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable { onSelectChat(thread.id) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            AvatarWithStoryRing(
              photoDataUrl = peerPhoto,
              username = peerUsername,
              size = 52.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "@$peerUsername",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = thread.lastMessageText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        }
      }
    }
  }

  if (showNewChatPicker) {
    val candidates = publicUsers.filter { it.userId != viewModel.currentUserId }
    AlertDialog(
      onDismissRequest = { showNewChatPicker = false },
      title = { Text("New Message") },
      text = {
        if (candidates.isEmpty()) {
          Text("No other users found yet. Invite friends to join InstaConnect!")
        } else {
          LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            items(candidates, key = { it.userId }) { peer ->
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clickable {
                      showNewChatPicker = false
                      viewModel.openOrCreateChat(peer) { chatId -> onSelectChat(chatId) }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                AvatarWithStoryRing(
                  photoDataUrl = peer.photoDataUrl,
                  username = peer.username,
                  size = 40.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "@${peer.username}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = peer.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showNewChatPicker = false }) { Text("Close") }
      },
    )
  }
}

@Composable
fun ChatDetailScreen(
  viewModel: InstaConnectViewModel,
  chatId: String,
  peerUserId: String,
  peerUsername: String,
  peerPhotoDataUrl: String?,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onBack() }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val messages by
    viewModel
      .observeMessages(chatId)
      .collectAsStateWithLifecycle(initialValue = emptyList())
  val listState = rememberLazyListState()

  var draft by remember { mutableStateOf("") }
  var isRecording by remember { mutableStateOf(false) }
  var playingMessageId by remember { mutableStateOf<String?>(null) }

  val voiceRecorder = remember(context) { VoiceRecorderHelper(context) }
  val voicePlayer = remember(context) { VoicePlayerHelper(context) }

  DisposableEffect(Unit) {
    onDispose {
      voiceRecorder.cancel()
      voicePlayer.stop()
    }
  }

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  val imagePicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          val dataUrl =
            withContext(Dispatchers.IO) { MediaUtils.encodeImageUriToDataUrl(context, uri) }
          if (dataUrl != null) {
            viewModel.sendMessage(
              chatId = chatId,
              peerUserId = peerUserId,
              messageType = ChatMessage.TYPE_IMAGE,
              text = "",
              mediaDataUrl = dataUrl,
            )
          }
        }
      }
    }

  val videoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          val (thumb, localVideoUri) =
            withContext(Dispatchers.IO) {
              MediaUtils.extractVideoThumbnailDataUrl(context, uri) to
                MediaUtils.copyVideoToInternalStorage(context, uri)
            }
          viewModel.sendMessage(
            chatId = chatId,
            peerUserId = peerUserId,
            messageType = ChatMessage.TYPE_VIDEO,
            text = "Video clip",
            mediaDataUrl = thumb,
            videoUri = localVideoUri,
          )
        }
      }
    }

  val audioPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        isRecording = voiceRecorder.startRecording()
      }
    }

  Column(modifier = modifier.fillMaxSize()) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
      AvatarWithStoryRing(
        photoDataUrl = peerPhotoDataUrl,
        username = peerUsername,
        size = 38.dp,
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = "@$peerUsername",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

    // Messages List
    LazyColumn(
      state = listState,
      modifier = Modifier.weight(1f).fillMaxWidth(),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      items(messages, key = { it.id }) { msg ->
        val isMine = msg.senderId == viewModel.currentUserId
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        ) {
          Box(
            modifier =
              Modifier.widthIn(max = 280.dp)
                .clip(
                  RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMine) 18.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 18.dp,
                  )
                )
                .then(
                  if (isMine) Modifier.background(ButtonGradient)
                  else
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                      .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(18.dp),
                      )
                )
                .padding(10.dp)
          ) {
            val contentColor = if (isMine) PureWhite else MaterialTheme.colorScheme.onSurface
            when (msg.messageType) {
              ChatMessage.TYPE_IMAGE -> {
                DataImage(
                  dataUrl = msg.mediaDataUrl,
                  contentDescription = "Photo message",
                  modifier =
                    Modifier.fillMaxWidth()
                      .aspectRatio(1f)
                      .clip(RoundedCornerShape(12.dp)),
                  contentScale = ContentScale.Crop,
                )
              }
              ChatMessage.TYPE_VIDEO -> {
                Box(
                  modifier =
                    Modifier.fillMaxWidth()
                      .aspectRatio(4f / 3f)
                      .clip(RoundedCornerShape(12.dp))
                      .background(PureBlack)
                ) {
                  VideoPlayerSurface(
                    videoUri = msg.videoUri,
                    thumbnailDataUrl = msg.mediaDataUrl,
                    isActive = true,
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              }
              ChatMessage.TYPE_VOICE -> {
                val isPlaying = playingMessageId == msg.id
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier =
                    Modifier.clickable {
                      if (isPlaying) {
                        voicePlayer.stop()
                        playingMessageId = null
                      } else if (!msg.mediaDataUrl.isNullOrBlank()) {
                        playingMessageId = msg.id
                        voicePlayer.play(msg.mediaDataUrl) { playingMessageId = null }
                      }
                    },
                ) {
                  Icon(
                    imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Stop voice note" else "Play voice note",
                    tint = contentColor,
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = contentColor,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Voice • ${msg.audioDurationSec ?: 1}s",
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                  )
                }
              }
              else -> {
                Text(
                  text = msg.text,
                  style = MaterialTheme.typography.bodyMedium,
                  color = contentColor,
                )
              }
            }
          }
        }
      }
    }

    // Composer Bar
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = {
          imagePicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_photo_button"),
      ) {
        Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Send photo")
      }
      IconButton(
        onClick = {
          videoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
          )
        },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_video_button"),
      ) {
        Icon(Icons.Outlined.Videocam, contentDescription = "Send video")
      }
      IconButton(
        onClick = {
          if (isRecording) {
            isRecording = false
            val encoded = voiceRecorder.stopAndEncode()
            if (encoded != null) {
              viewModel.sendMessage(
                chatId = chatId,
                peerUserId = peerUserId,
                messageType = ChatMessage.TYPE_VOICE,
                text = "Voice note",
                mediaDataUrl = encoded.first,
                audioDurationSec = encoded.second,
              )
            }
          } else {
            val hasPerm =
              ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
              isRecording = voiceRecorder.startRecording()
            } else {
              audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
          }
        },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("chat_voice_button"),
      ) {
        Icon(
          imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
          contentDescription = if (isRecording) "Stop and send voice message" else "Record voice message",
          tint = if (isRecording) DangerRed else MaterialTheme.colorScheme.onSurface,
        )
      }

      OutlinedTextField(
        value = draft,
        onValueChange = { draft = it.take(1000) },
        placeholder = {
          Text(if (isRecording) "Recording voice..." else "Message...")
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.weight(1f).testTag("chat_message_input"),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Surface(
        shape = CircleShape,
        color = Color.Transparent,
        modifier =
          Modifier.size(46.dp)
            .clip(CircleShape)
            .background(ButtonGradient)
            .clickable(enabled = draft.isNotBlank()) {
              val text = draft.trim()
              if (text.isNotEmpty()) {
                viewModel.sendMessage(
                  chatId = chatId,
                  peerUserId = peerUserId,
                  messageType = ChatMessage.TYPE_TEXT,
                  text = text,
                )
                draft = ""
              }
            }
            .testTag("chat_send_button"),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send message",
            tint = PureWhite,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}
