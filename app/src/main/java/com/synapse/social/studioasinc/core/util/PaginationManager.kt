package com.synapse.social.studioasinc.core.util

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException



class PaginationManager<T>(
    private val pageSize: Int = 20,
    private val scrollThreshold: Int = 5,
    private val maxCachedItems: Int = 200,
    private val onLoadPage: suspend (page: Int, pageSize: Int) -> Result<List<T>>,
    private val onError: (String) -> Unit,
    private val coroutineScope: CoroutineScope
) {

    private val _paginationState = MutableStateFlow<PaginationState<T>>(PaginationState.Initial)
    val paginationState: StateFlow<PaginationState<T>> = _paginationState.asStateFlow()


    private var currentPage = 0
    private var isLoading = false
    private var hasMoreData = true
    private val loadedItems = mutableListOf<T>()


    private var scrollListener: RecyclerView.OnScrollListener? = null
    private var loadJob: Job? = null
    private var scrollJob: Job? = null


    private var lastLoadStartTime: Long = 0
    private var totalLoadTime: Long = 0
    private var loadCount: Int = 0
    private var errorCount: Int = 0



    sealed class PaginationState<out T> {
        object Initial : PaginationState<Nothing>()
        object Refreshing : PaginationState<Nothing>()
        data class LoadingMore<T>(val currentItems: List<T>) : PaginationState<T>()
        data class Success<T>(val items: List<T>, val hasMore: Boolean) : PaginationState<T>()
        data class Error<T>(val message: String, val currentItems: List<T>) : PaginationState<T>()
        data class EndOfList<T>(val items: List<T>) : PaginationState<T>()
    }



    sealed class PaginationError {
        data class NetworkError(val message: String) : PaginationError()
        data class ApiError(val code: String, val message: String) : PaginationError()
        data class DataError(val message: String) : PaginationError()
        object NoMoreData : PaginationError()
    }



    fun refresh() {

        if (isLoading && _paginationState.value is PaginationState.Refreshing) {
            android.util.Log.d("PaginationManager", "Refresh already in progress, skipping")
            return
        }


        loadJob?.cancel()
        scrollJob?.cancel()


        _paginationState.value = PaginationState.Refreshing
        isLoading = true


        currentPage = 0
        hasMoreData = true
        loadedItems.clear()


        lastLoadStartTime = System.currentTimeMillis()


        loadJob = coroutineScope.launch {
            try {

                val result = onLoadPage(0, pageSize)


                val loadDuration = System.currentTimeMillis() - lastLoadStartTime
                totalLoadTime += loadDuration
                loadCount++
                android.util.Log.d("PaginationManager", "Refresh completed in ${loadDuration}ms (avg: ${totalLoadTime / loadCount}ms)")

                result.fold(
                    onSuccess = { items ->

                        loadedItems.clear()
                        loadedItems.addAll(items)


                        logMemoryUsage()


                        hasMoreData = items.size >= pageSize


                        _paginationState.value = PaginationState.Success(
                            items = loadedItems.toList(),
                            hasMore = hasMoreData
                        )

                        isLoading = false
                    },
                    onFailure = { error ->

                        errorCount++
                        android.util.Log.e("PaginationManager", "Pagination error (total errors: $errorCount): ${error.message}")
                        handleError(error, loadedItems.toList())
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                errorCount++
                android.util.Log.e("PaginationManager", "Pagination error (total errors: $errorCount): ${e.message}")
                handleError(e, loadedItems.toList())
                isLoading = false
            }
        }
    }



    fun loadNextPage() {

        if (isLoading || !hasMoreData) {
            return
        }


        _paginationState.value = PaginationState.LoadingMore(loadedItems.toList())
        isLoading = true


        val nextPage = currentPage + 1


        lastLoadStartTime = System.currentTimeMillis()


        loadJob = coroutineScope.launch {
            try {

                val result = onLoadPage(nextPage, pageSize)


                val loadDuration = System.currentTimeMillis() - lastLoadStartTime
                totalLoadTime += loadDuration
                loadCount++
                android.util.Log.d("PaginationManager", "LoadNextPage completed in ${loadDuration}ms (avg: ${totalLoadTime / loadCount}ms)")

                result.fold(
                    onSuccess = { items ->
                        if (items.isEmpty()) {

                            hasMoreData = false
                            _paginationState.value = PaginationState.EndOfList(loadedItems.toList())
                        } else {

                            loadedItems.addAll(items)


                            enforceItemLimit()


                            logMemoryUsage()


                            currentPage = nextPage


                            hasMoreData = items.size >= pageSize


                            if (hasMoreData) {
                                _paginationState.value = PaginationState.Success(
                                    items = loadedItems.toList(),
                                    hasMore = true
                                )
                            } else {
                                _paginationState.value = PaginationState.EndOfList(loadedItems.toList())
                            }
                        }

                        isLoading = false
                    },
                    onFailure = { error ->

                        errorCount++
                        android.util.Log.e("PaginationManager", "Pagination error (total errors: $errorCount): ${error.message}")
                        handleError(error, loadedItems.toList())
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                errorCount++
                android.util.Log.e("PaginationManager", "Pagination error (total errors: $errorCount): ${e.message}")
                handleError(e, loadedItems.toList())
                isLoading = false
            }
        }
    }



    fun attachToRecyclerView(recyclerView: RecyclerView) {

        scrollListener?.let { recyclerView.removeOnScrollListener(it) }


        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)


                if (dy <= 0) return

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return


                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                val distanceFromEnd = totalItemCount - lastVisibleItem - 1


                if (distanceFromEnd <= scrollThreshold && !isLoading && hasMoreData) {

                    scrollJob?.cancel()
                    scrollJob = coroutineScope.launch {
                        delay(300)
                        if (!isLoading && hasMoreData) {
                            loadNextPage()
                        }
                    }
                }
            }
        }


        recyclerView.addOnScrollListener(scrollListener!!)
    }



    private fun enforceItemLimit() {
        if (loadedItems.size > maxCachedItems) {
            val itemsToRemove = loadedItems.size - maxCachedItems


            repeat(itemsToRemove) {
                if (loadedItems.isNotEmpty()) {
                    loadedItems.removeAt(0)
                }
            }


            currentPage = (loadedItems.size / pageSize)
        }
    }



    private fun handleError(error: Throwable, currentItems: List<T>) {
        val paginationError = when {
            error is IOException -> PaginationError.NetworkError(error.message ?: "Network error")
            error.message?.contains("unauthorized", ignoreCase = true) == true ->
                PaginationError.ApiError("AUTH_ERROR", "Authentication failed")
            error.message?.contains("serialization", ignoreCase = true) == true ->
                PaginationError.DataError("Failed to parse data")
            error.message?.contains("no more data", ignoreCase = true) == true ->
                PaginationError.NoMoreData
            else -> PaginationError.NetworkError(error.message ?: "Unknown error")
        }


        val userMessage = getUserFriendlyMessage(paginationError)


        _paginationState.value = PaginationState.Error(
            message = userMessage,
            currentItems = currentItems
        )


        onError(userMessage)
    }



    private fun getUserFriendlyMessage(error: PaginationError): String {
        return when (error) {
            is PaginationError.NetworkError -> "Check your internet connection and try again"
            is PaginationError.ApiError -> "Unable to load data. Please try again later"
            is PaginationError.DataError -> "Something went wrong. Please try again"
            PaginationError.NoMoreData -> "No more items to load"
        }
    }



    fun reset() {

        loadJob?.cancel()
        scrollJob?.cancel()


        currentPage = 0
        isLoading = false
        hasMoreData = true
        loadedItems.clear()


        _paginationState.value = PaginationState.Initial
    }



    fun detachFromRecyclerView(recyclerView: RecyclerView) {
        scrollListener?.let {
            recyclerView.removeOnScrollListener(it)
            scrollListener = null
        }


        loadJob?.cancel()
        scrollJob?.cancel()
    }



    fun getCurrentItems(): List<T> {
        return loadedItems.toList()
    }



    fun isAtEnd(): Boolean {
        return !hasMoreData
    }



    private fun logMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val itemCount = loadedItems.size

        android.util.Log.d("PaginationManager",
            "Memory usage: ${usedMemory}MB / ${maxMemory}MB | Items loaded: $itemCount")


        if (usedMemory > maxMemory * 0.8) {
            android.util.Log.w("PaginationManager",
                "High memory usage detected: ${usedMemory}MB / ${maxMemory}MB")
        }
    }



    fun getPerformanceMetrics(): Map<String, Any> {
        return mapOf(
            "total_loads" to loadCount,
            "total_errors" to errorCount,
            "average_load_time_ms" to if (loadCount > 0) totalLoadTime / loadCount else 0,
            "items_loaded" to loadedItems.size,
            "current_page" to currentPage,
            "error_rate" to if (loadCount > 0) errorCount.toFloat() / loadCount else 0f
        )
    }
}
