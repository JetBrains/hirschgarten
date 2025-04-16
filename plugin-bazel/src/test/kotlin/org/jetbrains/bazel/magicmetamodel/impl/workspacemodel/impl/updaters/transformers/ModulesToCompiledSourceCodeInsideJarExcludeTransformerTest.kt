package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

@DisplayName("ModulesToCompiledSourceCodeInsideJarExcludeTransformer.transform(modules) tests")
class ModulesToCompiledSourceCodeInsideJarExcludeTransformerTest {
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
    val entity = ModulesToCompiledSourceCodeInsideJarExcludeTransformer().transform(listOf(module))

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
    entity.namesInsideJarToExclude shouldBe emptyList()
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
    val entity = ModulesToCompiledSourceCodeInsideJarExcludeTransformer().transform(listOf(module))

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
    entity.namesInsideJarToExclude shouldBe emptyList()
  }

  @Test
  fun `should add excludes for xml resources but not for other ones`() {
    // given
    val resourceRoots =
      listOf(
        Path("/src/resources/plugin.xml"),
        Path("/src/resources/plugin.info"),
      )
    val module = createModuleWithRoots(emptyList(), resourceRoots)

    // when
    val entity = ModulesToCompiledSourceCodeInsideJarExcludeTransformer().transform(listOf(module))

    // then
    entity.relativePathsInsideJarToExclude shouldBe emptyList()
    entity.namesInsideJarToExclude shouldBe
      setOf(
        "plugin.xml",
      )
  }

  private data class JavaSourceRoot(val sourcePath: Path, val packagePrefix: String = "")

  private fun createModuleWithRoots(sourceRoots: List<JavaSourceRoot>, resourceRoots: List<Path> = emptyList()): ModuleDetails =
    ModuleDetails(
      target =
        BuildTarget(
          Label.parse("target"),
          listOf("library"),
          listOf("java"),
          emptyList(),
          TargetKind(
            kindString = "java_binary",
            ruleType = RuleType.BINARY,
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
      scalacOptions = null,
      libraryDependencies = null,
      moduleDependencies = emptyList(),
      defaultJdkName = null,
      jvmBinaryJars = emptyList(),
    )
}
