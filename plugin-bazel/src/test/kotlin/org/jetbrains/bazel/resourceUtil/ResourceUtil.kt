package org.jetbrains.bazel.resourceUtil

import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createParentDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.toPath
import kotlin.jvm.javaClass

object ResourceUtil {
  @OptIn(ExperimentalPathApi::class)
  fun useResource(
    resourceLocation: String,
    resourceUrl: URL? = null,
    block: ((Path) -> Unit),
  ) {
    val resourceUrl =
      resourceUrl ?: requireNotNull(javaClass.classLoader.getResource(resourceLocation)) {
        "Can't find resource at location $resourceLocation"
      }
    // check if the resource is inside a jar
    // if it is, it will look something like this: jar:file:/home/andrzej.gluszak/.cache/bazel/_bazel_andrzej.gluszak/e06fcdf30b05e14cf4fddcd75b67a39c/execroot/_main/bazel-out/k8-fastbuild/bin/performance-testing/performance-testing.jar!/plugin.zip
    // we need to extract it to a temporary file then
    // This happens when the test is run through bazel
    if (resourceUrl.protocol == "jar") {
      val jarPath = resourceUrl.path.substringAfter("file:").substringBefore("!/")
      // extract the jar (which is a zip) to a temporary directory
      val tempDir = createTempDirectory("bazel-test-resource")
      val targetPath = (tempDir / resourceLocation).also { it.createParentDirectories() }

      extractFileFromJar(jarPath, resourceLocation, targetPath.toString())
      try {
        block(targetPath)
      } finally {
        tempDir.deleteRecursively()
      }
    } else {
      block(resourceUrl.toURI().toPath())
    }
  }

  private fun extractFileFromJar(
    jarFilePath: String,
    fileName: String,
    destFilePath: String,
  ) {
    JarFile(jarFilePath).use { jarFile ->
      val jarEntry = jarFile.getJarEntry(fileName)
      jarFile.getInputStream(jarEntry).use { input ->
        FileOutputStream(destFilePath).use { output ->
          input.copyTo(output)
        }
      }
    }
  }
}
