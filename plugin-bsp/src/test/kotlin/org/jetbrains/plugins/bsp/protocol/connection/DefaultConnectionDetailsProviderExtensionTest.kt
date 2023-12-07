package org.jetbrains.plugins.bsp.protocol.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.Gson
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findOrCreateFile
import com.intellij.openapi.vfs.writeText
import com.intellij.testFramework.utils.vfs.createFile
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.extension.points.bspBuildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.server.connection.ConnectionDetailsProviderExtension
import org.jetbrains.plugins.bsp.server.connection.DefaultConnectionDetailsProviderExtension
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.io.path.createTempDirectory

@DisplayName("DefaultConnectionDetailsProviderExtension tests")
class DefaultConnectionDetailsProviderExtensionTest : MockProjectBaseTest() {
  private lateinit var extension: ConnectionDetailsProviderExtension

  private lateinit var projectRoot: VirtualFile

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()
    ConnectionDetailsProviderExtension.ep.point.registerExtension(DefaultConnectionDetailsProviderExtension(), projectModel.disposableRule.disposable)
    extension = ConnectionDetailsProviderExtension.ep.withBuildToolIdOrDefault(bspBuildToolId)

    projectRoot = createTempDirectory("root").also { it.toFile().deleteOnExit() }.toVirtualFile()
    project.rootDir = projectRoot
  }

  @Nested
  @DisplayName("DefaultConnectionDetailsProviderExtension#onFirstOpening and DefaultConnectionDetailsProviderExtension#provideNewConnectionDetails tests")
  inner class OnFirstOpeningAndProvideNewConnectionDetailsTest {
    @Test
    fun `onFirstOpening should fail if there is no connection file`() {
      // given

      // when & then
      shouldThrowAny {
        runBlocking {
          extension.onFirstOpening(project, projectRoot)
        }
      }
    }

    @Test
    fun `onFirstOpening should fail if there connection file is not parsable`() {
      // given
      runWriteAction {
        projectRoot.createFile(".bsp/invalid-connection-file.json")
          .writeText("it is not parsable")
      }

      // when & then
      shouldThrowAny {
        runBlocking {
          extension.onFirstOpening(project, projectRoot)
        }
      }
    }

    @Test
    fun `onFirstOpening should true if there is one connection file and todo should point to this file (headless mode)`() {
      // given
      val bspConnectionDetails = BspConnectionDetails(
        "build-tool-id",
        listOf("build-tool", "bsp"),
        "1.2.37",
        "2.0.0",
        listOf()
      )
      runWriteAction {
        bspConnectionDetails.saveInFile("connection-file")
      }

      // when
      val onFirstOpeningResult = runBlocking {
        extension.onFirstOpening(project, projectRoot)
      }

      // then
      onFirstOpeningResult shouldBe true
      extension.provideNewConnectionDetails(project, null) shouldBe bspConnectionDetails
    }

    @Test
    fun `onFirstOpening should true if there are multiple connection files and connectionFile should point to one of them (headless mode)`() {
      // given
      val bspConnectionDetails1 = BspConnectionDetails(
        "build-tool-id1",
        listOf("build-tool1", "bsp"),
        "1.2.37",
        "2.0.0",
        listOf()
      )
      val bspConnectionDetails2 = BspConnectionDetails(
        "build-tool-id2",
        listOf("build-tool3", "bsp"),
        "1.2.37",
        "2.0.0",
        listOf()
      )
      val bspConnectionDetails3 = BspConnectionDetails(
        "build-tool-id3",
        listOf("build-tool3", "bsp"),
        "1.2.37",
        "2.0.0",
        listOf()
      )
      runWriteAction {
        bspConnectionDetails1.saveInFile("connection-file1")
        bspConnectionDetails2.saveInFile("connection-file2")
        bspConnectionDetails3.saveInFile("connection-file3")
      }

      // when
      val onFirstOpeningResult = runBlocking {
        extension.onFirstOpening(project, projectRoot)
      }

      // then
      onFirstOpeningResult shouldBe true
      extension.provideNewConnectionDetails(project, null) shouldBeIn
        listOf(bspConnectionDetails1, bspConnectionDetails2, bspConnectionDetails3)
    }
  }

  @Nested
  @DisplayName("BspConnectionFileProviderExtension#provideNewConnectionDetails tests")
  inner class ProvideNewConnectionDetailsTest {
    private lateinit var connectionFileName: String
    private lateinit var initConnectionDetails: BspConnectionDetails

    @BeforeEach
    fun beforeEach() {
      // given
      initConnectionDetails = BspConnectionDetails(
        "build-tool-id",
        listOf("build-tool", "bsp"),
        "1.2.37",
        "2.0.0",
        listOf()
      )
      connectionFileName = "connection-file"
      runWriteAction {
        initConnectionDetails.saveInFile(connectionFileName)
      }

      runBlocking {
        extension.onFirstOpening(project, projectRoot)
      }
    }

    @Test
    fun `should return false if connection file has not changed since init`() {
      // given

      // when
      val result = runBlocking {
        extension.provideNewConnectionDetails(project, initConnectionDetails)
      }

      // then
      result shouldBe null
    }

    @Test
    fun `should return true if connection file (version) has changed since init`() {
      // given
      val newConnectionDetails = BspConnectionDetails(
        "build-tool-id",
        listOf("build-tool", "bsp"),
        "1.2.38",
        "2.0.0",
        listOf()
      )
      runWriteAction {
        newConnectionDetails.saveInFile(connectionFileName)
      }

      // when
      val result = runBlocking {
        extension.provideNewConnectionDetails(project, initConnectionDetails)
      }

      // then
      result shouldBe newConnectionDetails
    }

    @Test
    fun `should return true if connection file (command) has changed since init`() {
      // given
      val newConnectionDetails = BspConnectionDetails(
        "build-tool-id",
        listOf("build-tool-another-command", "bsp"),
        "1.2.37",
        "2.0.0",
        listOf()
      )
      runWriteAction {
        newConnectionDetails.saveInFile(connectionFileName)
      }

      // when
      val result = runBlocking {
        extension.provideNewConnectionDetails(project, initConnectionDetails)
      }

      // then
      result shouldBe newConnectionDetails
    }

    @Test
    fun `should return true if connection file has changed multiple times since init`() {
      // given
      val newConnectionDetails1 = BspConnectionDetails(
        "build-tool-id",
        listOf("build-tool", "bsp"),
        "1.2.38",
        "2.0.0",
        listOf()
      )
      runWriteAction {
        newConnectionDetails1.saveInFile(connectionFileName)
      }

      val newConnectionDetails2 = BspConnectionDetails(
        "build-tool-id",
        listOf("build-tool", "bsp"),
        "1.2.39",
        "2.0.0",
        listOf()
      )
      runWriteAction {
        newConnectionDetails2.saveInFile(connectionFileName)
      }

      // when
      val result = runBlocking {
        extension.provideNewConnectionDetails(project, initConnectionDetails)
      }

      // then
      result shouldBe newConnectionDetails2
    }
  }

  private fun BspConnectionDetails.saveInFile(fileName: String) {
    val connectionFile = projectRoot.findOrCreateFile(".bsp/$fileName.json")
    connectionFile.writeText(this.toJson())
  }

  private fun BspConnectionDetails.toJson(): String =
    Gson().toJson(this)
}
