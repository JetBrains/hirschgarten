package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.EnvironmentUtil
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path

public class TemporarySbtBspConnectionDetailsGenerator : BspConnectionDetailsGeneratorExtension {
  private val log = logger<TemporarySbtBspConnectionDetailsGenerator>()

  override fun id(): String = "sbt"

  override fun displayName(): String = "Sbt"

  override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean =
    projectPath.children.any { it.name == "build.sbt" }

  override fun generateBspConnectionDetailsFile(projectPath: VirtualFile, outputStream: OutputStream): VirtualFile {
    executeAndWait(
      command = listOf(findCoursierExecutableOrPrepare(projectPath).toString(), "launch", "sbt", "--", "bspConfig"),
      projectPath = projectPath,
      outputStream = outputStream,
      log = log
    )
    return getChild(projectPath, listOf(".bsp", "sbt.json"))!!
  }

  // TODO copied from bazel extension, can we somehow move it to the ep?
  private fun findCoursierExecutableOrPrepare(projectPath: VirtualFile): Path =
    findCoursierExecutable() ?: prepareCoursierIfNotExists(projectPath)

  private fun findCoursierExecutable(): Path? =
    EnvironmentUtil.getEnvironmentMap()["PATH"]
      ?.split(File.pathSeparator)
      ?.map { File(it, CoursierUtils.calculateCoursierExecutableName()) }
      ?.firstOrNull { it.canExecute() }
      ?.toPath()

  private fun prepareCoursierIfNotExists(projectPath: VirtualFile): Path {
    // TODO we should pass it to syncConsole - it might take some time if the connection is really bad
    val coursierDestination = calculateCoursierExecutableDestination(projectPath)

    CoursierUtils.prepareCoursierIfDoesntExistInTheDestination(coursierDestination)

    return coursierDestination
  }

  private fun calculateCoursierExecutableDestination(projectPath: VirtualFile): Path {
    val dotBazelBsp = projectPath.toNioPath().resolve(".bazelbsp")
    Files.createDirectories(dotBazelBsp)

    return dotBazelBsp.resolve(CoursierUtils.calculateCoursierExecutableName())
  }
}
