package org.jetbrains.bazel.redcodes

import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.openapi.application.EDT
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.workspaceModel.ide.toPath
import io.kotest.matchers.collections.shouldBeSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
class MixedGeneratedSourceRootsTest {

  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDir = tempPathFixture()
  private val fixture by bazelSyncCodeInsightFixture(projectFixture, tempDir)

  @Test
  @DisabledOnOs(OS.WINDOWS) // can't find bash
  fun testHighlighting(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    check(BazelFeatureFlags.mergeSourceRoots) {
      "MixedGeneratedSourceRootsTest only makes sense with BazelFeatureFlags.mergeSourceRoots enabled"
    }
    fixture.copyBazelTestProject("redcodes/mixed_generated_source")
    fixture.performBazelSync(buildProject = true)
    withContext(Dispatchers.EDT) {
      val moduleEntity = WorkspaceModel.getInstance(fixture.project)
        .currentSnapshot
        .entities(ModuleEntity::class.java)
        .single()
      val sourceRoots = moduleEntity.contentRoots.flatMap { it.sourceRoots }
      val (generatedRoots, realRoots) = sourceRoots.partition { it.isGenerated() }
      realRoots.shouldBeSingleton { it.url.toPath().endsWith(Path("module/src/main/java")) }
      generatedRoots.shouldBeSingleton { it.url.toPath().endsWith(Path("bin/module/com/example")) }
      fixture.checkHighlighting("module/src/main/java/com/example/App.java")
    }
  }

  private fun SourceRootEntity.isGenerated(): Boolean = javaSourceRoots.single().generated
}
