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
        commitHash = "b5cdb19c0247e2c810de6bb8425d4f9293a790f5",
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
        commitHash = "90fcdecc4ea14ca4e453565e667f67d2cb27eb6e",
        branchName = "main",
        relativePath = "bazelProjectModelModifierTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

  val BazelProjectOpenByRootDir = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "599425b1bc7b525e13849c64aa3ecc12880568b0",
      branchName = "main",
      relativePath = "simpleKotlinTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

  val BazelProjectOpenByModuleFile = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "599425b1bc7b525e13849c64aa3ecc12880568b0",
      branchName = "main",
      relativePath = "simpleKotlinTest/MODULE.bazel",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

  val BazelLegacyPluginProject = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "24c031faaa9435a64344d604d0eee7c64038e362",
      branchName = "main",
      relativePath = "legacyGooglePluginTest/.ijwb",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

  val CompiledSourceCodeInsideJarExclude = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "9631bed77c65907263171ac8590cd4ffad95966c",
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
        commitHash = "11f03260cf8611309d8a8c643881e88e45815739",
        branchName = "main",
        relativePath = "protobufTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
      )
    )

    val SimpleScalaTest = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "b3991962f00c8e60bc60e5ba7287b90195baa01e",
        branchName = "main",
        relativePath = "simpleScalaTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )


    val BazelCoverage = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "90fcdecc4ea14ca4e453565e667f67d2cb27eb6e",
        branchName = "main",
        relativePath = "coverageTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val ImportRunConfigurationsSyncHook = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "90fcdecc4ea14ca4e453565e667f67d2cb27eb6e",
        branchName = "main",
        relativePath = "importRunConfigurations",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context, createProjectView = false) },
      )
    )

    val ExternalRepoResolve = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "b5cdb19c0247e2c810de6bb8425d4f9293a790f5",
        branchName = "main",
        relativePath = "starlarkResolveTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val NonModuleTargets = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "3cee0003dcd3d9c3664b9bb3bc9161ce99fa8184",
        branchName = "main",
        relativePath = "nonModuleTargetsTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      ),
    )

   val LocalPathOverride = withBazelProject(
     projectInfo = withDefaults(
       repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
       commitHash = "395beb872a02605bb8da254ddedee49affbcee94",
       branchName = "main",
       relativePath = "localPathOverride",
       configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
     )
   )

    val RunAllTestsAction = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "b5cdb19c0247e2c810de6bb8425d4f9293a790f5",
        branchName = "main",
        relativePath = "runAllTests",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val DisabledKotlinPlugin = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "90fcdecc4ea14ca4e453565e667f67d2cb27eb6e",
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
      commitHash = "b5cdb19c0247e2c810de6bb8425d4f9293a790f5",
      branchName = "main",
      relativePath = "jetbrainsTestRunner",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    ),
  )

  val LabelAllTabSESplit = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
      commitHash = "90fcdecc4ea14ca4e453565e667f67d2cb27eb6e",
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
      commitHash = "b5cdb19c0247e2c810de6bb8425d4f9293a790f5",
      branchName = "main",
      relativePath = "syntheticRunTargetTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

  val SimpleKotlinCombined = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "7f5b9b5279e28e9c74ef3efe1381bc78adbab10b",
      branchName = "main",
      relativePath = "simpleKotlinTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

  val SimpleJavaCombined = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "2292b18f90fddc40a490a481e94225ddfa854ef0",
      branchName = "main",
      relativePath = "simpleJavaTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

  val ProjectViewCombined = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "12a0b2091b1317e4b64aaed25ecac6004ef984fe",
      branchName = "main",
      relativePath = "projectViewCombinedTest",
      configure = { context ->
        BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context, createProjectView = false)
        preCacheBazelisk(context)
      },
    )
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
      commitHash = "ee100511a31d6a658f0dfdf340a861e504e7f01a",
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
      commitHash = "b5cdb19c0247e2c810de6bb8425d4f9293a790f5",
      branchName = "main",
      relativePath = "simplePythonTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
    )
  )

  val PyCharm = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "90fcdecc4ea14ca4e453565e667f67d2cb27eb6e",
      branchName = "main",
      relativePath = "simpleMultiLanguageTest",
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
