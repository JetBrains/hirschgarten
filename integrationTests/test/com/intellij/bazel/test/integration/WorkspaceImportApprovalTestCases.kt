package com.intellij.bazel.test.integration

import com.intellij.ide.starter.extended.config.Const.SSH_GIT_SPACE_PREFIX
import com.intellij.ide.starter.extended.data.PlatformGitProject
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.project.GitHubProject
import com.intellij.ide.starter.project.TestCaseTemplate
import com.intellij.tools.ide.starter.product.idea.ultimate.IdeaUltimate

object WorkspaceImportApprovalTestCases : TestCaseTemplate(IdeInfo.IdeaUltimate) {

  private fun simpleBazelProjectsForTesting(name: String) = withProject(
    GitHubProject.fromGithub(
      branchName = "main",
      commitHash = "6b8eb8de37dbb91e2906d6a059596101c22762f8",
      repoRelativeUrl = "JetBrainsBazelBot/simpleBazelProjectsForTesting",
    ).copy(projectHomeRelativePath = { it.resolve(name) }),
  )

  val SimpleKotlinTest = simpleBazelProjectsForTesting("simpleKotlinTest")
  val KotlinStrictDepsTest = simpleBazelProjectsForTesting("kotlinStrictDepsTest")
  val LocalPathOverrideTest = simpleBazelProjectsForTesting("localPathOverride")
  val NonModuleTargetsTest = simpleBazelProjectsForTesting("nonModuleTargetsTest")
  val SimpleJavaTest = simpleBazelProjectsForTesting("simpleJavaTest")
  val SimpleScalaTest = simpleBazelProjectsForTesting("simpleScalaTest")

  val InSaneBazel = withProject(
    PlatformGitProject.fromProjectPath(
      branchName = "main",
      commitHash = "87d2b400ce6eb4c2463ba6d8ee41f8e39fe47b34",
      repositoryUrl = "$SSH_GIT_SPACE_PREFIX/bazel/inSaneBazel.git",
    ),
  )

}
