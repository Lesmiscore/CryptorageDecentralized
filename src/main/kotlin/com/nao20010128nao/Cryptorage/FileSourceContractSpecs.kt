package com.nao20010128nao.Cryptorage

enum class FileSourceContractSpecs(val hasMultipleAddDel: Boolean = false) {
    NON_EXISTENT, V1(false), V2(true)
}