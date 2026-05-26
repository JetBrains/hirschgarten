package com.intellij.bazel.test.integration

import org.jetbrains.bazel.test.framework.BazelPathManager
import java.nio.file.Path

internal open class WorkspaceImportApprovalTestData(
  val name: String,
  val heavy: Boolean = false,
  val projectView: (projectDir: Path) -> Path? = { null },
  val onProjectInit: (projectDir: Path) -> Unit = {},
) {
  open val expectedWorkspaceModelFile = BazelPathManager.getIntegrationTestFixturePath("workspaceModelApprovalData/$name.json")
}
