package com.nao20010128nao.Cryptorage

import com.nao20010128nao.Cryptorage.internal.contract.FileSourceContract
import com.nao20010128nao.Cryptorage.runner.SimpleAsyncTaskScheduler
import io.ipfs.multiaddr.MultiAddress
import org.junit.Test
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

class Tests {
    val privateKey = "21414846105131974946245667778939696990523040918621528349361374646791088162864".toBigInteger()
    val contract = "0xc72c24c055e59d3c07e3f473d39ce6fb73ec9799"
    val options = DecentralizedFileSourceOptions(
            contractAddress = contract,
            privateKey = privateKey
    )

    //@Test
    fun testDeploy() {
        val options = DecentralizedFileSourceOptions(
                contractAddress = "",
                privateKey = privateKey
        )
        val w3j = Web3j.build(HttpService(options.ethRemote))
        val cred = Credentials.create(ECKeyPair.create(privateKey))
        println(FileSourceContract.deploy(w3j, cred, options.gasPrice, options.gasLimit).send().contractAddress)
    }

    //@Test
    fun testMultiaddress() {
        val ma = MultiAddress("/ip6/ipfs.infura.io/tcp/80")
        println(ma)
        println(ma.bytes.size)
        println(ma.bytes.joinToString("") { "%02x".format(it) })
    }

    @Test
    fun testSimple() {
        val fs = DecentralizedFileSource(options)
        val cryptorage = fs.withV1Encryption("Decentralized")
        println("Writing")
        cryptorage.put("Test").write("nao20010128nao".toByteArray())
        cryptorage.close()
    }

    @Test
    fun testSimple2() {
        val fs = DecentralizedFileSource(options)
        val cryptorage = fs.withV1Encryption("Decentralized")
        println("Writing")
        (1..10).forEach {
            println("Writing: $it")
            cryptorage.put("aa$it").write("Say YES for decentralized file system!".toByteArray())
        }
        cryptorage.close()
    }

    @Test
    fun testRead() {
        val fs = DecentralizedFileSource(options)
        val cryptorage = fs.withV1Encryption("Decentralized")
        println(cryptorage.list().joinToString("\n"))
        println(cryptorage.open("Test").read().toString(Charsets.UTF_8))
    }

    @Test
    fun testCompress() {
        val fs = DecentralizedFileSource(options)
        val cryptorage = fs.withV1Encryption("Decentralized")
        cryptorage.compressIfPossible(true)
    }

    @Test
    fun testAsync() {
        val fs = DecentralizedFileSource(options.copy(ethScheduler = SimpleAsyncTaskScheduler()))
        val cryptorage = fs.withV1Encryption("Decentralized")
        println("Writing")
        (1..10).forEach {
            println("Writing: $it")
            cryptorage.put("aa$it").write("Say YES for decentralized file system!".toByteArray())
        }
        cryptorage.close()
    }

    //@Test
    fun testExplosion() {
        val fs = DecentralizedFileSource(options)
        fs.explode()
    }
}