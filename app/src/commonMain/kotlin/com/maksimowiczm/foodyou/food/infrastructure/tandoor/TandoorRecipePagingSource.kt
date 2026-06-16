package com.maksimowiczm.foodyou.food.infrastructure.tandoor

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.maksimowiczm.foodyou.food.domain.entity.TandoorRecipeListItem
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.model.TandoorRecipeListItemDto

internal class TandoorRecipePagingSource(
    private val dataSource: TandoorRemoteDataSource,
    private val query: String,
    private val pageSize: Int,
) : PagingSource<Int, TandoorRecipeListItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TandoorRecipeListItem> {
        val page = params.key ?: 1
        val result = dataSource.listRecipes(query = query, page = page, pageSize = pageSize)

        return result.fold(
            onSuccess = { response ->
                LoadResult.Page(
                    data = response.results.map(TandoorRecipeListItemDto::toDomain),
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (response.next == null || response.results.isEmpty()) null else page + 1,
                )
            },
            onFailure = { error -> LoadResult.Error(error) },
        )
    }

    override fun getRefreshKey(state: PagingState<Int, TandoorRecipeListItem>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchorPosition) ?: return null
        return anchorPage.prevKey?.plus(1) ?: anchorPage.nextKey?.minus(1)
    }
}

private fun TandoorRecipeListItemDto.toDomain() =
    TandoorRecipeListItem(
        id = id,
        name = name,
        imageUrl = imageUrl,
    )
