package org.jetbrains.bazel.workspace.model.matchers.entries

import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ExcludeUrlEntityBuilder
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.workspace.model.matchers.shouldContainExactlyInAnyOrder

public data class ExpectedContentRootEntity(
  val url: VirtualFileUrl,
  val parentModuleEntity: ModuleEntity,
  val excludedUrls: List<ExcludeUrlEntityBuilder> = emptyList(),
)

public infix fun ContentRootEntity.shouldBeEqual(expected: ExpectedContentRootEntity): Unit = validateContentRootEntity(this, expected)

public infix fun Collection<ContentRootEntity>.shouldContainExactlyInAnyOrder(expectedValues: Collection<ExpectedContentRootEntity>) {
  this.shouldContainExactlyInAnyOrder(
    assertion = { actual, expected -> validateContentRootEntity(actual, expected) },
    expectedValues = expectedValues,
  )
}

private fun validateContentRootEntity(actual: ContentRootEntity, expected: ExpectedContentRootEntity) {
  actual.url shouldBe expected.url
  actual.excludedUrls.shouldContainExactlyInAnyOrder(
    assertion = { left, right ->
      left.url shouldBe right.url
      left.entitySource shouldBe right.entitySource
    },
    expectedValues = expected.excludedUrls,
  )
}
