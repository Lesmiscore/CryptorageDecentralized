package com.nao20010128nao.Cryptorage

import com.nao20010128nao.Cryptorage.runner.DirectTaskScheduler
import com.nao20010128nao.Cryptorage.runner.SimpleAsyncTaskScheduler
import com.nao20010128nao.Cryptorage.runner.TaskScheduler
import okhttp3.OkHttpClient
import sun.java2d.pipe.SpanShapeRenderer
import java.math.BigInteger

data class DecentralizedFileSourceOptions(
        val ethRemote: String = "https://ropsten.infura.io/7WqSusm77r0qpoOZQ5bD",
        val ipfsRemote: String = "/ip6/ipfs.infura.io/tcp/5001",
        val contractAddress: String,
        val privateKey: BigInteger,
        val gasPrice: BigInteger = gwei,
        val gasLimit: BigInteger = defaultGasLimit,
        val ethScheduler: TaskScheduler = DirectTaskScheduler,
        val ipfsScheduler: TaskScheduler = SimpleAsyncTaskScheduler(),
        val httpClient: OkHttpClient = OkHttpClient.Builder().build()!!,
        val ethSleepDuration: Int = 15 * 1000,
        val ethSleepAttempts: Int = 40
)
