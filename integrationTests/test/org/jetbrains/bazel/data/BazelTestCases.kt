package org.jetbrains.bazel.data

import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.project.GitProjectInfo
import com.intellij.ide.starter.project.ProjectInfoSpec
import com.intellij.ide.starter.project.TestCaseTemplate
import org.jetbrains.bazel.test.compat.IntegrationTestCompat

object IdeaBazelCases : BaseBazelCasesParametrized(BazelTestContext.IDEA) {
    val FastBuild = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "e4ed2e7c204fd63c3d81950516a27fa163cb5169",
        branchName = "main",
        relativePath = "fastBuildTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
      )
    )

    val BazelProjectModelModifier = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "7b6fe68ddb6b21145fd41434bb95af41af48d483",
        branchName = "main",
        relativePath = "bazelProjectModelModifierTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val BazelProjectOpenProcessorStarter = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "be362148696afcec38035064a91d48ce76a4c10f",
        branchName = "main",
        relativePath = "invalidProjectOpeningTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
      )
    )

    val HotSwap = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "c88d3cb84fae0c8a42cd8ddd78306a40595ff764",
        branchName = "main",
        relativePath = "simpleKotlinTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val CompiledSourceCodeInsideJarExclude = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "60e9e037ca25be0734ea6760614defe228728dcb",
        branchName = "main",
        relativePath = "generatedCodeTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val CoroutineDebug = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "ee100511a31d6a658f0dfdf340a861e504e7f01a",
        branchName = "main",
        relativePath = "coroutineDebug",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
      )
    )

    val ProtobufResolve = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/Krystian20857/simpleBazelProjectsForTesting.git",
        commitHash = "148ddeddd07cfc678db20e50d9c955d4d9bbbafb",
        branchName = "main",
        relativePath = "protobufTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
      )
    )



    val ReopenWithoutResync = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "c88d3cb84fae0c8a42cd8ddd78306a40595ff764",
        branchName = "main",
        relativePath = "simpleKotlinTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val BazelCoverage = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "89fac5f21551110b00022e6373de527b20304ad0",
        branchName = "main",
        relativePath = "coverageTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val ImportRunConfigurationsSyncHook = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "895ec375e08ef59436740036a9164f70e3cd5b4c",
        branchName = "main",
        relativePath = "importRunConfigurations",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context, createProjectView = false) },
      )
    )

    val BazelVersionUpdate = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "c170099e3051be5f17df3848fbd719f208fd10d2",
        branchName = "main",
        relativePath = "simpleJavaTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val ProjectViewOpen = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "c170099e3051be5f17df3848fbd719f208fd10d2",
        branchName = "main",
        relativePath = "simpleJavaTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val ExternalRepoResolve = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "cc8120484e09df881e2e41aa19a2de4a27791dc4",
        branchName = "main",
        relativePath = "starlarkResolveTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val NonModuleTargets = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "6cebc477be7c37074af51cc91b3b7602ff9b4d1e",
        branchName = "main",
        relativePath = "nonModuleTargetsTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val RecoverDotBazelBsp = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "c88d3cb84fae0c8a42cd8ddd78306a40595ff764",
        branchName = "main",
        relativePath = "simpleKotlinTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val RunAllTestsAction = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "448f723b7b32c31821908a81fbe70fcf43aa48d4",
        branchName = "main",
        relativePath = "runAllTests",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val RunLineMarker = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "c88d3cb84fae0c8a42cd8ddd78306a40595ff764",
        branchName = "main",
        relativePath = "simpleKotlinTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

  val TestTargetActionResultsTree = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "c88d3cb84fae0c8a42cd8ddd78306a40595ff764",
      branchName = "main",
      relativePath = "simpleKotlinTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

    val TestHiddenTargetsTest = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "c88d3cb84fae0c8a42cd8ddd78306a40595ff764",
        branchName = "main",
        relativePath = "simpleKotlinTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val DisabledKotlinPlugin = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "18020888ba1564927d7cfe672f8a3f7ca24c1e23",
        branchName = "main",
        relativePath = "simpleMultiLanguageTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val MoveKotlinFile = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
        commitHash = "7beddfa764a06bd5fcd4211e00c744847b7a0da9",
        branchName = "main",
        relativePath = "moveFilesTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
      )
    )
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
      commitHash = "1fe1e07dcf5d50868e10f3e6e87f2c4e95b4c290",
      branchName = "main",
      relativePath = "simplePythonTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
    )
  )

  val PyCharm = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
      commitHash = "226f7ff221c4f63c60ad5a6662e11deea2a7779c",
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
