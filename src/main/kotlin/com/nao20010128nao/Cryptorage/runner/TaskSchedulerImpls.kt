package com.nao20010128nao.Cryptorage.runner

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

open class ExecutorTaskScheduler(private val executor: ExecutorService) : TaskScheduler {
    override fun execute(func: () -> Unit) {
        executor.execute {
            func()
        }
    }

    override fun waitForAll() {
        executor.shutdown()
        executor.awaitTermination(1000, TimeUnit.DAYS)
    }
}

object DirectTaskScheduler : TaskScheduler {
    override fun execute(func: () -> Unit) {
        func()
    }

    override fun waitForAll() {
    }
}

class SimpleAsyncTaskScheduler : ExecutorTaskScheduler(Executors.newSingleThreadExecutor())
