package org.jetbrains.workspace.model.matchers.entries

import com.intellij.workspaceModel.storage.bridgeEntities.api.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.JavaResourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.SourceRootEntity
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.matchers.shouldContainExactlyInAnyOrder

public data class ExpectedJavaResourceRootEntity(
  val javaResourceRootEntity: JavaResourceRootEntity,
  val sourceRootEntity: SourceRootEntity,
  val contentRootEntity: ContentRootEntity,
  val parentModuleEntity: ModuleEntity,
)

public infix fun JavaResourceRootEntity.shouldBeEqual(expected: ExpectedJavaResourceRootEntity): Unit =
  validateJavaResourceRootEntity(this, expected)

public infix fun Collection<JavaResourceRootEntity>.shouldContainExactlyInAnyOrder(
  expectedValues: Collection<ExpectedJavaResourceRootEntity>,
): Unit =
  this.shouldContainExactlyInAnyOrder(::validateJavaResourceRootEntity, expectedValues)

private fun validateJavaResourceRootEntity(
  actual: JavaResourceRootEntity,
  expected: ExpectedJavaResourceRootEntity
) {
  actual.generated shouldBe expected.javaResourceRootEntity.generated
  actual.relativeOutputPath shouldBe expected.javaResourceRootEntity.relativeOutputPath

  actual.sourceRoot shouldBeEqual toExpectedSourceRootEntity(expected)
}

private fun toExpectedSourceRootEntity(expected: ExpectedJavaResourceRootEntity): ExpectedSourceRootEntity =
  ExpectedSourceRootEntity(
    sourceRootEntity = expected.sourceRootEntity,
    contentRootEntity = expected.contentRootEntity,
    parentModuleEntity = expected.parentModuleEntity,
  )
