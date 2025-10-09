package org.jetbrains.bazel.annotation

import com.google.common.reflect.ClassPath
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.annotations.InternalApi
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.test.framework.ResourceUtil
import org.jetbrains.bazel.test.framework.annotation.OnlyHirschgertenTest
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.net.URLClassLoader
import kotlin.io.path.readText

private const val PLUGIN_BAZEL_JAR = "plugin-bazel/plugin-bazel.jar"
private const val PUBLIC_API_CLASSES = "public_api/public-api-classes.txt"
private const val PUBLIC_API_METHODS = "public_api/public-api-methods.txt"
private const val PUBLIC_API_FIELDS = "public_api/public-api-fields.txt"
private const val PACKAGE_PREFIX = "org.jetbrains.bazel"

/**
 * If this test fails after you've deleted, renamed, or moved a class to a different package, then revert that change
 * because we don't want to break API for other plugins (Scala, internal customer plugins) that depend on us.
 *
 * If this test fails because you annotated a new class or member with [PublicApi]:
 * - For Bazel plugin team members: open the `test.log`, search for `but was:`,
 * and use that to add the new fields the corresponding list (`public-api-classes.txt`, `public-api-methods.txt`, `public-api-fields.txt`).
 * You can also add [InternalApi] on methods or fields of public classes that you don't want to be public.
 *
 * - For other contributors: consult with Bazel plugin team members before changing any public APIs, as any changes can break other dependent plugins/customers.
 */
@OnlyHirschgertenTest
class PublicApiCheckTest {
  @Test
  fun `check that plugin's public API did not change`() =
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
      val methods = classes.flatMap { it.declaredMethods.toList() }
      val fields = classes.flatMap { it.declaredFields.toList() }

      val publicApiAnnotationClass = PublicApi::class.java
      val internalApiAnnotationClass = InternalApi::class.java
      val publicApiClasses = classes.filter { it.isAnnotationPresent(publicApiAnnotationClass) }
      val publicApiClassesWithoutAdditionalAnnotationsForMembers =
        publicApiClasses.filter { publicApiClass ->
          publicApiClass.declaredMethods.none { it.isAnnotationPresent(publicApiAnnotationClass) } &&
            publicApiClass.declaredMethods.none { it.isAnnotationPresent(internalApiAnnotationClass) } &&
            publicApiClass.declaredFields.none { it.isAnnotationPresent(publicApiAnnotationClass) } &&
            publicApiClass.declaredFields.none { it.isAnnotationPresent(internalApiAnnotationClass) }
        }
      val publicApiMethods =
        methods.filter { it.isAnnotationPresent(publicApiAnnotationClass) } +
          publicApiClassesWithoutAdditionalAnnotationsForMembers.flatMap { it.declaredMethods.toList() }.filter {
            Modifier.isPublic(
              it.modifiers,
            )
          }
      val publicApiFields =
        fields.filter { it.isAnnotationPresent(publicApiAnnotationClass) } +
          publicApiClassesWithoutAdditionalAnnotationsForMembers.flatMap { it.declaredFields.toList() }.filter {
            Modifier.isPublic(
              it.modifiers,
            )
          }

      val expectedPublicApiClasses = BazelPathManager.getTestFixturePath(PUBLIC_API_CLASSES)
      publicApiClasses
        .map { it.toString() }
        .distinct()
        .sorted()
        .joinToString("\n") shouldBe expectedPublicApiClasses.readText().trim()

      val expectedPublicApiMethods = BazelPathManager.getTestFixturePath(PUBLIC_API_METHODS)
      publicApiMethods
        .map { it.toString() }
        .distinct()
        .sorted()
        .joinToString("\n") shouldBe expectedPublicApiMethods.readText().trim()

      val expectedPublicApiFields = BazelPathManager.getTestFixturePath(PUBLIC_API_FIELDS)
      publicApiFields
        .map { it.toString() }
        .distinct()
        .sorted()
        .joinToString("\n") shouldBe expectedPublicApiFields.readText().trim()

    }
}
