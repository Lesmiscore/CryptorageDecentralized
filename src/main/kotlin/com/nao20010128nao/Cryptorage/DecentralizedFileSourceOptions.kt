package com.nao20010128nao.Cryptorage

import java.math.BigInteger

data class DecentralizedFileSourceOptions(
        val ethRemote: String = "https://ropsten.infura.io/7WqSusm77r0qpoOZQ5bD",
        val ipfsRemote: String = "/ip6/ipfs.infura.io/tcp/5001",
        val contractAddress: String,
        val privateKey: BigInteger,
        val gasPrice: BigInteger = gwei,
        val gasLimit: BigInteger = defaultGasLimit
)
