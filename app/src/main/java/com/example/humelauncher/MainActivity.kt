package com.example.humelauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
        setContent { HumeHomeScreen() }
    }
}

@Composable
fun HumeHomeScreen() {
    val context = LocalContext.current
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var currentTime by remember { mutableStateOf("") }
    var isDrawerOpen by remember { mutableStateOf(false) }

    val iconCache = remember { mutableStateMapOf<String, ImageBitmap>() }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(30_000)
        }
    }

    LaunchedEffect(Unit) {
        val apps = withContext(Dispatchers.IO) { getAllApps(context) }
        allApps = apps.sortedBy { it.name }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0F0F12), Color(0xFF000000))))) {
        if (allApps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Загрузка...", color = Color.White, fontSize = 24.sp)
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Погода", color = Color.White.copy(0.8f), fontSize = 18.sp)
                Text(currentTime, color = Color.White.copy(0.8f), fontSize = 18.sp)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxSize().padding(top = 100.dp, bottom = 200.dp, start = 64.dp, end = 64.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                itemsIndexed(allApps.take(11)) { index, app ->
                    HumeAppCard(app = app, iconCache = iconCache, onClick = { openApp(context, app) })
                }
                if (allApps.size > 11) {
                    item { HumeMoreAppsCard { isDrawerOpen = true } }
                }
            }

            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).fillMaxWidth(0.6f).height(100.dp).background(Color(0x33FFFFFF), RoundedCornerShape(24.dp)).blur(10.dp), contentAlignment = Alignment.Center) {
                Text("Liquid Glass Dock", color = Color.White.copy(0.5f), fontSize = 16.sp)
            }
        }

        if (isDrawerOpen) {
            Box(Modifier.fillMaxSize().background(Color(0xFA0A0A0A)).padding(48.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Все приложения", color = Color.White, fontSize = 32.sp)
                        Text("Закрыть", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(8.dp).clickable { isDrawerOpen = false })
                    }
                    LazyVerticalGrid(columns = GridCells.Adaptive(160.dp)) {
                        itemsIndexed(allApps) { _, app ->
                            HumeAppCard(app = app, iconCache = iconCache, onClick = { openApp(context, app) })
                        }
                    }
                }
            }
        }
    }
}

fun getAllApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val map = mutableMapOf<String, AppInfo>()
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    pm.queryIntentActivities(intent, 0).forEach {
        map[it.activityInfo.packageName] = AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.activityInfo.name, it.loadIcon(pm))
    }
    return map.values.toList()
}

fun openApp(context: Context, app: AppInfo) {
    try {
        context.startActivity(Intent().apply { setClassName(app.packageName, app.activityName); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    } catch (_: Exception) { Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show() }
}
