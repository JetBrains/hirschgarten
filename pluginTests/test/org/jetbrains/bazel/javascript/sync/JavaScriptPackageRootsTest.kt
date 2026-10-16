package org.jetbrains.bazel.javascript.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

class JavaScriptPackageRootsTest {
  @TempDir
  lateinit var workspaceRoot: Path

  @Test
  fun `uses the closest directory with a package json`() {
    val rendering = createPackage("backend/rendering")
    val nestedTargetDirectory = workspaceRoot.resolve("backend/rendering/src/api").createDirectories()

    assertThat(findJavaScriptPackageRoots(workspaceRoot, setOf(rendering, nestedTargetDirectory)))
      .containsExactly(rendering)
  }

  @Test
  fun `falls back to the target directory without a package json below the workspace root`() {
    createPackage("")
    val tools = workspaceRoot.resolve("tools/lint").createDirectories()

    assertThat(findJavaScriptPackageRoots(workspaceRoot, setOf(tools))).containsExactly(tools)
  }

  @Test
  fun `never uses the workspace root`() {
    createPackage("")

    assertThat(findJavaScriptPackageRoots(workspaceRoot, setOf(workspaceRoot))).isEmpty()
  }

  @Test
  fun `ignores directories outside the workspace and keeps outermost roots only`() {
    val frontend = createPackage("frontend")
    val nestedPackage = createPackage("frontend/packages/ui")

    assertThat(findJavaScriptPackageRoots(workspaceRoot, setOf(nestedPackage, frontend, Path.of("/elsewhere/pkg"))))
      .containsExactly(frontend)
  }

  private fun createPackage(relativePath: String): Path =
    workspaceRoot.resolve(relativePath).createDirectories()
      .also { it.resolve("package.json").createFile() }
}
