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
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.ceil
import kotlin.math.min

class DecentralizedFileSource(private val options: DecentralizedFileSourceOptions) : FileSource {
    private val web3j: Web3j = obtainWeb3j(HttpService(options.ethRemote, options.httpClient, false))
    private val ipfs: IPFS = IPFS(options.ipfsRemote.toCrazyMultiAddress())
    private val keyPair = ECKeyPair.create(options.privateKey)
    private val contract = FileSourceContract.load(
            options.contractAddress,
            web3j,
            RawTransactionManager(
                    web3j,
                    Credentials.create(keyPair),
                    options.ethSleepAttempts,
                    options.ethSleepDuration
            ),
            options.gasPrice,
            options.gasLimit
    )
    private val contractVersion: FileSourceContractSpecs

    private var closed = false
    private var listCache: List<String> = invalidatedList()
    private var removePending: List<String> = invalidatedList()
    private var addPending: Map<String, String> = invalidatedMap()
    private var rawListInContract: List<String> = invalidatedList()

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
            if (contractVersion.hasMultipleAddDel || contractVersion.hasRangeListAndRemoveAt) {
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
                contract.getFileListCombined(bel()).send().split(belChar).dropLastWhile { it.isEmpty() }
            } catch (e: Throwable) {
                if (contractVersion.hasRangeListAndRemoveAt) {
                    val length = contract.fileListLength.send().toInt()
                    val split = ceil(length / 200.0).toInt()
                    (0 until split).map {
                        contract.getFileListRanged(bel(), (it * 200).toBigInteger(), min((it + 1) * 200 + 1, length).toBigInteger()).send()
                    }.flatMap {
                        it.split(belChar)
                    }.also {
                        rawListInContract = it
                    }
                } else {
                    emptyList()
                }
                /*try {
                    (0 until contract.fileListLength.send().intValueExact()).map { contract.getFileList(it.toBigInteger()).send() }
                } catch (e: ContractCallException) {
                    emptyList()
                }*/
            }
        }
        listCache.distinct().toTypedArray()
    }

    override fun has(name: String): Boolean = super.has(name) || contract.getFile("manifest").send().isNotEmpty()

    override fun open(name: String, offset: Int): ByteSource = object : ByteSource() {
        val ipfsAddress by lazy { getCorrespondingIpfsFile(name) }
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
                    setIpfsFileDirectly(name, whatToSend)
                }
            }
        }
    }

    internal fun getCorrespondingIpfsFile(name: String): String = (addPending[name] ?: contract.getFile(name).send())!!

    internal fun setIpfsFileDirectly(name: String, whatToSend: String) {
        if (contractVersion.hasMultipleAddDel) {
            addPending = addPending.setValue(name, whatToSend)
            sendOrNothing(Action.SET_FILE)
        } else {
            options.ethScheduler.execute {
                contract.setFile(name, whatToSend).send()
            }
        }
    }

    internal fun getAllIpfsFiles(): Map<String, String> = list().map { it to getCorrespondingIpfsFile(it) }.toMap()

    private fun sendOrNothing(action: Action) {
        if (!(contractVersion.hasMultipleAddDel || contractVersion.hasRangeListAndRemoveAt)) {
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
                if (addPending.isNotEmpty()) {
                    flushAddPending()
                }
                if (removePending.isNotEmpty()) {
                    flushRemovePending()
                }
            }
        }
    }

    private fun flushRemovePending() {
        val rmPending = removePending.toList()
        removePending = invalidatedList()
        val joined = rmPending.joinToString("")
        val split = generateSequence { randomHex(3) }.first { it !in joined }
        val toSend = rmPending.joinToString(split)
        options.ethScheduler.execute {
            try {
                contract.removeFilesMultiple(toSend, split).send()
            } catch (e: Throwable) {
                rmPending.forEach {
                    try {
                        if (contractVersion.hasRangeListAndRemoveAt)
                            contract.removeFileAt(rawListInContract.indexOf(it).toBigInteger(), it).send()
                        else
                            error("Placeholder")
                    } catch (e: Throwable) {
                        contract.removeFile(it).send()
                    }
                    rawListInContract -= it
                }
            }
        }
    }

    private fun flushAddPending() {
        val order = addPending.entries.toList()
        addPending = invalidatedMap()
        val keyJoined = order.joinToString("") { it.key }
        val valueJoined = order.joinToString("") { it.value }
        val compSplit = generateSequence { randomHex(3) }.first { it !in keyJoined && it !in valueJoined }

        val toSendKey = order.joinToString(compSplit) { it.key }
        val toSendValue = order.joinToString(compSplit) { it.value }
        val keyValueJoined = toSendKey + toSendValue
        val kvSplit = generateSequence { randomHex(3) }.first { it !in keyValueJoined }
        val toSendFinal = toSendKey + kvSplit + toSendValue

        options.ethScheduler.execute {
            try {
                contract.setFilesMultiple(toSendFinal, kvSplit, compSplit).send()
            } catch (e: Throwable) {
                order.forEach {
                    try {
                        contract.setFile(it.key, it.value).send()
                    } catch (e: Throwable) {
                    }
                }
            }
        }
    }

    fun explode() {
        list().forEach {
            delete(it)
        }
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