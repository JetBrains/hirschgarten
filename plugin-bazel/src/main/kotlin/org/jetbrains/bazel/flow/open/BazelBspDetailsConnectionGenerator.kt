package org.jetbrains.bazel.flow.open

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.assets.BspPluginTemplates
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.jdk.javaapi.CollectionConverters.asJava
import scala.jdk.javaapi.CollectionConverters.asScala
import java.io.File
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.collections.List
import kotlin.collections.map
import kotlin.io.path.exists
import kotlin.io.path.writeText
import coursier.core.Dependency as CoursierDependency
import coursier.core.Module as CoursierModule
import scala.collection.immutable.List as ScalaImmutableList
import scala.collection.immutable.Map as ScalaImmutableMap

private const val DEFAULT_PROJECT_VIEW_FILE_NAME = "projectview.bazelproject"

internal class BazelBspDetailsConnectionGenerator : BspConnectionDetailsGeneratorExtension {
  private lateinit var projectViewFilePath: Path
  public override fun id(): String = BazelPluginConstants.ID

  public override fun displayName(): String = "Bazel"

  public override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean =
    projectPath.children.any { it.name in BazelPluginConstants.WORKSPACE_FILE_NAMES }

  public override fun generateBspConnectionDetailsFile(
    projectPath: VirtualFile,
    outputStream: OutputStream,
    project: Project,
  ): VirtualFile {
    initializeProjectViewFile(projectPath)
    executeAndWait(
      command = calculateInstallerCommand(),
      projectPath = projectPath,
      outputStream = outputStream,
      project = project,
    )
    return getChild(projectPath, listOf(".bsp", "bazelbsp.json"))!!
  }

  private fun initializeProjectViewFile(projectPath: VirtualFile) {
    projectViewFilePath = calculateProjectViewFilePath(projectPath)
    setDefaultProjectViewFilePathContentIfNotExists()
  }

  private fun setDefaultProjectViewFilePathContentIfNotExists() {
    if (!projectViewFilePath.exists()) {
      projectViewFilePath.writeText(BspPluginTemplates.defaultBazelProjectViewContent)
    }
  }

  private fun calculateInstallerCommand(): List<String> = listOf(
    ExternalCommandUtils.calculateJavaExecPath(),
    "-cp",
    ExternalCommandUtils.calculateNeededJars(
      org = "org.jetbrains.bsp",
      name = "bazel-bsp",
      version = "3.1.0-20231020-cd64dbb-NIGHTLY",
    )
      .joinToString(":"),
    "org.jetbrains.bsp.bazel.install.Install",
  ) + calculateProjectViewFileInstallerOption()

  private fun calculateProjectViewFileInstallerOption(): List<String> =
    listOf("-p", "$projectViewFilePath")

  private fun calculateProjectViewFilePath(projectPath: VirtualFile): Path =
    projectPath.toNioPath().toAbsolutePath().resolve(DEFAULT_PROJECT_VIEW_FILE_NAME)
}

internal object ExternalCommandUtils {
  fun calculateJavaExecPath(): String {
    val javaHome = System.getProperty("java.home")
    if (javaHome == null) {
      error("Java needs to be set up before running the plugin")
    } else {
      return "${Paths.get(javaHome, "bin", "java")}"
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun calculateNeededJars(org: String, name: String, version: String): List<String> {
    val attributes = ScalaImmutableMap.from(asScala(mapOf<String, String>()))
    val dependencies = listOf<CoursierDependency>(
      CoursierDependency.apply(
        CoursierModule.apply(org, name, attributes),
        version,
      ),
    )
    val fetchTask = coursier
      .Fetch
      .apply()
      .addDependencies(asScala(dependencies).toSeq())
    val executionContext = fetchTask.cache().ec()
    val future = fetchTask.io().future(executionContext)
    val futureResult = Await.result(future, Duration.Inf())
    return asJava(futureResult as ScalaImmutableList<File>).map { it.canonicalPath }
  }
}
