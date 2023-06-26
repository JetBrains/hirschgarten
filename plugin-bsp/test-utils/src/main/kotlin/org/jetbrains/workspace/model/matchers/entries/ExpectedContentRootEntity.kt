package org.jetbrains.workspace.model.matchers.entries

import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.matchers.shouldContainExactlyInAnyOrder

public data class ExpectedContentRootEntity(
  val url: VirtualFileUrl,
  val excludedUrls: List<VirtualFileUrl>,
  val excludedPatterns: List<String>,
  val parentModuleEntity: ModuleEntity,
)

public infix fun ContentRootEntity.shouldBeEqual(expected: ExpectedContentRootEntity): Unit =
  validateContentRootEntity(this, expected)

public infix fun Collection<ContentRootEntity>.shouldContainExactlyInAnyOrder(
  expectedValues: Collection<ExpectedContentRootEntity>,
): Unit =
  this.shouldContainExactlyInAnyOrder(
    assertion = { actual, expected -> validateContentRootEntity(actual, expected) },
    expectedValues = expectedValues
  )

private fun validateContentRootEntity(
  actual: ContentRootEntity,
  expected: ExpectedContentRootEntity
) {
  actual.url shouldBe expected.url
  actual.excludedUrls.map { it.url } shouldContainExactlyInAnyOrder expected.excludedUrls
  actual.excludedPatterns shouldContainExactlyInAnyOrder expected.excludedPatterns
}
