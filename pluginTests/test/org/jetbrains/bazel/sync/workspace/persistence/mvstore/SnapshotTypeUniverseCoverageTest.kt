package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.junit.jupiter.api.Test

@TestApplication
class SnapshotTypeUniverseCoverageTest {

  private val project by projectFixture()

  @Test
  fun `every provided build target type is contributed to the snapshot type universe`() {
    val registeredTypes = SnapshotTypeUniverse.buildFromProject(project).registrations.map { it.type }.toSet()

    val missing = LanguagePlugin.EP_NAME.extensionList
      .flatMap { plugin -> plugin.providedBuildTargetTypes.map { plugin::class.qualifiedName to it.java } }
      .filter { (_, type) -> type !in registeredTypes }
      .map { (plugin, type) -> "`${type.name}` (provided by `$plugin`)" }

    withClue(
      "every type in `LanguagePlugin.providedBuildTargetTypes` must be registered through a " +
        "`workspaceTypeContributor` extension, or storing the workspace snapshot fails at runtime",
    ) {
      missing.shouldBeEmpty()
    }
  }
}
