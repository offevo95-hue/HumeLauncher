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
    
    // Док с 5 слотами
    var dockApps by remember { mutableStateOf<MutableList<AppInfo?>>(mutableListOf(null, null, null, null, null)) }
    
    var currentTime by remember { mutableStateOf("") }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var isPickerOpen by remember { mutableStateOf(false) }
    var targetDockIndex by remember { mutableIntStateOf(-1) }
    
    // Переменные для отслеживания двойного нажатия (секретный жест)
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    var lockedPackages by remember { mutableStateOf(setOf<String>()) }
    
    var savedPin by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showEnterPinDialog by remember { mutableStateOf(false) }
    var showRemovePinDialog by remember { mutableStateOf(false) }
    var pendingAppAction by remember { mutableStateOf<AppInfo?>(null) }

    val iconCache = remember { mutableStateMapOf<String, ImageBitmap>() }

    BackHandler(enabled = true) {
        when {
            showSetPinDialog -> showSetPinDialog = false
            showEnterPinDialog -> showEnterPinDialog = false
            showRemovePinDialog -> showRemovePinDialog = false
            selectedAppForMenu != null -> selectedAppForMenu = null
            isPickerOpen -> isPickerOpen = false
            isDrawerOpen -> isDrawerOpen = false
            else -> {}
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
        if (dockApps.all { it == null } && allApps.isNotEmpty()) {
            val initial = allApps.take(3)
            val list = mutableListOf<AppInfo?>()
            list.addAll(initial)
            while (list.size < 5) list.add(null)
            dockApps = list
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
            // ВЕРХНЯЯ ПАНЕЛЬ (с секретным двойным кликом по зоне часов/погоды для открытия меню)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 32.dp).clickable {
                    val currentTimeMillis = System.currentTimeMillis()
                    if (currentTimeMillis - lastClickTime < 600) {
                        // Двойное нажатие за полсекунды — открываем скрытое меню всех приложений!
                        isDrawerOpen = true
                    }
                    lastClickTime = currentTimeMillis
                },
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

            // НИЖНИЙ ДОК (ЧИСТЫЙ, БЕЗ КНОПКИ ТРИ ТОЧКИ)
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
                        var isFocused by remember { mutableStateOf(false) }

                        if (app != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .focusable()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isFocused) Color(0xFF2A444C) else Color(0xFF162227))
                                    .border(
                                        width = if (isFocused) 3.dp else 1.dp,
                                        color = if (isFocused) Color.White else Color(0x33FFFFFF),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedAppForMenu = app },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val bmp = iconCache[app.packageName] ?: app.icon.toBitmap().asImageBitmap().also { iconCache[app.packageName] = it }
                                    Image(
                                        bitmap = bmp, 
                                        contentDescription = app.name, 
                                        modifier = Modifier.size(40.dp), 
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = app.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                                if (lockedPackages.contains(app.packageName)) {
                                    Text("🔒", fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .focusable()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isFocused) Color(0xFF2A444C) else Color(0x10FFFFFF))
                                    .border(
                                        width = if (isFocused) 3.dp else 1.dp,
                                        color = if (isFocused) Color.White else Color(0x33FFFFFF),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        targetDockIndex = index
                                        isPickerOpen = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = Color.White.copy(0.7f), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // СКРЫТЫЙ ЭКРАН ВСЕХ ПРИЛОЖЕНИЙ (ВЫЗЫВАЕТСЯ ДВОЙНЫМ КЛИКОМ)
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

        // ОКНО ВЫБОРА ПРИЛОЖЕНИЯ ДЛЯ ПЛЮСИКА (+)
        if (isPickerOpen) {
            Box(Modifier.fillMaxSize().background(Color(0xCC000000)).focusable(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(0.8f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF162B32))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                        .padding(32.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Выберите приложение в док", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("Закрыть ✕", color = Color.White, fontSize = 18.sp, modifier = Modifier.clickable { isPickerOpen = false })
                    }
                    Spacer(Modifier.height(24.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(allApps) { _, app ->
                            HumeAppCard(
                                app = app,
                                iconCache = iconCache,
                                onClick = {
                                    if (targetDockIndex in dockApps.indices) {
                                        val list = dockApps.toMutableList()
                                        list[targetDockIndex] = app
                                        dockApps = list
                                    }
                                    isPickerOpen = false
                                },
                                isLocked = lockedPackages.contains(app.packageName)
                            )
                        }
                    }
                }
            }
        }

        // КОНТЕКСТНОЕ МЕНЮ
        selectedAppForMenu?.let { app ->
            val isInDock = dockApps.contains(app)
            val appIndex = dockApps.indexOf(app)

            Box(
                Modifier.fillMaxSize()
                    .background(Color(0xCC000000))
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF162B32))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                        .focusable(),
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
                            val list = dockApps.toMutableList()
                            val idx = list.indexOf(app)
                            if (idx != -1) list[idx] = null
                            dockApps = list
                            selectedAppForMenu = null
                        }
                        Spacer(Modifier.height(8.dp))
                    } else {
                        MenuButton("Добавить в док") {
                            val emptyIdx = dockApps.indexOf(null)
                            if (emptyIdx != -1) {
                                val list = dockApps.toMutableList()
                                list[emptyIdx] = app
                                dockApps = list
                            } else {
                                Toast.makeText(context, "В доке нет свободных мест", Toast.LENGTH_SHORT).show()
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

        // Диалоги PIN
        if (showSetPinDialog) {
            Box(Modifier.fillMaxSize().background(Color(0xCC000000)).focusable(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.width(320.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF162B32)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)).padding(24.dp).focusable(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Придумайте свой PIN-код", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = pinInput, onValueChange = { pinInput = it }, label = { Text("Ваш PIN-код") }, singleLine = true, visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MenuButton("Сохранить", modifier = Modifier.weight(1f)) {
                            if (pinInput.isNotEmpty()) {
                                savedPin = pinInput
                                pendingAppAction?.let { lockedPackages = lockedPackages + it.packageName }
                                showSetPinDialog = false
                                pendingAppAction = null
                                Toast.makeText(context, "PIN установлен!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        MenuButton("Отмена", modifier = Modifier.weight(1f)) { showSetPinDialog = false; pendingAppAction = null }
                    }
                }
            }
        }

        if (showEnterPinDialog && pendingAppAction != null) {
            Box(Modifier.fillMaxSize().background(Color(0xCC000000)).focusable(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.width(320.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF162B32)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)).padding(24.dp).focusable(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Введите ваш PIN-код", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(pendingAppAction!!.name, color = Color.White.copy(0.7f), fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = pinInput, onValueChange = { pinInput = it }, label = { Text("PIN-код") }, singleLine = true, visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MenuButton("ОК", modifier = Modifier.weight(1f)) {
                            if (pinInput == savedPin) {
                                openApp(context, pendingAppAction!!)
                                showEnterPinDialog = false
                                pendingAppAction = null
                            } else {
                                Toast.makeText(context, "Неверный PIN!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        MenuButton("Отмена", modifier = Modifier.weight(1f)) { showEnterPinDialog = false; pendingAppAction = null }
                    }
                }
            }
        }

        if (showRemovePinDialog && pendingAppAction != null) {
            Box(Modifier.fillMaxSize().background(Color(0xCC000000)).focusable(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.width(320.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF162B32)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)).padding(24.dp).focusable(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Введите PIN для снятия блока", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = pinInput, onValueChange = { pinInput = it }, label = { Text("PIN-код") }, singleLine = true, visualTransformation = PasswordVisualTransformation()
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
                                Toast.makeText(context, "Неверный PIN!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        MenuButton("Отмена", modifier = Modifier.weight(1f)) { showRemovePinDialog = false; pendingAppAction = null }
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
    val bmp = iconCache[app.packageName] getDataOrFetch app.icon.toBitmap().asImageBitmap().also { iconCache[app.packageName] = it }

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
        context.startActivity(Intent(android.1provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${app.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (_: Exception) { Toast.makeText(context, "Ошибка настроек", Toast.CHART_SHORT).show() }
}

