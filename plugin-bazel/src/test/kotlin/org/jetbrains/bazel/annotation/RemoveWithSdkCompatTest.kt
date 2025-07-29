package org.jetbrains.bazel.annotation

import com.google.common.reflect.ClassPath
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.annotations.RemoveWithSdkCompat
import org.jetbrains.bazel.resourceUtil.ResourceUtil
import org.junit.jupiter.api.Test
import java.net.URLClassLoader
import java.nio.file.Paths
import kotlin.io.path.readText

private const val PLUGIN_BAZEL_JAR = "plugin-bazel/plugin-bazel.jar"
private const val PACKAGE_PREFIX = "org.jetbrains.bazel"
private const val SDK_COMPAT_STRUCTURE_OUTPUT = "plugin-bazel/src/test/kotlin/org/jetbrains/bazel/annotation/sdkCompatStructure"

/**
 * Test for the [RemoveWithSdkCompat] annotation.
 *
 * The [RemoveWithSdkCompat] annotation is used to mark classes that should be removed
 * when removing sdkcompat targets like //sdkcompat/v251 and //sdkcompat/v252.
 */
class RemoveWithSdkCompatTest {
  @Test
  fun `should verify RemoveWithSdkCompat annotation properties`() {
    // Verify the annotation class exists and has the expected properties
    val annotationClass = RemoveWithSdkCompat::class

    // Check that it's an annotation class
    annotationClass.java.isAnnotation shouldBe true

    // Check that it has a version parameter
    val versionParameter =
      annotationClass.constructors
        .first()
        .parameters
        .first()
    versionParameter.name shouldBe "version"
    versionParameter.type.classifier shouldBe String::class
  }

  @Test
  fun `should remove classes or methods along with the removal of sdkcompat layer`() {
    val currentSdkCompatVersions = extractVersions()
    ResourceUtil.useResource(PLUGIN_BAZEL_JAR) { pluginJar ->
      val classLoader = URLClassLoader(arrayOf(pluginJar.toUri().toURL()))
      val classPath = ClassPath.from(classLoader)
      val classNames = classPath.allClasses.map { it.name }.filter { it.startsWith(PACKAGE_PREFIX) }

      val classes =
        classNames.mapNotNull { className ->
          try {
            Class.forName(className)
          } catch (_: Throwable) {
            null
          }
        }
      val classesWithAnnotation = classes.filter { it.isAnnotationPresent(RemoveWithSdkCompat::class.java) }
      val classesToRemove = mutableSetOf<String>()
      classesWithAnnotation.forEach { clazz ->
        clazz.annotations.filterIsInstance<RemoveWithSdkCompat>().forEach {
          if (it.version !in currentSdkCompatVersions) {
            classesToRemove.add(clazz.name)
          }
        }
      }
      assert(classesToRemove.isEmpty()) { "The following classes should be removed: $classesToRemove" }

      val methods = classes.flatMap { it.declaredMethods.toList() }
      val methodsWithAnnotation = methods.filter { it.isAnnotationPresent(RemoveWithSdkCompat::class.java) }
      val methodsToRemove = mutableSetOf<String>()
      methodsWithAnnotation.forEach { method ->
        method.annotations.filterIsInstance<RemoveWithSdkCompat>().forEach {
          if (it.version !in currentSdkCompatVersions) {
            methodsToRemove.add(method.name)
          }
        }
      }
      assert(methodsToRemove.isEmpty()) { "The following methods should be removed: $methodsToRemove" }
    }
  }

  private fun extractVersions(): Set<String> {
    val labels = Paths.get(SDK_COMPAT_STRUCTURE_OUTPUT).readText().split("\n")
    val regex = Regex("sdkcompat/(v\\d+)")
    val versions =
      labels
        .mapNotNull { label ->
          regex
            .find(label)
            ?.groups
            ?.get(1)
            ?.value
        }.toSet()
    if (versions.isEmpty()) {
      throw IllegalStateException("No sdkcompat versions found")
    }
    return versions
  }
}
