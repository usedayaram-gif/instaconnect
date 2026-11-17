package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Post
import com.example.model.UserProfile
import com.example.ui.InstaConnectViewModel
import com.example.ui.components.AnimatedHeartButton
import com.example.ui.components.AvatarWithStoryRing
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.DataImage
import com.example.ui.components.GradientButton
import com.example.ui.components.ReportDialog
import com.example.ui.components.VideoPlayerSurface
import com.example.ui.theme.BrandMagenta
import com.example.ui.theme.ButtonGradient
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun ReelsScreen(
  viewModel: InstaConnectViewModel,
  allPosts: List<Post>,
  publicUsers: List<UserProfile>,
  followingIds: Set<String>,
  onOpenCreateReel: () -> Unit,
  onOpenUserProfile: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val reels =
    remember(allPosts) {
      allPosts.filter { it.postKind == Post.KIND_REEL || it.mediaType == Post.MEDIA_VIDEO }
    }
  val usersById = remember(publicUsers) { publicUsers.associateBy { it.userId } }

  var commentsReel by remember { mutableStateOf<Post?>(null) }
  var reportReel by remember { mutableStateOf<Post?>(null) }

  Box(modifier = modifier.fillMaxSize().background(PureBlack)) {
    if (reels.isEmpty()) {
      Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Box(
          modifier =
            Modifier.size(80.dp)
              .clip(CircleShape)
              .background(ButtonGradient),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Outlined.VideoLibrary,
            contentDescription = null,
            tint = PureWhite,
            modifier = Modifier.size(38.dp),
          )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
          text = "No Reels Yet",
          style = MaterialTheme.typography.headlineMedium,
          color = PureWhite,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Upload a short vertical video reel with trending audio and share it with the community.",
          style = MaterialTheme.typography.bodyMedium,
          color = PureWhite.copy(alpha = 0.72f),
        )
        Spacer(modifier = Modifier.height(20.dp))
        GradientButton(
          text = "Upload First Reel",
          onClick = onOpenCreateReel,
          modifier = Modifier.fillMaxWidth(0.7f).testTag("empty_upload_reel_btn"),
        )
      }
    } else {
      val pagerState = rememberPagerState(pageCount = { reels.size })
      VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().testTag("reels_vertical_pager"),
      ) { page ->
        val reel = reels[page]
        val isPageActive = pagerState.currentPage == page
        val isLiked by
          viewModel
            .observePostLikeState(reel.id)
            .collectAsStateWithLifecycle(initialValue = false)
        val isOwnReel = reel.authorId == viewModel.currentUserId
        val isFollowing = followingIds.contains(reel.authorId)
        val creator = usersById[reel.authorId]

        Box(modifier = Modifier.fillMaxSize()) {
          if (reel.mediaType == Post.MEDIA_VIDEO) {
            VideoPlayerSurface(
              videoUri = reel.videoUri,
              thumbnailDataUrl = reel.mediaDataUrl,
              isActive = isPageActive,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            DataImage(
              dataUrl = reel.mediaDataUrl,
              contentDescription = reel.caption,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
            )
          }

          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    listOf(
                      PureBlack.copy(alpha = 0.35f),
                      Color.Transparent,
                      PureBlack.copy(alpha = 0.78f),
                    )
                  )
                )
          )

          // Top header
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Reels",
              style = MaterialTheme.typography.titleLarge,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
          }

          // Bottom-left creator metadata
          Column(
            modifier =
              Modifier.align(Alignment.BottomStart)
                .fillMaxWidth(0.76f)
                .padding(start = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              AvatarWithStoryRing(
                photoDataUrl = reel.authorPhotoDataUrl,
                username = reel.authorUsername,
                size = 40.dp,
                onClick = { onOpenUserProfile(reel.authorId) },
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "@${reel.authorUsername}",
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onOpenUserProfile(reel.authorId) },
              )
              if (!isOwnReel && creator != null) {
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                  color = Color.Transparent,
                  shape = RoundedCornerShape(10.dp),
                  modifier =
                    Modifier.border(1.dp, PureWhite.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                      .clickable { viewModel.toggleFollow(creator, isFollowing) },
                ) {
                  Text(
                    text = if (isFollowing) "Following" else "Follow",
                    style = MaterialTheme.typography.labelLarge,
                    color = PureWhite,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                  )
                }
              }
            }

            if (reel.caption.isNotBlank()) {
              Text(
                text = reel.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = PureWhite,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
              )
            }

            if (!reel.audioTitle.isNullOrBlank()) {
              Surface(
                color = PureBlack.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(14.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = reel.audioTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }

          // Right-side engagement rail
          Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              AnimatedHeartButton(
                isLiked = isLiked,
                onClick = { viewModel.toggleLike(reel, isLiked) },
                inactiveTint = PureWhite,
                modifier = Modifier.testTag("reel_like_${reel.id}"),
              ) { tint, _ ->
                Icon(
                  imageVector =
                    if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                  contentDescription = "Like reel",
                  tint = tint,
                  modifier = Modifier.size(30.dp),
                )
              }
              Text(
                text = "${reel.likesCount}",
                style = MaterialTheme.typography.labelLarge,
                color = PureWhite,
              )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              IconButton(
                onClick = { commentsReel = reel },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = Icons.Outlined.ChatBubbleOutline,
                  contentDescription = "Comments",
                  tint = PureWhite,
                  modifier = Modifier.size(28.dp),
                )
              }
              Text(
                text = "${reel.commentsCount}",
                style = MaterialTheme.typography.labelLarge,
                color = PureWhite,
              )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              IconButton(
                onClick = {
                  viewModel.sharePost(reel.id)
                  val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(
                        Intent.EXTRA_TEXT,
                        "Watch @${reel.authorUsername}'s Reel on InstaConnect: ${reel.caption}",
                      )
                    }
                  context.startActivity(Intent.createChooser(shareIntent, "Share Reel"))
                },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.Send,
                  contentDescription = "Share reel",
                  tint = PureWhite,
                  modifier = Modifier.size(28.dp),
                )
              }
              Text(
                text = "${reel.sharesCount}",
                style = MaterialTheme.typography.labelLarge,
                color = PureWhite,
              )
            }

            if (!isOwnReel) {
              IconButton(
                onClick = { reportReel = reel },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Flag,
                  contentDescription = "Report reel",
                  tint = PureWhite.copy(alpha = 0.8f),
                )
              }
            }
          }
        }
      }
    }

    FloatingActionButton(
      onClick = onOpenCreateReel,
      containerColor = BrandMagenta,
      contentColor = PureWhite,
      modifier =
        Modifier.align(Alignment.TopEnd)
          .padding(14.dp)
          .testTag("create_reel_fab"),
    ) {
      Icon(Icons.Filled.Add, contentDescription = "Create Reel")
    }
  }

  commentsReel?.let { reel ->
    val comments by
      viewModel
        .observeComments(reel.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    CommentsBottomSheet(
      comments = comments,
      onDismiss = { commentsReel = null },
      onSendComment = { text -> viewModel.addComment(reel, text) },
    )
  }

  reportReel?.let { reel ->
    ReportDialog(
      targetSummary = "Reel by @${reel.authorUsername}",
      onDismiss = { reportReel = null },
      onSubmit = { reason ->
        viewModel.submitReport("post", reel.id, "Reel @${reel.authorUsername}", reason)
        reportReel = null
      },
    )
  }
}
