package org.jetbrains.bazel.data

import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.ide.starter.project.TestCaseTemplate
import org.jetbrains.bazel.test.compat.IntegrationTestCompat
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object IdeaBazelCases : BaseBazelCasesParametrized(BazelTestContext.IDEA) {
    val FastBuild = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
        branchName = "main",
        relativePath = "fastBuildTest",
        configure = { context ->
          BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context)
          val projectHome = context.resolvedProjectHome.let { if (it.isDirectory()) it else it.parent }
          (projectHome / ".bazelrc").toFile().appendText("\ncommon --disk_cache=\n")
        },
      )
    )

    val BazelProjectModelModifier = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
        branchName = "main",
        relativePath = "bazelProjectModelModifierTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

  val BazelProjectOpenByRootDir = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
      branchName = "main",
      relativePath = "simpleKotlinTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

  val BazelProjectOpenByModuleFile = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
      branchName = "main",
      relativePath = "simpleKotlinTest/MODULE.bazel",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

  val BazelLegacyPluginProject = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
      branchName = "main",
      relativePath = "legacyGooglePluginTest/.ijwb",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

  val CompiledSourceCodeInsideJarExclude = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
        branchName = "main",
        relativePath = "generatedCodeTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val CoroutineDebug = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "90fcdecc4ea14ca4e453565e667f67d2cb27eb6e",
        branchName = "main",
        relativePath = "coroutineDebug",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
      )
    )

    val ProtobufResolve = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
        branchName = "main",
        relativePath = "protobufTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
      )
    )

    val SimpleScalaTest = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
        branchName = "main",
        relativePath = "simpleScalaTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )


    val BazelCoverage = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
        branchName = "main",
        relativePath = "coverageTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val ImportRunConfigurationsSyncHook = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
        branchName = "main",
        relativePath = "importRunConfigurations",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context, createProjectView = false) },
      )
    )

    val ExternalRepoResolve = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
        branchName = "main",
        relativePath = "starlarkResolveTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val NonModuleTargets = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
        branchName = "main",
        relativePath = "nonModuleTargetsTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      ),
    )

   val LocalPathOverride = withBazelProject(
     projectInfo = withDefaults(
       repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
       commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
       branchName = "main",
       relativePath = "localPathOverride",
       configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
     )
   )

   val BrokenDeps = withBazelProject(
     projectInfo = withDefaults(
       repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
       commitHash = "41a8dc6b668681d114d6760e7755de0daa25ab12",
       branchName = "main",
       relativePath = "broken",
       configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
     )
   )

    val RunAllTestsAction = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
        branchName = "main",
        relativePath = "runAllTests",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val DisabledKotlinPlugin = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
        branchName = "main",
        relativePath = "simpleMultiLanguageTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val MoveKotlinFile = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
        commitHash = "f22ef1c1e220efb12c58583d53da06ed62d87813",
        branchName = "main",
        relativePath = "moveFilesTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
      ),
    )

  val JetBrainsTestRunner = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
      commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
      branchName = "main",
      relativePath = "jetbrainsTestRunner",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    ),
  )

  val LabelAllTabSESplit = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
      commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
      branchName = "main",
      relativePath = "simpleMultiLanguageTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

  val NonIndexableFilesAllTabSESplit = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
      commitHash = "90fcdecc4ea14ca4e453565e667f67d2cb27eb6e",
      branchName = "main",
      relativePath = "simpleMultiLanguageTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
    )
  )

  val SyntheticRunTarget = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
      commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
      branchName = "main",
      relativePath = "syntheticRunTargetTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

  val SimpleKotlinCombined = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
      branchName = "main",
      relativePath = "simpleKotlinTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

  val SimpleJavaCombined = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
      branchName = "main",
      relativePath = "simpleJavaTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

  val ProjectViewCombined = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
      branchName = "main",
      relativePath = "projectViewCombinedTest",
      configure = { context ->
        BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context, createProjectView = false)
        preCacheBazelisk(context)
      },
    ),
  )

  val ProjectViewChange = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "1b07410678cfef0042e82ecfaadbbd9f34c1cd03",
      branchName = "main",
      relativePath = "projectViewChangeTest",
      configure = { context ->
        BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context, createProjectView = false)
        preCacheBazelisk(context)
      },
    )
  )

  val ProjectViewAppearance = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "bba2152c660576119e8b734cd639eec179c40231",
      branchName = "main",
      relativePath = "projectViewAppearanceTest",
      configure = { context ->
        BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context, createProjectView = false)
        preCacheBazelisk(context)
      },
    )
  )
}

private fun preCacheBazelisk(context: IDETestContext) {
  val systemBazelisk = listOf("/opt/homebrew/bin/bazelisk", "/usr/local/bin/bazelisk")
    .map { Path.of(it) }
    .firstOrNull { it.exists() } ?: return
  val cacheDir = (context.paths.systemDir / "bazel-plugin").createDirectories()
  val target = cacheDir / "bazelisk"
  if (!target.exists()) {
    target.createSymbolicLinkPointingTo(systemBazelisk)
  }
}

object GoLandBazelCases : BaseBazelCasesParametrized(BazelTestContext.GOLAND) {
  val GolandSync = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
      branchName = "main",
      relativePath = "with_go_source",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context, createProjectView = false) },
    )
  )
}

object PyCharmBazelCases : BaseBazelCasesParametrized(BazelTestContext.PYCHARM) {
  val SimplePython = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
      branchName = "main",
      relativePath = "simplePythonTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
    )
  )

  val PyCharm = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "af3e066604a8af0c9505b088ae5e7055ecf3b6d5",
      branchName = "main",
      relativePath = "simpleMultiLanguageTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
    )
  )

  val PythonProtobufTest = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "2a408a472e9161dc823fa3b45c779479898cf22d",
      branchName = "main",
      relativePath = "pythonProtobufTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
    )
  )
}

open class BaseBazelCasesParametrized(val context: BazelTestContext) : TestCaseTemplate(context.getIdeInfo()) {
  protected fun withDefaults(
    repositoryUrl: String,
    commitHash: String,
    branchName: String,
    relativePath: String? = null,
    configure: (IDETestContext) -> Unit = { }
  ) = GitProjectInfo(
    repositoryUrl = repositoryUrl,
    commitHash = commitHash,
    branchName = branchName,
    projectHomeRelativePath = { p -> relativePath?.let { p.resolve(it) } ?: p },
    isReusable = false,
    configureProjectBeforeUse = configure,
  )

  protected fun <T : ProjectInfoSpec> withBazelProject(projectInfo: T): TestCase<T> {
    return withProject(projectInfo)
      .let { IntegrationTestCompat.interceptTestCase(it, context.getIdeInfo()) }
  }
}
