package com.nao20010128nao.Cryptorage

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.internal.contract.FileSourceContract
import com.nao20010128nao.Cryptorage.internal.file.FileSource
import io.ipfs.api.IPFS
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

class DecentralizedFileSource(private val options: DecentralizedFileSourceOptions) : FileSource {
    private val web3j: Web3j = Web3j.build(HttpService(options.ethRemote))
    private val ipfs: IPFS = IPFS(options.ipfsRemote)
    private val keyPair = ECKeyPair.create(options.privateKey)
    private val contract = FileSourceContract.load(
            options.contractAddress,
            web3j,
            Credentials.create(keyPair),
            options.gasPrice,
            options.gasLimit
    )

    private var closed = false
    private var hasChanged: Boolean = true
    private var listCache: List<String> = emptyList()

    override val isReadOnly: Boolean = false

    override fun close() {
    }

    override fun commit() {
    }

    override fun delete(name: String) {
        invalidating {
            contract.removeFile(name).send()
        }
    }

    override fun list(): Array<String> {
        if (hasChanged) {
            // terminate each by BEL
            listCache = contract.getFileListCombined(byteArrayOf(7)).send().split(7.toChar())
        }
        hasChanged = false
        return listCache.toTypedArray()
    }

    override fun open(name: String, offset: Int): ByteSource {
        TODO("not implemented")
    }

    override fun put(name: String): ByteSink {
        TODO("not implemented")
    }

    private inline fun <T> notClosed(f: () -> T): T {
        require(!closed)
        return f()
    }

    private inline fun <T> invalidating(f: () -> T): T = notClosed {
        hasChanged = true
        f()
    }
}