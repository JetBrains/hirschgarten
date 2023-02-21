package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.EnvironmentUtil
import java.io.File
import java.io.OutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

public class TemporarySbtBspConnectionDetailsGenerator : BspConnectionDetailsGeneratorExtension {
  override fun id(): String = "sbt"

  override fun displayName(): String = "Sbt"

  override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean =
    projectPath.children.any { it.name == "build.sbt" }

  override fun generateBspConnectionDetailsFile(projectPath: VirtualFile, outputStream: OutputStream): VirtualFile {
    executeAndWait(listOf(findCoursierExecutableOrDownload(projectPath).toString(), "launch", "sbt", "--", "bspConfig"), projectPath, outputStream)
    return getChild(projectPath, listOf(".bsp", "sbt.json"))!!
  }

  // TODO copied from bazel extension, can we somehow move it to the ep?
  private fun findCoursierExecutableOrDownload(projectPath: VirtualFile): Path =
    findCoursierExecutable() ?: downloadCoursierIfNotDownloaded(projectPath)

  private fun findCoursierExecutable(): Path? =
    EnvironmentUtil.getEnvironmentMap()["PATH"]
      ?.split(File.pathSeparator)
      ?.map { File(it, "cs") }
      ?.firstOrNull { it.canExecute() }
      ?.toPath()

  private fun downloadCoursierIfNotDownloaded(projectPath: VirtualFile): Path {
    // TODO we should pass it to syncConsole - it might take some time if the connection is really bad
    val coursierUrl = "https://git.io/coursier-cli"
    val coursierDestination = calculateCoursierDownloadDestination(projectPath)

    downloadCoursierIfDoesntExistInTheDestination(coursierDestination, coursierUrl)

    return coursierDestination
  }

  private fun calculateCoursierDownloadDestination(projectPath: VirtualFile): Path {
    val dotBazelBsp = projectPath.toNioPath().resolve(".bazelbsp")
    Files.createDirectories(dotBazelBsp)

    return dotBazelBsp.resolve("cs")
  }

  private fun downloadCoursierIfDoesntExistInTheDestination(coursierDestination: Path, coursierUrl: String) {
    if (!coursierDestination.toFile().exists()) {
      downloadCoursier(coursierUrl, coursierDestination)
    }
  }

  private fun downloadCoursier(coursierUrl: String, coursierDestination: Path) {
    Files.copy(URL(coursierUrl).openStream(), coursierDestination)
    coursierDestination.toFile().setExecutable(true)
  }
}
