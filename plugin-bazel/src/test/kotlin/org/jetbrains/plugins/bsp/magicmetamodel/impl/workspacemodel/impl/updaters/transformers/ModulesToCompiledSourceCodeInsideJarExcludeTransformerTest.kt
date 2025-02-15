package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.JavaModule
import org.jetbrains.plugins.bsp.workspacemodel.entities.JavaSourceRoot
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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
          generated = false,
          packagePrefix = "com.example.cat",
          rootType = SourceRootTypeId("java-source"),
        ),
        JavaSourceRoot(
          sourcePath = Path("/src/EmptyPackage.java"),
          generated = false,
          packagePrefix = "",
          rootType = SourceRootTypeId("java-source"),
        ),
        JavaSourceRoot(
          sourcePath = Path("/src/weird_casing.java"),
          generated = false,
          packagePrefix = "com.example",
          rootType = SourceRootTypeId("java-source"),
        ),
        JavaSourceRoot(
          sourcePath = Path("/src/example.py"),
          generated = false,
          packagePrefix = "",
          rootType = SourceRootTypeId("python-source"),
        ),
      )
    val module = createModuleWithSourceRoots(sourceRoots)

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
  }

  @Test
  fun `should add correct excludes for kotlin files`() {
    // given
    val sourceRoots =
      listOf(
        JavaSourceRoot(
          sourcePath = Path("/src/Cat.kt"),
          generated = false,
          packagePrefix = "com.example.cat",
          rootType = SourceRootTypeId("java-source"),
        ),
        JavaSourceRoot(
          sourcePath = Path("/src/EmptyPackage.kt"),
          generated = false,
          packagePrefix = "",
          rootType = SourceRootTypeId("java-source"),
        ),
        JavaSourceRoot(
          sourcePath = Path("/src/weird_casing.kt"),
          generated = false,
          packagePrefix = "com.example",
          rootType = SourceRootTypeId("java-source"),
        ),
        JavaSourceRoot(
          sourcePath = Path("/src/example.py"),
          generated = false,
          packagePrefix = "",
          rootType = SourceRootTypeId("python-source"),
        ),
      )
    val module = createModuleWithSourceRoots(sourceRoots)

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
  }

  private fun createModuleWithSourceRoots(sourceRoots: List<JavaSourceRoot>): JavaModule =
    JavaModule(
      genericModuleInfo =
        GenericModuleInfo(
          name = "@//target",
          type = ModuleTypeId("JAVA_MODULE"),
          modulesDependencies = emptyList(),
          librariesDependencies = emptyList(),
        ),
      baseDirContentRoot = null,
      sourceRoots = sourceRoots,
      resourceRoots = emptyList(),
      moduleLevelLibraries = emptyList(),
    )
}
