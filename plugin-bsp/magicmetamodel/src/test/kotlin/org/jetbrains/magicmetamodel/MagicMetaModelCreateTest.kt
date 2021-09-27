package org.jetbrains.magicmetamodel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.SourcesItem
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.MagicMetaModelImpl
import org.jetbrains.magicmetamodel.impl.WorkspaceModelTestMockImpl
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("MagicMetaModel.create(workspaceModel, targets, sources) tests")
class MagicMetaModelCreateTest {

  @Test
  fun `should return MagicMetaModelImpl`() {
    // given
    val workspaceModel = WorkspaceModelTestMockImpl()
    val targets = emptyList<BuildTarget>()
    val sources = emptyList<SourcesItem>()

    // when
    val magicMetaModel = MagicMetaModel.create(workspaceModel, targets, sources)

    // then
    magicMetaModel::class shouldBe MagicMetaModelImpl::class
  }
}
