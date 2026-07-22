package com.intellij.bazel.devkit.monorepo.run

import com.intellij.monorepo.devkit.bazel.BazelTargetsInfoCache
import com.intellij.monorepo.devkit.bazel.JpsToBazelConverterRunner
import com.intellij.monorepo.devkit.bazel.useBazelCompile
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.java.ui.gutters.BazelJavaRunLineMarkerContributor
import org.jetbrains.bazel.kotlin.ui.gutters.BazelKotlinRunLineMarkerContributor
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.repomapping.PersistentBazelRepoMappingService
import org.jetbrains.bazel.languages.starlark.repomapping.calculateLabel
import org.jetbrains.bazel.project.DefaultProjectViewService
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService
import org.jetbrains.bazel.ui.gutters.NonImportedExecutableTarget
import org.jetbrains.bazel.ui.gutters.StarlarkRunLineMarkerContributor
import org.jetbrains.bsp.protocol.ExecutableTarget

private val LOG = fileLogger()

internal class MonorepoBazelJavaRunLineMarkerContributor : BazelJavaRunLineMarkerContributor() {
  override fun isProjectApplicable(project: Project): Boolean = MonorepoRunLineMarkerContributorUtil.isProjectApplicable(project)

  override fun getTargets(element: PsiElement): List<ExecutableTarget> {
    val mainClassFqn = if (element.isMainMethod()) element.getContainingClassFqn() else null
    return MonorepoRunLineMarkerContributorUtil.getTargets(element, mainClassFqn)
  }
}

internal class MonorepoBazelKotlinRunLineMarkerContributor : BazelKotlinRunLineMarkerContributor() {
  override fun isProjectApplicable(project: Project): Boolean = MonorepoRunLineMarkerContributorUtil.isProjectApplicable(project)

  override fun getTargets(element: PsiElement): List<ExecutableTarget> {
    val mainClassFqn = if (element.isMainMethod()) element.getContainingClassFqn() else null
    return MonorepoRunLineMarkerContributorUtil.getTargets(element, mainClassFqn)
  }
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

  fun getTargets(element: PsiElement, mainClassFqn: String?): List<ExecutableTarget> {
    val project = element.project
    val containingFile = element.containingFile?.virtualFile ?: return emptyList()
    val projectFileIndex = ProjectFileIndex.getInstance(project)
    val module = projectFileIndex.getModuleForFile(containingFile) ?: return emptyList()

    // Run gutters won't work without target information, launch JPS to Bazel converter in the background
    if (!BazelTargetsInfoCache.getInstance(project).targetsInfo.filePresent) {
      JpsToBazelConverterRunner.getInstance(project).launch(focus = false, shouldSaveEverything = false)
      return emptyList()
    }

    val bazelInfo = try {
      BazelTargetsInfoCache.getInstance(project).targetsInfo.getModuleDescription(module.name)
    }
    catch (e: Throwable) {
      LOG.warn(e)
      return emptyList()
    }

    val binaryLabel = getBinaryLabel(module, mainClassFqn)
    if (binaryLabel != null) {
      val kind = TargetKindService.getInstance().guessFromRuleName("java_binary")
      return listOf(
        NonImportedExecutableTarget(
          id = binaryLabel,
          kind = kind,
        ),
      )
    }

    val kind = TargetKindService.getInstance().guessFromRuleName("jps_test")
    return listOfNotNull(
      bazelInfo.testTargets.firstOrNull()?.let { target ->
        NonImportedExecutableTarget(
          id = Label.parse(target.removeSuffix("_lib.jar")),
          kind = kind,
        )
      },
    )
  }

  private fun getBinaryLabel(module: Module, fqn: String?): Label? {
    if (fqn == null) return null
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
