package com.maksimowiczm.foodyou.app.ui.database.exportcsvproducts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

@Composable
actual fun ExportCsvProductsScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(Unit) { onBack() }
}
