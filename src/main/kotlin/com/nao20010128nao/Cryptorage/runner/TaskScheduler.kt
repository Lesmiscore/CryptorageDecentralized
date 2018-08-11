package com.nao20010128nao.Cryptorage.runner

import java.util.concurrent.Future

interface TaskScheduler {
    fun execute(func: () -> Unit)
    fun <T> executeWithValue(func: () -> T): Future<T>
    fun waitForAll()
}