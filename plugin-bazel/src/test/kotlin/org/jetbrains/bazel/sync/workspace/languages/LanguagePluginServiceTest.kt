package org.jetbrains.bazel.sync.workspace.languages

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.sync.workspace.languages.go.GoLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.jvm.JVMPackagePrefixResolver
import org.jetbrains.bazel.sync.workspace.languages.kotlin.KotlinLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.scala.ScalaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.thrift.ThriftLanguagePlugin
import org.jetbrains.bazel.workspace.model.test.framework.BazelPathsResolverMock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

class LanguagePluginServiceTest {
  private lateinit var languagePluginsService: LanguagePluginsService
  private lateinit var workspaceRoot: Path
  private lateinit var projectViewFile: Path
  private lateinit var dotBazelBspDirPath: Path

  @BeforeEach
  fun beforeEach() {
    workspaceRoot = createTempDirectory("workspaceRoot")
    projectViewFile = workspaceRoot.resolve("projectview.bazelproject")
    dotBazelBspDirPath = workspaceRoot.resolve(".bazelbsp")
    val bazelPathsResolver = BazelPathsResolverMock.create()
    languagePluginsService = LanguagePluginsService()
    languagePluginsService.registerDefaultPlugins(bazelPathsResolver)
  }

  @Nested
  @DisplayName("Tests for the method shouldGetPlugin")
  inner class ShouldGetPluginTest {
    @Test
    fun `should return JavaLanguagePlugin for Java Language`() {
      // given
      val languages: Set<LanguageClass> = hashSetOf(LanguageClass.JAVA)

      // when
      val plugin = languagePluginsService.getLanguagePlugin(languages) as? JavaLanguagePlugin

      // then
      plugin shouldNotBe null
    }

    @Test
    fun `should return KotlinLanguagePlugin for Kotlin Language`() {
      // given
      val languages: Set<LanguageClass> = hashSetOf(LanguageClass.KOTLIN)

      // when
      val plugin = languagePluginsService.getLanguagePlugin(languages) as? KotlinLanguagePlugin

      // then
      plugin shouldNotBe null
    }

    @Test
    fun `should return ScalaLanguagePlugin for Scala Language`() {
      // given
      val languages: Set<LanguageClass> = hashSetOf(LanguageClass.SCALA)

      // when
      val plugin = languagePluginsService.getLanguagePlugin(languages) as? ScalaLanguagePlugin

      // then
      plugin shouldNotBe null
    }

    @Test
    fun `should return null for no Language`() {
      // given
      val languages: Set<LanguageClass> = hashSetOf()

      // when
      val plugin = languagePluginsService.getLanguagePlugin(languages)

      // then
      plugin shouldBe null
    }

    @Test
    fun `should return ThriftLanguagePlugin for Thrift Language`() {
      // given
      val languages: Set<LanguageClass> = hashSetOf(LanguageClass.THRIFT)

      // when
      val plugin = languagePluginsService.getLanguagePlugin(languages) as? ThriftLanguagePlugin

      // then
      plugin shouldNotBe null
    }

    @Test
    fun `should return GoLanguagePlugin for Go Language`() {
      // given
      val languages: Set<LanguageClass> = hashSetOf(LanguageClass.GO)

      // when
      val plugin = languagePluginsService.getLanguagePlugin(languages) as? GoLanguagePlugin

      // then
      plugin shouldNotBe null
    }
  }

  @Nested
  @DisplayName("Tests for the method shouldGetSourceSet")
  inner class ShouldGetSourceSetTest {
    private lateinit var tmpRepo: Path

    @BeforeEach
    fun beforeEach() {
      tmpRepo = createTempDirectory()
    }

    private fun createFileAndWrite(
      dirString: String,
      filename: String,
      content: String,
    ): Path {
      val dirPath = tmpRepo.resolve(dirString)
      Files.createDirectories(dirPath)
      val filePath = dirPath.resolve(filename)
      Files.createFile(filePath)
      File(filePath.toUri()).writeText(content)
      return filePath
    }

    @AfterEach
    fun afterEach() {
      val tmpDir = tmpRepo.toFile()
      tmpDir.deleteRecursively()
    }

    @Test
    fun `should return package prefix for Java Language`() {
      // given
      val dirString = "org/jetbrains/bazel/server/sync/languages/java/"
      val filename = "JavaPackageTest.java"
      val content =
        """
                |package org.jetbrains.bazel.server.sync.languages.java;
                |
                |            public class JavaPackageTest{
                |              public public static void main(String[] args) {
                |                return;
                |              }
                |            }
        """.trimMargin()
      val filePath = createFileAndWrite(dirString, filename, content)
      val plugin = languagePluginsService.getLanguagePlugin(hashSetOf(LanguageClass.JAVA))

      // when
      val result = (plugin as JVMPackagePrefixResolver).resolveJvmPackagePrefix(filePath)

      // then
      result shouldBe "org.jetbrains.bazel.server.sync.languages.java"
    }

    @Test
    fun `should return null for empty file in Java Language`() {
      // given
      val dirString = "org/jetbrains/bazel/server/sync/languages/java/"
      val filename = "JavaPackageTest.java"
      val content = ""
      val filePath = createFileAndWrite(dirString, filename, content)
      val plugin = languagePluginsService.getLanguagePlugin(hashSetOf(LanguageClass.JAVA))

      // when
      val result = (plugin as JVMPackagePrefixResolver).resolveJvmPackagePrefix(filePath)

      // then
      result shouldBe null
    }

    @Test
    fun `should package prefix for Java Language with wrong package declaration`() {
      // given
      val dirString = "org/jetbrains/bazel/server/sync/languages/java/"
      val filename = "JavaPackageTest.java"
      val content =
        """
                |package org.jetbrains.bazel.server.sync.languages;
                |package java;
                |public class JavaPackageTest{
                |  public public static void main(String[] args) {
                |    return;
                |  }
                |}
        """.trimMargin()
      val filePath = createFileAndWrite(dirString, filename, content)
      val plugin = languagePluginsService.getLanguagePlugin(hashSetOf(LanguageClass.JAVA))

      // when
      val result = (plugin as JVMPackagePrefixResolver).resolveJvmPackagePrefix(filePath)

      // then
      result shouldBe "org.jetbrains.bazel.server.sync.languages"
    }

    @Test
    fun `should return package prefix for Scala Language from one line package declaration`() {
      // given
      val dirString = "org/jetbrains/bazel/server/sync/languages/"
      val filename = "ScalaPackageTest.java"
      val content =
        """
                |package org.jetbrains.bazel.server.sync.languages;
                |
                |   class ScalaPackageTest{
                |        def main(){
                |                return null;
                |              }
                |            }
        """.trimMargin()
      val filePath = createFileAndWrite(dirString, filename, content)
      val plugin = languagePluginsService.getLanguagePlugin(hashSetOf(LanguageClass.SCALA))

      // when
      val result = (plugin as JVMPackagePrefixResolver).resolveJvmPackagePrefix(filePath)

      // then
      result shouldBe "org.jetbrains.bazel.server.sync.languages"
    }

    @Test
    fun `should return package prefix for Scala Language from two line package declaration`() {
      // given
      val dirString = "org/jetbrains/bazel/server/sync/languages/scala/"
      val filename = "ScalaPackageTest.java"
      val content =
        """
                |package org.jetbrains.bazel.server.sync.languages;
                |package scala;
                |
                |class ScalaPackageTest{
                |  def main(){
                |    return null;
                |  }
                |}
        """.trimMargin()
      val filePath = createFileAndWrite(dirString, filename, content)
      val plugin = languagePluginsService.getLanguagePlugin(hashSetOf(LanguageClass.SCALA))

      // when
      val result = (plugin as JVMPackagePrefixResolver).resolveJvmPackagePrefix(filePath)

      // then
      result shouldBe "org.jetbrains.bazel.server.sync.languages.scala"
    }

    @Test
    fun `should return package prefix for Scala Language from multi line package declaration`() {
      // given
      val dirString = "org/jetbrains/bazel/server/sync/languages/scala/"
      val filename = "ScalaPackageTest.java"
      val content =
        """
                |package org.jetbrains.bazel;
                |package server.sync.languages;
                |
                |
                |package scala;
                |
                |class ScalaPackageTest{
                |  def main(){
                |    return null;
                |  }
                |}
        """.trimMargin()
      val filePath = createFileAndWrite(dirString, filename, content)
      val plugin = languagePluginsService.getLanguagePlugin(hashSetOf(LanguageClass.SCALA))

      // when
      val result = (plugin as JVMPackagePrefixResolver).resolveJvmPackagePrefix(filePath)

      // then
      result shouldBe "org.jetbrains.bazel.server.sync.languages.scala"
    }

    @Test
    fun `should return null for Scala Language from empty package declaration`() {
      // given
      val dirString = "org/jetbrains/bazel/server/sync/languages/"
      val filename = "ScalaPackageTest.java"
      val content = ""
      val filePath = createFileAndWrite(dirString, filename, content)
      val plugin = languagePluginsService.getLanguagePlugin(hashSetOf(LanguageClass.SCALA))

      // when
      val result = (plugin as JVMPackagePrefixResolver).resolveJvmPackagePrefix(filePath)

      // then
      result shouldBe null
    }

    @Test
    fun `should return package prefix for Kotlin Language`() {
      // given
      val dirString = "org/jetbrains/bazel/server/sync/languages/kotlin/"
      val filename = "KotlinPackageTest.kt"
      val content =
        """
                |package org.jetbrains.bazel.server.sync.languages.kotlin
                |
                |fun main() {
                |    println("Hello, World!")
                |}
        """.trimMargin()
      val filePath = createFileAndWrite(dirString, filename, content)
      val plugin = languagePluginsService.getLanguagePlugin(hashSetOf(LanguageClass.KOTLIN))

      // when
      val result = (plugin as JVMPackagePrefixResolver).resolveJvmPackagePrefix(filePath)

      // then
      result shouldBe "org.jetbrains.bazel.server.sync.languages.kotlin"
    }

    @Test
    fun `should return null for Kotlin Language for empty file`() {
      // given
      val dirString = "org/jetbrains/bazel/server/sync/languages/kotlin/"
      val filename = "KotlinPackageTest.kt"
      val content = ""
      val filePath = createFileAndWrite(dirString, filename, content)
      val plugin = languagePluginsService.getLanguagePlugin(hashSetOf(LanguageClass.KOTLIN))

      // when
      val result = (plugin as JVMPackagePrefixResolver).resolveJvmPackagePrefix(filePath)

      // then
      result shouldBe null
    }

    @Test
    fun `should return null for no Language`() {
      // given
      val dirString = "org/jetbrains/bazel/server/sync/languages/"
      val filename = "EmptyPackageTest.java"
      val content =
        """
                |package org.jetbrains.bazel.server.sync.languages
                |
        """.trimMargin()

      val filePath = createFileAndWrite(dirString, filename, content)
      val plugin = languagePluginsService.getLanguagePlugin(hashSetOf())

      // when
      val result = (plugin as? JVMPackagePrefixResolver)?.resolveJvmPackagePrefix(filePath)

      // then
      result shouldBe null
    }
  }
}
