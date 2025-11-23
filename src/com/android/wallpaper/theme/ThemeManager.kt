package com.android.wallpaper.theme

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.provider.Settings
import com.android.wallpaper.R
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    enum class Category(val overlayCategory: String) {
        FONT("android.theme.customization.font"),
        ICON_PACK("android.theme.customization.icon_pack.android")
    }

    data class ThemeOption(
        val packageName: String?,
        val label: String
    )

    private val packageManager = context.packageManager
    private val updateTrigger = MutableSharedFlow<Unit>(replay = 1)

    fun currentSelection(category: Category): Flow<ThemeOption> {
        return updateTrigger.onStart { emit(Unit) }.map { getCurrentOption(category) ?: ThemeOption(null, "Default") }
    }

    fun getOptions(category: Category): List<ThemeOption> {
        val options = mutableListOf<ThemeOption>()
        options.add(ThemeOption(null, context.getString(R.string.default_theme_title)))

        val overlayPackages = getOverlayInfos(category.overlayCategory)
        overlayPackages.forEach { packageName ->
             try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val label = appInfo.loadLabel(packageManager).toString()
                options.add(ThemeOption(packageName, label))
             } catch (e: PackageManager.NameNotFoundException) {
             }
        }
        
        return options
    }

    fun getCurrentOption(category: Category): ThemeOption? {
        val jsonString = Settings.Secure.getStringForUser(
            context.contentResolver,
            "theme_customization_overlay_packages",
            UserHandle.myUserId()
        ) ?: return ThemeOption(null, context.getString(R.string.default_theme_title))

        return try {
            val json = JSONObject(jsonString)
            if (json.has(category.overlayCategory)) {
                val pkg = json.getString(category.overlayCategory)
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                ThemeOption(pkg, appInfo.loadLabel(packageManager).toString())
            } else {
                ThemeOption(null, context.getString(R.string.default_theme_title))
            }
        } catch (e: Exception) {
            ThemeOption(null, context.getString(R.string.default_theme_title))
        }
    }

    fun applyOption(category: Category, option: ThemeOption) {
        val jsonString = Settings.Secure.getStringForUser(
            context.contentResolver,
            "theme_customization_overlay_packages",
            UserHandle.myUserId()
        ) ?: "{}"

        try {
            val json = JSONObject(jsonString)
            if (option.packageName == null) {
                json.remove(category.overlayCategory)
                if (category == Category.ICON_PACK) {
                     json.remove("android.theme.customization.icon_pack.systemui")
                     json.remove("android.theme.customization.icon_pack.settings")
                     json.remove("android.theme.customization.icon_pack.launcher")
                     json.remove("android.theme.customization.icon_pack.themepicker")
                }
            } else {
                json.put(category.overlayCategory, option.packageName)
                if (category == Category.ICON_PACK) {
                    val prefix = option.packageName.substringBeforeLast(".")
                    json.put("android.theme.customization.icon_pack.systemui", "$prefix.systemui")
                    json.put("android.theme.customization.icon_pack.settings", "$prefix.settings")
                    json.put("android.theme.customization.icon_pack.launcher", "$prefix.launcher")
                    json.put("android.theme.customization.icon_pack.themepicker", "$prefix.themepicker")
                }
            }

            Settings.Secure.putStringForUser(
                context.contentResolver,
                "theme_customization_overlay_packages",
                json.toString(),
                UserHandle.myUserId()
            )
            updateTrigger.tryEmit(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTypeface(packageName: String?): Typeface {
        if (packageName == null) return Typeface.DEFAULT
        try {
            val remoteRes = packageManager.getResourcesForApplication(packageName)
            val id = remoteRes.getIdentifier("config_bodyFontFamily", "string", packageName)
            if (id != 0) {
                val familyName = remoteRes.getString(id)
                return Typeface.create(familyName, Typeface.NORMAL)
            }
        } catch (e: Exception) {
        }
        return Typeface.DEFAULT
    }

    fun getIconPreview(packageName: String?): Drawable? {
        if (packageName == null) {
            return context.getDrawable(android.R.drawable.sym_def_app_icon)
        }
        try {
            val remoteRes = packageManager.getResourcesForApplication(packageName)
            val id = remoteRes.getIdentifier("ic_wifi_signal_3", "drawable", packageName)
            if (id != 0) return remoteRes.getDrawable(id, null)
            return packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            return context.getDrawable(android.R.drawable.sym_def_app_icon)
        }
    }

    private fun getOverlayInfos(category: String): List<String> {
        val packages = mutableListOf<String>()
        try {
            val smClass = Class.forName("android.os.ServiceManager")
            val getService = smClass.getMethod("getService", String::class.java)
            val binder = getService.invoke(null, "overlay") as android.os.IBinder
            
            val iOmStub = Class.forName("android.content.om.IOverlayManager\$Stub")
            val asInterface = iOmStub.getMethod("asInterface", android.os.IBinder::class.java)
            val overlayManager = asInterface.invoke(null, binder)
            
            val getOverlays = overlayManager.javaClass.getMethod(
                "getOverlayInfosForTarget", 
                String::class.java, 
                Int::class.javaPrimitiveType
            )
            
            val overlayInfos = getOverlays.invoke(overlayManager, "android", UserHandle.myUserId()) as List<*>
            
            for (info in overlayInfos) {
                if (info == null) continue
                val infoClass = info.javaClass
                val catField = infoClass.getField("category")
                val pkgField = infoClass.getField("packageName")
                
                val cat = catField.get(info) as? String
                if (cat == category) {
                    val pkg = pkgField.get(info) as String
                    packages.add(pkg)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return packages
    }
}
