package org.jetbrains.bazel.server.sync.languages

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.server.model.Module
import org.jetbrains.bazel.server.sync.languages.android.AndroidLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.cpp.CppLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.cpp.CppModule
import org.jetbrains.bazel.server.sync.languages.go.GoLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.kotlin.KotlinLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.python.PythonLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.scala.ScalaLanguagePlugin
import org.jetbrains.bazel.server.sync.languages.thrift.ThriftLanguagePlugin
import org.jetbrains.bazel.workspacecontext.WorkspaceContext

class LanguagePluginsService(
  val scalaLanguagePlugin: ScalaLanguagePlugin,
  private val javaLanguagePlugin: JavaLanguagePlugin,
  val cppLanguagePlugin: CppLanguagePlugin,
  private val kotlinLanguagePlugin: KotlinLanguagePlugin,
  private val thriftLanguagePlugin: ThriftLanguagePlugin,
  val pythonLanguagePlugin: PythonLanguagePlugin,
  private val androidLanguagePlugin: AndroidLanguagePlugin,
  val goLanguagePlugin: GoLanguagePlugin,
) {
  private val emptyLanguagePlugin: EmptyLanguagePlugin = EmptyLanguagePlugin()

  fun prepareSync(targetInfos: Sequence<BspTargetInfo.TargetInfo>, workspaceContext: WorkspaceContext) {
    scalaLanguagePlugin.prepareSync(targetInfos, workspaceContext)
    javaLanguagePlugin.prepareSync(targetInfos, workspaceContext)
    cppLanguagePlugin.prepareSync(targetInfos, workspaceContext)
    thriftLanguagePlugin.prepareSync(targetInfos, workspaceContext)
    pythonLanguagePlugin.prepareSync(targetInfos, workspaceContext)
    androidLanguagePlugin.prepareSync(targetInfos, workspaceContext)
    goLanguagePlugin.prepareSync(targetInfos, workspaceContext)
  }

  fun getPlugin(languages: Set<LanguageClass>): LanguagePlugin<*> =
    when {
      languages.contains(LanguageClass.ANDROID) -> androidLanguagePlugin
      languages.contains(LanguageClass.KOTLIN) -> kotlinLanguagePlugin
      languages.contains(LanguageClass.SCALA) -> scalaLanguagePlugin
      languages.contains(LanguageClass.JAVA) -> javaLanguagePlugin
      languages.contains(LanguageClass.C) -> cppLanguagePlugin
      languages.contains(LanguageClass.THRIFT) -> thriftLanguagePlugin
      languages.contains(LanguageClass.PYTHON) -> pythonLanguagePlugin
      languages.contains(LanguageClass.GO) -> goLanguagePlugin
      else -> emptyLanguagePlugin
    }

  fun extractCppModule(module: Module): CppModule? =
    module.languageData?.let {
      when (it) {
        is CppModule -> it
        else -> null
      }
    }
}
