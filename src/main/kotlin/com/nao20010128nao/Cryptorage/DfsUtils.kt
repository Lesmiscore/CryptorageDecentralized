@file:Suppress("NOTHING_TO_INLINE")

package com.nao20010128nao.Cryptorage

import io.ipfs.multiaddr.MultiAddress
import java.security.SecureRandom

internal val gwei = 1_000_000_000.toBigInteger()
internal val defaultGasLimit = 4500000.toBigInteger()

private class CrazyMultiAddress(val str: String) : MultiAddress(str) {
    override fun toString(): String = str
}

internal fun String.toCrazyMultiAddress(): MultiAddress = CrazyMultiAddress(this)

internal object InvalidatedList : List<Nothing> by emptyList()
internal object InvalidatedMap : Map<Nothing, Nothing> by emptyMap()

internal inline fun <E> invalidatedList(): List<E> = InvalidatedList
internal inline fun <K, V> invalidatedMap(): Map<K, V> = InvalidatedMap as Map<K, V>

internal inline fun randomBytes(size: Int = 16): ByteArray = ByteArray(size).also {
    SecureRandom().nextBytes(it)
}

internal inline fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
internal inline fun randomHex(size: Int = 16): String = randomBytes(size).toHex()
internal inline fun <K, V> Map<K, V>.setValue(key: K, value: V): Map<K, V> = toMutableMap().also {
    it[key] = value
}
