package com.maksimowiczm.foodyou.app.ui.theme

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.maksimowiczm.foodyou.theme.Theme
import com.maksimowiczm.foodyou.theme.ThemeSettings

@Composable
actual fun ColumnScope.PlatformAdditionalSettings(
    themeSettings: ThemeSettings,
    onUpdateTheme: (Theme) -> Unit,
) = Unit
