package org.jetbrains.bazel.flow.sync

import com.intellij.openapi.components.service
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.shouldBe

class IdeaVFSUtilTest : BasePlatformTestCase() {
  fun `test unix-like path`() {
    val virtualFileUrlManager = project.service<WorkspaceModel>().getVirtualFileUrlManager()
    val virtualUrl = IdeaVFSUtil.toVirtualFileUrl("/home/user/project", virtualFileUrlManager)
    virtualUrl.url shouldBe "/home/user/project"
  }

  fun `test windows path`() {
    val virtualFileUrlManager = project.service<WorkspaceModel>().getVirtualFileUrlManager()
    val virtualUrl = IdeaVFSUtil.toVirtualFileUrl("C:/Users/user/project", virtualFileUrlManager)
    virtualUrl.url shouldBe "C:/Users/user/project"
  }
}
