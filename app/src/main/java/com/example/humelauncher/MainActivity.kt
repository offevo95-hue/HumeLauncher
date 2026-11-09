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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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

    // Хранилище иконок в памяти
    val iconCache = remember { mutableStateMapOf<String, ImageBitmap>() }

    // Обновление времени
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(30_000)
        }
    }

    // Загрузка приложений
    LaunchedEffect(Unit) {
        val apps = withContext(Dispatchers.IO) { getAllApps(context) }
        allApps = apps.sortedBy { it.name }
    }

    // Обои (Градиент как на Apple TV)
    Box(Modifier.fillMaxSize().background(
        Brush.linearGradient(
            colors = listOf(Color(0xFF103A4B), Color(0xFF0F2027), Color(0xFF000000))
        )
    )) {
        if (allApps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Загрузка...", color = Color.White, fontSize = 24.sp)
            }
        } else {
            // ВЕРХНЯЯ ПАНЕЛЬ (Статус-бар)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Капсула с погодой (Стекло)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("☁️", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("8° London", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Cloudy", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                }

                // Время и иконки
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currentTime, color = Color.White.copy(0.8f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(16.dp))
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                        Text("🔍", fontSize = 18.sp)
                    }
                }
            }

            // СЕТКА ПРИЛОЖЕНИЙ
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.fillMaxSize().padding(top = 100.dp, bottom = 220.dp, start = 64.dp, end = 64.dp),
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

            // НИЖНИЙ ДОК (Liquid Glass)
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .fillMaxWidth(0.85f) // Широкий док
                    .height(130.dp)
                    // Эффект размытого стекла
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0x22FFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Внутри дока выводим избранные приложения (первые 6)
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    allApps.take(5).forEach { app ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(0.8f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .clickable { openApp(context, app) },
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = iconCache[app.packageName] ?: app.icon.toBitmap().asImageBitmap().also { iconCache[app.packageName] = it }
                            Image(bitmap = bmp, contentDescription = app.name, modifier = Modifier.size(48.dp))
                        }
                    }
                    // Плюс кнопка "Ещё"
                     Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(0.8f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x33FFFFFF))
                            .clickable { isDrawerOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("•••", color = Color.White, fontSize = 24.sp)
                    }
                }
            }
        }

        // Экран всех приложений
        if (isDrawerOpen) {
            Box(Modifier.fillMaxSize().background(Color(0xFA0A0A0A)).padding(48.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Все приложения", color = Color.White, fontSize = 32.sp)
                        Text("Закрыть", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable { isDrawerOpen = false })
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
