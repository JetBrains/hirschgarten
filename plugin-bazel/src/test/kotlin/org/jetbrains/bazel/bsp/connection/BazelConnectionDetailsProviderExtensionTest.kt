package org.jetbrains.bazel.bsp.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.Gson
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.writeText
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.rules.ProjectModelExtension
import com.intellij.testFramework.utils.vfs.createFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.settings.BazelApplicationServerSettings
import org.jetbrains.bazel.settings.BazelApplicationSettings
import org.jetbrains.bazel.settings.BazelApplicationSettingsService
import org.jetbrains.bsp.bazel.commons.Constants
import org.jetbrains.plugins.bsp.server.connection.ConnectionDetailsProviderExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

@TestApplication
open class MockProjectBaseTest {
  @JvmField
  @RegisterExtension
  protected val projectModel: ProjectModelExtension = ProjectModelExtension()

  protected val project: Project
    get() = projectModel.project

  private val virtualFileManager: VirtualFileManager
    get() = VirtualFileManager.getInstance()

  @BeforeEach
  protected open fun beforeEach() {}

  protected fun Path.toVirtualFile(): VirtualFile = virtualFileManager.findFileByNioPath(this)!!

  protected fun <T> runWriteAction(task: () -> T): T {
    var result: T? = null
    WriteCommandAction.runWriteCommandAction(project) {
      result = task()
    }

    return result!!
  }
}

class BazelConnectionDetailsProviderExtensionTest: MockProjectBaseTest() {

  private lateinit var extension: ConnectionDetailsProviderExtension

  private lateinit var projectRoot: VirtualFile

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()
    extension = BazelConnectionDetailsProviderExtension()

    projectRoot = createTempDirectory("root").also { it.toFile().deleteOnExit() }.toVirtualFile()
  }

  @Nested
  @DisplayName("BazelConnectionDetailsProviderExtension tests for creation project view file")
  inner class DefaultProjectViewFileTest {
    @Test
    fun `should create project view file if does not exist`() {
      // given
      // when
      val onFirstOpeningResult = runBlocking {
        extension.onFirstOpening(project, projectRoot)
      }

      // then
      onFirstOpeningResult shouldBe true
      projectRoot.findFile("projectview.bazelproject") shouldNotBe null
    }

    @Test
    fun `should keep the existing project view file if exists`() {
      // given
      runWriteAction {
        val projectViewFile = projectRoot.createFile("projectview.bazelproject")
        projectViewFile.writeText("example content")
      }

      // when
      val onFirstOpeningResult = runBlocking {
        extension.onFirstOpening(project, projectRoot)
      }

      // then
      onFirstOpeningResult shouldBe true
      projectRoot.findFile("projectview.bazelproject")?.readText() shouldBe "example content"
    }
  }

  @Nested
  @DisplayName("BazelConnectionDetailsProviderExtension tests when connection file is defined")
  inner class DefinedConnectionFileTest {
    private lateinit var initConnectionDetails: BspConnectionDetails
    private lateinit var connectionFile: VirtualFile

    @BeforeEach
    fun beforeEach() {
      // given
      initConnectionDetails = BspConnectionDetails(
        "bazelbsp",
        listOf("path/to/java", "-classpath", "classpath", "bazelbsp"),
        Constants.VERSION,
        "2.0.0",
        listOf()
      )
      runWriteAction {
        connectionFile = projectRoot.createFile(".bsp/bazelbsp.json")
        connectionFile.writeText(Gson().toJson(initConnectionDetails))
      }

      runBlocking {
        extension.onFirstOpening(project, projectRoot)
      }
    }

    @Test
    fun `should return defined connection file for current connection details equals null`() {
      // given
      // when
      val newConnectionDetails = extension.provideNewConnectionDetails(project, null)

      // then
      newConnectionDetails shouldBe initConnectionDetails
    }

    @Test
    fun `should return null if defined connection file has not changed`() {
      // given
      // when
      val newConnectionDetails = extension.provideNewConnectionDetails(project, initConnectionDetails)

      // then
      newConnectionDetails shouldBe null
    }

    @Test
    fun `should return new connection details if defined connection file has changed`() {
      // given
      val changedConnectionDetails = BspConnectionDetails(
        "bazelbsp",
        listOf("bazelbsp", "bsp"),
        "1.2.38",
        "2.0.0",
        listOf()
      )

      runWriteAction {
        connectionFile.writeText(Gson().toJson(changedConnectionDetails))
      }

      // when
      val newConnectionDetails = extension.provideNewConnectionDetails(project, initConnectionDetails)

      // then
      newConnectionDetails shouldBe changedConnectionDetails
    }
  }

  @Nested
  @DisplayName("BazelConnectionDetailsProviderExtension tests when connection file is undefined")
  inner class UndefinedConnectionFileTest {
    @BeforeEach
    fun beforeEach() {
      // given
      runBlocking {
        extension.onFirstOpening(project, projectRoot)
      }
    }

    @Test
    fun `should return connection details for current connection details equal null`() {
      // given
      // when
      val newConnectionDetails = extension.provideNewConnectionDetails(project, null)

      // then
      newConnectionDetails shouldNotBe null
    }

    @Test
    fun `should return null if connection details have not changed`() {
      // given
      // when
      val connectionDetails = extension.provideNewConnectionDetails(project, null)
      val newConnectionDetails = extension.provideNewConnectionDetails(project, connectionDetails)

      // then
      newConnectionDetails shouldBe null
    }

    @Test
    fun `should return new connection details if connection details have changed`() {
      // given
      val changedConnectionDetails = BspConnectionDetails(
        "bazelbsp",
        listOf("bazelbsp", "bsp"),
        "1.2.38",
        "2.0.0",
        listOf()
      )
      // when
      val newConnectionDetails = extension.provideNewConnectionDetails(project, changedConnectionDetails)

      // then
      newConnectionDetails shouldNotBe null
    }

    @Test
    fun `should return new connection details if selected java has changed`() {
      // given
      val selectedJdk = ProjectJdkImpl("New Jdk", JavaSdk.getInstance(), "test/home/path", null)
      val bazelApplicationSettings = BazelApplicationSettings(
        serverSettings = BazelApplicationServerSettings(
          selectedJdk = selectedJdk
        )
      )
      val bazelApplicationSettingsService = BazelApplicationSettingsService.getInstance()

      // when
      val connectionDetails = extension.provideNewConnectionDetails(project, null)
      bazelApplicationSettingsService.settings = bazelApplicationSettings
      val newConnectionDetails = extension.provideNewConnectionDetails(project, connectionDetails)

      // then
      newConnectionDetails shouldNotBe null
      newConnectionDetails?.argv?.get(0) shouldBe "test/home/path/bin/java"
    }
  }
}
