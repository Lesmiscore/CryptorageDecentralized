package com.nao20010128nao.Cryptorage

import com.google.common.io.ByteSink
import com.google.common.io.ByteSource
import com.nao20010128nao.Cryptorage.internal.file.FileSource

class DecentralizedFileSource(private val options: DecentralizedFileSourceOptions): FileSource {
    override val isReadOnly: Boolean = !options.isPriv

    override fun close() {
        TODO("not implemented")
    }

    override fun commit() {
        TODO("not implemented")
    }

    override fun delete(name: String) {
        TODO("not implemented")
    }

    override fun list(): Array<String> {
        TODO("not implemented")
    }

    override fun open(name: String, offset: Int): ByteSource {
        TODO("not implemented")
    }

    override fun put(name: String): ByteSink {
        TODO("not implemented")
    }
}