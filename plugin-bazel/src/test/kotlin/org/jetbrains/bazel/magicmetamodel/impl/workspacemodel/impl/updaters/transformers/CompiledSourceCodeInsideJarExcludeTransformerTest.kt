package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

@DisplayName("CompiledSourceCodeInsideJarExcludeTransformer.transform tests")
class CompiledSourceCodeInsideJarExcludeTransformerTest {
  @Test
  fun `should add correct excludes for java files`() {
    // given
    val sourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = Path("/src/Cat.java"),
          packagePrefix = "com.example.cat",
        ),
        JavaSourceRoot(
          sourcePath = Path("/src/EmptyPackage.java"),
          packagePrefix = "",
        ),
        JavaSourceRoot(
          sourcePath = Path("/src/weird_casing.java"),
          packagePrefix = "com.example",
        ),
        JavaSourceRoot(
          sourcePath = Path("/src/example.py"),
          packagePrefix = "",
        ),
      )
    val module = createModuleWithRoots(sourceRoots)

    // when
    val entity = CompiledSourceCodeInsideJarExcludeTransformer().transform(listOf(module), emptyList())

    // then
    entity.relativePathsInsideJarToExclude shouldBe
      setOf(
        "com/example/cat/Cat.java",
        "com/example/cat/Cat.class",
        "EmptyPackage.java",
        "EmptyPackage.class",
        "com/example/weird_casing.java",
        "com/example/weird_casing.class",
      )
  }

  @Test
  fun `should add correct excludes for kotlin files`() {
    // given
    val sourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = Path("/src/Cat.kt"),
          packagePrefix = "com.example.cat",
        ),
        JavaSourceRoot(
          sourcePath = Path("/src/EmptyPackage.kt"),
          packagePrefix = "",
        ),
        JavaSourceRoot(
          sourcePath = Path("/src/weird_casing.kt"),
          packagePrefix = "com.example",
        ),
        JavaSourceRoot(
          sourcePath = Path("/src/example.py"),
          packagePrefix = "",
        ),
      )
    val module = createModuleWithRoots(sourceRoots)

    // when
    val entity = CompiledSourceCodeInsideJarExcludeTransformer().transform(listOf(module), emptyList())

    // then
    entity.relativePathsInsideJarToExclude shouldBe
      setOf(
        "com/example/cat/Cat.kt",
        "com/example/cat/Cat.class",
        "com/example/cat/CatKt.class",
        "EmptyPackage.kt",
        "EmptyPackage.class",
        "EmptyPackageKt.class",
        "com/example/weird_casing.kt",
        "com/example/weird_casing.class",
        "com/example/Weird_casingKt.class",
      )
  }

  @Test
  fun `should add correct excludes for libraries from internal target`() {
    // given
    val libraryFromInternalTarget =
      LibraryItem(
        id = Label.synthetic("1"),
        dependencies = emptyList(),
        ijars = listOf(Path("/path/to/ijar.jar")),
        jars = listOf(Path("/path/to/jar.jar")),
        sourceJars = listOf(Path("/path/to/sourceJar.jar")),
        mavenCoordinates = null,
        isFromInternalTarget = true,
      )

    val usualLibrary =
      LibraryItem(
        id = Label.synthetic("1"),
        dependencies = emptyList(),
        ijars = listOf(Path("/path/to/another/ijar.jar")),
        jars = listOf(Path("/path/to/another/jar.jar")),
        sourceJars = listOf(Path("/path/to/another/sourceJar.jar")),
        mavenCoordinates = null,
        isFromInternalTarget = false,
      )

    // when
    val entity = CompiledSourceCodeInsideJarExcludeTransformer().transform(emptyList(), listOf(libraryFromInternalTarget, usualLibrary))

    // then
    entity.librariesFromInternalTargetsUrls shouldBe
      setOf(
        "jar:///path/to/ijar.jar!/",
        "jar:///path/to/jar.jar!/",
        "jar:///path/to/sourceJar.jar!/",
      )
  }

  private data class JavaSourceRoot(val sourcePath: Path, val packagePrefix: String = "")

  private fun createModuleWithRoots(sourceRoots: List<JavaSourceRoot>, resourceRoots: List<Path> = emptyList()): ModuleDetails =
    ModuleDetails(
      target =
        RawBuildTarget(
          Label.parse("target"),
          listOf(),
          emptyList(),
          TargetKind(
            kindString = "java_binary",
            ruleType = RuleType.BINARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
          sources =
            sourceRoots.map {
              SourceItem(
                path = it.sourcePath,
                generated = false,
                jvmPackagePrefix = it.packagePrefix,
              )
            },
          resources = resourceRoots,
          baseDirectory = Path("base/dir"),
        ),
      javacOptions = null,
      dependencies = emptyList(),
      defaultJdkName = null,
      jvmBinaryJars = emptyList(),
    )
}
