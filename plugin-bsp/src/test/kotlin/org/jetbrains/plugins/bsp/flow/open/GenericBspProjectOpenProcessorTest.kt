package org.jetbrains.plugins.bsp.flow.open

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.writeText
import com.intellij.testFramework.registerExtension
import com.intellij.testFramework.utils.vfs.createFile
import com.intellij.util.application
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private class TestableProjectOpenProcessorExtension(
  override val buildToolId: BuildToolId,
  override val shouldBspProjectOpenProcessorBeAvailable: Boolean,
) : BspProjectOpenProcessorExtension

class GenericBspProjectOpenProcessorTest : MockProjectBaseTest() {
  private lateinit var processor: BspProjectOpenProcessor

  private lateinit var projectRoot: VirtualFile

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()
    processor = BspProjectOpenProcessor()
    projectRoot = projectModel.projectRootDir.toVirtualFile()

    WriteCommandAction.runWriteCommandAction(project) {
      projectRoot.createFile("not-a-connection-file")
      projectRoot.createFile("another-not-a-connection-file")
    }

    cleanupExtensionPoints()
  }

  private fun cleanupExtensionPoints() {
    val epName = BspProjectOpenProcessorExtension.ep.name
    application.extensionArea.unregisterExtensionPoint(epName)
    application.extensionArea.registerExtensionPoint(epName, BspProjectOpenProcessorExtension::class.java.name, ExtensionPoint.Kind.INTERFACE, true)
  }

  @Nested
  @DisplayName("GenericBspProjectOpenProcessor#canOpenProject(file) tests")
  inner class CanOpenProjectTest {
    @Test
    fun `should return false if there is no connection file`() {
      // given
      // when
      val canOpen = processor.canOpenProject(projectRoot)

      // then
      canOpen shouldBe false
    }

    @Test
    fun `should return true if there is one connection file`() {
      // given
      WriteCommandAction.runWriteCommandAction(project) {
        val connectionFile = projectRoot.createFile(".bsp/connection-file.json")
        connectionFile.writeText(
          """
            |{
            | "name": "build-tool-id",
            | "argv": ["build-tool", "bsp"],
            | "version": "1.2.37",
            | "bspVersion": "2.0.0",
            | "languages": ["scala", "kotlin"]
            |}
          """.trimMargin(),
        )
      }

      // when
      val canOpen = processor.canOpenProject(projectRoot)

      // then
      canOpen shouldBe true
    }

    @Test
    fun `should return false if there is a connection file and extension point for this build tool disabling default processor`() {
      // given
      WriteCommandAction.runWriteCommandAction(project) {
        val connectionFile = projectRoot.createFile(".bsp/connection-file.json")
        connectionFile.writeText(
          """
            |{
            | "name": "build-tool-id",
            | "argv": ["build-tool", "bsp"],
            | "version": "1.2.37",
            | "bspVersion": "2.0.0",
            | "languages": ["scala", "kotlin"]
            |}
          """.trimMargin(),
        )

        TestableProjectOpenProcessorExtension(
          buildToolId = BuildToolId("build-tool-id"),
          shouldBspProjectOpenProcessorBeAvailable = false,
        ).register()
      }

      // when
      val canOpen = processor.canOpenProject(projectRoot)

      // then
      canOpen shouldBe false
    }

    @Test
    fun `should return true if there is a connection file and extension point for this build tool not disabling default processor`() {
      // given
      WriteCommandAction.runWriteCommandAction(project) {
        val connectionFile = projectRoot.createFile(".bsp/connection-file.json")
        connectionFile.writeText(
          """
            |{
            | "name": "build-tool-id",
            | "argv": ["build-tool", "bsp"],
            | "version": "1.2.37",
            | "bspVersion": "2.0.0",
            | "languages": ["scala", "kotlin"]
            |}
          """.trimMargin(),
        )

        TestableProjectOpenProcessorExtension(
          buildToolId = BuildToolId("build-tool-id"),
          shouldBspProjectOpenProcessorBeAvailable = true,
        ).register()
      }

      // when
      val canOpen = processor.canOpenProject(projectRoot)

      // then
      canOpen shouldBe true
    }

    @Test
    fun `should return true if there is multiple connection files and one extension point for one of build tools disabling default processor`() {
      // given
      WriteCommandAction.runWriteCommandAction(project) {
        val connectionFile1 = projectRoot.createFile(".bsp/connection-file-1.json")
        connectionFile1.writeText(
          """
            |{
            | "name": "build-tool-id-1",
            | "argv": ["build-tool", "bsp"],
            | "version": "1.2.37",
            | "bspVersion": "2.0.0",
            | "languages": ["scala", "kotlin"]
            |}
          """.trimMargin(),
        )

        val connectionFile2 = projectRoot.createFile(".bsp/connection-file-2.json")
        connectionFile2.writeText(
          """
            |{
            | "name": "build-tool-id-2",
            | "argv": ["build-tool", "bsp"],
            | "version": "1.2.37",
            | "bspVersion": "2.0.0",
            | "languages": ["scala", "kotlin"]
            |}
          """.trimMargin(),
        )

        val connectionFile3 = projectRoot.createFile(".bsp/connection-file-3.json")
        connectionFile3.writeText(
          """
            |{
            | "name": "build-tool-id-3",
            | "argv": ["build-tool", "bsp"],
            | "version": "1.2.37",
            | "bspVersion": "2.0.0",
            | "languages": ["scala", "kotlin"]
            |}
          """.trimMargin(),
        )

        TestableProjectOpenProcessorExtension(
          buildToolId = BuildToolId("build-tool-id-1"),
          shouldBspProjectOpenProcessorBeAvailable = false,
        ).register()
      }

      // when
      val canOpen = processor.canOpenProject(projectRoot)

      // then
      canOpen shouldBe true
    }
  }

  private fun BspProjectOpenProcessorExtension.register() {
    application.registerExtension(BspProjectOpenProcessorExtension.ep, this, project)
  }
}
