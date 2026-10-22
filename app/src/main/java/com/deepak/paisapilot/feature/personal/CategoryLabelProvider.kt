package com.deepak.paisapilot.feature.personal

import android.content.Context
import com.deepak.paisapilot.domain.personal.Category
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Resolves category display labels outside Compose, for search matching in ViewModels. */
fun interface CategoryLabelProvider {
    fun label(category: Category): String
}

@Singleton
class ResourceCategoryLabelProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : CategoryLabelProvider {
    override fun label(category: Category): String = context.getString(category.labelRes())
}
