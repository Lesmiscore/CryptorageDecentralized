package com.nao20010128nao.Cryptorage.runner

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

    override fun <T> executeWithValue(func: () -> T): Future<T> = object : Future<T> {
        val funcLazy = lazy { func() }
        val funcValue by funcLazy

        override fun isDone(): Boolean = funcLazy.isInitialized()

        override fun get(): T = funcValue

        override fun get(p0: Long, p1: TimeUnit?): T = funcValue

        override fun cancel(p0: Boolean): Boolean = false

        override fun isCancelled(): Boolean = false
    }

    override fun waitForAll() {
    }
}

class SimpleAsyncTaskScheduler : ExecutorTaskScheduler(Executors.newSingleThreadExecutor())
