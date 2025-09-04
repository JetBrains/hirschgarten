package org.jetbrains.bazel.sync.workspace.languages

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.startup.IntellijEnvironmentProvider
import org.jetbrains.bazel.sync.workspace.languages.go.GoLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.jvm.JVMPackagePrefixResolver
import org.jetbrains.bazel.sync.workspace.languages.kotlin.KotlinLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.scala.ScalaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.thrift.ThriftLanguagePlugin
import org.jetbrains.bazel.workspace.model.test.framework.BazelPathsResolverMock
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

class LanguagePluginServiceTest : BasePlatformTestCase() {
  private lateinit var languagePluginsService: LanguagePluginsService
  private lateinit var workspaceRoot: Path
  private lateinit var projectViewFile: Path
  private lateinit var dotBazelBspDirPath: Path
  private lateinit var tmpRepo: Path

  override fun setUp() {
    super.setUp()
    EnvironmentProvider.provideEnvironmentProvider(IntellijEnvironmentProvider)
    
    workspaceRoot = createTempDirectory("workspaceRoot")
    projectViewFile = workspaceRoot.resolve("projectview.bazelproject")
    dotBazelBspDirPath = workspaceRoot.resolve(".bazelbsp")
    val bazelPathsResolver = BazelPathsResolverMock.create()
    languagePluginsService = LanguagePluginsService(project)
    languagePluginsService.registerDefaultPlugins(bazelPathsResolver, DefaultJvmPackageResolver())
    
    tmpRepo = createTempDirectory()
  }

  override fun tearDown() {
    try {
      if (::tmpRepo.isInitialized) {
        val tmpDir = tmpRepo.toFile()
        tmpDir.deleteRecursively()
      }
    } finally {
      super.tearDown()
    }
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

  fun testShouldReturnJavaLanguagePluginForJavaLanguage() {
    // given
    val languages: Set<LanguageClass> = hashSetOf(LanguageClass.JAVA)

    // when
    val plugin = languagePluginsService.getLanguagePlugin(languages) as? JavaLanguagePlugin

    // then
    assertNotNull(plugin)
  }

  fun testShouldReturnKotlinLanguagePluginForKotlinLanguage() {
    // given
    val languages: Set<LanguageClass> = hashSetOf(LanguageClass.KOTLIN)

    // when
    val plugin = languagePluginsService.getLanguagePlugin(languages) as? KotlinLanguagePlugin

    // then
    assertNotNull(plugin)
  }

  fun testShouldReturnScalaLanguagePluginForScalaLanguage() {
    // given
    val languages: Set<LanguageClass> = hashSetOf(LanguageClass.SCALA)

    // when
    val plugin = languagePluginsService.getLanguagePlugin(languages) as? ScalaLanguagePlugin

    // then
    assertNotNull(plugin)
  }

  fun testShouldReturnNullForNoLanguage() {
    // given
    val languages: Set<LanguageClass> = hashSetOf()

    // when
    val plugin = languagePluginsService.getLanguagePlugin(languages)

    // then
    assertNull(plugin)
  }

  fun testShouldReturnThriftLanguagePluginForThriftLanguage() {
    // given
    val languages: Set<LanguageClass> = hashSetOf(LanguageClass.THRIFT)

    // when
    val plugin = languagePluginsService.getLanguagePlugin(languages) as? ThriftLanguagePlugin

    // then
    assertNotNull(plugin)
  }

  fun testShouldReturnGoLanguagePluginForGoLanguage() {
    // given
    val languages: Set<LanguageClass> = hashSetOf(LanguageClass.GO)

    // when
    val plugin = languagePluginsService.getLanguagePlugin(languages) as? GoLanguagePlugin

    // then
    assertNotNull(plugin)
  }

  fun testShouldReturnPackagePrefixForJavaLanguage() {
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
    assertEquals("org.jetbrains.bazel.server.sync.languages.java", result)
  }

  fun testShouldReturnNullForEmptyFileInJavaLanguage() {
    // given
    val dirString = "org/jetbrains/bazel/server/sync/languages/java/"
    val filename = "JavaPackageTest.java"
    val content = ""
    val filePath = createFileAndWrite(dirString, filename, content)
    val plugin = languagePluginsService.getLanguagePlugin(hashSetOf(LanguageClass.JAVA))

    // when
    val result = (plugin as JVMPackagePrefixResolver).resolveJvmPackagePrefix(filePath)

    // then
    assertNull(result)
  }

  fun testShouldReturnPackagePrefixForJavaLanguageWithWrongPackageDeclaration() {
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
    assertEquals("org.jetbrains.bazel.server.sync.languages", result)
  }

  fun testShouldReturnPackagePrefixForScalaLanguageFromOneLinePackageDeclaration() {
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
    assertEquals("org.jetbrains.bazel.server.sync.languages", result)
  }

  fun testShouldReturnPackagePrefixForScalaLanguageFromTwoLinePackageDeclaration() {
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
    assertEquals("org.jetbrains.bazel.server.sync.languages.scala", result)
  }

  fun testShouldReturnPackagePrefixForScalaLanguageFromMultiLinePackageDeclaration() {
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
    assertEquals("org.jetbrains.bazel.server.sync.languages.scala", result)
  }

  fun testShouldReturnNullForScalaLanguageFromEmptyPackageDeclaration() {
    // given
    val dirString = "org/jetbrains/bazel/server/sync/languages/"
    val filename = "ScalaPackageTest.java"
    val content = ""
    val filePath = createFileAndWrite(dirString, filename, content)
    val plugin = languagePluginsService.getLanguagePlugin(hashSetOf(LanguageClass.SCALA))

    // when
    val result = (plugin as JVMPackagePrefixResolver).resolveJvmPackagePrefix(filePath)

    // then
    assertNull(result)
  }

  fun testShouldReturnPackagePrefixForKotlinLanguage() {
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
    assertEquals("org.jetbrains.bazel.server.sync.languages.kotlin", result)
  }

  fun testShouldReturnNullForKotlinLanguageForEmptyFile() {
    // given
    val dirString = "org/jetbrains/bazel/server/sync/languages/kotlin/"
    val filename = "KotlinPackageTest.kt"
    val content = ""
    val filePath = createFileAndWrite(dirString, filename, content)
    val plugin = languagePluginsService.getLanguagePlugin(hashSetOf(LanguageClass.KOTLIN))

    // when
    val result = (plugin as JVMPackagePrefixResolver).resolveJvmPackagePrefix(filePath)

    // then
    assertNull(result)
  }

  fun testShouldReturnNullForNoLanguageSourceSet() {
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
    assertNull(result)
  }
}
