package com.example.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.model.Comment
import com.example.ui.theme.BrandGradient
import com.example.ui.theme.BrandMagenta
import com.example.ui.theme.ButtonGradient
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StoryRingBrush
import com.example.util.MediaUtils

@Composable
fun GradientButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isLoading: Boolean = false,
  gradient: Brush = ButtonGradient,
) {
  Button(
    onClick = onClick,
    enabled = enabled && !isLoading,
    colors =
      ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = PureWhite,
      ),
    contentPadding = PaddingValues(),
    shape = RoundedCornerShape(14.dp),
    modifier = modifier.heightIn(min = 48.dp),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 48.dp)
          .background(
            brush =
              if (enabled) gradient
              else Brush.horizontalGradient(
                listOf(
                  MaterialTheme.colorScheme.surfaceVariant,
                  MaterialTheme.colorScheme.surfaceVariant,
                )
              ),
            shape = RoundedCornerShape(14.dp),
          )
          .padding(horizontal = 20.dp, vertical = 12.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(22.dp),
          color = PureWhite,
          strokeWidth = 2.dp,
        )
      } else {
        Text(
          text = text,
          style = MaterialTheme.typography.labelLarge,
          color = if (enabled) PureWhite else MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
fun DataImage(
  dataUrl: String?,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Crop,
  fallbackText: String = "",
) {
  val bitmap = remember(dataUrl) { MediaUtils.decodeDataUrlToBitmap(dataUrl) }
  when {
    bitmap != null -> {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
      )
    }
    !dataUrl.isNullOrBlank() && !dataUrl.startsWith("data:") -> {
      AsyncImage(
        model = dataUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
      )
    }
    else -> {
      Box(
        modifier = modifier.background(BrandGradient),
        contentAlignment = Alignment.Center,
      ) {
        if (fallbackText.isNotBlank()) {
          Text(
            text = fallbackText.take(2).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
fun AvatarWithStoryRing(
  photoDataUrl: String?,
  username: String,
  size: Dp = 56.dp,
  hasStory: Boolean = false,
  isAddBadge: Boolean = false,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .size(size + 6.dp)
        .then(
          if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
    contentAlignment = Alignment.Center,
  ) {
    if (hasStory) {
      Box(
        modifier =
          Modifier.size(size + 6.dp)
            .border(width = 2.5.dp, brush = StoryRingBrush, shape = CircleShape)
            .padding(3.5.dp)
      )
    }
    DataImage(
      dataUrl = photoDataUrl,
      contentDescription = "$username avatar",
      fallbackText = username,
      modifier =
        Modifier.size(size)
          .clip(CircleShape)
          .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
    if (isAddBadge) {
      Box(
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .size(20.dp)
            .clip(CircleShape)
            .background(ButtonGradient)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text("+", color = PureWhite, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun VideoPlayerSurface(
  videoUri: String?,
  thumbnailDataUrl: String?,
  isActive: Boolean,
  modifier: Modifier = Modifier,
) {
  var isMuted by remember { mutableStateOf(false) }
  var isPaused by remember { mutableStateOf(false) }
  var hasVideoError by remember(videoUri) { mutableStateOf(false) }
  var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

  DisposableEffect(isActive, isPaused) {
    val vv = videoViewRef
    if (vv != null && !hasVideoError) {
      if (isActive && !isPaused) {
        runCatching { vv.start() }
      } else {
        runCatching { if (vv.isPlaying) vv.pause() }
      }
    }
    onDispose {
      runCatching { videoViewRef?.pause() }
    }
  }

  Box(
    modifier =
      modifier
        .background(PureBlack)
        .clickable {
          isPaused = !isPaused
        },
    contentAlignment = Alignment.Center,
  ) {
    DataImage(
      dataUrl = thumbnailDataUrl,
      contentDescription = "Video preview",
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop,
    )

    if (!videoUri.isNullOrBlank() && !hasVideoError && isActive) {
      AndroidView(
        factory = { ctx ->
          VideoView(ctx).apply {
            setVideoURI(Uri.parse(videoUri))
            setOnPreparedListener { mp ->
              mp.isLooping = true
              val vol = if (isMuted) 0f else 1f
              mp.setVolume(vol, vol)
              if (!isPaused) start()
            }
            setOnErrorListener { _, _, _ ->
              hasVideoError = true
              true
            }
            videoViewRef = this
          }
        },
        update = { view ->
          videoViewRef = view
          if (!isPaused && !view.isPlaying) {
            runCatching { view.start() }
          } else if (isPaused && view.isPlaying) {
            runCatching { view.pause() }
          }
        },
        modifier = Modifier.fillMaxSize(),
      )
    }

    if (isPaused || videoUri.isNullOrBlank() || hasVideoError) {
      Box(
        modifier =
          Modifier.size(56.dp)
            .clip(CircleShape)
            .background(PureBlack.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.PlayArrow,
          contentDescription = "Play video",
          tint = PureWhite,
          modifier = Modifier.size(32.dp),
        )
      }
    }

    if (!videoUri.isNullOrBlank() && !hasVideoError) {
      IconButton(
        onClick = { isMuted = !isMuted },
        modifier =
          Modifier.align(Alignment.TopEnd)
            .padding(12.dp)
            .clip(CircleShape)
            .background(PureBlack.copy(alpha = 0.55f))
            .minimumInteractiveComponentSize(),
      ) {
        Icon(
          imageVector =
            if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff
            else Icons.AutoMirrored.Outlined.VolumeUp,
          contentDescription = if (isMuted) "Unmute video" else "Mute video",
          tint = PureWhite,
        )
      }
    }
  }
}

@Composable
fun AnimatedHeartButton(
  isLiked: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  inactiveTint: Color = MaterialTheme.colorScheme.onSurface,
  icon: @Composable (tint: Color, scale: Float) -> Unit,
) {
  val scale by
    animateFloatAsState(
      targetValue = if (isLiked) 1.18f else 1f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
      label = "heart_scale",
    )
  val tint by
    animateColorAsState(
      targetValue = if (isLiked) BrandMagenta else inactiveTint,
      label = "heart_tint",
    )
  IconButton(
    onClick = onClick,
    modifier = modifier.minimumInteractiveComponentSize(),
  ) {
    Box(modifier = Modifier.scale(scale)) {
      icon(tint, scale)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
  comments: List<Comment>,
  onDismiss: () -> Unit,
  onSendComment: (String) -> Unit,
) {
  var draft by remember { mutableStateOf("") }
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Comments (${comments.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Outlined.Close, contentDescription = "Close comments")
        }
      }
      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

      if (comments.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxWidth().height(160.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Be the first to start the conversation",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 320.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(comments, key = { it.id }) { comment ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.Top,
            ) {
              AvatarWithStoryRing(
                photoDataUrl = comment.authorPhotoDataUrl,
                username = comment.authorUsername,
                size = 34.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "@${comment.authorUsername}",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = comment.text,
                  style = MaterialTheme.typography.bodyMedium,
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = draft,
          onValueChange = { draft = it.take(300) },
          placeholder = { Text("Add a comment...") },
          modifier = Modifier.weight(1f).testTag("comment_input"),
          singleLine = true,
          shape = RoundedCornerShape(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          shape = CircleShape,
          color = Color.Transparent,
          modifier =
            Modifier.size(48.dp)
              .clip(CircleShape)
              .background(ButtonGradient)
              .clickable(enabled = draft.isNotBlank()) {
                val txt = draft.trim()
                if (txt.isNotEmpty()) {
                  onSendComment(txt)
                  draft = ""
                }
              }
              .testTag("send_comment_button"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = "Post comment",
              tint = PureWhite,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
fun ReportDialog(
  targetSummary: String,
  onDismiss: () -> Unit,
  onSubmit: (String) -> Unit,
) {
  var reason by remember { mutableStateOf("") }
  val quickReasons = listOf("Spam or scam", "Harassment", "Inappropriate media", "Misleading info")
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Report Content", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = targetSummary,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          quickReasons.take(2).forEach { chip ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.clickable { reason = chip },
            ) {
              Text(
                text = chip,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it.take(300) },
          label = { Text("Reason") },
          placeholder = { Text("Describe the issue...") },
          modifier = Modifier.fillMaxWidth().testTag("report_reason_input"),
          minLines = 2,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { if (reason.trim().length >= 3) onSubmit(reason.trim()) },
        enabled = reason.trim().length >= 3,
        modifier = Modifier.testTag("submit_report_button"),
      ) {
        Text("Submit Report")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
