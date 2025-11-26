package com.android.wallpaper.picker.theme.data.repository

import android.content.Context
import android.content.om.OverlayInfo
import android.content.om.OverlayManager
import android.os.UserHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeCustomizationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val overlayManager: OverlayManager
) {
    companion object {
        const val FONT_CATEGORY = "android.theme.customization.font"
        const val ICON_PACK_CATEGORY = "android.theme.customization.icon_pack.android"
        
        const val SYSUI_ICON_CATEGORY = "android.theme.customization.icon_pack.systemui"
        const val SETTINGS_ICON_CATEGORY = "android.theme.customization.icon_pack.settings"
        const val ANDROID_PACKAGE = "android"
    }

    private val _fontOptions = MutableStateFlow<List<OverlayInfo>>(emptyList())
    val fontOptions: StateFlow<List<OverlayInfo>> = _fontOptions.asStateFlow()

    private val _iconOptions = MutableStateFlow<List<OverlayInfo>>(emptyList())
    val iconOptions: StateFlow<List<OverlayInfo>> = _iconOptions.asStateFlow()

    init {
        refreshOptions()
    }

    fun refreshOptions() {
        val user = UserHandle.of(UserHandle.myUserId())
        
        val fonts = overlayManager.getOverlayInfosForTarget(ANDROID_PACKAGE, user)
            .filter { it.category == FONT_CATEGORY }
        _fontOptions.value = fonts

        val icons = overlayManager.getOverlayInfosForTarget(ANDROID_PACKAGE, user)
            .filter { it.category == ICON_PACK_CATEGORY }
        _iconOptions.value = icons
    }

    fun getSystemDefaultOption(category: String): String? {
        return null
    }

    fun isDefaultApplied(category: String): Boolean {
        val user = UserHandle.of(UserHandle.myUserId())
        val overlays = overlayManager.getOverlayInfosForTarget(ANDROID_PACKAGE, user)
        return overlays.none { it.category == category && it.isEnabled }
    }

    fun applyFont(packageName: String?) {
        val user = UserHandle.of(UserHandle.myUserId())
         if (packageName != null) {
            overlayManager.setEnabledExclusiveInCategory(packageName, user)
        } else {
            // Disable all in category (Revert to Default)
            overlayManager.getOverlayInfosForTarget(ANDROID_PACKAGE, user)
                .filter { it.category == FONT_CATEGORY && it.isEnabled }
                .forEach { overlayManager.setEnabled(it.packageName, false, user) }

        }
        
        refreshOptions()
    }

    fun applyIconPack(packageName: String?) {
        val user = UserHandle.of(UserHandle.myUserId())

        if (packageName != null) {
            overlayManager.setEnabledExclusiveInCategory(packageName, user)

            try { overlayManager.setEnabledExclusiveInCategory(packageName, user) } catch(e: Exception) {}
        } else {
            fun disableExisting(targetPackage: String, category: String) {
                overlayManager.getOverlayInfosForTarget(targetPackage, user)
                .filter { it.category == category && it.isEnabled }
                .forEach { overlayManager.setEnabled(it.packageName, false, user) }
            }
            disableExisting(ANDROID_PACKAGE, ICON_PACK_CATEGORY)
            disableExisting("com.android.systemui", SYSUI_ICON_CATEGORY)
            disableExisting("com.android.settings", SETTINGS_ICON_CATEGORY)
         }

        refreshOptions()
    }
}
