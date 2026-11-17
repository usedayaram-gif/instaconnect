package com.example.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.signOutUser
import com.example.model.Post
import com.example.model.UiState
import com.example.model.UserPrivateInfo
import com.example.model.UserProfile
import com.example.ui.UserProfileViewModel
import com.example.ui.components.AvatarWithStoryRing
import com.example.ui.components.DataImage
import com.example.ui.components.GradientButton
import com.example.ui.components.VideoPlayerSurface
import com.example.ui.theme.BrandMagenta
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.util.MediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun UserProfileView(
  viewModel: UserProfileViewModel,
  isDarkTheme: Boolean,
  isAdmin: Boolean,
  onToggleTheme: () -> Unit,
  onOpenAdminPanel: () -> Unit,
  onOpenChat: (UserProfile) -> Unit,
  onBack: (() -> Unit)? = null,
  onStatusMessage: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  if (onBack != null) {
    BackHandler { onBack() }
  }

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var showEditDialog by remember { mutableStateOf(false) }
  var showFollowListMode by remember { mutableStateOf<String?>(null) }
  var selectedPreviewPost by remember { mutableStateOf<Post?>(null) }

  LaunchedEffect(uiState.statusMessage) {
    val msg = uiState.statusMessage
    if (!msg.isNullOrBlank()) {
      onStatusMessage(msg)
      viewModel.clearStatusMessage()
    }
  }

  val displayPosts =
    remember(uiState.posts, uiState.reels, uiState.selectedTab) {
      if (uiState.selectedTab == 0) uiState.posts else uiState.reels
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize().widthIn(max = 720.dp)) {
      // Top Header
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (onBack != null) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
        Text(
          text = "@${uiState.username.ifBlank { "..." }}",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        IconButton(
          onClick = { viewModel.refreshProfile() },
          enabled = !uiState.isRefreshing,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("refresh_profile_button"),
        ) {
          if (uiState.isRefreshing) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = BrandMagenta,
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = Icons.Outlined.Refresh,
              contentDescription = "Refresh profile",
            )
          }
        }
        if (uiState.isOwnProfile) {
          if (isAdmin) {
            IconButton(
              onClick = onOpenAdminPanel,
              modifier = Modifier.minimumInteractiveComponentSize().testTag("admin_panel_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.AdminPanelSettings,
                contentDescription = "Admin Panel",
                tint = BrandMagenta,
              )
            }
          }
          IconButton(
            onClick = onToggleTheme,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("theme_toggle_button"),
          ) {
            Icon(
              imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
              contentDescription = "Toggle light/dark theme",
            )
          }
          IconButton(
            onClick = { signOutUser(credentialManager, scope) },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("sign_out_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.Logout,
              contentDescription = "Sign out",
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

      if (uiState.profileState is UiState.Loading && uiState.username.isBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = BrandMagenta)
        }
      } else {
        // Profile Avatar, Counts & Bio
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            AvatarWithStoryRing(
              photoDataUrl = uiState.avatarDataUrl,
              username = uiState.username.ifBlank { "U" },
              size = 84.dp,
              hasStory = true,
            )
            Spacer(modifier = Modifier.width(20.dp))
            Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              ProfileStatColumn(
                count = "${uiState.postsCount}",
                label = "Posts",
                onClick = {},
              )
              ProfileStatColumn(
                count = "${uiState.followersCount}",
                label = "Followers",
                onClick = { showFollowListMode = "followers" },
                modifier = Modifier.testTag("followers_count_stat"),
              )
              ProfileStatColumn(
                count = "${uiState.followingCount}",
                label = "Following",
                onClick = { showFollowListMode = "following" },
                modifier = Modifier.testTag("following_count_stat"),
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = uiState.displayName.ifBlank { "InstaConnector" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          if (uiState.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = uiState.bio,
              style = MaterialTheme.typography.bodyMedium,
            )
          }
          if (
            uiState.isOwnProfile &&
              (!uiState.privateInfo?.email.isNullOrBlank() ||
                !uiState.privateInfo?.phoneNumber.isNullOrBlank())
          ) {
            Spacer(modifier = Modifier.height(4.dp))
            val contact =
              listOfNotNull(uiState.privateInfo?.email, uiState.privateInfo?.phoneNumber)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
            if (contact.isNotBlank()) {
              Text(
                text = contact,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          if (uiState.isOwnProfile) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              GradientButton(
                text = "Edit Profile",
                onClick = { showEditDialog = true },
                modifier = Modifier.weight(1f).testTag("edit_profile_button"),
              )
              if (isAdmin) {
                OutlinedButton(
                  onClick = onOpenAdminPanel,
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.height(48.dp),
                ) {
                  Text("Admin")
                }
              }
            }
          } else if (uiState.profile != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              if (uiState.isFollowing) {
                OutlinedButton(
                  onClick = { viewModel.toggleFollow() },
                  enabled = !uiState.isFollowUpdating,
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("follow_toggle_button"),
                ) {
                  if (uiState.isFollowUpdating) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(18.dp),
                      strokeWidth = 2.dp,
                      color = BrandMagenta,
                    )
                  } else {
                    Icon(
                      imageVector = Icons.Filled.Check,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Following", fontWeight = FontWeight.Bold)
                  }
                }
              } else {
                GradientButton(
                  text = "Follow",
                  isLoading = uiState.isFollowUpdating,
                  onClick = { viewModel.toggleFollow() },
                  modifier = Modifier.weight(1f).testTag("follow_toggle_button"),
                )
              }

              OutlinedButton(
                onClick = { uiState.profile?.let(onOpenChat) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(48.dp),
              ) {
                Text("Message")
              }
            }
          }
        }

        // Posts / Reels Tabs
        TabRow(selectedTabIndex = uiState.selectedTab) {
          Tab(
            selected = uiState.selectedTab == 0,
            onClick = { viewModel.selectTab(0) },
            icon = { Icon(Icons.Outlined.GridOn, contentDescription = "Posts grid") },
            text = { Text("Posts") },
          )
          Tab(
            selected = uiState.selectedTab == 1,
            onClick = { viewModel.selectTab(1) },
            icon = { Icon(Icons.Outlined.VideoLibrary, contentDescription = "Reels grid") },
            text = { Text("Reels") },
          )
        }

        if (displayPosts.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (uiState.selectedTab == 0) "No posts shared yet" else "No reels uploaded yet",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        } else {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 115.dp),
            contentPadding = PaddingValues(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize().testTag("user_posts_grid"),
          ) {
            items(displayPosts, key = { it.id }) { post ->
              Box(
                modifier =
                  Modifier.aspectRatio(1f)
                    .background(PureBlack)
                    .clickable { selectedPreviewPost = post }
              ) {
                DataImage(
                  dataUrl = post.mediaDataUrl,
                  contentDescription = post.caption.ifBlank { "Post by @${post.authorUsername}" },
                  modifier = Modifier.fillMaxSize(),
                  contentScale = ContentScale.Crop,
                )
                if (post.mediaType == Post.MEDIA_VIDEO || post.postKind == Post.KIND_REEL) {
                  Box(
                    modifier =
                      Modifier.align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(PureBlack.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Filled.PlayArrow,
                      contentDescription = "Reel",
                      tint = PureWhite,
                      modifier = Modifier.size(14.dp),
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

  if (showEditDialog && uiState.profile != null) {
    EditUserProfileDialog(
      profile = uiState.profile!!,
      privateInfo = uiState.privateInfo,
      onDismiss = { showEditDialog = false },
      onSave = { username, displayName, bio, photoDataUrl, isPublic, email, phone ->
        viewModel.updateProfile(
          username = username,
          displayName = displayName,
          bio = bio,
          photoDataUrl = photoDataUrl,
          isPublic = isPublic,
          email = email,
          phoneNumber = phone,
          onComplete = { showEditDialog = false },
        )
      },
    )
  }

  showFollowListMode?.let { mode ->
    val itemsList = if (mode == "followers") uiState.followers else uiState.following
    AlertDialog(
      onDismissRequest = { showFollowListMode = null },
      title = { Text(if (mode == "followers") "Followers" else "Following") },
      text = {
        if (itemsList.isEmpty()) {
          Text("No accounts here yet.")
        } else {
          LazyColumn(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            items(itemsList, key = { it.id }) { rel ->
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                AvatarWithStoryRing(
                  photoDataUrl = rel.photoDataUrl,
                  username = rel.username,
                  size = 38.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "@${rel.username}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showFollowListMode = null }) { Text("Close") }
      },
    )
  }

  selectedPreviewPost?.let { post ->
    Dialog(onDismissRequest = { selectedPreviewPost = null }) {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "@${post.authorUsername}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { selectedPreviewPost = null }) {
              Icon(Icons.Outlined.Close, contentDescription = "Close preview")
            }
          }
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .aspectRatio(1f)
                .background(PureBlack)
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
                contentDescription = post.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
              )
            }
          }
          Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(16.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.Favorite,
                  contentDescription = null,
                  tint = BrandMagenta,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("${post.likesCount}", style = MaterialTheme.typography.labelLarge)
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Outlined.ChatBubbleOutline,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("${post.commentsCount}", style = MaterialTheme.typography.labelLarge)
              }
            }
            if (post.caption.isNotBlank()) {
              Text(text = post.caption, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProfileStatColumn(
  count: String,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 4.dp),
  ) {
    Text(
      text = count,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun EditUserProfileDialog(
  profile: UserProfile,
  privateInfo: UserPrivateInfo?,
  onDismiss: () -> Unit,
  onSave: (
    username: String,
    displayName: String,
    bio: String,
    photoDataUrl: String?,
    isPublic: Boolean,
    email: String?,
    phoneNumber: String?,
  ) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var username by remember { mutableStateOf(profile.username) }
  var displayName by remember { mutableStateOf(profile.displayName) }
  var bio by remember { mutableStateOf(profile.bio) }
  var photoDataUrl by remember { mutableStateOf(profile.photoDataUrl) }
  var isPublic by remember { mutableStateOf(profile.isPublic) }
  var email by remember { mutableStateOf(privateInfo?.email.orEmpty()) }
  var phone by remember { mutableStateOf(privateInfo?.phoneNumber.orEmpty()) }

  val avatarPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          val encoded =
            withContext(Dispatchers.IO) {
              MediaUtils.encodeImageUriToDataUrl(context, uri, maxDim = 420, quality = 74)
            }
          if (encoded != null) photoDataUrl = encoded
        }
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Profile", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier =
            Modifier.clickable {
              avatarPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
          contentAlignment = Alignment.BottomEnd,
        ) {
          AvatarWithStoryRing(
            photoDataUrl = photoDataUrl,
            username = username,
            size = 76.dp,
            hasStory = true,
          )
          Surface(
            shape = CircleShape,
            color = BrandMagenta,
            modifier = Modifier.size(26.dp).border(2.dp, PureWhite, CircleShape),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Change photo",
                tint = PureWhite,
                modifier = Modifier.size(14.dp),
              )
            }
          }
        }
        Text(
          text = "Tap photo to update avatar",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = username,
          onValueChange = { username = it.lowercase().take(30) },
          label = { Text("Username") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("edit_username_input"),
        )
        OutlinedTextField(
          value = displayName,
          onValueChange = { displayName = it.take(50) },
          label = { Text("Display Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = bio,
          onValueChange = { bio = it.take(200) },
          label = { Text("Bio") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth().testTag("edit_bio_input"),
        )
        OutlinedTextField(
          value = email,
          onValueChange = { email = it.take(120) },
          label = { Text("Contact Email (Private)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it.take(30) },
          label = { Text("Mobile Number (Private)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Discoverable in Search", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = isPublic, onCheckedChange = { isPublic = it })
        }
      }
    },
    confirmButton = {
      GradientButton(
        text = "Save",
        onClick = { onSave(username, displayName, bio, photoDataUrl, isPublic, email, phone) },
        modifier = Modifier.testTag("save_profile_button"),
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
