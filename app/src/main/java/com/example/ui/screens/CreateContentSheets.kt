package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.Post
import com.example.ui.components.DataImage
import com.example.ui.components.GradientButton
import com.example.ui.theme.BrandMagenta
import com.example.ui.theme.PureBlack
import com.example.util.MediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostSheet(
  initialKind: String = Post.KIND_POST,
  onDismiss: () -> Unit,
  onPublish: (
    postKind: String,
    mediaType: String,
    mediaDataUrl: String,
    videoUri: String?,
    caption: String,
    audioTitle: String?,
  ) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var postKind by remember { mutableStateOf(initialKind) }
  var mediaType by remember { mutableStateOf(Post.MEDIA_IMAGE) }
  var mediaDataUrl by remember { mutableStateOf<String?>(null) }
  var videoUri by remember { mutableStateOf<String?>(null) }
  var caption by remember { mutableStateOf("") }
  var audioTitle by remember { mutableStateOf("") }
  var isProcessingMedia by remember { mutableStateOf(false) }

  val photoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        isProcessingMedia = true
        scope.launch {
          val encoded =
            withContext(Dispatchers.IO) { MediaUtils.encodeImageUriToDataUrl(context, uri) }
          if (encoded != null) {
            mediaType = Post.MEDIA_IMAGE
            mediaDataUrl = encoded
            videoUri = null
          }
          isProcessingMedia = false
        }
      }
    }

  val videoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        isProcessingMedia = true
        scope.launch {
          val (thumb, localUri) =
            withContext(Dispatchers.IO) {
              MediaUtils.extractVideoThumbnailDataUrl(context, uri) to
                MediaUtils.copyVideoToInternalStorage(context, uri)
            }
          mediaType = Post.MEDIA_VIDEO
          mediaDataUrl = thumb
          videoUri = localUri
          isProcessingMedia = false
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
          text = if (postKind == Post.KIND_REEL) "Create Reel" else "New Post",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Outlined.Close, contentDescription = "Close")
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(
          selected = postKind == Post.KIND_POST,
          onClick = { postKind = Post.KIND_POST },
          label = { Text("Feed Post") },
        )
        FilterChip(
          selected = postKind == Post.KIND_REEL,
          onClick = { postKind = Post.KIND_REEL },
          label = { Text("Vertical Reel") },
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = {
            photoPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("pick_post_photo_button"),
        ) {
          Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Pick Photo")
        }
        OutlinedButton(
          onClick = {
            videoPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("pick_post_video_button"),
        ) {
          Icon(Icons.Outlined.Videocam, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Pick Video")
        }
      }

      if (mediaDataUrl != null) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .aspectRatio(16f / 10f)
              .clip(RoundedCornerShape(16.dp))
              .background(PureBlack)
        ) {
          DataImage(
            dataUrl = mediaDataUrl,
            contentDescription = "Selected media preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
        }
      } else {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier =
            Modifier.fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        ) {
          Text(
            text =
              "Pick a photo or video above — or publish with a generated studio gradient card.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
          )
        }
      }

      OutlinedTextField(
        value = caption,
        onValueChange = { caption = it.take(500) },
        label = { Text("Caption & #hashtags") },
        placeholder = { Text("Write a caption... #photography #instaconnect") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth().testTag("post_caption_input"),
      )

      OutlinedTextField(
        value = audioTitle,
        onValueChange = { audioTitle = it.take(100) },
        label = { Text("Music / Audio Track (Optional)") },
        leadingIcon = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      GradientButton(
        text = if (postKind == Post.KIND_REEL) "Publish Reel" else "Share Post",
        isLoading = isProcessingMedia,
        onClick = {
          val finalMedia =
            mediaDataUrl
              ?: MediaUtils.generateGradientCoverDataUrl(
                caption.ifBlank { "InstaConnect ${System.currentTimeMillis()}" }
              )
          onPublish(
            postKind,
            mediaType,
            finalMedia,
            videoUri,
            caption,
            audioTitle.takeIf { it.isNotBlank() },
          )
        },
        modifier = Modifier.fillMaxWidth().testTag("publish_post_button"),
      )
    }
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
