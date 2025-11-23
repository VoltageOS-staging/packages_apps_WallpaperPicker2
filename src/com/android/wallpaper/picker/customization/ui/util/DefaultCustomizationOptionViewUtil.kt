/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wallpaper.picker.customization.ui.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.widget.Button
import android.graphics.Typeface
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.theme.ThemeManager
import dagger.hilt.android.qualifiers.ActivityContext
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect

import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsData
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class DefaultCustomizationOptionViewUtil @Inject constructor(
    @ActivityContext private val context: Context,
    private val themeManager: ThemeManager
) : CustomizationOptionViewUtil {

    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun getOptionEntries(
        customizationOptionsData: CustomizationOptionsData,
        screen: Screen,
        optionContainer: LinearLayout,
        layoutInflater: LayoutInflater,
    ): List<Pair<CustomizationOption, View>> {
        val views = mutableListOf<Pair<CustomizationOption, View>>()
        
        if (screen == Screen.HOME_SCREEN) {
            val fontOption = DefaultCustomizationOptionUtil.ThemeOption(ThemeManager.Category.FONT)
            val fontView = createEntryView(
                layoutInflater, 
                R.drawable.ic_font, 
                R.string.theme_font_section_title,
                ThemeManager.Category.FONT,
                true
            )
            views.add(fontOption to fontView)

            // Icon Pack Entry
            val iconOption = DefaultCustomizationOptionUtil.ThemeOption(ThemeManager.Category.ICON_PACK)
            val iconView = createEntryView(
                layoutInflater, 
                R.drawable.ic_icon_pack, 
                R.string.theme_icon_pack_section_title,
                ThemeManager.Category.ICON_PACK,
            )
            views.add(iconOption to iconView)
        }
        
        return views
    }

    override fun initFloatingSheet(
        customizationOptionsData: CustomizationOptionsData,
        bottomSheetContainer: FrameLayout,
        layoutInflater: LayoutInflater,
    ): Map<CustomizationOption, View> {
       val map = mutableMapOf<CustomizationOption, View>()
        
        val fontOption = DefaultCustomizationOptionUtil.ThemeOption(ThemeManager.Category.FONT)
        map[fontOption] = createFloatingSheet(layoutInflater, ThemeManager.Category.FONT, R.string.theme_font_section_title)

        val iconOption = DefaultCustomizationOptionUtil.ThemeOption(ThemeManager.Category.ICON_PACK)
        map[iconOption] = createFloatingSheet(layoutInflater, ThemeManager.Category.ICON_PACK, R.string.theme_icon_pack_section_title)

        return map
    }

    override fun createClockPreviewAndAddToParent(
        parentView: ViewGroup,
        layoutInflater: LayoutInflater,
    ): View? {
        return null
    }
    private fun createEntryView(
        inflater: LayoutInflater,
        iconRes: Int,
        titleRes: Int,
        category: ThemeManager.Category,
        isFont: Boolean = false
    ): View {
        val view = inflater.inflate(R.layout.customization_option_entry_layout, null)
        val icon = view.findViewById<ImageView>(R.id.option_entry_icon)
        val title = view.findViewById<TextView>(R.id.option_entry_title)
        val subtitle = view.findViewById<TextView>(R.id.option_entry_subtitle)

        icon.setImageResource(iconRes)
        title.setText(titleRes)
        
        uiScope.launch {
            themeManager.currentSelection(category).collect { option ->
                subtitle.text = option.label
                if (!isFont && option.packageName != null) {
                     val preview = withContext(Dispatchers.IO) { themeManager.getIconPreview(option.packageName) }
                     if (preview != null) icon.setImageDrawable(preview)
                }
            }
        }
        return view
    }

    private fun createFloatingSheet(
        inflater: LayoutInflater,
        category: ThemeManager.Category,
        titleRes: Int
    ): View {
        val view = inflater.inflate(R.layout.floating_sheet_theme_picker, null)
        val titleView = view.findViewById<TextView>(R.id.sheet_title)
        val recyclerView = view.findViewById<RecyclerView>(R.id.options_recycler)
        val applyButton = view.findViewById<Button>(R.id.apply_button)

        titleView.setText(titleRes)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        uiScope.launch {
            val options = withContext(Dispatchers.IO) { themeManager.getOptions(category) }
            val current = withContext(Dispatchers.IO) { themeManager.getCurrentOption(category) }
            
            val adapter = ThemeOptionAdapter(options, current, category) { selected ->
                 applyButton.setOnClickListener {
                     uiScope.launch(Dispatchers.IO) {
                         themeManager.applyOption(category, selected)
                     }
                 }
                 applyButton.isEnabled = true 
            }
            recyclerView.adapter = adapter
        }
        return view
    }

    private inner class ThemeOptionAdapter(
        private val options: List<ThemeManager.ThemeOption>,
        private var selectedOption: ThemeManager.ThemeOption?,
        private val category: ThemeManager.Category,
        private val onOptionSelected: (ThemeManager.ThemeOption) -> Unit
    ) : RecyclerView.Adapter<ThemeOptionAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val container: View = view.findViewById(R.id.option_container)
            val tile: View = view.findViewById(R.id.option_tile)
            val icon: ImageView = view.findViewById(R.id.option_icon)
            val fontPreview: TextView = view.findViewById(R.id.option_font_preview)
            val label: TextView = view.findViewById(R.id.option_label)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_theme_option, parent, false)
            return ViewHolder(view)
       }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val option = options[position]
            val isSelected = option.packageName == selectedOption?.packageName

            holder.tile.isSelected = isSelected
            holder.label.text = option.label

            if (category == ThemeManager.Category.FONT) {
                holder.fontPreview.visibility = View.VISIBLE
                holder.icon.visibility = View.GONE
                try {
                    val typeface = themeManager.getTypeface(option.packageName)
                    holder.fontPreview.typeface = typeface
                } catch (e: Exception) {
                    holder.fontPreview.typeface = Typeface.DEFAULT
                }
            } else {
                holder.fontPreview.visibility = View.GONE
                holder.icon.visibility = View.VISIBLE
                holder.icon.setImageDrawable(themeManager.getIconPreview(option.packageName))
            }

            holder.container.setOnClickListener {
                selectedOption = option
                notifyDataSetChanged()
                onOptionSelected(option)
            }
        }
        override fun getItemCount() = options.size
    }
}
