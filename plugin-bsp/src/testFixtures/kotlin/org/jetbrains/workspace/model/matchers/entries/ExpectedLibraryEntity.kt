package org.jetbrains.workspace.model.matchers.entries

import com.intellij.platform.workspace.jps.entities.LibraryEntity
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.matchers.shouldContainExactlyInAnyOrder

public data class ExpectedLibraryEntity(
  val libraryEntity: LibraryEntity,
)

public infix fun LibraryEntity.shouldBeEqual(expected: ExpectedLibraryEntity): Unit =
  validateLibraryEntity(this, expected)

public infix fun Collection<LibraryEntity>.shouldContainExactlyInAnyOrder(
  expectedValues: Collection<ExpectedLibraryEntity>,
): Unit =
  this.shouldContainExactlyInAnyOrder({ actual, expected -> validateLibraryEntity(actual, expected) }, expectedValues)

private fun validateLibraryEntity(
  actual: LibraryEntity,
  expected: ExpectedLibraryEntity,
) {
  actual.tableId shouldBe expected.libraryEntity.tableId
  actual.name shouldBe expected.libraryEntity.name
  actual.roots shouldContainExactlyInAnyOrder expected.libraryEntity.roots
  actual.excludedRoots shouldContainExactlyInAnyOrder expected.libraryEntity.excludedRoots
}
