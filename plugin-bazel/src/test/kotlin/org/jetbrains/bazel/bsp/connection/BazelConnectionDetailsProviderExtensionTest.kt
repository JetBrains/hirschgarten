package org.jetbrains.bazel.bsp.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.Gson
import com.google.idea.testing.BazelTestApplication
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.findOrCreateDirectory
import com.intellij.openapi.vfs.findOrCreateFile
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.writeText
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.rules.ProjectModelExtension
import com.intellij.testFramework.utils.vfs.createFile
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.settings.BazelProjectSettings
import org.jetbrains.bazel.settings.bazelProjectSettings
import org.jetbrains.bsp.bazel.commons.Constants
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.impl.server.connection.ConnectionDetailsProviderExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory

@BazelTestApplication
open class MockProjectBaseTest : Disposable {
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

  override fun dispose() {
    Disposer.dispose(project)
  }
}

class BazelConnectionDetailsProviderExtensionTest : MockProjectBaseTest() {
  private lateinit var extension:
    ConnectionDetailsProviderExtension

  private lateinit var projectRoot: VirtualFile

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()
    extension = BazelConnectionDetailsProviderExtension()

    projectRoot = createTempDirectory("root").also { it.toFile().deleteOnExit() }.toVirtualFile()
    project.rootDir = projectRoot
  }

  @Nested
  @DisplayName("BazelConnectionDetailsProviderExtension tests for creation project view file")
  inner class DefaultProjectViewFileTest {
    @Test
    fun `should create project view file if does not exist`() {
      // given
      // when
      val onFirstOpeningResult =
        runBlocking {
          extension.onFirstOpening(project, projectRoot)
        }

      // then
      onFirstOpeningResult shouldBe true
      projectRoot.findFile(".bazelbsp/.bazelproject") shouldNotBe null
    }

    @Test
    fun `should keep the existing legacy project view file if exists`() {
      // given
      runWriteAction {
        val projectViewFile = projectRoot.createFile(".bazelbsp/projectview.bazelproject")
        projectViewFile.writeText("example content")
      }

      // when
      val onFirstOpeningResult =
        runBlocking {
          extension.onFirstOpening(project, projectRoot)
        }

      // then
      onFirstOpeningResult shouldBe true
      projectRoot.findFile(".bazelbsp/.bazelproject") shouldBe null
      projectRoot.findFile(".bazelbsp/projectview.bazelproject")?.readText() shouldBe "example content"
    }
  }

  @Test
  fun `should keep the existing project view file if exists`() {
    // given
    runWriteAction {
      val projectViewFile = projectRoot.createFile(".bazelbsp/.bazelproject")
      projectViewFile.writeText("example content")
    }

    // when
    val onFirstOpeningResult =
      runBlocking {
        extension.onFirstOpening(project, projectRoot)
      }

    // then
    onFirstOpeningResult shouldBe true
    projectRoot.findFile(".bazelbsp/projectview.bazelproject") shouldBe null
    projectRoot.findFile(".bazelbsp/.bazelproject")?.readText() shouldBe "example content"
  }

  fun `should not generate the new project view file if managed file exists`() {
    // given
    runWriteAction {
      val projectViewFile = projectRoot.createFile("tools/intellij/.managed.bazelproject")
      projectViewFile.writeText("example content")
    }

    // when
    val onFirstOpeningResult =
      runBlocking {
        extension.onFirstOpening(project, projectRoot)
      }

    // then
    onFirstOpeningResult shouldBe true
    projectRoot.findFile(".bazelbsp/projectview.bazelproject") shouldBe null
    projectRoot.findFile(".bazelbsp/.bazelproject") shouldBe null
  }

  @Nested
  @DisplayName("BazelConnectionDetailsProviderExtension tests when connection file is defined")
  inner class DefinedConnectionFileTest {
    private lateinit var initConnectionDetails: BspConnectionDetails
    private lateinit var connectionFile: VirtualFile

    @BeforeEach
    fun beforeEach() {
      // given
      initConnectionDetails =
        BspConnectionDetails(
          "bazelbsp",
          listOf("path/to/java", "-classpath", "classpath", "bazelbsp"),
          Constants.VERSION,
          "2.0.0",
          listOf(),
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
      val changedConnectionDetails =
        BspConnectionDetails(
          "bazelbsp",
          listOf("bazelbsp", "bsp"),
          "1.2.38",
          "2.0.0",
          listOf(),
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
    val selectedJdkName = "New Jdk"

    @BeforeEach
    fun beforeEach() {
      // given
      runBlocking {
        writeAction {
          val jdkMockHomePath = projectRoot.findOrCreateDirectory("jdk")
          jdkMockHomePath
            .findOrCreateFile("bin/java")
            .toNioPath()
            .toFile()
            .setExecutable(true)

          val mockJdk = IdeaTestUtil.createMockJdk(selectedJdkName, jdkMockHomePath.toNioPath().toString())
          ProjectJdkTable.getInstance().addJdk(mockJdk)
        }
      }

      runBlocking {
        extension.onFirstOpening(project, projectRoot)
      }
    }

    @AfterEach
    fun afterEach() {
      val jdkTable = ProjectJdkTable.getInstance()
      runBlocking {
        writeAction {
          jdkTable.findJdk(selectedJdkName)?.also { jdkTable.removeJdk(it) }
        }
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
      val changedConnectionDetails =
        BspConnectionDetails(
          "bazelbsp",
          listOf("bazelbsp", "bsp"),
          "1.2.38",
          "2.0.0",
          listOf(),
        )
      // when
      val newConnectionDetails = extension.provideNewConnectionDetails(project, changedConnectionDetails)

      // then
      newConnectionDetails shouldNotBe null
    }

    @Test
    fun `should return new connection details if selected java has changed`() {
      // given
      val bazelProjectSettings =
        BazelProjectSettings(
          projectViewPath = null,
          selectedServerJdkName = selectedJdkName,
        )

      // when
      val connectionDetails = extension.provideNewConnectionDetails(project, null)
      project.bazelProjectSettings = bazelProjectSettings
      val newConnectionDetails = extension.provideNewConnectionDetails(project, connectionDetails)

      // then
      newConnectionDetails shouldNotBe null
    }

    @Test
    fun `should return new connection details if server custom jvm options have changed`() {
      // given
      val customJvmOptions = listOf("-XCustomOption", "-XAnotherCustomOption")

      val bazelProjectSettings =
        BazelProjectSettings(
          projectViewPath = null,
          selectedServerJdkName = selectedJdkName,
          customJvmOptions = customJvmOptions,
        )

      // when
      val connectionDetails = extension.provideNewConnectionDetails(project, null)
      project.bazelProjectSettings = bazelProjectSettings
      val newConnectionDetails = extension.provideNewConnectionDetails(project, connectionDetails)

      // then
      newConnectionDetails shouldNotBe null
      newConnectionDetails?.argv!! shouldContainAll customJvmOptions
    }

    @Test
    fun `should return new connection details if server custom jvm options have changed to empty list`() {
      // given
      val initCustomJvmOptions = listOf("-XCustomOption", "-XAnotherCustomOption")

      val initBazelProjectSettings =
        BazelProjectSettings(
          projectViewPath = null,
          selectedServerJdkName = selectedJdkName,
          customJvmOptions = initCustomJvmOptions,
        )
      project.bazelProjectSettings = initBazelProjectSettings
      val connectionDetails = extension.provideNewConnectionDetails(project, null)

      // when
      val bazelProjectSettings =
        BazelProjectSettings(
          projectViewPath = null,
          selectedServerJdkName = selectedJdkName,
          customJvmOptions = emptyList(),
        )
      project.bazelProjectSettings = bazelProjectSettings
      val newConnectionDetails = extension.provideNewConnectionDetails(project, connectionDetails)

      // then
      newConnectionDetails shouldNotBe null
      newConnectionDetails?.argv!! shouldNotContainAnyOf initCustomJvmOptions
    }
  }
}
