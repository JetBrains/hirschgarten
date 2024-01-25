package org.jetbrains.plugins.bsp.protocol.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.bspBuildToolId
import org.jetbrains.plugins.bsp.server.connection.ConnectionDetailsProviderExtension
import org.jetbrains.plugins.bsp.server.connection.ConnectionDetailsProviderExtensionAdapter
import org.jetbrains.plugins.bsp.server.connection.ConnectionDetailsProviderExtensionJavaShim
import org.jetbrains.plugins.bsp.server.connection.DefaultConnectionDetailsProviderExtension
import org.jetbrains.plugins.bsp.server.connection.connectionDetailsProvider
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

private val testBuildToolId = BuildToolId("testBuildTool")

private class TestBuildToolExtension: ConnectionDetailsProviderExtension {
  override val buildToolId: BuildToolId = testBuildToolId

  override suspend fun onFirstOpening(project: Project, projectPath: VirtualFile): Boolean = true

  override fun provideNewConnectionDetails(
    project: Project,
    currentConnectionDetails: BspConnectionDetails?,
  ): BspConnectionDetails? = null
}

private class TestBuildToolExtensionJavaShim: ConnectionDetailsProviderExtensionJavaShim {
  override val buildToolId: BuildToolId = testBuildToolId
  override fun onFirstOpening(project: Project, projectPath: VirtualFile): CompletableFuture<Boolean> =
    CompletableFuture.completedFuture(true)

  override fun provideNewConnectionDetails(
    project: Project,
    currentConnectionDetails: BspConnectionDetails?,
  ): BspConnectionDetails? = null
}

@DisplayName("ConnectionDetailsProviderExtensionTest related things tests")
class ConnectionDetailsProviderExtensionTest : MockProjectBaseTest() {
  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    registerExtension(DefaultConnectionDetailsProviderExtension())
  }

  @Nested
  @DisplayName("val Project.connectionDetailsProvider tests")
  inner class ProjectConnectionDetailsProviderTest {
    @Test
    fun `should return default (bsp) implementation if bsp is the build tool`() {
      // given
      project.buildToolId = bspBuildToolId
      registerExtension(TestBuildToolExtension())
      registerExtensionJavaShim(TestBuildToolExtensionJavaShim())

      // when
      val provider = project.connectionDetailsProvider

      // then
      provider::class shouldBe DefaultConnectionDetailsProviderExtension::class
    }

    @Test
    fun `should return default (bsp) implementation if implementation for the build tool is not specified`() {
      // given
      project.buildToolId = BuildToolId("another build tool")

      // when
      val provider = project.connectionDetailsProvider

      // then
      provider::class shouldBe DefaultConnectionDetailsProviderExtension::class
    }

    @Test
    fun `should return build tool implementation if implementation for the build tool is specified`() {
      // given
      project.buildToolId = testBuildToolId
      registerExtension(TestBuildToolExtension())

      // when
      val provider = project.connectionDetailsProvider

      // then
      provider::class shouldBe TestBuildToolExtension::class
    }

    @Test
    fun `should return build tool java shim implementation in adapter if implementation for the build tool is specified`() {
      // given
      project.buildToolId = testBuildToolId
      registerExtensionJavaShim(TestBuildToolExtensionJavaShim())

      // when
      val provider = project.connectionDetailsProvider

      // then
      provider::class shouldBe ConnectionDetailsProviderExtensionAdapter::class
      provider.buildToolId shouldBe testBuildToolId
    }
  }

  private fun registerExtension(extension: ConnectionDetailsProviderExtension) {
    ConnectionDetailsProviderExtension.ep.point.registerExtension(extension, projectModel.disposableRule.disposable)
  }

  private fun registerExtensionJavaShim(extension: ConnectionDetailsProviderExtensionJavaShim) {
    ConnectionDetailsProviderExtensionJavaShim.ep.point.registerExtension(extension, projectModel.disposableRule.disposable)
  }
}
