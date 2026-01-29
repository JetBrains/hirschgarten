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
        commitHash = "dae34bfa1692fcb2857204ed82ae4dea61af5a63",
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
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "ef520dd2fb1c831641d651ab8997e1a6feda07d6",
        branchName = "main",
        relativePath = "protobufTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
      )
    )



    val ReopenWithoutResync = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "cda781ad6c4d9db964842c5b0aa28d77d0fde687",
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
        commitHash = "682aed9b15c007bbb9bed2246f54ff4c44ec1bf7",
        branchName = "main",
        relativePath = "importRunConfigurations",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context, createProjectView = false) },
      )
    )

    val BazelVersionUpdate = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "cda781ad6c4d9db964842c5b0aa28d77d0fde687",
        branchName = "main",
        relativePath = "simpleJavaTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val ProjectViewOpen = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "cda781ad6c4d9db964842c5b0aa28d77d0fde687",
        branchName = "main",
        relativePath = "simpleJavaTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val BytecodeViewer = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "cda781ad6c4d9db964842c5b0aa28d77d0fde687",
        branchName = "main",
        relativePath = "simpleJavaTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val ExternalRepoResolve = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "aefb5e4a3c606e67304eab2ace525b1872e92ba6",
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

    val RecoverDotBazelBsp = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "cda781ad6c4d9db964842c5b0aa28d77d0fde687",
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
        commitHash = "cda781ad6c4d9db964842c5b0aa28d77d0fde687",
        branchName = "main",
        relativePath = "simpleKotlinTest",
        configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
      )
    )

    val TestTargetActionResultsTree = withBazelProject(
      projectInfo = withDefaults(
        repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting.git",
        commitHash = "cda781ad6c4d9db964842c5b0aa28d77d0fde687",
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
      ),
    )

  val JetBrainsTestRunner = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
      commitHash = "cda781ad6c4d9db964842c5b0aa28d77d0fde687",
      branchName = "main",
      relativePath = "jetbrainsTestRunner",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    ),
  )

  val LabelAllTabSESplit = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
      commitHash = "dafe4cdad22db8e3f51a28f0dbfd3ddd07240830",
      branchName = "main",
      relativePath = "simpleMultiLanguageTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
    )
  )

  val NonIndexableFilesAllTabSESplit = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
      commitHash = "dafe4cdad22db8e3f51a28f0dbfd3ddd07240830",
      branchName = "main",
      relativePath = "simpleMultiLanguageTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUseWithoutBazelClean(context) },
    )
  )

  val SyntheticRunTarget = withBazelProject(
    projectInfo = withDefaults(
      repositoryUrl = "https://github.com/JetBrainsBazelBot/simpleBazelProjectsForTesting",
      commitHash = "ef520dd2fb1c831641d651ab8997e1a6feda07d6",
      branchName = "main",
      relativePath = "syntheticRunTargetTest",
      configure = { context -> BazelProjectConfigurer.configureProjectBeforeUse(context) },
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
