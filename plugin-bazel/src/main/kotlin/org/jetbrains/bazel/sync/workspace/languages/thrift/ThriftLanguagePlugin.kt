package org.jetbrains.bazel.sync.workspace.languages.thrift

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bsp.protocol.VoidBuildTarget

class ThriftLanguagePlugin : LanguagePlugin<ThriftModule, VoidBuildTarget> {
  // override fun dependencySources(targetInfo: BspTargetInfo.TargetInfo, dependencyGraph: DependencyGraph): Set<Path> {
  //  val transitiveSourceDeps =
  //    dependencyGraph
  //      .transitiveDependenciesWithoutRootTargets(targetInfo.label())
  //      .filter(::isThriftLibrary)
  //      .flatMap(BspTargetInfo.TargetInfo::getSourcesList)
  //      .map(bazelPathsResolver::resolve)
  //      .toHashSet()
  //
  //  val directSourceDeps = sourcesFromJvmTargetInfo(targetInfo)
  //
  //  return transitiveSourceDeps + directSourceDeps
  // }
  //
  // private fun sourcesFromJvmTargetInfo(targetInfo: BspTargetInfo.TargetInfo): HashSet<Path> =
  //  if (targetInfo.hasJvmTargetInfo()) {
  //    targetInfo
  //      .jvmTargetInfo
  //      .jarsList
  //      .flatMap { it.sourceJarsList }
  //      .map(bazelPathsResolver::resolve)
  //      .toHashSet()
  //  } else {
  //    HashSet()
  //  }

  // private fun isThriftLibrary(target: BspTargetInfo.TargetInfo): Boolean = target.kind == THRIFT_LIBRARY_RULE_NAME
  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.THRIFT)

  override fun createIntermediateModel(targetInfo: BspTargetInfo.TargetInfo): ThriftModule = ThriftModule()

  override fun createBuildTargetData(context: LanguagePluginContext, ir: ThriftModule): VoidBuildTarget = VoidBuildTarget

  // override fun applyModuleData(module: Module, moduleData: ThriftModule, buildTarget: RawBuildTarget) {}

  companion object {
    // private const val THRIFT_LIBRARY_RULE_NAME = "thrift_library"
  }
}
