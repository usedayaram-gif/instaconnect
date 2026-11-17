package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.SocialRepository
import com.example.ui.InstaConnectViewModel
import com.example.ui.UserProfileViewModel

@Composable
fun ProfileScreen(
  viewModel: InstaConnectViewModel,
  viewedUserId: String,
  isDarkTheme: Boolean,
  followingIds: Set<String>,
  isAdmin: Boolean,
  onToggleTheme: () -> Unit,
  onOpenAdminPanel: () -> Unit,
  onOpenChat: (String) -> Unit,
  onBack: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val appContext = LocalContext.current.applicationContext
  val userProfileViewModel: UserProfileViewModel =
    viewModel(
      key = "${viewModel.currentUserId}_profile_$viewedUserId",
      factory =
        viewModelFactory {
          initializer {
            UserProfileViewModel(
              repository = SocialRepository(appContext),
              currentUserId = viewModel.currentUserId,
              viewedUserId = viewedUserId,
            )
          }
        },
    )

  UserProfileView(
    viewModel = userProfileViewModel,
    isDarkTheme = isDarkTheme,
    isAdmin = isAdmin,
    onToggleTheme = onToggleTheme,
    onOpenAdminPanel = onOpenAdminPanel,
    onOpenChat = { peer ->
      viewModel.openOrCreateChat(peer) { chatId -> onOpenChat(chatId) }
    },
    onBack = onBack,
    onStatusMessage = { msg -> viewModel.postStatusBanner(msg) },
    modifier = modifier,
  )
}
