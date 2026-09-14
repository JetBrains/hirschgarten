package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Each Kotlin module must see the stdlib of its own Kotlin toolchain only.
 * See BAZEL-3549.
 */
class KotlinStdlibPerToolchainTest {

  /** Two Kotlin toolchains with different stdlib versions, selected per configuration. */
  @Nested
  @BazelTestApplication
  inner class PerConfiguration {

    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/kotlin_stdlib_per_toolchain",
      bazelVersion = "9.1.0",
      projectView = ".bazelproject",
    )

    @Test
    fun testStdlibPerTarget(): Unit = runBlocking(Dispatchers.Default) {
      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("lib/a/Lib.kt")
        fixture.checkHighlighting("lib/b/Lib.kt")

        // one stdlib library per Kotlin toolchain, each with one stdlib version
        val snapshot = WorkspaceModel.getInstance(fixture.project).currentSnapshot
        val libraries = stdlibLibraries(snapshot)
        libraries shouldHaveSize 2
        val bundledStdlib = libraries.single { it.hasRoot(BUNDLED_STDLIB_JAR) }
        val oldStdlib = libraries.single { it.hasRoot(OLD_STDLIB_JAR) }
        bundledStdlib.hasRoot(OLD_STDLIB_JAR) shouldBe false
        oldStdlib.hasRoot(BUNDLED_STDLIB_JAR) shouldBe false

        moduleDependingOn(snapshot, bundledStdlib).hasContentRoot("/lib/a") shouldBe true
        moduleDependingOn(snapshot, oldStdlib).hasContentRoot("/lib/b") shouldBe true
      }
    }
  }

  /** A Kotlin target of a nested bzlmod module, with a Kotlin toolchain whose stdlib is older than the bundled one. */
  @Nested
  @BazelTestApplication
  inner class NestedModule {

    private val fixture by bazelSyncCodeInsightFixture(
      "redcodes/kotlin_stdlib_nested_module",
      bazelVersion = "9.1.0",
      projectView = ".bazelproject",
    )

    @Test
    fun testNestedModuleStdlibVersion(): Unit = runBlocking(Dispatchers.Default) {
      withContext(Dispatchers.EDT) {
        fixture.checkHighlighting("nested/Lib.kt")

        val snapshot = WorkspaceModel.getInstance(fixture.project).currentSnapshot
        val stdlib = stdlibLibraries(snapshot).single()
        stdlib.hasRoot(OLD_STDLIB_JAR) shouldBe true
        stdlib.hasRoot(BUNDLED_STDLIB_JAR) shouldBe false

        moduleDependingOn(snapshot, stdlib).hasContentRoot("/nested") shouldBe true
      }
    }
  }
}

private const val BUNDLED_STDLIB_JAR = "kotlin-stdlib.jar"
private const val OLD_STDLIB_JAR = "kotlin-stdlib-1.9.25.jar"

private fun stdlibLibraries(snapshot: ImmutableEntityStorage): List<LibraryEntity> =
  snapshot.entities(LibraryEntity::class.java)
    .filter { it.name.startsWith("rules_kotlin_kotlin-stdlibs") }
    .toList()

private fun LibraryEntity.hasRoot(jarName: String): Boolean = roots.any { it.url.url.endsWith("/$jarName!/") }

private fun moduleDependingOn(snapshot: ImmutableEntityStorage, library: LibraryEntity): ModuleEntity =
  snapshot.entities(ModuleEntity::class.java)
    .single { module -> module.dependencies.any { it is LibraryDependency && it.library.name == library.name } }

private fun ModuleEntity.hasContentRoot(pathSuffix: String): Boolean = contentRoots.any { it.url.url.endsWith(pathSuffix) }
