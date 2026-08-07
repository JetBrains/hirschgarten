package org.jetbrains.bazel.languages.projectview

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.jetbrains.bazel.languages.projectview.imports.Import
import org.junit.jupiter.api.Test

class ProjectViewUnresolvedImportsTest {
  private fun projectView(vararg imports: Import) = ProjectView(sections = emptyMap(), imports = imports.toList())

  @Test
  fun `returns required imports that could not be resolved`() {
    val requiredMissing = Import.Unresolved("local.bazelproject", position = null, isRequired = true)
    val optionalMissing = Import.Unresolved("optional.bazelproject", position = null, isRequired = false) // try_import

    projectView(requiredMissing, optionalMissing).unresolvedRequiredImports() shouldContainExactly listOf(requiredMissing)
  }

  @Test
  fun `is empty when no required import is unresolved`() {
    val optionalMissing = Import.Unresolved("optional.bazelproject", position = null, isRequired = false)

    projectView(optionalMissing).unresolvedRequiredImports().shouldBeEmpty()
    projectView().unresolvedRequiredImports().shouldBeEmpty()
  }
}
