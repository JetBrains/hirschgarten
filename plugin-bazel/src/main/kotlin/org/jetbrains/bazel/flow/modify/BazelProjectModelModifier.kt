package org.jetbrains.bazel.flow.modify

import com.intellij.application.options.CodeStyle
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingMode
import com.intellij.java.library.getMavenCoordinates
import com.intellij.openapi.application.readAction
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
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.pom.java.LanguageLevel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
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
import org.jetbrains.bazel.ui.notifications.BazelBalloonNotifier
import org.jetbrains.bazel.ui.widgets.findBuildFile
import org.jetbrains.bazel.ui.widgets.jumpToBuildFile
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.await

private val log = logger<BazelProjectModelModifier>()

@OptIn(ExperimentalCoroutinesApi::class)
class BazelProjectModelModifier(private val project: Project) : JavaProjectModelModifier() {
  private val ideaProjectModelModifier = IdeaProjectModelModifier(project)

  override fun addModuleDependency(
    from: Module,
    to: Module,
    scope: DependencyScope,
    exported: Boolean,
  ): Promise<Void>? =
    asyncPromise {
      val labelToInsert = project.targetUtils.getTargetForModuleId(to.name)
      if (tryAddingModuleDependencyToBuildFile(from, labelToInsert)) {
        // We used to do a partial resync here, but simply modifying the project model is quicker
        ideaProjectModelModifier.addModuleDependency(from, to, scope, true)?.await()
      } else {
        from.jumpToBuildFile()
        notifyAutomaticDependencyAdditionFailure()
      }
    }

  override fun addLibraryDependency(
    from: Module,
    library: Library,
    scope: DependencyScope,
    exported: Boolean,
  ): Promise<Void>? =
    asyncPromise {
      val labelToInsert = library.name?.let { libraryId -> project.targetUtils.getTargetForLibraryId(libraryId) }
      if (tryAddingModuleDependencyToBuildFile(from, labelToInsert)) {
        if (BazelFeatureFlags.isWrapLibrariesInsideModulesEnabled) {
          // In this case we should actually depend on the library module, not the library itself
          val libraryModuleName = checkNotNull(library.name).addLibraryModulePrefix()
          val libraryModule = ModuleManager.getInstance(project).findModuleByName(libraryModuleName)
          if (libraryModule != null) {
            ideaProjectModelModifier.addModuleDependency(from, libraryModule, scope, true)?.await()
            return@asyncPromise
          } else {
            log.warn("Can't find library module $libraryModuleName")
          }
        }
        ideaProjectModelModifier.addLibraryDependency(from, library, scope, true)?.await()
      } else {
        from.jumpToBuildFile()
        notifyAutomaticDependencyAdditionFailure()
      }
    }

  private suspend fun tryAddingModuleDependencyToBuildFile(from: Module, labelToInsert: Label?): Boolean {
    if (labelToInsert !is ResolvedLabel) return false
    val fromBuildTarget = from.project.targetUtils.getBuildTargetForModule(from) ?: return false
    val targetBuildFile = readAction { findBuildFile(from.project, fromBuildTarget) } ?: return false
    val targetRuleLabel = fromBuildTarget.id
    val ruleTarget = readAction { targetBuildFile.findRuleTarget(targetRuleLabel.targetName) } ?: return false
    val depsList =
      readAction {
        ruleTarget
          .getArgumentList()
          ?.getDepsArgument()
          ?.children
          ?.first { it is StarlarkListLiteralExpression } as? StarlarkListLiteralExpression
      }
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
      BazelCoroutineService.getInstance(from.project).start {
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
    BazelBalloonNotifier.warn(
      BazelPluginBundle.message("balloon.add.target.dependency.to.build.file.failed.title"),
      BazelPluginBundle.message("balloon.add.target.dependency.to.build.file.failed.message"),
    )
  }

  override fun addExternalLibraryDependency(
    modules: Collection<Module>,
    descriptor: ExternalLibraryDescriptor,
    scope: DependencyScope,
  ): Promise<Void>? =
    asyncPromise {
      if (modules.size != 1) {
        modules.firstOrNull()?.jumpToBuildFile()
        notifyAutomaticDependencyAdditionFailure()
        return@asyncPromise
      }

      val libraryToAdd =
        LibraryTablesRegistrar.getInstance().getLibraryTable(project).libraries.firstOrNull {
          val mavenCoordinates = it.getMavenCoordinates() ?: return@firstOrNull false
          mavenCoordinates.groupId == descriptor.libraryGroupId && mavenCoordinates.artifactId == descriptor.libraryArtifactId
        }
      if (libraryToAdd != null) {
        addLibraryDependency(modules.single(), libraryToAdd, scope, true)?.await()
        return@asyncPromise
      }

      modules.firstOrNull()?.jumpToBuildFile()
      notifyAutomaticDependencyAdditionFailure()
    }

  override fun changeLanguageLevel(module: Module, level: LanguageLevel): Promise<Void>? =
    asyncPromise {
      module.jumpToBuildFile()
    }

  private suspend fun Module.jumpToBuildFile() {
    val buildTarget = project.targetUtils.getBuildTargetForModule(this) ?: return
    jumpToBuildFile(project, buildTarget)
  }

  private fun asyncPromise(callable: suspend () -> Unit): Promise<Void> =
    AsyncPromise<Void>().also { promise ->
      BazelCoroutineService.getInstance(project).startAsync(callable = callable).invokeOnCompletion { throwable ->
        if (throwable != null) {
          promise.setError(throwable)
        } else {
          promise.setResult(null)
        }
      }
    }
}
