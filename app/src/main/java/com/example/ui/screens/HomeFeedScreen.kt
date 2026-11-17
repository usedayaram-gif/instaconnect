package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.Post
import com.example.model.Story
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.InstaConnectViewModel
import com.example.ui.components.AnimatedHeartButton
import com.example.ui.components.AvatarWithStoryRing
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.DataImage
import com.example.ui.components.GradientButton
import com.example.ui.components.ReportDialog
import com.example.ui.components.VideoPlayerSurface
import com.example.ui.theme.BrandGradient
import com.example.ui.theme.BrandMagenta
import com.example.ui.theme.ButtonGradient
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.delay

@Composable
fun HomeFeedScreen(
  viewModel: InstaConnectViewModel,
  currentProfile: UserProfile?,
  feedState: UiState<List<Post>>,
  stories: List<Story>,
  publicUsers: List<UserProfile>,
  followingIds: Set<String>,
  unreadNotificationsCount: Int,
  onOpenCreatePost: () -> Unit,
  onOpenCreateStory: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenMessages: () -> Unit,
  onOpenUserProfile: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var activeStoryGroup by remember { mutableStateOf<List<Story>?>(null) }
  var commentsPost by remember { mutableStateOf<Post?>(null) }
  var reportTargetPost by remember { mutableStateOf<Post?>(null) }

  val usersById = remember(publicUsers) { publicUsers.associateBy { it.userId } }
  val storiesByAuthor = remember(stories) { stories.groupBy { it.authorId } }

  Column(modifier = modifier.fillMaxSize()) {
    // Top Brand Bar
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "InstaConnect",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        IconButton(
          onClick = onOpenCreatePost,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("top_create_post_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.AddBox,
            contentDescription = "Create post or reel",
          )
        }
        IconButton(
          onClick = onOpenNotifications,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("top_notifications_button"),
        ) {
          BadgedBox(
            badge = {
              if (unreadNotificationsCount > 0) {
                Badge(containerColor = BrandMagenta, contentColor = PureWhite) {
                  Text(unreadNotificationsCount.coerceAtMost(99).toString())
                }
              }
            }
          ) {
            Icon(
              imageVector = Icons.Outlined.Notifications,
              contentDescription = "Notifications",
            )
          }
        }
        IconButton(
          onClick = onOpenMessages,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("top_messages_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.Send,
            contentDescription = "Direct messages",
          )
        }
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

    when (feedState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = BrandMagenta)
        }
      }
      is UiState.Error -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = feedState.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
      is UiState.Success -> {
        val posts = feedState.data
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
          LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 620.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
          ) {
            // Stories Strip
            item(key = "stories_row") {
              LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                item(key = "my_story") {
                  val myStories =
                    currentProfile?.let { storiesByAuthor[it.userId] }.orEmpty()
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                      Modifier.width(72.dp).testTag("add_story_item"),
                  ) {
                    AvatarWithStoryRing(
                      photoDataUrl = currentProfile?.photoDataUrl,
                      username = currentProfile?.username ?: "You",
                      size = 62.dp,
                      hasStory = myStories.isNotEmpty(),
                      isAddBadge = true,
                      onClick = {
                        if (myStories.isNotEmpty()) {
                          activeStoryGroup = myStories
                        } else {
                          onOpenCreateStory()
                        }
                      },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = if (myStories.isNotEmpty()) "Your story" else "Add story",
                      style = MaterialTheme.typography.labelSmall,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.clickable { onOpenCreateStory() },
                    )
                  }
                }

                val otherAuthorStories =
                  storiesByAuthor.entries
                    .filter { it.key != currentProfile?.userId }
                    .map { it.value }
                items(otherAuthorStories, key = { it.first().authorId }) { group ->
                  val first = group.first()
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(72.dp),
                  ) {
                    AvatarWithStoryRing(
                      photoDataUrl = first.authorPhotoDataUrl,
                      username = first.authorUsername,
                      size = 62.dp,
                      hasStory = true,
                      onClick = { activeStoryGroup = group },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = first.authorUsername,
                      style = MaterialTheme.typography.labelSmall,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }

            if (posts.isEmpty()) {
              item(key = "empty_feed") {
                EmptyFeedCard(
                  onCreatePost = onOpenCreatePost,
                  onCreateStory = onOpenCreateStory,
                )
              }
            } else {
              items(posts, key = { it.id }) { post ->
                val isLiked by
                  viewModel
                    .observePostLikeState(post.id)
                    .collectAsStateWithLifecycle(initialValue = false)
                val isOwnPost = post.authorId == viewModel.currentUserId
                val isFollowingAuthor = followingIds.contains(post.authorId)
                val targetUser = usersById[post.authorId]

                FeedPostCard(
                  post = post,
                  isLiked = isLiked,
                  isOwnPost = isOwnPost,
                  isFollowing = isFollowingAuthor,
                  onLikeToggle = { viewModel.toggleLike(post, isLiked) },
                  onOpenComments = { commentsPost = post },
                  onShare = { viewModel.sharePost(post.id) },
                  onFollowToggle = {
                    if (targetUser != null) {
                      viewModel.toggleFollow(targetUser, isFollowingAuthor)
                    }
                  },
                  onAuthorClick = { onOpenUserProfile(post.authorId) },
                  onDeletePost = { viewModel.deletePost(post) },
                  onReportPost = { reportTargetPost = post },
                )
              }
            }
          }
        }
      }
    }
  }

  activeStoryGroup?.let { group ->
    StoryViewerDialog(
      stories = group,
      onStoryViewed = { viewModel.markStoryViewed(it.id) },
      onDismiss = { activeStoryGroup = null },
    )
  }

  commentsPost?.let { post ->
    val comments by
      viewModel
        .observeComments(post.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    CommentsBottomSheet(
      comments = comments,
      onDismiss = { commentsPost = null },
      onSendComment = { text -> viewModel.addComment(post, text) },
    )
  }

  reportTargetPost?.let { post ->
    ReportDialog(
      targetSummary = "@${post.authorUsername}: ${post.caption.ifBlank { "Media post" }}",
      onDismiss = { reportTargetPost = null },
      onSubmit = { reason ->
        viewModel.submitReport(
          targetType = "post",
          targetId = post.id,
          targetSummary = "@${post.authorUsername} - ${post.caption.take(80)}",
          reason = reason,
        )
        reportTargetPost = null
      },
    )
  }
}

@Composable
private fun FeedPostCard(
  post: Post,
  isLiked: Boolean,
  isOwnPost: Boolean,
  isFollowing: Boolean,
  onLikeToggle: () -> Unit,
  onOpenComments: () -> Unit,
  onShare: () -> Unit,
  onFollowToggle: () -> Unit,
  onAuthorClick: () -> Unit,
  onDeletePost: () -> Unit,
  onReportPost: () -> Unit,
) {
  val context = LocalContext.current
  var showMenu by remember { mutableStateOf(false) }
  var showHeartBurst by remember { mutableStateOf(false) }

  LaunchedEffect(showHeartBurst) {
    if (showHeartBurst) {
      delay(650)
      showHeartBurst = false
    }
  }

  Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AvatarWithStoryRing(
        photoDataUrl = post.authorPhotoDataUrl,
        username = post.authorUsername,
        size = 38.dp,
        onClick = onAuthorClick,
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f).clickable(onClick = onAuthorClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = post.authorUsername,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          if (post.postKind == Post.KIND_REEL) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = "REEL",
                style = MaterialTheme.typography.labelSmall,
                color = BrandMagenta,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
        if (!post.audioTitle.isNullOrBlank()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.MusicNote,
              contentDescription = null,
              modifier = Modifier.size(12.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = post.audioTitle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      if (!isOwnPost) {
        TextButton(
          onClick = onFollowToggle,
          modifier = Modifier.testTag("follow_button_${post.authorId}"),
        ) {
          Text(
            text = if (isFollowing) "Following" else "Follow",
            color = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else BrandMagenta,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      Box {
        IconButton(onClick = { showMenu = true }) {
          Icon(Icons.Outlined.MoreVert, contentDescription = "Post options")
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
          if (isOwnPost) {
            DropdownMenuItem(
              text = { Text("Delete post") },
              leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
              onClick = {
                showMenu = false
                onDeletePost()
              },
            )
          } else {
            DropdownMenuItem(
              text = { Text("Report post") },
              leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
              onClick = {
                showMenu = false
                onReportPost()
              },
            )
          }
        }
      }
    }

    // Media Container (Photo or Video)
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .aspectRatio(if (post.postKind == Post.KIND_REEL) 0.85f else 1f)
          .background(PureBlack)
          .pointerInput(isLiked) {
            detectTapGestures(
              onDoubleTap = {
                showHeartBurst = true
                if (!isLiked) onLikeToggle()
              }
            )
          },
      contentAlignment = Alignment.Center,
    ) {
      if (post.mediaType == Post.MEDIA_VIDEO) {
        VideoPlayerSurface(
          videoUri = post.videoUri,
          thumbnailDataUrl = post.mediaDataUrl,
          isActive = true,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        DataImage(
          dataUrl = post.mediaDataUrl,
          contentDescription = post.caption.ifBlank { "Post by ${post.authorUsername}" },
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
      }

      androidx.compose.animation.AnimatedVisibility(
        visible = showHeartBurst,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        Icon(
          imageVector = Icons.Filled.Favorite,
          contentDescription = null,
          tint = PureWhite.copy(alpha = 0.92f),
          modifier = Modifier.size(88.dp),
        )
      }
    }

    // Action Bar
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AnimatedHeartButton(
        isLiked = isLiked,
        onClick = onLikeToggle,
        modifier = Modifier.testTag("like_button_${post.id}"),
      ) { tint, _ ->
        Icon(
          imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
          contentDescription = if (isLiked) "Unlike" else "Like",
          tint = tint,
        )
      }
      Text(
        text = "${post.likesCount}",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(end = 10.dp),
      )

      IconButton(
        onClick = onOpenComments,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("comment_button_${post.id}"),
      ) {
        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comments")
      }
      Text(
        text = "${post.commentsCount}",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(end = 10.dp),
      )

      IconButton(
        onClick = {
          onShare()
          val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(
                Intent.EXTRA_TEXT,
                "Check out @${post.authorUsername} on InstaConnect: ${post.caption}",
              )
            }
          context.startActivity(Intent.createChooser(shareIntent, "Share via InstaConnect"))
        },
        modifier = Modifier.minimumInteractiveComponentSize().testTag("share_button_${post.id}"),
      ) {
        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Share post")
      }
      Text(
        text = "${post.sharesCount}",
        style = MaterialTheme.typography.labelLarge,
      )
    }

    // Caption & Hashtags
    if (post.caption.isNotBlank()) {
      val annotatedCaption = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
          append(post.authorUsername)
          append(" ")
        }
        val words = post.caption.split(" ")
        words.forEachIndexed { idx, word ->
          if (word.startsWith("#")) {
            withStyle(SpanStyle(color = BrandMagenta, fontWeight = FontWeight.SemiBold)) {
              append(word)
            }
          } else {
            append(word)
          }
          if (idx < words.lastIndex) append(" ")
        }
      }
      Text(
        text = annotatedCaption,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
      )
    }

    if (post.commentsCount > 0) {
      Text(
        text = "View all ${post.commentsCount} comments",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
          Modifier.clickable(onClick = onOpenComments)
            .padding(horizontal = 16.dp, vertical = 4.dp),
      )
    }
    HorizontalDivider(
      modifier = Modifier.padding(top = 8.dp),
      color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    )
  }
}

@Composable
private fun EmptyFeedCard(
  onCreatePost: () -> Unit,
  onCreateStory: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier.fillMaxWidth().padding(20.dp),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_feed),
        contentDescription = "No posts yet",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(18.dp)),
      )
      Text(
        text = "Your InstaConnect feed is ready",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text =
          "Share your first photo, upload a vertical Reel, or drop a 24-hour Story with music & stickers.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        GradientButton(
          text = "New Post",
          onClick = onCreatePost,
          modifier = Modifier.weight(1f).testTag("empty_create_post_btn"),
        )
        GradientButton(
          text = "Add Story",
          onClick = onCreateStory,
          gradient = BrandGradient,
          modifier = Modifier.weight(1f).testTag("empty_create_story_btn"),
        )
      }
    }
  }
}

@Composable
fun StoryViewerDialog(
  stories: List<Story>,
  onStoryViewed: (Story) -> Unit,
  onDismiss: () -> Unit,
) {
  var index by remember { mutableIntStateOf(0) }
  var progress by remember(index) { mutableStateOf(0f) }
  val currentStory = stories.getOrNull(index)

  LaunchedEffect(index, stories.size) {
    if (currentStory == null) {
      onDismiss()
      return@LaunchedEffect
    }
    onStoryViewed(currentStory)
    progress = 0f
    val steps = 50
    for (i in 1..steps) {
      delay(100)
      progress = i / steps.toFloat()
    }
    if (index < stories.lastIndex) {
      index += 1
    } else {
      onDismiss()
    }
  }

  if (currentStory != null) {
    Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(PureBlack)
            .safeDrawingPadding()
            .pointerInput(index) {
              detectTapGestures { offset ->
                if (offset.x < size.width * 0.3f && index > 0) {
                  index -= 1
                } else if (index < stories.lastIndex) {
                  index += 1
                } else {
                  onDismiss()
                }
              }
            }
      ) {
        DataImage(
          dataUrl = currentStory.mediaDataUrl,
          contentDescription = "Story by ${currentStory.authorUsername}",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )

        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(
                    PureBlack.copy(alpha = 0.55f),
                    Color.Transparent,
                    PureBlack.copy(alpha = 0.65f),
                  )
                )
              )
        )

        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            stories.forEachIndexed { idx, _ ->
              LinearProgressIndicator(
                progress = {
                  when {
                    idx < index -> 1f
                    idx == index -> progress
                    else -> 0f
                  }
                },
                modifier = Modifier.weight(1f).height(3.dp).clip(CircleShape),
                color = PureWhite,
                trackColor = PureWhite.copy(alpha = 0.3f),
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            AvatarWithStoryRing(
              photoDataUrl = currentStory.authorPhotoDataUrl,
              username = currentStory.authorUsername,
              size = 38.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = currentStory.authorUsername,
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
              )
              if (!currentStory.musicTitle.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(13.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = currentStory.musicTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite.copy(alpha = 0.9f),
                  )
                }
              }
            }
            IconButton(onClick = onDismiss) {
              Icon(Icons.Outlined.Close, contentDescription = "Close story", tint = PureWhite)
            }
          }
        }

        // Sticker & Caption Overlays
        Column(
          modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (!currentStory.stickerLabel.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = Color.Transparent,
              modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(ButtonGradient),
            ) {
              Text(
                text = currentStory.stickerLabel,
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
              )
            }
          }
          if (!currentStory.caption.isNullOrBlank()) {
            Text(
              text = currentStory.caption,
              style = MaterialTheme.typography.bodyLarge,
              color = PureWhite,
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Visibility,
              contentDescription = null,
              tint = PureWhite.copy(alpha = 0.8f),
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${currentStory.viewsCount} views • Expires in 24h",
              style = MaterialTheme.typography.labelSmall,
              color = PureWhite.copy(alpha = 0.8f),
            )
          }
        }
      }
    }
  }
}
