package org.jetbrains.bsp.bazel.server.sync.languages.proto

import ch.epfl.scala.bsp4j.BuildTarget
import org.jetbrains.bsp.bazel.info.BspTargetInfo
import org.jetbrains.bsp.bazel.server.model.Language
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.ProtoBuildTarget

class ProtoLanguagePlugin(private val bazelPathsResolver: BazelPathsResolver) : LanguagePlugin<ProtoModule>() {
  override fun applyModuleData(
    moduleData: ProtoModule,
    buildTarget: BuildTarget
  ) {
    val protoBuildTarget =
      with(moduleData) {
        ProtoBuildTarget(
          sources = sources,
          ruleKind = ruleKind,
        )
      }
    buildTarget.dataKind = "proto"
    buildTarget.data = protoBuildTarget
  }

  override fun resolveModule(targetInfo: BspTargetInfo.TargetInfo): ProtoModule? {
    val kind = targetInfo.kind
    if (!Language.PROTO.targetKinds.contains(kind)) return null
    return ProtoModule(
      sources = targetInfo.sourcesList.mapNotNull { bazelPathsResolver.resolveUri(it) },
      ruleKind = kind,
    )
  }
}
