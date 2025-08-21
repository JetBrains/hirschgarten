package org.jetbrains.bazel.install

import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.server.bsp.utils.FileUtils.writeIfDifferent
import java.nio.file.FileAlreadyExistsException
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

class EnvironmentCreator(private val projectRootDir: Path) {
  fun create() = createDotBazelBsp()

  private fun createDotBazelBsp(): Path {
    val bazelBspDir = createDir(projectRootDir, Constants.DOT_BAZELBSP_DIR_NAME)
    // Always run idempotent creators: they only write when needed and won't duplicate work
    createDotBazelBspFiles(bazelBspDir)
    return bazelBspDir
  }

  private fun createDotBazelBspFiles(dotBazelBspDir: Path) {
    createGitIgnoreFile(dotBazelBspDir)
    copyAspects(dotBazelBspDir)
    createEmptyBuildFile(dotBazelBspDir)
  }

  private fun copyAspects(dotBazelBspDir: Path) {
    val destinationAspectsPath = dotBazelBspDir.resolve(Constants.ASPECTS_ROOT)
    copyAspectsFromResources("/" + Constants.ASPECTS_ROOT, destinationAspectsPath)
  }

  private fun createEmptyBuildFile(dotBazelBspDir: Path) {
    val destinationBuildFilePath = dotBazelBspDir.resolve(Constants.defaultBuildFileName())
    val destinationWorkspaceFilePath = dotBazelBspDir.resolve(Constants.WORKSPACE_FILE_NAME)
    destinationBuildFilePath.toFile().createNewFile()
    destinationWorkspaceFilePath.toFile().createNewFile()
  }

  fun createGitIgnoreFile(dotBazelBspDir: Path) {
    val outputFile = dotBazelBspDir.resolve(".gitignore")
    outputFile.writeIfDifferent("*")
  }

  private fun copyAspectsFromResources(aspectsJarPath: String, destinationPath: Path) =
    javaClass.getResource(aspectsJarPath)?.let {
      val uri = it.toURI()
      // this is the case in bazel build
      if (uri.scheme == "jar") {
        FileSystems.newFileSystem(uri, emptyMap<String, String>()).use { fileSystem ->
          copyFileTree(fileSystem.getPath(aspectsJarPath), destinationPath)
        }
        // and this in JPS
      } else if (uri.scheme == "file") {
        copyFileTree(Path.of(uri), destinationPath)
      }
    } ?: error("Missing aspects resource")

  private fun copyFileTree(source: Path, destination: Path) {
    Files.walk(source).use { sourceFiles ->
      sourceFiles.forEach { copyUsingRelativePath(source, it, destination) }
    }
    Files.walk(destination).use { destinationFiles ->
      destinationFiles.forEach { deleteExtraFileUsingRelativePath(source, it, destination) }
    }
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
  //
  // private fun copyUsingRelativePath(
  //  sourcePrefix: Path,
  //  source: Path,
  //  destinationPrefix: Path,
  // ) {
  //  val rel = sourcePrefix.relativize(source).toString()
  //  if (rel.isEmpty()) return
  //  val dest = destinationPrefix.resolve(rel)
  //  if (source.isDirectory()) {
  //    try {
  //      Files.createDirectories(dest)
  //    } catch (_: FileAlreadyExistsException) {}
  //    return
  //  }
  //  // Ensure parent directory exists
  //  dest.parent?.let { Files.createDirectories(it) }
  //  val shouldCopy = if (Files.exists(dest)) {
  //    val srcBytes = Files.readAllBytes(source)
  //    val dstBytes = Files.readAllBytes(dest)
  //    !srcBytes.contentEquals(dstBytes)
  //  } else {
  //    true
  //  }
  //  if (shouldCopy) {
  //    Files.copy(source, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
  //  }
  // }

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
    val templateAbsolutePath = sourceAbsolutePath.resolveSibling(sourceAbsolutePath.name + Constants.TEMPLATE_EXTENSION)
    if (sourceAbsolutePath.notExists() && templateAbsolutePath.notExists()) {
      destination.deleteRecursively()
    }
  }

  private fun createDir(rootDir: Path, name: String): Path {
    val dir = rootDir.resolve(name)
    try {
      Files.createDirectories(dir)
    } catch (_: FileAlreadyExistsException) {
    }
    return dir
  }
}
