package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ModerationReport
import com.example.model.Post
import com.example.model.Story
import com.example.model.UserProfile
import com.example.ui.InstaConnectViewModel
import com.example.ui.components.AvatarWithStoryRing
import com.example.ui.components.DataImage
import com.example.ui.theme.BrandMagenta
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessEmerald

@Composable
fun AdminPanelScreen(
  viewModel: InstaConnectViewModel,
  users: List<UserProfile>,
  posts: List<Post>,
  stories: List<Story>,
  reports: List<ModerationReport>,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onBack() }

  var selectedTab by remember { mutableIntStateOf(0) } // 0=Reports, 1=Users, 2=Posts
  val openReportsCount = remember(reports) { reports.count { it.status == ModerationReport.STATUS_OPEN } }

  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
      Text(
        text = "Admin Console",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
    }

    // App Statistics Row
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      AdminStatCard(
        label = "Users",
        value = "${users.size}",
        icon = Icons.Outlined.Group,
        modifier = Modifier.weight(1f),
      )
      AdminStatCard(
        label = "Posts & Reels",
        value = "${posts.size}",
        icon = Icons.Outlined.PhotoLibrary,
        modifier = Modifier.weight(1f),
      )
      AdminStatCard(
        label = "Stories",
        value = "${stories.size}",
        icon = Icons.Outlined.CheckCircle,
        modifier = Modifier.weight(1f),
      )
      AdminStatCard(
        label = "Open Reports",
        value = "$openReportsCount",
        icon = Icons.Outlined.Flag,
        modifier = Modifier.weight(1f),
      )
    }

    TabRow(selectedTabIndex = selectedTab) {
      Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Reports") })
      Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Users") })
      Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Posts") })
    }

    when (selectedTab) {
      0 -> {
        if (reports.isEmpty()) {
          EmptyAdminMessage("No user reports submitted.")
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(reports, key = { it.id }) { report ->
              Card(
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                  ),
                shape = RoundedCornerShape(16.dp),
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = "${report.targetType.uppercase()} REPORT",
                      style = MaterialTheme.typography.labelLarge,
                      color = BrandMagenta,
                      fontWeight = FontWeight.Bold,
                    )
                    Surface(
                      color =
                        when (report.status) {
                          ModerationReport.STATUS_OPEN -> DangerRed.copy(alpha = 0.15f)
                          else -> SuccessEmerald.copy(alpha = 0.15f)
                        },
                      shape = RoundedCornerShape(8.dp),
                    ) {
                      Text(
                        text = report.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color =
                          if (report.status == ModerationReport.STATUS_OPEN) DangerRed
                          else SuccessEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = report.targetSummary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = "Reason: ${report.reason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (report.status == ModerationReport.STATUS_OPEN) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      if (report.targetType == "post") {
                        Button(
                          onClick = {
                            viewModel.adminDeletePost(report.targetId)
                            viewModel.resolveReport(report.id, ModerationReport.STATUS_RESOLVED)
                          },
                          colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        ) {
                          Text("Delete Post & Resolve")
                        }
                      } else {
                        Button(
                          onClick = {
                            viewModel.resolveReport(report.id, ModerationReport.STATUS_RESOLVED)
                          }
                        ) {
                          Text("Resolve")
                        }
                      }
                      OutlinedButton(
                        onClick = {
                          viewModel.resolveReport(report.id, ModerationReport.STATUS_DISMISSED)
                        }
                      ) {
                        Text("Dismiss")
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      1 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize().testTag("admin_users_list"),
          contentPadding = PaddingValues(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(users, key = { it.userId }) { user ->
            Card(
              colors =
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
              shape = RoundedCornerShape(16.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                    text =
                      "${user.postsCount} posts • ${user.followersCount} followers" +
                        if (user.isSuspended) " • SUSPENDED" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                      if (user.isSuspended) DangerRed
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (user.userId != viewModel.currentUserId) {
                  OutlinedButton(onClick = { viewModel.adminToggleSuspend(user) }) {
                    Icon(
                      imageVector = Icons.Outlined.Block,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (user.isSuspended) "Reinstate" else "Suspend")
                  }
                }
              }
            }
          }
        }
      }
      else -> {
        if (posts.isEmpty()) {
          EmptyAdminMessage("No posts published yet.")
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("admin_posts_list"),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(posts, key = { it.id }) { post ->
              Card(
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                  ),
                shape = RoundedCornerShape(16.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  DataImage(
                    dataUrl = post.mediaDataUrl,
                    contentDescription = post.caption,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "@${post.authorUsername} (${post.postKind})",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = post.caption.ifBlank { "No caption" },
                      style = MaterialTheme.typography.bodyMedium,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  IconButton(onClick = { viewModel.adminDeletePost(post.id) }) {
                    Icon(
                      imageVector = Icons.Outlined.DeleteOutline,
                      contentDescription = "Delete inappropriate post",
                      tint = DangerRed,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminStatCard(
  label: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    shape = RoundedCornerShape(14.dp),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(icon, contentDescription = null, tint = BrandMagenta, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun EmptyAdminMessage(text: String) {
  Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
