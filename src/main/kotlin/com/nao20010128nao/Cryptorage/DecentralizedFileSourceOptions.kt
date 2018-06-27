package com.nao20010128nao.Cryptorage

import java.math.BigInteger

data class DecentralizedFileSourceOptions(
        val ethRemote: String = "https://ropsten.infura.io/7WqSusm77r0qpoOZQ5bD",
        val ipfsRemote: String = "/ip4/ipfs.infura.io/tcp/80",
        val contractAddress: String,
        val pubOrPriv: BigInteger,
        val isPriv: Boolean
) {
}