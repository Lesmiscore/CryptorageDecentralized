package com.nao20010128nao.Cryptorage.runner

interface TaskScheduler {
    fun execute(func: () -> Unit)
    fun waitForAll()
}