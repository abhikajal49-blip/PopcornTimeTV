package com.popcorntime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.popcorntime.model.Torrent
import com.popcorntime.model.TorrentHealth

@Composable
fun TorrentHealthBadge(
    torrent: Torrent,
    modifier: Modifier = Modifier
) {
    val healthColor = Color(torrent.health.colorHex)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .testTag("torrent_health_badge")
            .background(Color(0xFF222634), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(healthColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${torrent.quality} (${torrent.health.label})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "• ${torrent.seeds}s / ${torrent.peers}p",
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF)
        )
        if (!torrent.size.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "• ${torrent.size}",
                fontSize = 11.sp,
                color = Color(0xFF00B4D8)
            )
        }
    }
}
