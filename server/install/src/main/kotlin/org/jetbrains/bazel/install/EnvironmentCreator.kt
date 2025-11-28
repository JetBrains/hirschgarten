package org.jetbrains.bazel.install

import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.server.bsp.utils.FileUtils.writeIfDifferent
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystem
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

private const val ASPECTS_JAR_PATH = "/" + Constants.ASPECTS_ROOT

class EnvironmentCreator(private val projectRootDir: Path) {
  fun create() = createDotBazelBsp()

  private fun createDotBazelBsp(): Path {
    val bazelBspDir = createDir(projectRootDir, Constants.DOT_BAZELBSP_DIR_NAME)
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
    copyAspectsFromResources(destinationAspectsPath)
  }

  private fun createEmptyBuildFile(dotBazelBspDir: Path) {
    dotBazelBspDir.resolve(Constants.defaultBuildFileName())
      .writeIfDifferent("")
  }

  fun createGitIgnoreFile(dotBazelBspDir: Path) {
    dotBazelBspDir.resolve(".gitignore")
      .writeIfDifferent("*")
  }

  private fun copyAspectsFromResources(destinationPath: Path) =
    javaClass.getResource(ASPECTS_JAR_PATH)?.let {
      val uri = it.toURI()
      // this is the case in bazel build
      if (uri.scheme == "jar") {
        val fileSystem = AspectsJarFileSystem.get()
        try {
          copyFileTree(fileSystem.getPath(ASPECTS_JAR_PATH), destinationPath)
        } finally {
          AspectsJarFileSystem.close()
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

    destination.resolve(Constants.BUILD_FILE_NAMES.last())
      .writeIfDifferent(
        """
        package(default_visibility = ["//visibility:public"])

        filegroup(
            name = "aspects",
            srcs = glob(["**/*"]),
        )
      """.trimIndent(),
      )
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

/**
 * See https://youtrack.jetbrains.com/issue/BAZEL-2444
 * If we unpack aspects in two different projects at the same time this may cause [FileSystemAlreadyExistsException]
 * unless we make sure only one jar file system exists at one time
 */
private object AspectsJarFileSystem {
  private var usages: Int = 0
  private var aspectsFileSystem: FileSystem? = null

  @Synchronized
  fun get(): FileSystem {
    if (usages > 0) {
      usages += 1
      return checkNotNull(aspectsFileSystem)
    }

    val url = javaClass.getResource(ASPECTS_JAR_PATH)
    val uri = url.toURI()
    check(uri.scheme == "jar")
    aspectsFileSystem = FileSystems.newFileSystem(uri, emptyMap<String, String>())
    usages += 1
    return checkNotNull(aspectsFileSystem)
  }

  @Synchronized
  fun close() {
    usages -= 1
    check(usages >= 0)
    if (usages == 0) {
      try {
        checkNotNull(aspectsFileSystem).close()
      } finally {
        aspectsFileSystem = null
      }
    }
  }
}
