package org.jetbrains.bazel.protobuf

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LanguageClassProvider

internal object ProtobufLanguageClass {
  val PROTOBUF = LanguageClass("protobuf", setOf("proto", "protodevel"))
}

internal class ProtobufLanguageClassProvider: LanguageClassProvider {
  override val languages: List<LanguageClass>
    get() = listOf(ProtobufLanguageClass.PROTOBUF)
}
