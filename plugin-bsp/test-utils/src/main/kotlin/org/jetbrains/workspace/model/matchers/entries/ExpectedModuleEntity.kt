package org.jetbrains.workspace.model.matchers.entries

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.matchers.shouldContainExactlyInAnyOrder

public data class ExpectedModuleEntity(
  val moduleEntity: ModuleEntity,
)

public infix fun ModuleEntity.shouldBeEqual(expected: ExpectedModuleEntity): Unit =
  validateModuleEntity(this, expected)

public infix fun Collection<ModuleEntity>.shouldContainExactlyInAnyOrder(
  expectedValues: Collection<ExpectedModuleEntity>,
): Unit =
  this.shouldContainExactlyInAnyOrder({ actual, expected -> validateModuleEntity(actual, expected) }, expectedValues)

private fun validateModuleEntity(
  actual: ModuleEntity,
  expected: ExpectedModuleEntity,
) {
  actual.name shouldBe expected.moduleEntity.name
  actual.type shouldBe expected.moduleEntity.type
  actual.dependencies shouldContainExactlyInAnyOrder expected.moduleEntity.dependencies
}
