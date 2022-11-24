package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.EnvironmentUtil
import java.io.File
import java.io.OutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

public class TemporarySbtBspConnectionDetailsGenerator : BspConnectionDetailsGeneratorExtension {
  override fun name(): String = "sbt"

  override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean =
    projectPath.children.any { it.name == "build.sbt" }

  override fun generateBspConnectionDetailsFile(projectPath: VirtualFile, outputStream: OutputStream): VirtualFile {
    executeAndWait(listOf(findCoursierExecutableOrDownload(projectPath).toString(), "launch", "sbt", "--", "bspConfig"), projectPath, outputStream)
    return getChild(projectPath, listOf(".bsp", "sbt.json"))!!
  }

  // TODO copied from bazel extension, can we somehow move it to the ep?
  private fun findCoursierExecutableOrDownload(projectPath: VirtualFile): Path =
    findCoursierExecutable() ?: downloadCoursier(projectPath)

  private fun findCoursierExecutable(): Path? =
    EnvironmentUtil.getEnvironmentMap()["PATH"]
      ?.split(File.pathSeparator)
      ?.map { File(it, "cs") }
      ?.firstOrNull { it.canExecute() }
      ?.toPath()

  private fun downloadCoursier(projectPath: VirtualFile): Path {
    val coursierUrl = "https://git.io/coursier-cli"
    val coursierDestination = calculateCoursierDownloadDestination(projectPath)

    Files.copy(URL(coursierUrl).openStream(), coursierDestination)
    coursierDestination.toFile().setExecutable(true)

    return coursierDestination
  }

  private fun calculateCoursierDownloadDestination(projectPath: VirtualFile): Path {
    val dotBazelBsp = projectPath.toNioPath().resolve(".bazelbsp")
    Files.createDirectories(dotBazelBsp)

    return dotBazelBsp.resolve("cs")
  }
}
