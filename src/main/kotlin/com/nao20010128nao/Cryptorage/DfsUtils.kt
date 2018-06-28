package com.nao20010128nao.Cryptorage

import io.ipfs.multiaddr.MultiAddress

internal val gwei = 1_000_000_000.toBigInteger()
internal val defaultGasLimit = 4500000.toBigInteger()

private class CrazyMultiAddress(val str: String) : MultiAddress(str) {
    override fun toString(): String = str
}

internal fun String.toCrazyMultiAddress(): MultiAddress = CrazyMultiAddress(this)
