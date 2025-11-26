package com.android.wallpaper.picker.theme.domain.interactor

import android.content.pm.PackageManager
import android.content.om.OverlayInfo
import com.android.wallpaper.picker.theme.data.repository.ThemeCustomizationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeInteractor @Inject constructor(
    private val repository: ThemeCustomizationRepository
) {
    fun getFontOptions(): Flow<List<ThemeOptionModel>> {
        return repository.fontOptions.map { overlays ->
            val list = mutableListOf<ThemeOptionModel>()
            // Add Default
            list.add(ThemeOptionModel(
                key = "default_font",
                title = "Default",
                packageName = null,
                isApplied = repository.isDefaultApplied(ThemeCustomizationRepository.FONT_CATEGORY)
            ))
            // Add Overlays
            list.addAll(overlays.map { 
                ThemeOptionModel(
                    key = it.packageName,
                    title = it.packageName, // Will be resolved to label in UI
                    packageName = it.packageName,
                    isApplied = it.isEnabled
                ) 
            })
            list
        }
    }

    fun getIconOptions(): Flow<List<ThemeOptionModel>> {
        return repository.iconOptions.map { overlays ->
            val list = mutableListOf<ThemeOptionModel>()
            list.add(ThemeOptionModel(
                key = "default_icons",
                title = "Default",
                packageName = null,
                isApplied = repository.isDefaultApplied(ThemeCustomizationRepository.ICON_PACK_CATEGORY)
            ))
            list.addAll(overlays.map {
                 ThemeOptionModel(it.packageName, it.packageName, it.packageName, it.isEnabled)
            })
            list
        }
    }

    fun applyFont(packageName: String?) = repository.applyFont(packageName)
    fun applyIconPack(packageName: String?) = repository.applyIconPack(packageName)
}

data class ThemeOptionModel(val key: String, val title: String, val packageName: String?, val isApplied: Boolean)
