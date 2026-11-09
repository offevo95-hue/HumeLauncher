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
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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

    // Глубокий фон с радиальным свечением
    Box(Modifier.fillMaxSize().background(
        Brush.radialGradient(
            colors = listOf(Color(0xFF144D59), Color(0xFF071B22), Color(0xFF000000)),
            center = androidx.compose.ui.geometry.Offset(1000f, 1000f),
            radius = 2500f
        )
    )) {
        if (allApps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Загрузка приложений...", color = Color.White, fontSize = 24.sp)
            }
        } else {
            // ВЕРХНЯЯ ПАНЕЛЬ
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Погода
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("☁️", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("8° London", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Cloudy", color = Color.White.copy(0.6f), fontSize = 11.sp)
                    }
                }

                // Время и кнопки
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(currentTime, color = Color.White.copy(0.9f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0x22FFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                        Text("⚙️", fontSize = 14.sp)
                    }
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0x22FFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                        Text("🔍", fontSize = 14.sp)
                    }
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0x22FFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                        Text("👤", fontSize = 14.sp)
                    }
                }
            }

            // ЦЕНТР ПУСТОЙ (как на референсе)

            // НИЖНИЙ ДОК (КАК НА СКРИНШОТЕ APPLE TV)
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .fillMaxWidth(0.95f)
                    .height(150.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0x11FFFFFF))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Первые 5 приложений в виде плиток
                    allApps.take(5).forEachIndexed { index, app ->
                        val colors = listOf(
                            Color(0xFF6A4CFF), // Фиолетовый
                            Color(0xFFD4C585), // Золотистый
                            Color(0xFF2B50FF), // Синий
                            Color(0xFFBCE81A), // Лаймовый
                            Color(0xFFF2B705)  // Желтый
                        )
                        val tileColor = colors[index % colors.size]
                        var isFocused by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onFocusChanged { isFocused = it.isFocused }
                                .focusable()
                                .clip(RoundedCornerShape(20.dp))
                                .background(tileColor)
                                .border(
                                    width = if (isFocused) 3.dp else 0.dp,
                                    color = if (isFocused) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { openApp(context, app) },
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = iconCache[app.packageName] ?: app.icon.toBitmap().asImageBitmap().also { iconCache[app.packageName] = it }
                            Image(
                                bitmap = bmp, 
                                contentDescription = app.name, 
                                modifier = Modifier.fillMaxSize(0.6f), 
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    
                    // Кнопка "Ещё" (•••)
                    var isMoreFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .onFocusChanged { isMoreFocused = it.isFocused }
                            .focusable()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF2B705))
                            .border(
                                width = if (isMoreFocused) 3.dp else 0.dp,
                                color = if (isMoreFocused) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { isDrawerOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("•••", color = Color.Black, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Стрелочка вверх по центру над доком
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-20).dp)
                        .clickable { isDrawerOpen = true }
                ) {
                    Text("^", color = Color.White.copy(0.7f), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ЭКРАН ВСЕХ ПРИЛОЖЕНИЙ (ВЫДВИЖНОЙ СПИСОК)
        if (isDrawerOpen) {
            Box(Modifier.fillMaxSize().background(Color(0xFA0A0A0A)).padding(48.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Все приложения", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text("Закрыть ✕", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable { isDrawerOpen = false })
                    }
                    Spacer(Modifier.height(24.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
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
    
    // Ищем обычные лаунчер-приложения и Leanback (для ТВ)
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    pm.queryIntentActivities(intent, 0).forEach {
        map[it.activityInfo.packageName] = AppInfo(
            it.loadLabel(pm).toString(), 
            it.activityInfo.packageName, 
            it.activityInfo.name, 
            it.loadIcon(pm)
        )
    }
    
    val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    pm.queryIntentActivities(leanbackIntent, 0).forEach {
        map[it.activityInfo.packageName] = AppInfo(
            it.loadLabel(pm).toString(), 
            it.activityInfo.packageName, 
            it.activityInfo.name, 
            it.loadIcon(pm)
        )
    }
    
    return map.values.toList()
}

fun openApp(context: Context, app: AppInfo) {
    try {
        context.startActivity(Intent().apply { 
            setClassName(app.packageName, app.activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) 
        })
    } catch (_: Exception) { 
        Toast.makeText(context, "Ошибка запуска", Toast.LENGTH_SHORT).show() 
    }
}

