package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.SocialRepository
import com.example.model.Post
import com.example.model.UserProfile
import com.example.ui.CreatePostViewModel
import com.example.ui.components.DataImage
import com.example.ui.components.GradientButton
import com.example.ui.theme.BrandMagenta
import com.example.util.MediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostSheet(
  currentUserId: String,
  currentAuthor: UserProfile?,
  initialKind: String = Post.KIND_POST,
  onDismiss: () -> Unit,
  onPostCreated: (postId: String, postKind: String) -> Unit,
) {
  val appContext = LocalContext.current.applicationContext
  val createPostViewModel: CreatePostViewModel =
    viewModel(
      key = "${currentUserId}_create_post",
      factory =
        viewModelFactory {
          initializer {
            CreatePostViewModel(
              repository = SocialRepository(appContext),
              currentUserId = currentUserId,
              initialKind = initialKind,
            )
          }
        },
    )

  LaunchedEffect(initialKind) {
    createPostViewModel.resetDraft(initialKind)
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    CreatePostView(
      viewModel = createPostViewModel,
      currentAuthor = currentAuthor,
      onClose = onDismiss,
      onPostSaved = onPostCreated,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStorySheet(
  onDismiss: () -> Unit,
  onPublish: (
    mediaDataUrl: String,
    caption: String?,
    musicTitle: String?,
    stickerLabel: String?,
  ) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var mediaDataUrl by remember { mutableStateOf<String?>(null) }
  var caption by remember { mutableStateOf("") }
  var selectedMusic by remember { mutableStateOf("Sunset Lofi • Studio Beats") }
  var selectedSticker by remember { mutableStateOf("🔥 Vibe") }

  val musicTracks =
    listOf(
      "Sunset Lofi • Studio Beats",
      "Midnight Drive • Synthwave",
      "Acoustic Morning • Indie",
      "City Pulse • Electronic",
      "No Music",
    )
  val stickers = listOf("🔥 Vibe", "🎵 Now Playing", "📍 City Lights", "☕ Mood", "✨ Featured")

  val imagePicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          val encoded =
            withContext(Dispatchers.IO) { MediaUtils.encodeImageUriToDataUrl(context, uri) }
          if (encoded != null) mediaDataUrl = encoded
        }
      }
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Add 24-Hour Story",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Outlined.Close, contentDescription = "Close")
        }
      }

      OutlinedButton(
        onClick = {
          imagePicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pick_story_photo_button"),
      ) {
        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (mediaDataUrl == null) "Choose Story Photo" else "Change Story Photo")
      }

      if (mediaDataUrl != null) {
        DataImage(
          dataUrl = mediaDataUrl,
          contentDescription = "Story preview",
          modifier =
            Modifier.fillMaxWidth()
              .height(170.dp)
              .clip(RoundedCornerShape(16.dp)),
          contentScale = ContentScale.Crop,
        )
      }

      Text("Stickers", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      LazyRow(
        contentPadding = PaddingValues(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(stickers) { sticker ->
          FilterChip(
            selected = selectedSticker == sticker,
            onClick = { selectedSticker = sticker },
            label = { Text(sticker) },
          )
        }
      }

      Text("Music", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      LazyRow(
        contentPadding = PaddingValues(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(musicTracks) { track ->
          FilterChip(
            selected = selectedMusic == track,
            onClick = { selectedMusic = track },
            leadingIcon = {
              Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = BrandMagenta,
                modifier = Modifier.size(16.dp),
              )
            },
            label = { Text(track) },
          )
        }
      }

      OutlinedTextField(
        value = caption,
        onValueChange = { caption = it.take(160) },
        label = { Text("Story Text (Optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("story_caption_input"),
      )

      GradientButton(
        text = "Share to Your Story (24h)",
        onClick = {
          val finalMedia =
            mediaDataUrl
              ?: MediaUtils.generateGradientCoverDataUrl(
                caption.ifBlank { selectedSticker }
              )
          onPublish(
            finalMedia,
            caption.takeIf { it.isNotBlank() },
            selectedMusic.takeIf { it != "No Music" },
            selectedSticker,
          )
        },
        modifier = Modifier.fillMaxWidth().testTag("publish_story_button"),
      )
    }
  }
}
