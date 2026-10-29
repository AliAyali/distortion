package com.aliayali.distortion.navigation

import androidx.annotation.StringRes
import com.aliayali.distortion.R
import com.aliayali.home.navigation.HomeNavKey
import com.aliayali.home.R as homeR

data class TopLevelNavItem(
    @param:StringRes val iconTextId: Int,
    @param:StringRes val titleTextId: Int,
)

val HOME = TopLevelNavItem(
    iconTextId = homeR.string.feature_home_title,
    titleTextId = R.string.app_name,
)

val TOP_LEVEL_NAV_ITEMS = mapOf(
    HomeNavKey to HOME,
)