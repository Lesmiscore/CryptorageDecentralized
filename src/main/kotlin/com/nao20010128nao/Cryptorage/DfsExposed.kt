package com.nao20010128nao.Cryptorage

import com.nao20010128nao.Cryptorage.internal.contract.FileSourceContract
import com.nao20010128nao.Cryptorage.internal.contract.FileSourceContractLight
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

fun DecentralizedFileSourceOptions.deploy(light: Boolean = false): Pair<DecentralizedFileSourceOptions, DecentralizedFileSource> {
    val web3j: Web3j = obtainWeb3j(HttpService(ethRemote))
    val keyPair = ECKeyPair.create(privateKey)
    val cred = Credentials.create(keyPair)
    val contract = (if (light)
        FileSourceContractLight.deploy(web3j, cred, gasPrice, gasLimit).send()
    else
        FileSourceContract.deploy(web3j, cred, gasPrice, gasLimit).send()).contractAddress
    val updated = copy(contractAddress = contract)
    return updated to DecentralizedFileSource(updated)
}

fun DecentralizedFileSource.transferTo(dest: DecentralizedFileSource) {
    getAllIpfsFiles().forEach { name, ipfs ->
        dest.setIpfsFileDirectly(name, ipfs)
    }
}
