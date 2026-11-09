package com.example.humelauncher

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.drawable.Drawable

data class AppInfo(val name: String, val packageName: String, val activityName: String, val icon: Drawable)

@Composable
fun HumeAppCard(app: AppInfo, iconCache: MutableMap<String, ImageBitmap>, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "")
    val bmp = iconCache[app.packageName] ?: app.icon.toBitmap().asImageBitmap().also { iconCache[app.packageName] = it }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale).onFocusChanged { isFocused = it.isFocused }
            .background(if (isFocused) Color(0x22FFFFFF) else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(8.dp)
    ) {
        Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(90.dp))
        Spacer(Modifier.height(8.dp))
        Text(app.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun HumeMoreAppsCard(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(Modifier.size(90.dp).background(Color(0x33FFFFFF), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Text("•••", color = Color.White, fontSize = 24.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text("Ещё", color = Color.White, fontSize = 14.sp)
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
