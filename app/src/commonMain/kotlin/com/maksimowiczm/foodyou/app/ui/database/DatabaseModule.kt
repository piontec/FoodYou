package com.maksimowiczm.foodyou.app.ui.database

import com.maksimowiczm.foodyou.app.ui.database.exportcsvproducts.exportCsvProductsModule
import com.maksimowiczm.foodyou.app.ui.database.externaldatabases.externalDatabasesModule
import com.maksimowiczm.foodyou.app.ui.database.importcsvproducts.importCsvProductsModule
import com.maksimowiczm.foodyou.app.ui.database.swissfoodcompositiondatabase.swissFoodCompositionDatabaseModule
import com.maksimowiczm.foodyou.app.ui.database.tandoor.TandoorBrowseViewModel
import com.maksimowiczm.foodyou.app.ui.database.tandoor.TandoorConnectionViewModel
import com.maksimowiczm.foodyou.app.ui.database.tandoor.TandoorImportViewModel
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorRemoteDataSource
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named

fun Module.database() {
    exportCsvProductsModule()
    externalDatabasesModule()
    importCsvProductsModule()
    swissFoodCompositionDatabaseModule()
    viewModelOf(::TandoorConnectionViewModel)
    viewModel {
        TandoorBrowseViewModel(
            credentialsRepository = get(),
            recipeRepository = get(),
            client = get(named(TandoorRemoteDataSource::class.qualifiedName!!)),
        )
    }
    viewModel { (recipeId: Int) ->
        TandoorImportViewModel(
            recipeId = recipeId,
            credentialsRepository = get(),
            client = get(named(TandoorRemoteDataSource::class.qualifiedName!!)),
            importTandoorRecipeUseCase = get(),
            autoLinkTandoorIngredientsUseCase = get(),
            productRepository = get(),
        )
    }
}
