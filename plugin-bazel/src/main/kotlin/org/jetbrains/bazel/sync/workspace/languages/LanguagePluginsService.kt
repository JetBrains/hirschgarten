package org.jetbrains.bazel.sync.workspace.languages

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.sync.workspace.languages.go.GoLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.kotlin.KotlinLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.python.PythonLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.scala.ScalaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.thrift.ThriftLanguagePlugin
import org.jetbrains.bazel.sync.workspace.model.Module
import org.jetbrains.bazel.workspacecontext.WorkspaceContext

class LanguagePluginsService(
  val scalaLanguagePlugin: ScalaLanguagePlugin,
  private val javaLanguagePlugin: JavaLanguagePlugin,
  private val kotlinLanguagePlugin: KotlinLanguagePlugin,
  private val thriftLanguagePlugin: ThriftLanguagePlugin,
  val pythonLanguagePlugin: PythonLanguagePlugin,
  val goLanguagePlugin: GoLanguagePlugin,
) {
  private val emptyLanguagePlugin: EmptyLanguagePlugin = EmptyLanguagePlugin()

  fun prepareSync(targetInfos: Sequence<BspTargetInfo.TargetInfo>, workspaceContext: WorkspaceContext) {
    scalaLanguagePlugin.prepareSync(targetInfos, workspaceContext)
    javaLanguagePlugin.prepareSync(targetInfos, workspaceContext)
    thriftLanguagePlugin.prepareSync(targetInfos, workspaceContext)
    pythonLanguagePlugin.prepareSync(targetInfos, workspaceContext)
    goLanguagePlugin.prepareSync(targetInfos, workspaceContext)
  }

  fun getPlugin(languages: Set<LanguageClass>): LanguagePlugin<*> =
    when {
      languages.contains(LanguageClass.KOTLIN) -> kotlinLanguagePlugin
      languages.contains(LanguageClass.SCALA) -> scalaLanguagePlugin
      languages.contains(LanguageClass.JAVA) -> javaLanguagePlugin
      languages.contains(LanguageClass.THRIFT) -> thriftLanguagePlugin
      languages.contains(LanguageClass.PYTHON) -> pythonLanguagePlugin
      languages.contains(LanguageClass.GO) -> goLanguagePlugin
      else -> emptyLanguagePlugin
    }
}
