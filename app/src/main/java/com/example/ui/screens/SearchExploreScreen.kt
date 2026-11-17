package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.Post
import com.example.model.UserProfile
import com.example.ui.InstaConnectViewModel
import com.example.ui.components.AvatarWithStoryRing
import com.example.ui.components.DataImage
import com.example.ui.components.VideoPlayerSurface
import com.example.ui.theme.BrandMagenta
import com.example.ui.theme.ButtonGradient
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun SearchExploreScreen(
  viewModel: InstaConnectViewModel,
  allPosts: List<Post>,
  publicUsers: List<UserProfile>,
  followingIds: Set<String>,
  onOpenUserProfile: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var query by remember { mutableStateOf("") }
  var selectedTag by remember { mutableStateOf<String?>(null) }
  var previewPost by remember { mutableStateOf<Post?>(null) }

  val trendingTags =
    remember(allPosts) {
      val computed =
        allPosts
          .flatMap { it.hashtags }
          .groupingBy { it }
          .eachCount()
          .entries
          .sortedByDescending { it.value }
          .map { it.key }
      if (computed.isNotEmpty()) computed.take(10)
      else listOf("photography", "reels", "streetstyle", "travel", "music", "design")
    }

  val matchingUsers =
    remember(publicUsers, query) {
      val q = query.trim().lowercase().removePrefix("@")
      if (q.isEmpty()) emptyList()
      else
        publicUsers.filter {
          it.userId != viewModel.currentUserId &&
            (it.username.lowercase().contains(q) || it.displayName.lowercase().contains(q))
        }
    }

  val filteredPosts =
    remember(allPosts, query, selectedTag) {
      val q = query.trim().lowercase().removePrefix("#")
      allPosts.filter { post ->
        val matchesTag = selectedTag == null || post.hashtags.contains(selectedTag)
        val matchesQuery =
          q.isEmpty() ||
            post.caption.lowercase().contains(q) ||
            post.authorUsername.lowercase().contains(q) ||
            post.hashtags.any { it.contains(q) }
        matchesTag && matchesQuery
      }
    }

  Column(modifier = modifier.fillMaxSize()) {
    // Search Bar
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
      Text(
        text = "Explore",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(10.dp))
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("Search users, #hashtags, or posts...") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { query = "" }) {
              Icon(Icons.Outlined.Close, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("search_input"),
      )
    }

    // Trending Hashtags Strip
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(bottom = 10.dp),
    ) {
      item(key = "all_tags") {
        HashtagChip(
          label = "All",
          selected = selectedTag == null,
          onClick = { selectedTag = null },
        )
      }
      items(trendingTags, key = { it }) { tag ->
        HashtagChip(
          label = "#$tag",
          selected = selectedTag == tag,
          onClick = { selectedTag = if (selectedTag == tag) null else tag },
        )
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

    // Matching Users List (when searching)
    if (query.isNotBlank() && matchingUsers.isNotEmpty()) {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      ) {
        items(matchingUsers.take(6), key = { it.userId }) { user ->
          val isFollowing = followingIds.contains(user.userId)
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable { onOpenUserProfile(user.userId) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            AvatarWithStoryRing(
              photoDataUrl = user.photoDataUrl,
              username = user.username,
              size = 44.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "${user.displayName} • ${user.followersCount} followers",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            TextButton(onClick = { viewModel.toggleFollow(user, isFollowing) }) {
              Text(
                text = if (isFollowing) "Following" else "Follow",
                color = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else BrandMagenta,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }

    // Explore Photos & Videos Grid
    if (filteredPosts.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Filled.Tag,
            contentDescription = null,
            tint = BrandMagenta,
            modifier = Modifier.size(44.dp),
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "No matching posts yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Try another hashtag or upload new photos and videos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 115.dp),
        contentPadding = PaddingValues(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize().testTag("explore_grid"),
      ) {
        items(filteredPosts, key = { it.id }) { post ->
          Box(
            modifier =
              Modifier.aspectRatio(1f)
                .background(PureBlack)
                .clickable { previewPost = post }
          ) {
            DataImage(
              dataUrl = post.mediaDataUrl,
              contentDescription = post.caption,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
            )
            if (post.mediaType == Post.MEDIA_VIDEO || post.postKind == Post.KIND_REEL) {
              Box(
                modifier =
                  Modifier.align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(PureBlack.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.PlayArrow,
                  contentDescription = "Video",
                  tint = PureWhite,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        }
      }
    }
  }

  previewPost?.let { post ->
    Dialog(onDismissRequest = { previewPost = null }) {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable {
                  previewPost = null
                  onOpenUserProfile(post.authorId)
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            AvatarWithStoryRing(
              photoDataUrl = post.authorPhotoDataUrl,
              username = post.authorUsername,
              size = 34.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "@${post.authorUsername}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { previewPost = null }) {
              Icon(Icons.Outlined.Close, contentDescription = "Close preview")
            }
          }
          Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(PureBlack)) {
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
                contentDescription = post.caption,
                modifier = Modifier.fillMaxSize(),
              )
            }
          }
          if (post.caption.isNotBlank()) {
            Text(
              text = post.caption,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(14.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HashtagChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = Color.Transparent,
    modifier =
      Modifier.clip(RoundedCornerShape(20.dp))
        .then(
          if (selected) Modifier.background(ButtonGradient)
          else
            Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
        )
        .clickable(onClick = onClick),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) PureWhite else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
    )
  }
}
