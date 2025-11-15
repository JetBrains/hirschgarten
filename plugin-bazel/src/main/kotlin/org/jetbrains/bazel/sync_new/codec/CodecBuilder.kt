package org.jetbrains.bazel.sync_new.codec

interface CodecBuilder {
}

fun codecBuilderOf(): CodecBuilder = object : CodecBuilder {}
