package com.twilio.conversations.app

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.paging.PositionalDataSource

class ItemDataSource<T>(private val items: List<T>) : PositionalDataSource<T>() {

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) {
        val endPosition = (params.startPosition + params.loadSize).coerceAtMost(items.size)
        val startPosition = params.startPosition.coerceAtMost(endPosition)
        callback.onResult(items.subList(startPosition, endPosition))
    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<T>) {
        val endPosition = params.requestedLoadSize.coerceAtMost(items.size)
        val startPosition = params.requestedStartPosition.coerceAtMost(endPosition)
        // Note params.pageSize is ignored at the moment as it's not relevant to the current usage
        // of this class
        callback.onResult(
            items.subList(startPosition, endPosition),
            startPosition,
            endPosition - startPosition
        )
    }

    companion object {
        fun <T> factory(list: List<T>) = object : DataSource.Factory<Int, T>() {
            override fun create(): DataSource<Int, T> {
                return ItemDataSource(list)
            }
        }
    }
}

fun <T> List<T>.asPagedList() = PagedList.Builder(ItemDataSource(this), size.coerceAtLeast(1))
    .setNotifyExecutor(ArchTaskExecutor.getMainThreadExecutor())
    .setFetchExecutor(ArchTaskExecutor.getIOThreadExecutor())
    .build()
