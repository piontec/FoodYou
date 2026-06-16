package com.maksimowiczm.foodyou.app.ui.database.importcsvproducts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

@Composable
actual fun ImportCsvProductsScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(Unit) { onBack() }
}
