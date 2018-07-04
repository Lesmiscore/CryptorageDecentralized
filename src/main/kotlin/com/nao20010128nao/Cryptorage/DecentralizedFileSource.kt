package com.nao20010128nao.Cryptorage

import com.google.common.base.Optional
import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.internal.contract.FileSourceContract
import com.nao20010128nao.Cryptorage.internal.file.FileSource
import io.ipfs.api.IPFS
import io.ipfs.api.NamedStreamable
import io.ipfs.multihash.Multihash
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jService
import org.web3j.protocol.http.HttpService
import org.web3j.tx.exceptions.ContractCallException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class DecentralizedFileSource(private val options: DecentralizedFileSourceOptions) : FileSource {
    private val web3j: Web3j = try {
        Web3j.build(HttpService(options.ethRemote))
    } catch (e: Throwable) {
        Class.forName("org.web3j.protocol.Web3jFactory").getMethod("build", Web3jService::class.java).invoke(null, HttpService(options.ethRemote))
    } as Web3j
    private val ipfs: IPFS = IPFS(options.ipfsRemote.toCrazyMultiAddress())
    private val keyPair = ECKeyPair.create(options.privateKey)
    private val contract = FileSourceContract.load(
            options.contractAddress,
            web3j,
            Credentials.create(keyPair),
            options.gasPrice,
            options.gasLimit
    )
    private val contractVersion: FileSourceContractSpecs

    private var closed = false
    private var listCache: List<String> = invalidatedList()
    private var removePending: List<String> = invalidatedList()
    private var addPending: Map<String, String> = invalidatedMap()

    init {
        // we check for its life
        require(contract.isAlive.send().signum() > 0)
        contractVersion = FileSourceContractSpecs.values()[contract.version.send().toInt()]
        require(contractVersion != FileSourceContractSpecs.NON_EXISTENT)
    }

    override val isReadOnly: Boolean = false

    override fun close() {
        options.ethScheduler.waitForAll()
    }

    override fun commit() {
        sendOrNothing(Action.COMMIT)
    }

    override fun delete(name: String) {
        invalidating {
            if (contractVersion.hasMultipleAddDel) {
                removePending += name
                sendOrNothing(Action.REMOVE)
            } else {
                options.ethScheduler.execute {
                    contract.removeFile(name).send()
                }
            }
        }
    }

    override fun list(): Array<String> = notClosed {
        if (listCache == InvalidatedList) {
            // terminate each by BEL
            listCache = try {
                contract.getFileListCombined(byteArrayOf(7)).send().split(7.toChar()).dropLastWhile { it.isEmpty() }
            } catch (e: ContractCallException) {
                if (e.message == "Empty value (0x) returned from contract") {
                    emptyList()
                } else {
                    throw e
                }
            }
        }
        listCache.toTypedArray()
    }

    override fun open(name: String, offset: Int): ByteSource = object : ByteSource() {
        val ipfsAddress by lazy { contract.getFile(name).send()!! }
        val ipfsResult by lazy { ipfs.cat(Multihash.fromBase58(ipfsAddress))!! }
        override fun openStream(): InputStream = ByteArrayInputStream(ipfsResult, offset, ipfsResult.size - offset)
        override fun sizeIfKnown(): Optional<Long> = try {
            Optional.of(ipfsResult.size.toLong() - offset)
        } catch (e: Throwable) {
            Optional.absent()
        }
    }

    override fun put(name: String): ByteSink = invalidating {
        object : ByteSink() {
            override fun openStream(): OutputStream = object : ByteArrayOutputStream() {
                override fun close() {
                    val whatToSend = ipfs.add(NamedStreamable.ByteArrayWrapper(name, toByteArray()))[0].hash.toBase58()
                    if (contractVersion.hasMultipleAddDel) {
                        addPending = addPending.setValue(name, whatToSend)
                        sendOrNothing(Action.SET_FILE)
                    } else {
                        options.ethScheduler.execute {
                            contract.setFile(name, whatToSend).send()
                        }
                    }
                }
            }
        }
    }

    private fun sendOrNothing(action: Action) {
        if (!contractVersion.hasMultipleAddDel) {
            return
        }


        when (action) {
            Action.SET_FILE -> {
                // flush remove
                if (removePending.isNotEmpty()) {
                    flushRemovePending()
                }
                // flush adds if overflow
                if (addPending.size > 10) {
                    flushAddPending()
                }
            }
            Action.REMOVE -> {
                // flush adds
                if (addPending.isNotEmpty()) {
                    flushAddPending()
                }
                // flush remove if overflow
                if (removePending.size > 10) {
                    flushRemovePending()
                }
            }
            Action.COMMIT -> {
                flushAddPending()
                flushRemovePending()
            }
        }
    }

    private fun flushRemovePending() {
        val joined = removePending.joinToString("")
        val split = generateSequence { randomHex() }.first { it !in joined }
        val toSend = removePending.joinToString(split)
        options.ethScheduler.execute {
            contract.removeFilesMultiple(toSend, split).send()
        }
        removePending = invalidatedList()
    }

    private fun flushAddPending() {
        val order = addPending.entries.toList()
        val keyJoined = order.joinToString("") { it.key }
        val valueJoined = order.joinToString("") { it.value }
        val compSplit = generateSequence { randomHex() }.first { it !in keyJoined && it !in valueJoined }

        val toSendKey = order.joinToString(compSplit) { it.key }
        val toSendValue = order.joinToString(compSplit) { it.value }
        val keyValueJoined = toSendKey + toSendValue
        val kvSplit = generateSequence { randomHex() }.first { it !in keyValueJoined }
        val toSendFinal = toSendKey + kvSplit + toSendValue

        options.ethScheduler.execute {
            contract.setFilesMultiple(toSendFinal, kvSplit, compSplit).send()
        }
        addPending = invalidatedMap()
    }

    fun explode() {
        options.ethScheduler.execute {
            contract.explode().send()
        }
        close()
    }

    private inline fun <T> notClosed(f: () -> T): T {
        require(!closed)
        return f()
    }

    private inline fun <T> invalidating(f: () -> T): T = notClosed {
        listCache = InvalidatedList
        f()
    }

    private enum class Action {
        SET_FILE, REMOVE, COMMIT
    }
}