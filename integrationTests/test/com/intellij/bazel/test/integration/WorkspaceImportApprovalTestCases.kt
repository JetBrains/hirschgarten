package com.intellij.bazel.test.integration

import com.intellij.ide.starter.extended.config.Const.SSH_GIT_SPACE_PREFIX
import com.intellij.ide.starter.extended.data.PlatformGitProject
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.project.ReusableLocalProjectInfo
import com.intellij.ide.starter.project.TestCaseTemplate
import com.intellij.tools.ide.starter.product.idea.ultimate.IdeaUltimate
import org.jetbrains.bazel.test.framework.BazelPathManager

object WorkspaceImportApprovalTestCases : TestCaseTemplate(IdeInfo.IdeaUltimate) {

  private fun bundledBazelProject(name: String) = withProject(
    ReusableLocalProjectInfo(BazelPathManager.bazelTestProjectsRoot.resolve(name)),
  )

  val SimpleKotlinTest = bundledBazelProject("simpleKotlinTest")
  val LocalPathOverrideTest = bundledBazelProject("localPathOverride")
  val NonModuleTargetsTest = bundledBazelProject("nonModuleTargetsTest")
  val SimpleJavaTest = bundledBazelProject("simpleJavaTest")
  val SimpleScalaTest = bundledBazelProject("simpleScalaTest")

  val InSaneBazel = withProject(
    PlatformGitProject.fromProjectPath(
      branchName = "main",
      commitHash = "87d2b400ce6eb4c2463ba6d8ee41f8e39fe47b34",
      repositoryUrl = "$SSH_GIT_SPACE_PREFIX/bazel/inSaneBazel.git",
    ),
  )

}
