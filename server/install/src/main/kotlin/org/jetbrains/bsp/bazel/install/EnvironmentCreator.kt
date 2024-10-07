package org.jetbrains.bsp.bazel.install

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.google.gson.GsonBuilder
import org.jetbrains.bsp.bazel.commons.Constants
import org.jetbrains.bsp.bazel.server.bsp.utils.FileUtils.writeIfDifferent
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.readText

abstract class EnvironmentCreator(private val projectRootDir: Path) {
  abstract fun create()

  protected fun createDotBazelBsp(): Path {
    val bazelBspDir = createDir(projectRootDir, Constants.DOT_BAZELBSP_DIR_NAME)
    createDotBazelBspFiles(bazelBspDir)
    return bazelBspDir
  }

  private fun createDotBazelBspFiles(dotBazelBspDir: Path) {
    copyAspects(dotBazelBspDir)
    createEmptyBuildFile(dotBazelBspDir)
  }

  private fun copyAspects(dotBazelBspDir: Path) {
    val destinationAspectsPath = dotBazelBspDir.resolve(Constants.ASPECTS_ROOT)
    copyAspectsFromResources("/" + Constants.ASPECTS_ROOT, destinationAspectsPath)
  }

  private fun createEmptyBuildFile(dotBazelBspDir: Path) {
    val destinationBuildFilePath = dotBazelBspDir.resolve(Constants.BUILD_FILE_NAME)
    val destinationWorkspaceFilePath = dotBazelBspDir.resolve(Constants.WORKSPACE_FILE_NAME)
    destinationBuildFilePath.toFile().createNewFile()
    destinationWorkspaceFilePath.toFile().createNewFile()
  }

  private fun copyAspectsFromResources(aspectsJarPath: String, destinationPath: Path) =
    javaClass.getResource(aspectsJarPath)?.let {
      FileSystems.newFileSystem(it.toURI(), emptyMap<String, String>()).use { fileSystem ->
        copyFileTree(fileSystem.getPath(aspectsJarPath), destinationPath)
      }
    } ?: error("Missing aspects resource")

  private fun copyFileTree(source: Path, destination: Path) {
    Files.walk(source).forEach { copyUsingRelativePath(source, it, destination) }
    Files.walk(destination).forEach { deleteExtraFileUsingRelativePath(source, it, destination) }
  }

  private fun copyUsingRelativePath(
    sourcePrefix: Path,
    source: Path,
    destination: Path,
  ) {
    val sourceRelativePath = sourcePrefix.relativize(source).toString()
    val destinationAbsolutePath = destination.resolve(sourceRelativePath)
    if (source.isDirectory()) {
      if (!destinationAbsolutePath.isDirectory()) {
        destinationAbsolutePath.deleteIfExists()
        destinationAbsolutePath.createDirectory()
      }
    } else {
      destinationAbsolutePath.writeIfDifferent(source.readText())
    }
  }

  @OptIn(ExperimentalPathApi::class)
  private fun deleteExtraFileUsingRelativePath(
    source: Path,
    destination: Path,
    destinationPrefix: Path,
  ) {
    val destinationRelativePath = destinationPrefix.relativize(destination).toString()
    val sourceAbsolutePath = source.resolve(destinationRelativePath)
    // extension.bzl is written in BazelBspLanguageExtensionsGenerator, we don't have to delete it
    if (destinationRelativePath == Constants.EXTENSIONS_BZL) return
    // Templates are expanded inside TemplateWriter, we don't have to delete them
    if (sourceAbsolutePath.notExists() && sourceAbsolutePath.resolveSibling("${sourceAbsolutePath.name}.template").notExists()) {
      destination.deleteRecursively()
    }
  }

  protected fun createDotBsp(discoveryDetails: BspConnectionDetails) {
    val dir = createDir(projectRootDir, Constants.DOT_BSP_DIR_NAME)
    createBspDiscoveryDetailsFile(dir, discoveryDetails)
  }

  private fun createDir(rootDir: Path, name: String): Path {
    val dir = rootDir.resolve(name)
    try {
      Files.createDirectories(dir)
    } catch (_: FileAlreadyExistsException) {
    }
    return dir
  }

  private fun createBspDiscoveryDetailsFile(dotBspDir: Path, discoveryDetails: BspConnectionDetails) {
    val destinationBspDiscoveryFilePath = dotBspDir.resolve(Constants.BAZELBSP_JSON_FILE_NAME)
    writeJsonToFile(destinationBspDiscoveryFilePath, discoveryDetails)
  }

  private fun <T> writeJsonToFile(destinationPath: Path, data: T) {
    val fileContent = GsonBuilder().setPrettyPrinting().create().toJson(data)
    Files.writeString(destinationPath, fileContent)
  }
}
