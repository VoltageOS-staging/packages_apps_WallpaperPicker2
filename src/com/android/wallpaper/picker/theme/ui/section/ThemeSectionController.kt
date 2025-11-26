package com.android.wallpaper.picker.theme.ui.section

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.wallpaper.R
import com.android.wallpaper.model.CustomizationSectionController
import com.android.wallpaper.picker.SectionView
import com.android.wallpaper.picker.customization.ui.section.ResponsiveLayoutSectionView
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter
import com.android.wallpaper.picker.option.ui.binder.OptionItemBinder
import com.android.wallpaper.picker.theme.ui.viewmodel.ThemePickerViewModel
import com.android.wallpaper.picker.common.ui.view.ItemSpacing
import kotlinx.coroutines.launch

class ThemeSectionController(
    private val category: ThemeCategory,
    private val viewModel: ThemePickerViewModel,
    private val lifecycleOwner: LifecycleOwner
) : CustomizationSectionController<ResponsiveLayoutSectionView> {

    enum class ThemeCategory {
        FONT,
        ICON_PACK
    }

    override fun isAvailable(context: Context): Boolean = true

    override fun createView(context: Context): ResponsiveLayoutSectionView {
        val view = LayoutInflater.from(context).inflate(
            R.layout.responsive_section,
            null
        ) as ResponsiveLayoutSectionView

        // Title
     
        val sectionTitle = TextView(context).apply {
            text = when(category) {
                ThemeCategory.FONT -> "Fonts" // Should use R.string
                ThemeCategory.ICON_PACK -> "Icon Packs"
            }
            setTextAppearance(R.style.TextAppearance_DeviceDefault_Small_TitleMedium)
            setTextColor(context.getColor(R.color.system_on_surface))
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.wallpaper_section_horizontal_padding),
                context.resources.getDimensionPixelSize(R.dimen.wallpaper_picker_entry_margin_vertical),
                0,
                context.resources.getDimensionPixelSize(R.dimen.wallpaper_picker_entry_margin_vertical)
            )
        }
        view.addView(sectionTitle)

        // Recycler View
        val recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            addItemDecoration(ItemSpacing(ItemSpacing.ITEM_SPACING_DP))
            clipToPadding = false
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.wallpaper_section_horizontal_padding),
                0,
                context.resources.getDimensionPixelSize(R.dimen.wallpaper_section_horizontal_padding),
                0
            )
        }

        val adapter = OptionItemAdapter<ThemeSectionController.ThemeCategory>(
            layoutResourceId = R.layout.option_item,
            lifecycleOwner = lifecycleOwner,
            bindIcon = { view, _ -> 
                // Icon logic handled in VM payload binding usually, but here we just let the binder handle standard logic
                // If we need custom icon binding per item, we do it here.
                // For fonts, we might want to hide the icon imageview and show text?
                // OptionItemBinder handles text visibility via ViewModel.
            }
        )
        
        recyclerView.adapter = adapter
        view.addView(recyclerView)

        lifecycleOwner.lifecycleScope.launch {
            val flow = when(category) {
                ThemeCategory.FONT -> viewModel.fontOptions(context)
                ThemeCategory.ICON_PACK -> viewModel.iconOptions(context)
            }
            flow.collect { items ->
                adapter.setItems(items)
            }
        }
        return view
    }
}
