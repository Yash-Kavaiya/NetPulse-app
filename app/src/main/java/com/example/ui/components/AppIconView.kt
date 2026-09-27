package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.stats.NetworkStatsHelper
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleYellow
import com.example.ui.theme.LocalGoogleColors

/**
 * Renders an app's Drawable icon or a fallback vector for special system UIDs
 */
@Composable
fun AppIconView(
    icon: Drawable?,
    uid: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val googleColors = LocalGoogleColors.current

    if (icon != null) {
        val bitmap = remember(icon) {
            try {
                if (icon is BitmapDrawable && icon.bitmap != null) {
                    icon.bitmap
                } else {
                    val w = if (icon.intrinsicWidth > 0) icon.intrinsicWidth else 96
                    val h = if (icon.intrinsicHeight > 0) icon.intrinsicHeight else 96
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    icon.setBounds(0, 0, canvas.width, canvas.height)
                    icon.draw(canvas)
                    bmp
                }
            } catch (e: Exception) {
                null
            }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "App icon",
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
            )
            return
        }
    }

    // Special UID fallback icons
    val (bgColor, iconVector) = when (uid) {
        NetworkStatsHelper.UID_REMOVED -> Pair(googleColors.errorBg, Icons.Default.DeleteOutline)
        NetworkStatsHelper.UID_TETHERING -> Pair(googleColors.warningBg, Icons.Default.WifiTethering)
        NetworkStatsHelper.SYSTEM_UID -> Pair(googleColors.infoBg, Icons.Default.Android)
        0 -> Pair(googleColors.infoBg, Icons.Default.PermDeviceInformation)
        else -> Pair(googleColors.infoBg, Icons.Default.Devices)
    }

    val tintColor = when (uid) {
        NetworkStatsHelper.UID_REMOVED -> GoogleRed
        NetworkStatsHelper.UID_TETHERING -> GoogleYellow
        NetworkStatsHelper.SYSTEM_UID -> GoogleGreen
        else -> GoogleBlue
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = "System icon",
            tint = tintColor,
            modifier = Modifier.size(size * 0.58f)
        )
    }
}
