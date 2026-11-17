package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.data.SocialRepository
import com.example.model.Post
import com.example.model.UiState
import com.example.ui.InstaConnectViewModel
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatsHubScreen
import com.example.ui.screens.CreatePostSheet
import com.example.ui.screens.CreateStorySheet
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReelsScreen
import com.example.ui.screens.SearchExploreScreen
import com.example.ui.theme.BrandMagenta
import com.example.ui.theme.InstaConnectTheme
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { InstaConnectRoot() }
  }
}

private enum class MainTab(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
  SEARCH("Explore", Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
  REELS("Reels", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle, "nav_reels"),
  CHAT("Chat", Icons.Filled.Forum, Icons.Outlined.Forum, "nav_chat"),
  PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile"),
}

private sealed interface SubDestination {
  data object None : SubDestination
  data object Notifications : SubDestination
  data class UserProfile(val userId: String) : SubDestination
  data object AdminPanel : SubDestination
}

@Composable
private fun InstaConnectRoot() {
  val authStateFlow = remember { Firebase.auth.authStateFlow() }
  val currentUser by authStateFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
  val systemDark = isSystemInDarkTheme()

  val user = currentUser
  if (user == null) {
    InstaConnectTheme(darkTheme = systemDark) {
      AuthScreen(onAuthSuccess = {})
    }
  } else {
    key(user.uid) {
      val context = LocalContext.current
      val appContext = context.applicationContext
      val viewModel: InstaConnectViewModel =
        viewModel(
          key = user.uid,
          factory =
            viewModelFactory {
              initializer {
                val db =
                  FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
                InstaConnectViewModel(
                  repository = SocialRepository(db, Firebase.auth),
                  currentUserId = user.uid,
                  currentUserEmail = user.email,
                  currentUserDisplayName = user.displayName,
                )
              }
            },
        )
      AuthenticatedInstaConnectShell(
        viewModel = viewModel,
        systemDark = systemDark,
      )
    }
  }
}

@Composable
private fun AuthenticatedInstaConnectShell(
  viewModel: InstaConnectViewModel,
  systemDark: Boolean,
) {
  val isDarkModeOverride by viewModel.isDarkModeOverride.collectAsStateWithLifecycle()
  val useDarkTheme = isDarkModeOverride ?: systemDark

  val currentProfileState by viewModel.currentProfile.collectAsStateWithLifecycle()
  val feedState by viewModel.feedPosts.collectAsStateWithLifecycle()
  val stories by viewModel.stories.collectAsStateWithLifecycle()
  val publicUsers by viewModel.publicUsers.collectAsStateWithLifecycle()
  val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
  val chats by viewModel.chats.collectAsStateWithLifecycle()
  val notifications by viewModel.notifications.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
  val adminPosts by viewModel.adminPosts.collectAsStateWithLifecycle()
  val adminReports by viewModel.adminReports.collectAsStateWithLifecycle()
  val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

  val currentProfile = (currentProfileState as? UiState.Success)?.data
  val allPosts = (feedState as? UiState.Success)?.data.orEmpty()
  val unreadNotificationsCount = remember(notifications) { notifications.count { !it.isRead } }

  var selectedTab by remember { mutableStateOf(MainTab.HOME) }
  var subDestination by remember { mutableStateOf<SubDestination>(SubDestination.None) }
  var activeChatId by remember { mutableStateOf<String?>(null) }
  var createPostKind by remember { mutableStateOf<String?>(null) }
  var showCreateStory by remember { mutableStateOf(false) }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(statusBanner) {
    val msg = statusBanner
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusBanner()
    }
  }

  if (subDestination == SubDestination.None && selectedTab != MainTab.HOME && activeChatId == null) {
    BackHandler { selectedTab = MainTab.HOME }
  }

  val openProfile: (String) -> Unit = { targetUserId ->
    if (targetUserId == viewModel.currentUserId) {
      subDestination = SubDestination.None
      selectedTab = MainTab.PROFILE
    } else {
      subDestination = SubDestination.UserProfile(targetUserId)
    }
  }

  val openChat: (String) -> Unit = { chatId ->
    subDestination = SubDestination.None
    selectedTab = MainTab.CHAT
    activeChatId = chatId
  }

  InstaConnectTheme(darkTheme = useDarkTheme) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavigationRail = maxWidth >= 600.dp
      val showNavigationChrome =
        subDestination == SubDestination.None && activeChatId == null

      val navColors =
        NavigationBarItemDefaults.colors(
          selectedIconColor = BrandMagenta,
          selectedTextColor = BrandMagenta,
          indicatorColor = BrandMagenta.copy(alpha = 0.12f),
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

      val railColors =
        NavigationRailItemDefaults.colors(
          selectedIconColor = BrandMagenta,
          selectedTextColor = BrandMagenta,
          indicatorColor = BrandMagenta.copy(alpha = 0.12f),
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

      Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
          if (!useNavigationRail && showNavigationChrome) {
            NavigationBar(
              containerColor =
                if (selectedTab == MainTab.REELS) PureBlack
                else MaterialTheme.colorScheme.surface,
              contentColor =
                if (selectedTab == MainTab.REELS) PureWhite
                else MaterialTheme.colorScheme.onSurface,
              windowInsets = WindowInsets.navigationBars,
            ) {
              MainTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                NavigationBarItem(
                  selected = selected,
                  onClick = {
                    subDestination = SubDestination.None
                    activeChatId = null
                    selectedTab = tab
                  },
                  icon = {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = tab.label,
                    )
                  },
                  label = { Text(tab.label, maxLines = 1) },
                  colors = navColors,
                  modifier = Modifier.testTag(tab.testTag),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(
          modifier =
            Modifier.fillMaxSize()
              .padding(innerPadding)
              .windowInsetsPadding(WindowInsets.statusBars)
        ) {
          if (useNavigationRail && showNavigationChrome) {
            NavigationRail(
              modifier = Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.navigationBars),
              containerColor = MaterialTheme.colorScheme.surface,
              header = {
                FloatingActionButton(
                  onClick = { createPostKind = Post.KIND_POST },
                  containerColor = BrandMagenta,
                  contentColor = PureWhite,
                  modifier = Modifier.padding(top = 8.dp),
                ) {
                  Icon(Icons.Filled.Add, contentDescription = "Create post")
                }
              },
            ) {
              Spacer(modifier = Modifier.height(12.dp))
              MainTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                NavigationRailItem(
                  selected = selected,
                  onClick = {
                    subDestination = SubDestination.None
                    activeChatId = null
                    selectedTab = tab
                  },
                  icon = {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = tab.label,
                    )
                  },
                  label = { Text(tab.label) },
                  colors = railColors,
                  modifier = Modifier.testTag(tab.testTag),
                )
              }
            }
          }

          Box(
            modifier =
              Modifier.weight(1f)
                .fillMaxHeight()
                .let { mod ->
                  if (showNavigationChrome) mod else mod.windowInsetsPadding(WindowInsets.navigationBars)
                }
          ) {
            when (val dest = subDestination) {
              is SubDestination.Notifications -> {
                NotificationsScreen(
                  viewModel = viewModel,
                  notifications = notifications,
                  onBack = { subDestination = SubDestination.None },
                  onOpenUserProfile = openProfile,
                  onOpenChat = openChat,
                )
              }
              is SubDestination.UserProfile -> {
                ProfileScreen(
                  viewModel = viewModel,
                  viewedUserId = dest.userId,
                  isDarkTheme = useDarkTheme,
                  followingIds = followingIds,
                  isAdmin = isAdmin,
                  onToggleTheme = { viewModel.toggleDarkTheme(useDarkTheme) },
                  onOpenAdminPanel = { subDestination = SubDestination.AdminPanel },
                  onOpenChat = openChat,
                  onBack = { subDestination = SubDestination.None },
                )
              }
              is SubDestination.AdminPanel -> {
                AdminPanelScreen(
                  viewModel = viewModel,
                  users = adminUsers,
                  posts = adminPosts,
                  stories = stories,
                  reports = adminReports,
                  onBack = { subDestination = SubDestination.None },
                )
              }
              SubDestination.None -> {
                Crossfade(targetState = selectedTab, label = "MainTabCrossfade") { tab ->
                  when (tab) {
                    MainTab.HOME -> {
                      HomeFeedScreen(
                        viewModel = viewModel,
                        currentProfile = currentProfile,
                        feedState = feedState,
                        stories = stories,
                        publicUsers = publicUsers,
                        followingIds = followingIds,
                        unreadNotificationsCount = unreadNotificationsCount,
                        onOpenCreatePost = { createPostKind = Post.KIND_POST },
                        onOpenCreateStory = { showCreateStory = true },
                        onOpenNotifications = { subDestination = SubDestination.Notifications },
                        onOpenMessages = {
                          activeChatId = null
                          selectedTab = MainTab.CHAT
                        },
                        onOpenUserProfile = openProfile,
                      )
                    }
                    MainTab.SEARCH -> {
                      SearchExploreScreen(
                        viewModel = viewModel,
                        allPosts = allPosts,
                        publicUsers = publicUsers,
                        followingIds = followingIds,
                        onOpenUserProfile = openProfile,
                      )
                    }
                    MainTab.REELS -> {
                      ReelsScreen(
                        viewModel = viewModel,
                        allPosts = allPosts,
                        publicUsers = publicUsers,
                        followingIds = followingIds,
                        onOpenCreateReel = { createPostKind = Post.KIND_REEL },
                        onOpenUserProfile = openProfile,
                      )
                    }
                    MainTab.CHAT -> {
                      ChatsHubScreen(
                        viewModel = viewModel,
                        chats = chats,
                        publicUsers = publicUsers,
                        activeChatId = activeChatId,
                        onSelectChat = { activeChatId = it },
                        onBackFromChats = {
                          activeChatId = null
                          selectedTab = MainTab.HOME
                        },
                      )
                    }
                    MainTab.PROFILE -> {
                      ProfileScreen(
                        viewModel = viewModel,
                        viewedUserId = viewModel.currentUserId,
                        isDarkTheme = useDarkTheme,
                        followingIds = followingIds,
                        isAdmin = isAdmin,
                        onToggleTheme = { viewModel.toggleDarkTheme(useDarkTheme) },
                        onOpenAdminPanel = { subDestination = SubDestination.AdminPanel },
                        onOpenChat = openChat,
                        onBack = null,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      createPostKind?.let { initialKind ->
        CreatePostSheet(
          initialKind = initialKind,
          onDismiss = { createPostKind = null },
          onPublish = { kind, mediaType, mediaDataUrl, videoUri, caption, audioTitle ->
            viewModel.createPost(
              postKind = kind,
              mediaType = mediaType,
              mediaDataUrl = mediaDataUrl,
              videoUri = videoUri,
              caption = caption,
              audioTitle = audioTitle,
              onCreated = { createPostKind = null },
            )
          },
        )
      }

      if (showCreateStory) {
        CreateStorySheet(
          onDismiss = { showCreateStory = false },
          onPublish = { mediaDataUrl, caption, musicTitle, stickerLabel ->
            viewModel.createStory(
              mediaDataUrl = mediaDataUrl,
              caption = caption,
              musicTitle = musicTitle,
              stickerLabel = stickerLabel,
              onCreated = { showCreateStory = false },
            )
          },
        )
      }
    }
  }
}
