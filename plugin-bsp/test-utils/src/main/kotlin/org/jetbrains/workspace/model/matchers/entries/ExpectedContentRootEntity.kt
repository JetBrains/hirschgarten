package org.jetbrains.workspace.model.matchers.entries

import com.intellij.workspaceModel.storage.bridgeEntities.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.matchers.shouldContainExactlyInAnyOrder

public data class ExpectedContentRootEntity(
  val contentRootEntity: ContentRootEntity,
  val parentModuleEntity: ModuleEntity,
)

public infix fun ContentRootEntity.shouldBeEqual(expected: ExpectedContentRootEntity): Unit =
  validateContentRootEntity(this, expected)

public infix fun Collection<ContentRootEntity>.shouldContainExactlyInAnyOrder(
  expectedValues: Collection<ExpectedContentRootEntity>,
): Unit =
  this.shouldContainExactlyInAnyOrder(::validateContentRootEntity, expectedValues)

private fun validateContentRootEntity(
  actual: ContentRootEntity,
  expected: ExpectedContentRootEntity
) {
  actual.url shouldBe expected.contentRootEntity.url
  actual.excludedUrls shouldContainExactlyInAnyOrder expected.contentRootEntity.excludedUrls
  actual.excludedPatterns shouldContainExactlyInAnyOrder expected.contentRootEntity.excludedPatterns

  actual.module shouldBeEqual toExpectedModuleEntity(expected)
}

private fun toExpectedModuleEntity(expected: ExpectedContentRootEntity): ExpectedModuleEntity =
  ExpectedModuleEntity(
    moduleEntity = expected.parentModuleEntity
  )
