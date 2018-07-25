package com.nao20010128nao.Cryptorage

import com.nao20010128nao.Cryptorage.runner.TaskScheduler
import okhttp3.OkHttpClient
import java.math.BigInteger

class DecentralizedFileSourceBuilder(val options: DecentralizedFileSourceOptions) {
    constructor(
            contractAddress: String,
            privateKey: BigInteger
    ) : this(DecentralizedFileSourceOptions(contractAddress = contractAddress, privateKey = privateKey))

    fun ethRemote(value: String) = DecentralizedFileSourceBuilder(options.copy(ethRemote = value))
    fun ipfsRemote(value: String) = DecentralizedFileSourceBuilder(options.copy(ipfsRemote = value))
    fun contractAddress(value: String) = DecentralizedFileSourceBuilder(options.copy(contractAddress = value))
    fun privateKey(value: BigInteger) = DecentralizedFileSourceBuilder(options.copy(privateKey = value))
    fun gasPrice(value: BigInteger) = DecentralizedFileSourceBuilder(options.copy(gasPrice = value))
    fun gasLimit(value: BigInteger) = DecentralizedFileSourceBuilder(options.copy(gasLimit = value))
    fun ethScheduler(value: TaskScheduler) = DecentralizedFileSourceBuilder(options.copy(ethScheduler = value))
    fun httpClient(value: OkHttpClient) = DecentralizedFileSourceBuilder(options.copy(httpClient = value))
    fun ethSleepDuration(value: Int) = DecentralizedFileSourceBuilder(options.copy(ethSleepDuration = value))
    fun ethSleepAttempts(value: Int) = DecentralizedFileSourceBuilder(options.copy(ethSleepAttempts = value))

    override fun equals(other: Any?): Boolean = other is DecentralizedFileSourceBuilder && other.options == options
    override fun hashCode(): Int = options.hashCode()

    fun build(): DecentralizedFileSource = DecentralizedFileSource(options)
}