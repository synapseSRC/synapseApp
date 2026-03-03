package com.synapse.social.studioasinc.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.synapse.social.studioasinc.data.repository.CommentRepository
import com.synapse.social.studioasinc.domain.model.CommentWithUser

class CommentPagingSource(
    private val repository: CommentRepository,
    private val postId: String
) : PagingSource<Int, CommentWithUser>() {

    override fun getRefreshKey(state: PagingState<Int, CommentWithUser>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CommentWithUser> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize

        return try {
            val response = repository.fetchComments(postId, limit = pageSize, offset = offset)
            val comments = response.getOrThrow()

            val nextKey = if (comments.size < pageSize) null else page + 1
            val prevKey = if (page == 0) null else page - 1

            LoadResult.Page(
                data = comments,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
