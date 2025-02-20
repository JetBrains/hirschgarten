package org.jetbrains.bazel.flow.modify

import com.intellij.application.options.CodeStyle
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingMode
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.openapi.roots.JavaProjectModelModifier
import com.intellij.openapi.roots.impl.IdeaProjectModelModifier
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.TextRange
import com.intellij.pom.java.LanguageLevel
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BspFeatureFlags
import org.jetbrains.bazel.coroutines.BspCoroutineService
import org.jetbrains.bazel.label.Apparent
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.languages.starlark.formatting.StarlarkFormattingService
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.repomapping.canonicalRepoNameToApparentName
import org.jetbrains.bazel.target.addLibraryModulePrefix
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.notifications.BspBalloonNotifier
import org.jetbrains.bazel.ui.widgets.findBuildFile
import org.jetbrains.bazel.ui.widgets.jumpToBuildFile
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

private val log = logger<BazelProjectModelModifier>()

class BazelProjectModelModifier(private val project: Project) : JavaProjectModelModifier() {
  private val ideaProjectModelModifier = IdeaProjectModelModifier(project)

  override fun addModuleDependency(
    from: Module,
    to: Module,
    scope: DependencyScope,
    exported: Boolean,
  ): Promise<Void>? {
    val labelToInsert = project.targetUtils.getTargetForModuleId(to.name)
    if (tryAddingModuleDependencyToBuildFile(from, labelToInsert)) {
      // We used to do a partial resync here, but simply modifying the project model is quicker
      ideaProjectModelModifier.addModuleDependency(from, to, scope, true)
    } else {
      from.jumpToBuildFile()
      notifyAutomaticDependencyAdditionFailure()
    }

    return resolvedPromise()
  }

  override fun addLibraryDependency(
    from: Module,
    library: Library,
    scope: DependencyScope,
    exported: Boolean,
  ): Promise<Void>? {
    val labelToInsert = library.name?.let { libraryId -> project.targetUtils.getTargetForLibraryId(libraryId) }
    if (tryAddingModuleDependencyToBuildFile(from, labelToInsert)) {
      if (BspFeatureFlags.isWrapLibrariesInsideModulesEnabled) {
        // In this case we should actually depend on the library module, not the library itself
        val libraryModuleName = checkNotNull(library.name).addLibraryModulePrefix()
        val libraryModule = ModuleManager.getInstance(project).findModuleByName(libraryModuleName)
        if (libraryModule != null) {
          ideaProjectModelModifier.addModuleDependency(from, libraryModule, scope, true)
          return resolvedPromise()
        } else {
          log.warn("Can't find library module $libraryModuleName")
        }
      }
      ideaProjectModelModifier.addLibraryDependency(from, library, scope, true)
    } else {
      from.jumpToBuildFile()
      notifyAutomaticDependencyAdditionFailure()
    }

    return resolvedPromise()
  }

  private fun tryAddingModuleDependencyToBuildFile(from: Module, labelToInsert: Label?): Boolean {
    if (labelToInsert == null) return false
    val fromBuildTargetInfo = from.project.targetUtils.getBuildTargetInfoForModule(from) ?: return false
    val targetBuildFile = findBuildFile(from.project, fromBuildTargetInfo) ?: return false
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

    try {
      WriteCommandAction.runWriteCommandAction(from.project) {
        depsList.insertString(labelToInsert.convertToApparentLabel().toShortString())
        insertSuccessful = true
      }
    } catch (e: Exception) {
      log.warn("Failed to insert target $labelToInsert as a dependency for target $targetRuleLabel", e)
    }
    if (insertSuccessful) {
      BspCoroutineService.getInstance(from.project).start {
        formatBuildFile(targetBuildFile)
      }
    }
    return insertSuccessful
  }

  private fun Label.convertToApparentLabel(): Label {
    if (this !is ResolvedLabel) return this
    if (this.repo !is Canonical) return this
    val apparentRepoName = project.canonicalRepoNameToApparentName[this.repo.repoName] ?: return this
    return this.copy(repo = Apparent(apparentRepoName))
  }

  private suspend fun formatBuildFile(buildFile: StarlarkFile) {
    val formattingService = StarlarkFormattingService()
    val textRange = TextRange.from(0, buildFile.textLength)
    val formattingContext = FormattingContext.create(buildFile, textRange, CodeStyle.getSettings(buildFile), FormattingMode.REFORMAT)
    writeAction {
      formattingService.formatDocument(buildFile.containingFile.fileDocument, listOf(textRange), formattingContext, true, true)
    }
  }

  private fun notifyAutomaticDependencyAdditionFailure() {
    BspBalloonNotifier.warn(
      BazelPluginBundle.message("balloon.add.target.dependency.to.build.file.failed.title"),
      BazelPluginBundle.message("balloon.add.target.dependency.to.build.file.failed.message"),
    )
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
    BspCoroutineService.getInstance(project).start {
      val buildTargetInfo = project.targetUtils.getBuildTargetInfoForModule(this) ?: return@start
      jumpToBuildFile(project, buildTargetInfo)
    }
  }
}
