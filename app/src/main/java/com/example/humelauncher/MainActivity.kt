package com.example.humelauncher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.Dp
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
        
        val prefs = getSharedPreferences("HumeLauncherPrefs", Context.MODE_PRIVATE)
        val autostartPkg = prefs.getString("autostart_pkg", "")
        if (!autostartPkg.isNullOrEmpty()) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(autostartPkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            } catch (_: Exception) {}
        }

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
    var currentHour by remember { mutableIntStateOf(12) }
    
    var isDrawerOpen by remember { mutableStateOf(false) }     
    var isPickerOpen by remember { mutableStateOf(false) }     
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isAutostartPickerOpen by remember { mutableStateOf(false) }
    var targetDockIndex by remember { mutableIntStateOf(-1) }
    var isEditMode by remember { mutableStateOf(false) }       
    
    var rightPressCount by remember { mutableIntStateOf(0) }
    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    
    var lockedPackages by remember { mutableStateOf(prefs.getStringSet("locked_packages", emptySet()) ?: emptySet()) }
    var savedPin by remember { mutableStateOf(prefs.getString("saved_pin", "") ?: "") }
    var pinInput by remember { mutableStateOf("") }
    var pinDialogType by remember { mutableStateOf<PinDialogType>(PinDialogType.NONE) }
    var pendingAppAction by remember { mutableStateOf<AppInfo?>(null) }

    var dockPosition by remember { mutableStateOf(prefs.getString("dock_pos", "BOTTOM") ?: "BOTTOM") }
    var cardSize by remember { mutableStateOf(prefs.getString("card_size", "MEDIUM") ?: "MEDIUM") }
    var themeMode by remember { mutableStateOf(prefs.getString("theme_mode", "AUTO") ?: "AUTO") }
    var timeFormat24 by remember { mutableStateOf(prefs.getBoolean("time_24h", true)) }
    var gridColumns by remember { mutableStateOf(prefs.getInt("grid_cols", 6)) }
    var autostartPkg by remember { mutableStateOf(prefs.getString("autostart_pkg", "") ?: "") }
    var hideSystemApps by remember { mutableStateOf(prefs.getBoolean("hide_system", true)) }
    var appSortMode by remember { mutableStateOf(prefs.getString("sort_mode", "NAME") ?: "NAME") }
    var iconShape by remember { mutableStateOf(prefs.getString("icon_shape", "ROUNDED") ?: "ROUNDED") }

    val iconCache = remember { mutableStateMapOf<String, ImageBitmap>() }

    val displayedApps = remember(allApps, hideSystemApps, appSortMode) {
        val filtered = if (hideSystemApps) {
            allApps.filter { app ->
                !app.packageName.contains("android.internal") &&
                !app.packageName.contains("providers") &&
                !app.packageName.contains("overlay") &&
                !app.packageName.contains("services") &&
                app.packageName != "com.example.humelauncher"
            }
        } else {
            allApps
        }

        when (appSortMode) {
            "NEWEST" -> filtered
            else -> filtered.sortedBy { it.name }
        }
    }

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
            isAutostartPickerOpen -> isAutostartPickerOpen = false
            isSettingsOpen -> isSettingsOpen = false
            isPickerOpen -> isPickerOpen = false
            isDrawerOpen -> isDrawerOpen = false
            isEditMode -> isEditMode = false
            else -> {}
        }
    }

    LaunchedEffect(timeFormat24) {
        while (true) {
            val now = Date()
            val pattern = if (timeFormat24) "HH:mm" else "hh:mm a"
            currentTime = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
            currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            delay(30_000)
        }
    }

    LaunchedEffect(Unit) {
        val apps = withContext(Dispatchers.IO) { getAllApps(context) }
        allApps = apps
        
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
        } else if (apps.isNotEmpty()) {
            val initial = apps.take(3)
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

    val bgGradient = when (themeMode) {
        "DARK" -> Brush.radialGradient(listOf(Color(0xFF142429), Color(0xFF071B22), Color(0xFF000000)))
        "TEAL" -> Brush.radialGradient(listOf(Color(0xFF144D59), Color(0xFF071B22), Color(0xFF000000)))
        "SUNSET" -> Brush.radialGradient(listOf(Color(0xFF4A1525), Color(0xFF1A0917), Color(0xFF000000)))
        else -> {
            if (currentHour in 6..18) {
                Brush.radialGradient(listOf(Color(0xFF1F5F6D), Color(0xFF0E303A), Color(0xFF051216)))
            } else {
                Brush.radialGradient(listOf(Color(0xFF102830), Color(0xFF071418), Color(0xFF000000)))
            }
        }
    }

    val cardHeight = when (cardSize) {
        "SMALL" -> 80.dp
        "LARGE" -> 115.dp
        else -> 95.dp
    }

    val iconSize = when (cardSize) {
        "SMALL" -> 40.dp
        "LARGE" -> 56.dp
        else -> 48.dp
    }

    Box(Modifier.fillMaxSize().background(bgGradient)) {
        if (allApps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Загрузка приложений...", color = Color.White, fontSize = 24.sp)
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 24.dp),
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
                    
                    var isSettingsBtnFocused by remember { mutableStateOf(false) }
                    Box(
                        Modifier.size(40.dp)
                            .onFocusChanged { isSettingsBtnFocused = it.isFocused }
                            .focusable()
                            .clip(CircleShape)
                            .background(if (isSettingsBtnFocused) Color(0x66FFFFFF) else Color(0x22FFFFFF))
                            .border(width = if (isSettingsBtnFocused) 2.dp else 1.dp, color = Color.White, shape = CircleShape)
                            .clickable { isSettingsOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚙️", fontSize = 16.sp)
                    }
                }
            }

            val dockAlignment = when (dockPosition) {
                "TOP" -> Alignment.TopCenter
                "CENTER" -> Alignment.Center
                else -> Alignment.BottomCenter
            }

            val dockPadding = when (dockPosition) {
                "TOP" -> Modifier.padding(top = 90.dp)
                "CENTER" -> Modifier.padding(0.dp)
                else -> Modifier.padding(bottom = 20.dp)
            }

            Box(
                Modifier.align(dockAlignment)
                    .then(dockPadding)
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

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onFocusChanged { 
                                    isFocused = it.isFocused
                                    if (!isFocused && isLastElement) rightPressCount = 0
                                }
                                .focusable()
                                .onKeyEvent { keyEvent ->
                                    if (!isEditMode && isLastElement && keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT && keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        rightPressCount++
                                        if (rightPressCount >= 2) {
                                            isEditMode = true
                                            Toast.makeText(context, "Режим редактора активирован!", Toast.LENGTH_SHORT).show()
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
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

        if (isDrawerOpen) {
            ScrollableAppGridModal(title = "Все приложения", apps = displayedApps, iconCache = iconCache, lockedPackages = lockedPackages, gridColumns = gridColumns, cardHeight = cardHeight, iconSize = iconSize, iconShape = iconShape, onClose = { isDrawerOpen = false }) { app ->
                selectedAppForMenu = app
            }
        }

        if (isPickerOpen) {
            ScrollableAppGridModal(title = "Выберите приложение в док", apps = displayedApps, iconCache = iconCache, lockedPackages = lockedPackages, isPicker = true, gridColumns = gridColumns, cardHeight = cardHeight, iconSize = iconSize, iconShape = iconShape, onClose = { isPickerOpen = false }) { app ->
                if (targetDockIndex in dockApps.indices) {
                    val list = dockApps.toMutableList()
                    list[targetDockIndex] = app
                    dockApps = list
                    saveDockToPrefs(dockApps)
                }
                isPickerOpen = false
            }
        }

        if (isSettingsOpen) {
            SettingsModal(
                dockPosition = dockPosition,
                cardSize = cardSize,
                themeMode = themeMode,
                timeFormat24 = timeFormat24,
                gridColumns = gridColumns,
                hideSystemApps = hideSystemApps,
                appSortMode = appSortMode,
                iconShape = iconShape,
                autostartPkg = autostartPkg,
                allApps = allApps,
                onDockPosChange = { dockPosition = it; prefs.edit().putString("dock_pos", it).apply() },
                onCardSizeChange = { cardSize = it; prefs.edit().putString("card_size", it).apply() },
                onThemeChange = { themeMode = it; prefs.edit().putString("theme_mode", it).apply() },
                onTimeFormatChange = { timeFormat24 = it; prefs.edit().putBoolean("time_24h", it).apply() },
                onGridColumnsChange = { gridColumns = it; prefs.edit().putInt("grid_cols", it).apply() },
                onHideSystemChange = { hideSystemApps = it; prefs.edit().putBoolean("hide_system", it).apply() },
                onSortModeChange = { appSortMode = it; prefs.edit().putString("sort_mode", it).apply() },
                onIconShapeChange = { iconShape = it; prefs.edit().putString("icon_shape", it).apply() },
                onOpenAutostartPicker = { isAutostartPickerOpen = true },
                onOpenSystemSettings = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    } catch (_: Exception) {
                        Toast.makeText(context, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show()
                    }
                },
                onClose = { isSettingsOpen = false }
            )
        }

        if (isAutostartPickerOpen) {
            ScrollableAppGridModal(title = "Выберите автозапуск", apps = displayedApps, iconCache = iconCache, lockedPackages = lockedPackages, gridColumns = gridColumns, cardHeight = cardHeight, iconSize = iconSize, iconShape = iconShape, onClose = { isAutostartPickerOpen = false }) { app ->
                autostartPkg = app.packageName
                prefs.edit().putString("autostart_pkg", app.packageName).apply()
                Toast.makeText(context, "Автозапуск: ${app.name}", Toast.LENGTH_SHORT).show()
                isAutostartPickerOpen = false
            }
        }

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

/**
 * ЗАЩИЩЕННОЕ ОКНО НАСТРОЕК (ФОКУС НАДЕЖНО УДЕРЖИВАЕТСЯ ВНУТРИ)
 */
@Composable
fun SettingsModal(
    dockPosition: String,
    cardSize: String,
    themeMode: String,
    timeFormat24: Boolean,
    gridColumns: Int,
    hideSystemApps: Boolean,
    appSortMode: String,
    iconShape: String,
    autostartPkg: String,
    allApps: List<AppInfo>,
    onDockPosChange: (String) -> Unit,
    onCardSizeChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onTimeFormatChange: (Boolean) -> Unit,
    onGridColumnsChange: (Int) -> Unit,
    onHideSystemChange: (Boolean) -> Unit,
    onSortModeChange: (String) -> Unit,
    onIconShapeChange: (String) -> Unit,
    onOpenAutostartPicker: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onClose: () -> Unit
) {
    val autostartAppName = allApps.find { it.packageName == autostartPkg }?.name ?: "Отключен"
    val firstOptionRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            firstOptionRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        Modifier.fillMaxSize()
            .background(Color(0xCC000000))
            .focusable(true), // Предотвращает вылет фокуса наружу окна
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF162B32))
                .border(2.dp, Color(0xFF33FFFF), RoundedCornerShape(24.dp))
                .focusable(true) // Жестко удерживает фокус внутри модалки
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚙️ Расширенные настройки", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .focusable(true),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Положение дока
                Text("Положение дока:", color = Color.White.copy(0.7f), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionButton("Внизу", selected = dockPosition == "BOTTOM", modifier = Modifier.weight(1f).focusRequester(firstOptionRequester)) { onDockPosChange("BOTTOM") }
                    OptionButton("По центру", selected = dockPosition == "CENTER", modifier = Modifier.weight(1f)) { onDockPosChange("CENTER") }
                    OptionButton("Вверху", selected = dockPosition == "TOP", modifier = Modifier.weight(1f)) { onDockPosChange("TOP") }
                }
                Spacer(Modifier.height(8.dp))

                // Размер карточек
                Text("Размер карточек:", color = Color.White.copy(0.7f), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionButton("Мелкий", selected = cardSize == "SMALL", modifier = Modifier.weight(1f)) { onCardSizeChange("SMALL") }
                    OptionButton("Средний", selected = cardSize == "MEDIUM", modifier = Modifier.weight(1f)) { onCardSizeChange("MEDIUM") }
                    OptionButton("Крупный", selected = cardSize == "LARGE", modifier = Modifier.weight(1f)) { onCardSizeChange("LARGE") }
                }
                Spacer(Modifier.height(8.dp))

                // Форма иконок
                Text("Форма иконок:", color = Color.White.copy(0.7f), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionButton("Скругленные", selected = iconShape == "ROUNDED", modifier = Modifier.weight(1f)) { onIconShapeChange("ROUNDED") }
                    OptionButton("Круглые", selected = iconShape == "CIRCLE", modifier = Modifier.weight(1f)) { onIconShapeChange("CIRCLE") }
                    OptionButton("Квадратные", selected = iconShape == "SQUARE", modifier = Modifier.weight(1f)) { onIconShapeChange("SQUARE") }
                }
                Spacer(Modifier.height(8.dp))

                // Колонок в сетке
                Text("Колонок в сетке приложений:", color = Color.White.copy(0.7f), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionButton("5 колонок", selected = gridColumns == 5, modifier = Modifier.weight(1f)) { onGridColumnsChange(5) }
                    OptionButton("6 колонок", selected = gridColumns == 6, modifier = Modifier.weight(1f)) { onGridColumnsChange(6) }
                    OptionButton("7 колонок", selected = gridColumns == 7, modifier = Modifier.weight(1f)) { onGridColumnsChange(7) }
                }
                Spacer(Modifier.height(8.dp))

                // Системные и сортировка
                Text("Фильтрация и сортировка:", color = Color.White.copy(0.7f), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionButton(if (hideSystemApps) "Системные: Скрыты" else "Системные: Видны", selected = hideSystemApps, modifier = Modifier.weight(1f)) { onHideSystemChange(!hideSystemApps) }
                    OptionButton(if (appSortMode == "NAME") "Сорт: А-Я" else "Сорт: Обычный", selected = appSortMode == "NAME", modifier = Modifier.weight(1f)) { onSortModeChange(if (appSortMode == "NAME") "NEWEST" else "NAME") }
                }
                Spacer(Modifier.height(8.dp))

                // Тема / Обои
                Text("Тема оформления:", color = Color.White.copy(0.7f), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionButton("Авто (День/Ночь)", selected = themeMode == "AUTO", modifier = Modifier.weight(1f)) { onThemeChange("AUTO") }
                    OptionButton("Темная", selected = themeMode == "DARK", modifier = Modifier.weight(1f)) { onThemeChange("DARK") }
                    OptionButton("Бирюза", selected = themeMode == "TEAL", modifier = Modifier.weight(1f)) { onThemeChange("TEAL") }
                    OptionButton("Закат", selected = themeMode == "SUNSET", modifier = Modifier.weight(1f)) { onThemeChange("SUNSET") }
                }
                Spacer(Modifier.height(8.dp))

                // Формат времени
                Text("Формат времени:", color = Color.White.copy(0.7f), fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionButton("24-часовой", selected = timeFormat24, modifier = Modifier.weight(1f)) { onTimeFormatChange(true) }
                    OptionButton("12-часовой (AM/PM)", selected = !timeFormat24, modifier = Modifier.weight(1f)) { onTimeFormatChange(false) }
                }
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MenuButton("Автозапуск: $autostartAppName", modifier = Modifier.weight(1f)) { onOpenAutostartPicker() }
                    MenuButton("Настройки Android ⚙️", modifier = Modifier.weight(1f)) { onOpenSystemSettings() }
                }
            }

            Spacer(Modifier.height(14.dp))
            MenuButton("Закрыть ✕") { onClose() }
        }
    }
}

@Composable
fun OptionButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(34.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF144D59) else if (isFocused) Color(0x44FFFFFF) else Color(0x15FFFFFF))
            .border(width = if (isFocused) 2.dp else if (selected) 1.dp else 0.dp, color = Color.White, shape = RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun ScrollableAppGridModal(
    title: String,
    apps: List<AppInfo>,
    iconCache: MutableMap<String, ImageBitmap>,
    lockedPackages: Set<String>,
    isPicker: Boolean = false,
    gridColumns: Int = 6,
    cardHeight: Dp = 95.dp,
    iconSize: Dp = 48.dp,
    iconShape: String = "ROUNDED",
    onClose: () -> Unit,
    onAppClick: (AppInfo) -> Unit
) {
    val firstItemRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            firstItemRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        Modifier.fillMaxSize()
            .background(Color(0xFF071B22))
            .focusable(true),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .let { 
                    if (isPicker) {
                        it.width(960.dp)
                          .height(560.dp)
                          .clip(RoundedCornerShape(24.dp))
                          .background(Color(0xFF162B32))
                          .border(2.dp, Color(0xFF33FFFF), RoundedCornerShape(24.dp))
                          .padding(24.dp)
                    } else {
                        it.fillMaxSize()
                          .padding(48.dp)
                    }
                }
                .focusable(true),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White, fontSize = if (isPicker) 22.sp else 32.sp, fontWeight = FontWeight.Bold)
                
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
                    Text("Закрыть ✕", color = Color.White, fontSize = if (isPicker) 16.sp else 20.sp)
                }
            }
            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusable(true)
            ) {
                itemsIndexed(apps) { index, app ->
                    val modifier = if (index == 0) Modifier.focusRequester(firstItemRequester) else Modifier
                    HumeAppCard(
                        app = app,
                        iconCache = iconCache,
                        onClick = { onAppClick(app) },
                        isLocked = lockedPackages.contains(app.packageName),
                        cardHeight = cardHeight,
                        iconSize = iconSize,
                        iconShape = iconShape,
                        modifier = modifier
                    )
                }
            }
        }
    }
}

@Composable
fun ModalContainer(onDismiss: () -> Unit, width: Dp, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize()
            .background(Color(0xCC000000))
            .focusable(true),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(width)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF162B32))
                .border(2.dp, Color(0xFF33FFFF), RoundedCornerShape(24.dp))
                .focusable(true)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
fun MenuButton(text: String, isRed: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) Color(0x66FFFFFF) else if (isRed) Color(0x44FF0000) else Color(0x22FFFFFF))
            .border(width = if (isFocused) 2.dp else 0.dp, color = Color.White, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isRed) Color(0xFFFF6B6B) else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun HumeAppCard(
    app: AppInfo, 
    iconCache: MutableMap<String, ImageBitmap>, 
    onClick: () -> Unit, 
    isLocked: Boolean, 
    cardHeight: Dp = 95.dp,
    iconSize: Dp = 48.dp,
    iconShape: String = "ROUNDED",
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val bmp = iconCache[app.packageName] ?: app.icon.toBitmap().asImageBitmap().also { iconCache[app.packageName] = it }

    val shape = when (iconShape) {
        "CIRCLE" -> CircleShape
        "SQUARE" -> RoundedCornerShape(4.dp)
        else -> RoundedCornerShape(10.dp)
    }

    Box(
        modifier = modifier
            .height(cardHeight)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFocused) Color(0x44FFFFFF) else Color.Transparent)
            .border(width = if (isFocused) 3.dp else 0.dp, color = Color.White, shape = RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp)
        ) {
            Image(bitmap = bmp, contentDescription = app.name, modifier = Modifier.size(iconSize).clip(shape), contentScale = ContentScale.Crop)
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
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${app.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (_: Exception) { Toast.makeText(context, "Ошибка настроек", Toast.LENGTH_SHORT).show() }
}

