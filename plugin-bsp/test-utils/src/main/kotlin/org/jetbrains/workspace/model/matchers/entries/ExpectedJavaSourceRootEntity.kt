package org.jetbrains.workspace.model.matchers.entries

import com.intellij.workspaceModel.storage.bridgeEntities.ContentRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.JavaSourceRootEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.SourceRootEntity
import io.kotest.matchers.shouldBe
import org.jetbrains.workspace.model.matchers.shouldContainExactlyInAnyOrder

public data class ExpectedJavaSourceRootEntity(
  val javaSourceRootEntity: JavaSourceRootEntity,
  val sourceRootEntity: SourceRootEntity,
  val contentRootEntity: ContentRootEntity,
  val parentModuleEntity: ModuleEntity,
)

public infix fun JavaSourceRootEntity.shouldBeEqual(expected: ExpectedJavaSourceRootEntity): Unit =
  validateJavaSourceRootEntity(this, expected)

public infix fun Collection<JavaSourceRootEntity>.shouldContainExactlyInAnyOrder(
  expectedValues: Collection<ExpectedJavaSourceRootEntity>,
): Unit =
  this.shouldContainExactlyInAnyOrder(::validateJavaSourceRootEntity, expectedValues)

private fun validateJavaSourceRootEntity(
  actual: JavaSourceRootEntity,
  expected: ExpectedJavaSourceRootEntity
) {
  actual.generated shouldBe expected.javaSourceRootEntity.generated
  actual.packagePrefix shouldBe expected.javaSourceRootEntity.packagePrefix

  actual.sourceRoot shouldBeEqual toExpectedSourceRootEntity(expected)
}

private fun toExpectedSourceRootEntity(expected: ExpectedJavaSourceRootEntity): ExpectedSourceRootEntity =
  ExpectedSourceRootEntity(
    sourceRootEntity = expected.sourceRootEntity,
    contentRootEntity = expected.contentRootEntity,
    parentModuleEntity = expected.parentModuleEntity,
  )
