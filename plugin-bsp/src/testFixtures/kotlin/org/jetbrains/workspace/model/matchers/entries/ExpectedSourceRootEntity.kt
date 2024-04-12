package org.jetbrains.workspace.model.matchers.entries

import com.intellij.java.workspace.entities.JavaResourceRootPropertiesEntity
import com.intellij.java.workspace.entities.JavaSourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaResourceRoots
import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.matchers.shouldContainExactlyInAnyOrder

public data class ExpectedSourceRootEntity(
  val sourceRootEntity: SourceRootEntity,
  val contentRootEntity: ContentRootEntity,
  val parentModuleEntity: ModuleEntity,
)

public infix fun SourceRootEntity.shouldBeEqual(expected: ExpectedSourceRootEntity): Unit =
  validateSourceRootEntity(this, expected)

public infix fun Collection<SourceRootEntity>.shouldContainExactlyInAnyOrder(
  expectedValues: Collection<ExpectedSourceRootEntity>,
): Unit =
  this.shouldContainExactlyInAnyOrder(
    assertion = { actual, expected -> validateSourceRootEntity(actual, expected) },
    expectedValues = expectedValues,
  )

private fun validateSourceRootEntity(
  actual: SourceRootEntity,
  expected: ExpectedSourceRootEntity,
) {
  actual.url shouldBe expected.sourceRootEntity.url
  actual.rootType shouldBe expected.sourceRootEntity.rootType

  actual.javaSourceRoots.shouldContainExactlyInAnyOrder(
    { actualEntity, expectedEntity -> validateJavaSourceRootEntity(actualEntity, expectedEntity) },
    expected.sourceRootEntity.javaSourceRoots,
  )
  actual.javaResourceRoots.shouldContainExactlyInAnyOrder(
    { actualEntity, expectedEntity -> validateJavaResourceRootEntity(actualEntity, expectedEntity) },
    expected.sourceRootEntity.javaResourceRoots,
  )

  actual.contentRoot shouldBeEqual toExpectedContentRootEntity(expected)
}

private fun validateJavaSourceRootEntity(
  actual: JavaSourceRootPropertiesEntity,
  expected: JavaSourceRootPropertiesEntity,
) {
  actual.generated shouldBe expected.generated
  actual.packagePrefix shouldBe expected.packagePrefix
}

private fun validateJavaResourceRootEntity(
  actual: JavaResourceRootPropertiesEntity,
  expected: JavaResourceRootPropertiesEntity,
) {
  actual.generated shouldBe expected.generated
  actual.relativeOutputPath shouldBe expected.relativeOutputPath
}

private fun toExpectedContentRootEntity(expected: ExpectedSourceRootEntity): ExpectedContentRootEntity =
  ExpectedContentRootEntity(
    url = expected.contentRootEntity.url,
    excludedUrls = expected.contentRootEntity.excludedUrls.map { it.url },
    excludedPatterns = expected.contentRootEntity.excludedPatterns,
    parentModuleEntity = expected.parentModuleEntity,
  )
