package com.example.humelauncher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.onKeyEvent
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
    val prefs: SharedPreferences = remember { context.getSharedPreferences("HumeLauncherPrefs", Context.MODE_PRIVATE) }

    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var dockApps by remember { mutableStateOf<MutableList<AppInfo?>>(mutableListOf(null, null, null, null, null)) }
    
    var currentTime by remember { mutableStateOf("") }
    var isDrawerOpen by remember { mutableStateOf(false) }     
    var isPickerOpen by remember { mutableStateOf(false) }     
    var targetDockIndex by remember { mutableIntStateOf(-1) }
    var isEditMode by remember { mutableStateOf(false) }       
    
    var rightPressCount by remember { mutableIntStateOf(0) }
    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    
    var lockedPackages by remember { mutableStateOf(prefs.getStringSet("locked_packages", emptySet()) ?: emptySet()) }
    var savedPin by remember { mutableStateOf(prefs.getString("saved_pin", "") ?: "") }
    var pinInput by remember { mutableStateOf("") }
    
    // Типы PIN-диалогов: NONE, SET, ENTER, REMOVE
    var pinDialogType by remember { mutableStateOf<PinDialogType>(PinDialogType.NONE) }
    var pendingAppAction by remember { mutableStateOf<AppInfo?>(null) }

    val iconCache = remember { mutableStateMapOf<String, ImageBitmap>() }

    val saveDockToPrefs = { currentDock: List<AppInfo?> ->
        val editor = prefs.edit()
        currentDock.forEachIndexed { idx, app ->
            if (app != null) {
                editor.putString("dock_pkg_$idx", app.packageName)
                editor.putString("dock_cls_$idx", app.activityName)
                editor.putString("dock_name_$idx", app.name)
            } else {
                editor.remove("dock_pkg_$idx")
                editor.remove("dock_cls_$idx")
                editor.remove("dock_name_$idx")
            }
        }
        editor.apply()
    }

    val saveLockedPackages = { locked: Set<String> ->
        prefs.edit().putStringSet("locked_packages", locked).apply()
        lockedPackages = locked
    }

    val savePinCode = { pin: String ->
        prefs.edit().putString("saved_pin", pin).apply()
        savedPin = pin
    }

    BackHandler(enabled = true) {
        when {
            pinDialogType != PinDialogType.NONE -> pinDialogType = PinDialogType.NONE
            selectedAppForMenu != null -> selectedAppForMenu = null
            isPickerOpen -> isPickerOpen = false
            isDrawerOpen -> isDrawerOpen = false
            isEditMode -> isEditMode = false
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
        
        val restoredDock = mutableListOf<AppInfo?>(null, null, null, null, null)
        var hasRestored = false
        
        for (i in 0..4) {
            val pkg = prefs.getString("dock_pkg_$i", null)
            val cls = prefs.getString("dock_cls_$i", null)
            val name = prefs.getString("dock_name_$i", null)
            if (pkg != null && cls != null && name != null) {
                val foundApp = apps.find { it.packageName == pkg }
                if (foundApp != null) {
                    restoredDock[i] = foundApp
                    hasRestored = true
                }
            }
        }

        if (hasRestored) {
            dockApps = restoredDock
        } else if (allApps.isNotEmpty()) {
            val initial = allApps.take(3)
            val list = mutableListOf<AppInfo?>()
            list.addAll(initial)
            while (list.size < 5) list.add(null)
            dockApps = list
            saveDockToPrefs(dockApps)
        }
    }

    val handleAppClick = { app: AppInfo ->
        if (lockedPackages.contains(app.packageName)) {
            pendingAppAction = app
            pinInput = ""
            pinDialogType = PinDialogType.ENTER
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
            // ВЕРХНЯЯ ПАНЕЛЬ
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
                    
                    repeat(3) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0x22FFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                            Text(when(it) { 0 -> "⚙️"; 1 -> "🔍"; else -> "👤" }, fontSize = 14.sp)
                        }
                    }
                }
            }

            // НИЖНИЙ ДОК
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .fillMaxWidth(0.92f)
                    .height(130.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(if (isEditMode) Color(0x35FF9800) else Color(0x15FFFFFF))
                    .border(width = if (isEditMode) 2.dp else 1.dp, color = if (isEditMode) Color(0xFFFF9800) else Color(0x33FFFFFF), shape = RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dockApps.forEachIndexed { index, app ->
                        var isFocused by remember { mutableStateOf(false) }
                        val isLastElement = (index == dockApps.size - 1)

                        val keyEventListener: (KeyEvent) -> Boolean = { keyEvent ->
                            if (!isEditMode && isLastElement && keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                rightPressCount++
                                if (rightPressCount >= 2) {
                                    isEditMode = true
                                    Toast.makeText(context, "Админ-режим активирован!", Toast.LENGTH_SHORT).show()
                                }
                                true
                            } else {
                                false
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onFocusChanged { 
                                    isFocused = it.isFocused
                                    if (!isFocused && isLastElement) rightPressCount = 0
                                }
                                .focusable()
                                .onKeyEvent(keyEventListener)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isFocused) Color(0xFF2A444C) else if (app != null) Color(0xFF162227) else Color(0x10FFFFFF))
                                .border(width = if (isFocused) 3.dp else 1.dp, color = if (isFocused) Color.White else Color(0x33FFFFFF), shape = RoundedCornerShape(20.dp))
                                .clickable {
                                    if (app != null) {
                                        if (isEditMode) selectedAppForMenu = app else handleAppClick(app)
                                    } else {
                                        targetDockIndex = index
                                        isPickerOpen = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (app != null) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val bmp = iconCache[app.packageName] ?: app.icon.toBitmap().asImageBitmap().also { iconCache[app.packageName] = it }
                                    Image(bitmap = bmp, contentDescription = app.name, modifier = Modifier.size(40.dp), contentScale = ContentScale.Fit)
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = app.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                }
                                if (lockedPackages.contains(app.packageName)) {
                                    Text("🔒", fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
                                }
                            } else {
                                Text("+", color = Color.White.copy(0.7f), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    // КНОПКА ••• В АДМИН-РЕЖИМЕ
                    if (isEditMode) {
                        var isMoreFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxHeight()
                                .onFocusChanged { isMoreFocused = it.isFocused }
                                .focusable()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isMoreFocused) Color(0xFF333333) else Color(0xFF1C282D))
                                .border(width = if (isMoreFocused) 3.dp else 1.dp, color = if (isMoreFocused) Color.White else Color(0x33FFFFFF), shape = RoundedCornerShape(20.dp))
                                .clickable { isDrawerOpen = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("•••", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ОКНО ВСЕХ ПРИЛОЖЕНИЙ
        if (isDrawerOpen) {
            AppGridModal(title = "Все приложения", apps = allApps, iconCache = iconCache, lockedPackages = lockedPackages, onClose = { isDrawerOpen = false }) { app ->
                selectedAppForMenu = app
            }
        }

        // ОКНО ВЫБОРА В ДОК
        if (isPickerOpen) {
            AppGridModal(title = "Выберите приложение в док", apps = allApps, iconCache = iconCache, lockedPackages = lockedPackages, isPicker = true, onClose = { isPickerOpen = false }) { app ->
                if (targetDockIndex in dockApps.indices) {
                    val list = dockApps.toMutableList()
                    list[targetDockIndex] = app
                    dockApps = list
                    saveDockToPrefs(dockApps)
                }
                isPickerOpen = false
            }
        }

        // КОНТЕКСТНОЕ МЕНЮ ЭЛЕМЕНТА ДОКА
        selectedAppForMenu?.let { app ->
            val isInDock = dockApps.contains(app)
            val appIndex = dockApps.indexOf(app)
            val isLocked = lockedPackages.contains(app.packageName)

            ModalContainer(onDismiss = { selectedAppForMenu = null }, width = 340.dp) {
                Text(app.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

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
                            saveDockToPrefs(dockApps)
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
                            saveDockToPrefs(dockApps)
                            selectedAppForMenu = null
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    MenuButton("Убрать из дока", isRed = true) {
                        val list = dockApps.toMutableList()
                        val idx = list.indexOf(app)
                        if (idx != -1) list[idx] = null
                        dockApps = list
                        saveDockToPrefs(dockApps)
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
                            saveDockToPrefs(dockApps)
                        } else {
                            Toast.makeText(context, "В доке нет свободных мест", Toast.LENGTH_SHORT).show()
                        }
                        selectedAppForMenu = null
                    }
                    Spacer(Modifier.height(8.dp))
                }

                MenuButton(if (isLocked) "Снять PIN-код" else "Заблокировать PIN") {
                    pendingAppAction = app
                    selectedAppForMenu = null
                    if (isLocked) {
                        pinInput = ""
                        pinDialogType = PinDialogType.REMOVE
                    } else {
                        if (savedPin.isEmpty()) {
                            pinInput = ""
                            pinDialogType = PinDialogType.SET
                        } else {
                            saveLockedPackages(lockedPackages + app.packageName)
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
                MenuButton("Отмена") { selectedAppForMenu = null }
            }
        }

        // УНИВЕРСАЛЬНЫЙ ДИАЛОГ PIN-КОДА (Один вместо трех!)
        if (pinDialogType != PinDialogType.NONE) {
            val title = when(pinDialogType) {
                PinDialogType.SET -> "Придумайте свой PIN-код"
                PinDialogType.ENTER -> "Введите ваш PIN-код"
                PinDialogType.REMOVE -> "Введите PIN для снятия блока"
                else -> ""
            }
            ModalContainer(onDismiss = { pinDialogType = PinDialogType.NONE; pendingAppAction = null }, width = 320.dp) {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (pinDialogType == PinDialogType.ENTER && pendingAppAction != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(pendingAppAction!!.name, color = Color.White.copy(0.7f), fontSize = 14.sp)
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = pinInput, onValueChange = { pinInput = it }, label = { Text("PIN-код") }, singleLine = true, visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MenuButton("ОК / Сохранить", modifier = Modifier.weight(1f)) {
                        when (pinDialogType) {
                            PinDialogType.SET -> {
                                if (pinInput.isNotEmpty()) {
                                    savePinCode(pinInput)
                                    pendingAppAction?.let { saveLockedPackages(lockedPackages + it.packageName) }
                                    Toast.makeText(context, "PIN установлен!", Toast.LENGTH_SHORT).show()
                                    pinDialogType = PinDialogType.NONE
                                    pendingAppAction = null
                                }
                            }
                            PinDialogType.ENTER -> {
                                if (pinInput == savedPin && pendingAppAction != null) {
                                    openApp(context, pendingAppAction!!)
                                    pinDialogType = PinDialogType.NONE
                                    pendingAppAction = null
                                } else {
                                    Toast.makeText(context, "Неверный PIN!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            PinDialogType.REMOVE -> {
                                if (pinInput == savedPin && pendingAppAction != null) {
                                    saveLockedPackages(lockedPackages - pendingAppAction!!.packageName)
                                    Toast.makeText(context, "Блокировка снята", Toast.LENGTH_SHORT).show()
                                    pinDialogType = PinDialogType.NONE
                                    pendingAppAction = null
                                } else {
                                    Toast.makeText(context, "Неверный PIN!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            else -> {}
                        }
                    }
                    MenuButton("Отмена", modifier = Modifier.weight(1f)) {
                        pinDialogType = PinDialogType.NONE
                        pendingAppAction = null
                    }
                }
            }
        }
    }
}

enum class PinDialogType { NONE, SET, ENTER, REMOVE }

@Composable
fun AppGridModal(title: String, apps: List<AppInfo>, iconCache: MutableMap<String, ImageBitmap>, lockedPackages: Set<String>, isPicker: Boolean = false, onClose: () -> Unit, onAppClick: (AppInfo) -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(if (isPicker) 0xCC000000 else 0xFA0A0A0A)).focusable(false),
        contentAlignment = if (isPicker) Alignment.Center else Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .let { if (isPicker) it.fillMaxSize(0.8f).clip(RoundedCornerShape(24.dp)).background(Color(0xFF162B32)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)).padding(32.dp) else it.fillMaxSize().padding(48.dp) }
                .focusable(false),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, fontSize = if (isPicker) 26.sp else 32.sp, fontWeight = FontWeight.Bold)
                
                var isCloseFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .onFocusChanged { isCloseFocused = it.isFocused }
                        .focusable()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isCloseFocused) Color(0x44FFFFFF) else Color.Transparent)
                        .border(width = if (isCloseFocused) 2.dp else 0.dp, color = Color.White, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable { onClose() }
                ) {
                    Text("Закрыть ✕", color = Color.White, fontSize = if (isPicker) 18.sp else 20.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isPicker) 5 else 6),
                horizontalArrangement = Arrangement.spacedBy(if (isPicker) 20.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(if (isPicker) 20.dp else 24.dp),
                modifier = Modifier.fillMaxSize().focusable(false)
            ) {
                itemsIndexed(apps) { index, app ->
                    HumeAppCard(
                        app = app, 
                        iconCache = iconCache, 
                        onClick = { onAppClick(app) },
                        isLocked = lockedPackages.contains(app.packageName),
                        autoFocus = (index == 0)
                    )
                }
            }
        }
    }
}

@Composable
fun ModalContainer(onDismiss: () -> Unit, width: androidx.compose.ui.unit.Dp, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)).focusable(false), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.width(width).clip(RoundedCornerShape(24.dp)).background(Color(0xFF162B32)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)).focusable(false).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
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
            .clickable { onClick() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isRed) Color(0xFFFF6B6B) else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun HumeAppCard(app: AppInfo, iconCache: MutableMap<String, ImageBitmap>, onClick: () -> Unit, isLocked: Boolean, autoFocus: Boolean = false) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val bmp = iconCache[app.packageName] ?: app.icon.toBitmap().asImageBitmap().also { iconCache[app.packageName] = it }

    LaunchedEffect(Unit) {
        if (autoFocus) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .size(90.dp)
            .focusRequester(focusRequester)
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
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    pm.queryIntentActivities(intent, 0).forEach {
        map[it.activityInfo.packageName] = AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.activityInfo.name, it.loadIcon(pm))
    }
    val regularIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    pm.queryIntentActivities(regularIntent, 0).forEach {
        if (!map.containsKey(it.activityInfo.packageName)) {
            map[it.activityInfo.packageName] = AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.activityInfo.name, it.loadIcon(pm))
        }
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

