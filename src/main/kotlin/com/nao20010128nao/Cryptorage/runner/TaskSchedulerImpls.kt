package com.nao20010128nao.Cryptorage.runner

import com.nao20010128nao.Cryptorage.lazyFuture
import java.util.concurrent.*

open class ExecutorTaskScheduler(private val executor: ExecutorService) : TaskScheduler {
    override fun execute(func: () -> Unit) {
        executor.execute {
            func()
        }
    }

    override fun <T> executeWithValue(func: () -> T): Future<T> = executor.submit(Callable { func() })

    override fun waitForAll() {
        executor.shutdown()
        executor.awaitTermination(1000, TimeUnit.DAYS)
    }
}

object DirectTaskScheduler : TaskScheduler {
    override fun execute(func: () -> Unit) {
        func()
    }

    override fun <T> executeWithValue(func: () -> T): Future<T> = lazyFuture { func() }

    override fun waitForAll() {
    }
}

class SimpleAsyncTaskScheduler : ExecutorTaskScheduler(Executors.newSingleThreadExecutor())
