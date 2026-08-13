package com.intellij.bazel.devkit.monorepo.run

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.MultipleRunLocationsProvider
import com.intellij.lang.java.JavaLanguage
import com.intellij.monorepo.devkit.bazel.BazelTargetsInfoCache
import com.intellij.monorepo.devkit.bazel.JpsToBazelConverterRunner
import com.intellij.monorepo.devkit.bazel.useBazelCompile
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.java.ui.gutters.BazelJavaRunConfigurationProducer
import org.jetbrains.bazel.kotlin.ui.gutters.BazelKotlinRunConfigurationProducer
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.repomapping.PersistentBazelRepoMappingService
import org.jetbrains.bazel.languages.starlark.repomapping.calculateLabel
import org.jetbrains.bazel.project.DefaultProjectViewService
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService
import org.jetbrains.bazel.ui.gutters.BazelRunLocation
import org.jetbrains.bazel.ui.gutters.NonImportedBuildTarget
import org.jetbrains.bazel.ui.gutters.StarlarkRunLineMarkerContributor
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.kotlin.idea.KotlinLanguage

private val LOG = fileLogger()

/**
 * See [org.jetbrains.bazel.ui.gutters.BazelContainingTargetsLocationsProvider]
 */
internal class MonorepoBazelContainingTargetsLocationsProvider : MultipleRunLocationsProvider() {
  override fun getAlternativeLocations(originalLocation: Location<*>): List<Location<*>> {
    if (!MonorepoRunLineMarkerContributorUtil.isProjectApplicable(originalLocation.project)) return emptyList()
    if (originalLocation !is PsiLocation) return emptyList()
    val element = originalLocation.psiElement

    if (element is PsiDirectory) {
      val virtualFile = element.virtualFile
      // Only show the "all tests" gutter on top-level directories because we don't support filtering by package
      val showAllTestsGutter = virtualFile.findChild("BUILD.bazel") != null ||
                               (element.virtualFile.name == "test" && element.parentDirectory?.findFile("BUILD.bazel") != null)
      if (!showAllTestsGutter) return emptyList()
    }

    val mainClassFqn: String? = getMainClassFqn(element)

    val bazelRunLocations = MonorepoRunLineMarkerContributorUtil.getTargets(element, mainClassFqn).map { target ->
      if (element is PsiDirectory) {
        BazelRunLocation(element.project, target)
      }
      else {
        BazelRunLocation(target, originalLocation)
      }
    }

    // main() method gutters only expect one element. E.g., ApplicationRunLineMarkerProvider calls
    // ExecutorAction.getActions(Integer.MAX_VALUE), meaning: take only the top config from context (as opposed to the default order = 0).
    // If we can provide a Bazel main() gutter, then we are forced to discard the JPS gutter here (otherwise our gutter won't be shown).
    return if (mainClassFqn != null && bazelRunLocations.isNotEmpty()) {
      bazelRunLocations
    }
    else {
      listOf(originalLocation) + bazelRunLocations
    }
  }

  private fun getMainClassFqn(element: PsiElement): String? {
    val producer: BazelJavaRunConfigurationProducer = when (element.language) {
      JavaLanguage.INSTANCE -> BazelJavaRunConfigurationProducer()
      KotlinLanguage.INSTANCE -> BazelKotlinRunConfigurationProducer()
      else -> return null
    }
    if (!producer.isMainMethod(element)) return null
    val identifier = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java, true) ?: return null
    val (clazz, method) = producer.toPsiClassOrMethod(identifier)
    val containingClazz = clazz ?: method?.containingClass ?: return null
    return containingClazz.qualifiedName
  }

  override fun getLocationDisplayName(
    locationCreatedFrom: Location<*>,
    originalLocation: Location<*>,
  ): String? = null
}

internal class MonorepoStarlarkRunLineMarkerContributor : StarlarkRunLineMarkerContributor() {
  override fun isProjectApplicable(project: Project): Boolean =
    MonorepoRunLineMarkerContributorUtil.isProjectApplicable(project)
}

internal class MonorepoProjectViewStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!MonorepoRunLineMarkerContributorUtil.isProjectApplicable(project)) return

    // Set the mapping to support community repo targets
    val rootDir = project.rootDir
    val rootDirPath = rootDir.toNioPath()
    val mapping = PersistentBazelRepoMappingService.getInstance(project)
    mapping.canonicalRepoNameToPath = mapOf("community+" to rootDirPath.resolve("community"), "" to rootDirPath)
    mapping.apparentRepoNameToCanonicalName = mapOf("community" to "community+", "" to "")
    mapping.canonicalRepoNameToApparentName = mapOf("community+" to "community", "" to "")

    // Set the project view. This is needed for these fields:
    // use_jetbrains_test_runner: true
    // run_config_run_with_bazel: false
    val projectViewPath = sequenceOf(rootDir.findChild(".bazelproject"), rootDir.findChild("ultimate.bazelproject"), rootDir.findChild("community.bazelproject"))
      .firstOrNull { it != null && it.exists() }
    if (projectViewPath == null) {
      LOG.warn("Missing project view path")
      return
    }
    DefaultProjectViewService.getInstance(project).forceLoadProjectViewFile(projectViewPath)
  }
}

private object MonorepoRunLineMarkerContributorUtil {
  fun isProjectApplicable(project: Project): Boolean =
    useBazelCompile(project) && !project.isBazelProject

  fun getTargets(element: PsiElement, mainClassFqn: String?): List<BuildTarget> {
    val project = element.project
    val containingFile = if (element is PsiDirectory) element.virtualFile else element.containingFile?.virtualFile ?: return emptyList()
    val projectFileIndex = ProjectFileIndex.getInstance(project)
    val module = projectFileIndex.getModuleForFile(containingFile) ?: return emptyList()

    // Run gutters won't work without bazel-targets.json, launch JPS to Bazel converter in the background
    if (!BazelTargetsInfoCache.getInstance(project).targetsInfo.filePresent) {
      JpsToBazelConverterRunner.getInstance(project).launch(focus = false, shouldSaveEverything = false)
      return emptyList()
    }

    val bazelInfo = try {
      BazelTargetsInfoCache.getInstance(project).targetsInfo.getModuleDescription(module.name)
    }
    catch (e: IllegalStateException) {
      // Module not found, which is probably caused by bazel-targets.json being outdated
      LOG.warn(e)
      JpsToBazelConverterRunner.getInstance(project).launch(focus = false, shouldSaveEverything = false)
      return emptyList()
    }
    catch (e: Throwable) {
      LOG.warn(e)
      return emptyList()
    }

    val baseDirectory = (containingFile.parent ?: containingFile).toNioPath()

    if (mainClassFqn != null) {
      val binaryLabel = getBinaryLabel(module, mainClassFqn) ?: return emptyList()
      val kind = TargetKindService.getInstance().guessFromRuleName("java_binary")
      return listOf(
        NonImportedBuildTarget(
          label = binaryLabel,
          kind = kind,
          baseDirectory = baseDirectory,
        ),
      )
    }

    val kind = TargetKindService.getInstance().guessFromRuleName("jps_test")
    return listOfNotNull(
      bazelInfo.testTargets.firstOrNull()?.let { target ->
        NonImportedBuildTarget(
          label = Label.parse(target.removeSuffix("_lib.jar")),
          kind = kind,
          baseDirectory = baseDirectory,
        )
      },
    )
  }

  private fun getBinaryLabel(module: Module, fqn: String): Label? {
    val project = module.project
    val buildFile = module.moduleFile?.parent?.findChild("BUILD.bazel") ?: return null
    val psiFile = PsiManager.getInstance(project).findFile(buildFile) as? StarlarkFile ?: return null
    val binaryTargetName = psiFile.getTargetRules()
                             .firstOrNull {
                               it.getArgumentList()?.getKeywordArgument("main_class")?.getArgumentStringValue() == fqn
                             }?.name ?: return null
    return calculateLabel(project, buildFile, binaryTargetName)
  }
}
