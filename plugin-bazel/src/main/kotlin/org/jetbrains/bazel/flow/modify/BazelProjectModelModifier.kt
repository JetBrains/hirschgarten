package org.jetbrains.bazel.flow.modify

import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.JavaProjectModelModifier
import com.intellij.openapi.roots.libraries.Library
import com.intellij.pom.java.LanguageLevel
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.ui.widgets.findBuildFile
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.sync.scope.PartialProjectSync
import org.jetbrains.plugins.bsp.sync.task.ProjectSyncTask
import org.jetbrains.plugins.bsp.target.targetUtils
import org.jetbrains.plugins.bsp.ui.notifications.BspBalloonNotifier

private val log = logger<BazelProjectModelModifier>()

class BazelProjectModelModifier : JavaProjectModelModifier() {
  override fun addModuleDependency(
    from: Module,
    to: Module,
    scope: DependencyScope,
    exported: Boolean,
  ): Promise<Void>? {
    if (!tryAddingModuleDependencyToBuildFile(from, to)) {
      from.jumpToBuildFile()
      notifyAutomaticDependencyAdditionFailure()
    }

    return resolvedPromise()
  }

  private fun tryAddingModuleDependencyToBuildFile(from: Module, to: Module): Boolean {
    val fromBuildTargetInfo = from.project.targetUtils.getBuildTargetInfoForModule(from) ?: return false
    val toBuildTargetInfo = to.project.targetUtils.getBuildTargetInfoForModule(to) ?: return false
    val targetBuildFile = findBuildFile(from.project, fromBuildTargetInfo) as? StarlarkFile ?: return false
    val targetRuleLabel = Label.parseOrNull(fromBuildTargetInfo.id.uri) ?: return false
    val ruleTarget = targetBuildFile.findRuleTarget(targetRuleLabel.targetName) ?: return false
    val depsList =
      ruleTarget
        .getArgumentList()
        ?.getDepsArgument()
        ?.children
        ?.first { it is StarlarkListLiteralExpression } as? StarlarkListLiteralExpression
        ?: return false
    var insertSuccessful = false
    val targetStringToInsert = toBuildTargetInfo.buildTargetName.trimStart('@')
    try {
      WriteCommandAction.runWriteCommandAction(from.project) {
        depsList.insertString(targetStringToInsert)
        insertSuccessful = true
      }
    } catch (e: Exception) {
      log.warn("Failed to insert target $targetStringToInsert as a dependency for target $targetRuleLabel", e)
    }

    val syncScope = PartialProjectSync(targetsToSync = listOf(fromBuildTargetInfo.id))
    if (insertSuccessful) {
      BspCoroutineService.getInstance(from.project).start {
        ProjectSyncTask(from.project).sync(syncScope = syncScope, buildProject = false)
      }
      return true
    }
    return false
  }

  private fun notifyAutomaticDependencyAdditionFailure() {
    BspBalloonNotifier.warn(
      BazelPluginBundle.message("balloon.add.target.dependency.to.build.file.failed.title"),
      BazelPluginBundle.message("balloon.add.target.dependency.to.build.file.failed.message"),
    )
  }

  override fun addLibraryDependency(
    from: Module,
    library: Library,
    scope: DependencyScope,
    exported: Boolean,
  ): Promise<Void>? {
    from.jumpToBuildFile()
    return resolvedPromise()
  }

  override fun addExternalLibraryDependency(
    modules: Collection<Module>,
    descriptor: ExternalLibraryDescriptor,
    scope: DependencyScope,
  ): Promise<Void>? {
    modules.firstOrNull()?.jumpToBuildFile()
    return resolvedPromise()
  }

  override fun changeLanguageLevel(module: Module, level: LanguageLevel): Promise<Void>? {
    module.jumpToBuildFile()
    return resolvedPromise()
  }

  private fun Module.jumpToBuildFile() {
    val buildTargetInfo = project.targetUtils.getBuildTargetInfoForModule(this) ?: return
    val buildFile = findBuildFile(project, buildTargetInfo) ?: return
    EditorHelper.openInEditor(buildFile, true, true)
  }
}
