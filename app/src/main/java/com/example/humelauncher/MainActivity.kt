package com.example.humelauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap // <-- ДОБАВЛЕН ИМПОРТ
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    Box(Modifier.fillMaxSize().background(
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F2027),
                Color(0xFF203A43),
                Color(0xFF2C5364)
            )
        )
    )) {
        if (allApps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Загрузка...", color = Color.White, fontSize = 24.sp)
            }
        } else {
            // Верхняя панель
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Погода
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x33FFFFFF))
                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("☁️", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("8° London", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Cloudy", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                }

                // Время и кнопки
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(currentTime, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x44FFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                        Text("⚙️", fontSize = 18.sp)
                    }
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x44FFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                        Text("🔍", fontSize = 18.sp)
                    }
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x44FFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                        Text("👤", fontSize = 18.sp)
                    }
                }
            }

            // Сетка приложений
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxSize().padding(top = 80.dp, bottom = 220.dp, start = 64.dp, end = 64.dp),
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

            // Док (Liquid Glass)
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .fillMaxWidth(0.85f)
                    .height(140.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0x22FFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    allApps.take(5).forEach { app ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(0.75f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .clickable { openApp(context, app) },
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = iconCache[app.packageName] ?: app.icon.toBitmap().asImageBitmap().also { iconCache[app.packageName] = it }
                            Image(bitmap = bmp, contentDescription = app.name, modifier = Modifier.size(64.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(0.75f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x33FFFFFF))
                            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(16.dp))
                            .clickable { isDrawerOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("•••", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Экран всех приложений
        if (isDrawerOpen) {
            Box(Modifier.fillMaxSize().background(Color(0xFA0A0A0A)).padding(48.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Все приложения", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text("Закрыть", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable { isDrawerOpen = false })
                    }
                    Spacer(Modifier.height(24.dp))
                    LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
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
    } catch (_: Exception) { Toast.makeText(context, "Ошибка запуска", Toast.LENGTH_SHORT).show() }
}
