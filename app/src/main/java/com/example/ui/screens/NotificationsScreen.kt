package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.model.AppNotification
import com.example.ui.InstaConnectViewModel
import com.example.ui.components.AvatarWithStoryRing
import com.example.ui.theme.BrandMagenta
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.ButtonGradient
import com.example.ui.theme.PureWhite
import com.example.ui.theme.VerifiedBlue

@Composable
fun NotificationsScreen(
  viewModel: InstaConnectViewModel,
  notifications: List<AppNotification>,
  onBack: () -> Unit,
  onOpenUserProfile: (String) -> Unit,
  onOpenChat: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onBack() }

  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
      Text(
        text = "Notifications",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

    if (notifications.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(ButtonGradient),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Outlined.NotificationsNone,
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(34.dp),
            )
          }
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "All Caught Up",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "New followers, likes, comments, and direct messages appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(notifications, key = { it.id }) { notif ->
          val icon =
            when (notif.type) {
              AppNotification.TYPE_FOLLOW -> Icons.Filled.PersonAdd
              AppNotification.TYPE_COMMENT -> Icons.Filled.ChatBubble
              AppNotification.TYPE_MESSAGE -> Icons.Filled.Mail
              else -> Icons.Filled.Favorite
            }
          val iconTint =
            when (notif.type) {
              AppNotification.TYPE_FOLLOW -> VerifiedBlue
              AppNotification.TYPE_COMMENT -> BrandOrange
              AppNotification.TYPE_MESSAGE -> MaterialTheme.colorScheme.onSurface
              else -> BrandMagenta
            }

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .background(
                  if (!notif.isRead) BrandMagenta.copy(alpha = 0.06f)
                  else MaterialTheme.colorScheme.background
                )
                .clickable {
                  if (!notif.isRead) viewModel.markNotificationRead(notif.id)
                  when (notif.type) {
                    AppNotification.TYPE_FOLLOW -> onOpenUserProfile(notif.actorId)
                    AppNotification.TYPE_MESSAGE -> onOpenChat(notif.targetId)
                    else -> onOpenUserProfile(notif.actorId)
                  }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            AvatarWithStoryRing(
              photoDataUrl = notif.actorPhotoDataUrl,
              username = notif.actorUsername,
              size = 44.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text =
                  buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                      append("@${notif.actorUsername} ")
                    }
                    append(notif.previewText)
                  },
                style = MaterialTheme.typography.bodyMedium,
              )
            }
            Icon(
              imageVector = icon,
              contentDescription = notif.type,
              tint = iconTint,
              modifier = Modifier.size(20.dp),
            )
            if (!notif.isRead) {
              Box(
                modifier = Modifier.size(8.dp).clip(CircleShape).background(BrandMagenta)
              )
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        }
      }
    }
  }
}
