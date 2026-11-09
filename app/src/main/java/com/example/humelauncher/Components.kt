package com.example.humelauncher

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AppInfo(val name: String, val packageName: String, val activityName: String, val icon: Drawable)

@Composable
fun HumeAppCard(app: AppInfo, iconCache: MutableMap<String, ImageBitmap>, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val bmp = iconCache[app.packageName] ?: app.icon.toBitmap().asImageBitmap().also { iconCache[app.packageName] = it }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .background(if (isFocused) Color(0x44FFFFFF) else Color.Transparent, RoundedCornerShape(16.dp))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(12.dp)
    ) {
        Image(bitmap = bmp, contentDescription = app.name, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.height(8.dp))
        Text(app.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

fun Drawable.toBitmap(): android.graphics.Bitmap {
    if (this is android.graphics.drawable.BitmapDrawable) return this.bitmap
    val w = if (intrinsicWidth > 0) intrinsicWidth else 100
    val h = if (intrinsicHeight > 0) intrinsicHeight else 100
    val b = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(b)
    setBounds(0, 0, c.width, c.height)
    draw(c)
    return b
}

