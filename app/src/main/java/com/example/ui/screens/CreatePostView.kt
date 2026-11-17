package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Post
import com.example.model.UserProfile
import com.example.ui.CreatePostViewModel
import com.example.ui.components.DataImage
import com.example.ui.components.GradientButton
import com.example.ui.theme.BrandMagenta
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.util.MediaUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePostView(
  viewModel: CreatePostViewModel,
  currentAuthor: UserProfile?,
  onClose: () -> Unit,
  onPostSaved: (postId: String, postKind: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

  val takePictureLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
      val uri = pendingCameraUri
      if (captured && uri != null) {
        viewModel.onImageSelectedOrCaptured(context, uri)
      }
      pendingCameraUri = null
    }

  val launchCameraCapture = {
    runCatching { MediaUtils.createCameraCaptureUri(context) }
      .onSuccess { uri ->
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
      }
      .onFailure {
        viewModel.setPermissionOrMediaError("Unable to open camera on this device.")
      }
  }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        launchCameraCapture()
      } else {
        viewModel.setPermissionOrMediaError(
          "Camera permission is required to capture a photo. You can still select an image from your gallery."
        )
      }
    }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        viewModel.onImageSelectedOrCaptured(context, uri)
      }
    }

  val videoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        viewModel.onVideoSelected(context, uri)
      }
    }

  Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 620.dp)
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
          text = if (uiState.postKind == Post.KIND_REEL) "Create Reel" else "Create Post",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        IconButton(
          onClick = onClose,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("close_create_post_button"),
        ) {
          Icon(Icons.Outlined.Close, contentDescription = "Close")
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(
          selected = uiState.postKind == Post.KIND_POST,
          onClick = { viewModel.setPostKind(Post.KIND_POST) },
          label = { Text("Feed Post") },
        )
        FilterChip(
          selected = uiState.postKind == Post.KIND_REEL,
          onClick = { viewModel.setPostKind(Post.KIND_REEL) },
          label = { Text("Vertical Reel") },
        )
      }

      // Capture or Select Media Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = {
            val hasCameraPerm =
              ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            if (hasCameraPerm) {
              launchCameraCapture()
            } else {
              cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
          },
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier.weight(1f).height(48.dp).testTag("capture_post_photo_button"),
        ) {
          Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Camera", maxLines = 1)
        }

        OutlinedButton(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier.weight(1f).height(48.dp).testTag("pick_post_photo_button"),
        ) {
          Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Gallery", maxLines = 1)
        }

        OutlinedButton(
          onClick = {
            videoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
          },
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier.weight(1f).height(48.dp).testTag("pick_post_video_button"),
        ) {
          Icon(Icons.Outlined.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Video", maxLines = 1)
        }
      }

      // Image / Media Preview Area
      if (uiState.isProcessingMedia) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .aspectRatio(16f / 10f)
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(color = BrandMagenta)
        }
      } else if (uiState.mediaDataUrl != null) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .aspectRatio(16f / 10f)
              .clip(RoundedCornerShape(16.dp))
              .background(PureBlack)
              .testTag("create_post_media_preview")
        ) {
          DataImage(
            dataUrl = uiState.mediaDataUrl,
            contentDescription = "Selected post image preview",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
          IconButton(
            onClick = { viewModel.clearMedia() },
            modifier =
              Modifier.align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(CircleShape)
                .background(PureBlack.copy(alpha = 0.65f))
                .minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Remove selected media",
              tint = PureWhite,
            )
          }
        }
      } else {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier =
            Modifier.fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.PhotoCamera,
              contentDescription = null,
              tint = BrandMagenta,
              modifier = Modifier.size(28.dp),
            )
            Text(
              text = "Capture a photo with Camera or select an image from Gallery",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              textAlign = TextAlign.Center,
            )
            Text(
              text = "Or write a caption below to publish with a generated studio cover.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      }

      OutlinedTextField(
        value = uiState.caption,
        onValueChange = { viewModel.setCaption(it) },
        label = { Text("Caption & #hashtags") },
        placeholder = { Text("Share what's on your mind... #photography #instaconnect") },
        minLines = 3,
        supportingText = {
          Text("${uiState.caption.length} / 500")
        },
        modifier = Modifier.fillMaxWidth().testTag("post_caption_input"),
      )

      if (uiState.extractedHashtags.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          uiState.extractedHashtags.forEach { tag ->
            AssistChip(
              onClick = {},
              label = { Text("#$tag") },
            )
          }
        }
      }

      OutlinedTextField(
        value = uiState.audioTitle,
        onValueChange = { viewModel.setAudioTitle(it) },
        label = { Text("Music / Audio Track (Optional)") },
        leadingIcon = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      uiState.errorMessage?.let { err ->
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = err,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      GradientButton(
        text = if (uiState.postKind == Post.KIND_REEL) "Publish Reel" else "Share Post",
        isLoading = uiState.isSaving || uiState.isProcessingMedia,
        onClick = {
          viewModel.savePostToFirestore(
            author = currentAuthor,
            onSuccess = onPostSaved,
          )
        },
        modifier = Modifier.fillMaxWidth().testTag("publish_post_button"),
      )
    }
  }
}
