package org.jetbrains.bazel.protobuf

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindProvider

internal class ProtobufTargetKindProvider : TargetKindProvider {
  override val targetKinds: Set<TargetKind>
    get() = setOf(
      TargetKind("proto_library", setOf(/*LanguageClass.JAVA, ??? */ ProtobufLanguageClass.PROTOBUF), RuleType.LIBRARY),
    )
}
