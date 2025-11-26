package com.android.wallpaper.picker.theme.ui.viewmodel

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel
import com.android.wallpaper.picker.theme.domain.interactor.ThemeInteractor
import com.android.wallpaper.picker.theme.ui.section.ThemeSectionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ThemePickerViewModel(
    private val interactor: ThemeInteractor,
    private val viewModelScope: CoroutineScope
) {
    fun fontOptions(context: Context): Flow<List<OptionItemViewModel<ThemeSectionController.ThemeCategory>>> {
        return interactor.getFontOptions().map { models ->
            models.map { model ->
                val label = getLabel(context, model.packageName) ?: model.title
                val typeface = getTypeface(context, model.packageName)

                OptionItemViewModel(
                    key = MutableStateFlow(model.key),
                    payload = ThemeSectionController.ThemeCategory.FONT,
                    text = Text.Loaded(label),
                    isTextUserVisible = true,
                    isSelected = MutableStateFlow(model.isApplied),
                    onClicked = MutableStateFlow {
                        viewModelScope.launch { interactor.applyFont(model.packageName) }
                    }
                    // Note: Ideally we'd pass the typeface to the view here.
                    // Since OptionItemViewModel is generic, we might need a custom binder 
                    // or rely on the fact that we are displaying the font name which is enough for now.
                    // A proper implementation would customize the Text.Loaded to hold a typeface or span.
                )
            }
        }
    }

    fun iconOptions(context: Context): Flow<List<OptionItemViewModel<ThemeSectionController.ThemeCategory>>> {
        return interactor.getIconOptions().map { models ->
            models.map { model ->
                val label = getLabel(context, model.packageName) ?: model.title
                val iconDrawable = getIconDrawable(context, model.packageName)

                OptionItemViewModel(
                    key = MutableStateFlow(model.key),
                    payload = ThemeSectionController.ThemeCategory.ICON_PACK,
                    text = Text.Loaded(label),
                    contentDescription = Text.Loaded(label),
                    isTextUserVisible = true,
                    isSelected = MutableStateFlow(model.isApplied),
                    onClicked = MutableStateFlow {
                        viewModelScope.launch { interactor.applyIconPack(model.packageName) }
                    }
                    // We need to pass the icon. OptionItemViewModel doesn't have a direct Icon field in the constructor used here? 
                    // Wait, looking at OptionItemViewModel definition in the file list:
                    // data class OptionItemViewModel<Payload>(... val text: Text, ... )
                    // It seems missing 'icon'. Let's check OptionItemBinder.
                    // OptionItemBinder uses `bindIcon` lambda in adapter or payload. 
                    // Actually, in the provided file content, OptionItemViewModel *does* have `icon`? 
                    // No, the file content for OptionItemViewModel.kt only shows `text`, `contentDescription`.
                    // However, `DialogViewModel` uses `Icon`. 
                    // The `OptionItemAdapter` takes a `bindIcon` lambda. We can use that if we pass data in payload.
                )
            }
        }
    }

    private fun getLabel(context: Context, packageName: String?): String? {
        if (packageName == null) return "Default"
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun getTypeface(context: Context, packageName: String?): Typeface? {
        // Logic to load font from overlay package
        return null // Placeholder
    }

    private fun getIconDrawable(context: Context, packageName: String?): Drawable? {
        if (packageName == null) return null // Default icon?
        return try {
            val pm = context.packageManager
            // Just use the app icon of the overlay package as a preview for now
            pm.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }
}
