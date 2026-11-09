package com.example.humelauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    var dockApps by remember { mutableStateOf<MutableList<AppInfo>>(mutableListOf()) }
    var currentTime by remember { mutableStateOf("") }
    var isDrawerOpen by remember { mutableStateOf(false) }
    
    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    var lockedPackages by remember { mutableStateOf(setOf<String>()) }
    
    var savedPin by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showEnterPinDialog by remember { mutableStateOf(false) }
    var showRemovePinDialog by remember { mutableStateOf(false) }
    var pendingAppAction by remember { mutableStateOf<AppInfo?>(null) }

    val iconCache = remember { mutableStateMapOf<String, ImageBitmap>() }

    // Умная обработка кнопки "Назад" с пульта
    BackHandler(enabled = true) {
        when {
            showSetPinDialog -> showSetPinDialog = false
            showEnterPinDialog -> showEnterPinDialog = false
            showRemovePinDialog -> showRemovePinDialog = false
            selectedAppForMenu != null -> selectedAppForMenu = null
            isDrawerOpen -> isDrawerOpen = false
            else -> {
                // На главном экране нажатие назад ничего не делает, чтобы лаунчер не закрывался случайно
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(30_000)
        }
    }

    LaunchedEffect(Unit) {
        val apps = withContext(Dispatchers.IO) { getAllApps(context) }
        allApps = apps.sortedBy { it.name }
        if (dockApps.isEmpty() && allApps.isNotEmpty()) {
            dockApps = allApps.take(5).toMutableList()
        }
    }

    val handleAppClick = { app: AppInfo ->
        if (lockedPackages.contains(app.packageName)) {
            pendingAppAction = app
            pinInput = ""
            showEnterPinDialog = true
        } else {
            openApp(context, app)
        }
    }

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
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        Text("8° Madrid", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Cloudy", color = Color.White.copy(0.6f), fontSize = 11.sp)
                    }
                }

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

            Box(
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .fillMaxWidth(0.92f)
                    .height(130.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0x15FFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dockApps.forEachIndexed { index, app ->
                        val colors = listOf(
                            Color(0xFF6A4CFF), Color(0xFFD4C585), Color(0xFF2B50FF), Color(0xFFBCE81A), Color(0xFFF2B705)
                        )
                        val tileColor = colors[index % colors.size]
                        var isFocused by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .weight(1.3f)
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
                                .clickable { selectedAppForMenu = app },
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = iconCache[app.packageName] ?: app.icon.toBitmap().asImageBitmap().also { iconCache[app.packageName] = it }
                            Image(
                                bitmap = bmp, 
                                contentDescription = app.name, 
                                modifier = Modifier.size(48.dp), 
                                contentScale = ContentScale.Fit
                            )
                            if (lockedPackages.contains(app.packageName)) {
                                Text("🔒", fontSize = 14.sp, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                            }
                        }
                    }
                    
                    var isMoreFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
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
                        Text("•••", color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-18).dp)
                        .clickable { isDrawerOpen = true }
                ) {
                    Text("^", color = Color.White.copy(0.7f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

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
                            HumeAppCard(
                                app = app, 
                                iconCache = iconCache, 
                                onClick = { selectedAppForMenu = app },
                                isLocked = lockedPackages.contains(app.packageName)
                            )
                        }
                    }
                }
            }
        }

        selectedAppForMenu?.let { app ->
            val isInDock = dockApps.contains(app)
            val appIndex = dockApps.indexOf(app)

            Box(
                Modifier.fillMaxSize().background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF162B32))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(app.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))

                    val isLocked = lockedPackages.contains(app.packageName)

                    MenuButton("Запустить приложение") {
                        selectedAppForMenu = null
                        handleAppClick(app)
                    }
                    Spacer(Modifier.height(8.dp))

                    if (isInDock) {
                        if (appIndex > 0) {
                            MenuButton("← Сдвинуть влево") {
                                val list = dockApps.toMutableList()
                                val item = list.removeAt(appIndex)
                                list.add(appIndex - 1, item)
                                dockApps = list
                                selectedAppForMenu = null
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        if (appIndex < dockApps.size - 1) {
                            MenuButton("Сдвинуть вправо →") {
                                val list = dockApps.toMutableList()
                                val item = list.removeAt(appIndex)
                                list.add(appIndex + 1, item)
                                dockApps = list
                                selectedAppForMenu = null
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        MenuButton("Убрать из дока", isRed = true) {
                            if (dockApps.size > 1) {
                                dockApps = dockApps.filter { it.packageName != app.packageName }.toMutableList()
                            } else {
                                Toast.makeText(context, "В доке должен остаться хотя бы 1 элемент", Toast.LENGTH_SHORT).show()
                            }
                            selectedAppForMenu = null
                        }
                        Spacer(Modifier.height(8.dp))
                    } else {
                        MenuButton("Добавить в док") {
                            if (dockApps.size < 5) {
                                dockApps = (dockApps + app).toMutableList()
                            } else {
                                Toast.makeText(context, "В доке максимум 5 приложений", Toast.LENGTH_SHORT).show()
                            }
                            selectedAppForMenu = null
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    MenuButton(if (isLocked) "Снять PIN-код" else "Заблокировать PIN") {
                        val targetApp = app
                        selectedAppForMenu = null
                        if (isLocked) {
                            pendingAppAction = targetApp
                            pinInput = ""
                            showRemovePinDialog = true
                        } else {
                            if (savedPin.isEmpty()) {
                                pendingAppAction = targetApp
                                pinInput = ""
                                showSetPinDialog = true
                            } else {
                                lockedPackages = lockedPackages + targetApp.packageName
                                Toast.makeText(context, "Приложение заблокировано", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    MenuButton("О приложении") {
                        openAppInfo(context, app)
                        selectedAppForMenu = null
                    }
                    Spacer(Modifier.height(8.dp))
                    MenuButton("Отмена") {
                        selectedAppForMenu = null
                    }
                }
            }
        }

        if (showSetPinDialog) {
            Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF162B32))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Придумайте свой PIN-код", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    
                    androidx.compose.material3.OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("Ваш PIN-код") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MenuButton("Сохранить", modifier = Modifier.weight(1f)) {
                            if (pinInput.isNotEmpty()) {
                                savedPin = pinInput
                                pendingAppAction?.let { lockedPackages = lockedPackages + it.packageName }
                                showSetPinDialog = false
                                pendingAppAction = null
                                Toast.makeText(context, "PIN успешно установлен!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Введите непустой PIN", Toast.LENGTH_SHORT).show()
                            }
                        }
                        MenuButton("Отмена", modifier = Modifier.weight(1f)) {
                            showSetPinDialog = false
                            pendingAppAction = null
                        }
                    }
                }
            }
        }

        if (showEnterPinDialog && pendingAppAction != null) {
            Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF162B32))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Введите ваш PIN-код", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(pendingAppAction!!.name, color = Color.White.copy(0.7f), fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    
                    androidx.compose.material3.OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("PIN-код") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MenuButton("ОК", modifier = Modifier.weight(1f)) {
                            if (pinInput == savedPin) {
                                openApp(context, pendingAppAction!!)
                                showEnterPinDialog = false
                                pendingAppAction = null
                            } else {
                                Toast.makeText(context, "Неверный PIN-код!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        MenuButton("Отмена", modifier = Modifier.weight(1f)) {
                            showEnterPinDialog = false
                            pendingAppAction = null
                        }
                    }
                }
            }
        }

        if (showRemovePinDialog && pendingAppAction != null) {
            Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF162B32))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Введите PIN для снятия блока", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    
                    androidx.compose.material3.OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("PIN-код") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MenuButton("Снять", modifier = Modifier.weight(1f)) {
                            if (pinInput == savedPin) {
                                lockedPackages = lockedPackages - pendingAppAction!!.packageName
                                showRemovePinDialog = false
                                pendingAppAction = null
                                Toast.makeText(context, "Блокировка снята", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Неверный PIN-код!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        MenuButton("Отмена", modifier = Modifier.weight(1f)) {
                            showRemovePinDialog = false
                            pendingAppAction = null
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuButton(text: String, isRed: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isRed) Color(0x44FF0000) else Color(0x22FFFFFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isRed) Color(0xFFFF6B6B) else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun HumeAppCard(app: AppInfo, iconCache: MutableMap<String, ImageBitmap>, onClick: () -> Unit, isLocked: Boolean) {
    var isFocused by remember { mutableStateOf(false) }
    val bmp = iconCache[app.packageName] ?: app.icon.toBitmap().asImageBitmap().also { iconCache[app.packageName] = it }

    Box(
        modifier = Modifier
            .size(90.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFocused) Color(0x44FFFFFF) else Color.Transparent)
            .border(width = if (isFocused) 2.dp else 0.dp, color = Color.White, shape = RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(bitmap = bmp, contentDescription = app.name, modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.height(4.dp))
            Text(app.name, color = Color.White, fontSize = 11.sp, maxLines = 1)
        }
        if (isLocked) {
            Text("🔒", fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
    }
}

fun getAllApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val map = mutableMapOf<String, AppInfo>()
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    pm.queryIntentActivities(intent, 0).forEach {
        map[it.activityInfo.packageName] = AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.activityInfo.name, it.loadIcon(pm))
    }
    val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    pm.queryIntentActivities(leanbackIntent, 0).forEach {
        map[it.activityInfo.packageName] = AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.activityInfo.name, it.loadIcon(pm))
    }
    return map.values.toList()
}

fun openApp(context: Context, app: AppInfo) {
    try {
        context.startActivity(Intent().apply { setClassName(app.packageName, app.activityName); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    } catch (_: Exception) { Toast.makeText(context, "Не удалось запустить", Toast.LENGTH_SHORT).show() }
}

fun openAppInfo(context: Context, app: AppInfo) {
    try {
        context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${app.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (_: Exception) { Toast.makeText(context, "Ошибка настроек", Toast.LENGTH_SHORT).show() }
}

