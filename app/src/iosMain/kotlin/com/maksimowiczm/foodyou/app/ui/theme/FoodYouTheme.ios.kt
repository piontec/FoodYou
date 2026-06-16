package com.maksimowiczm.foodyou.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.maksimowiczm.foodyou.app.ui.common.theme.DarkNutrientsPalette
import com.maksimowiczm.foodyou.app.ui.common.theme.LightNutrientsPalette
import com.maksimowiczm.foodyou.app.ui.common.theme.LocalNutrientsPalette
import com.maksimowiczm.foodyou.theme.NutrientsColors
import com.maksimowiczm.foodyou.theme.ThemeSettings
import com.materialkolor.rememberDynamicColorScheme

@Composable
internal actual fun FoodYouTheme(
    themeSettings: ThemeSettings?,
    nutrientsColors: NutrientsColors?,
    content: @Composable () -> Unit,
) {
    val isDark = themeSettings?.isDark() ?: isSystemInDarkTheme()
    val colorScheme = rememberDynamicColorScheme(seedColor = MaterialDeepPurple, isDark = isDark)
    val nutrientsPalette = if (isDark) DarkNutrientsPalette else LightNutrientsPalette

    CompositionLocalProvider(
        LocalNutrientsPalette provides nutrientsPalette.applyColors(nutrientsColors)
    ) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
