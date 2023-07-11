package org.jetbrains.magicmetamodel

import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.MagicMetaModelImpl
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("MagicMetaModel tests")
class MagicMetaModelTest : WorkspaceModelBaseTest() {

  @Nested
  @DisplayName("MagicMetaModel.create(magicMetaModelProjectConfig, projectDetails) tests")
  inner class MagicMetaModelCreateTest {

    @Test
    fun `should return MagicMetaModelImpl`() {
      // given
      val testMagicMetaModelProjectConfig =
        MagicMetaModelProjectConfig(workspaceModel, virtualFileUrlManager, null, projectBasePath)

      val projectDetails = ProjectDetails(
        targetsId = emptyList(),
        targets = emptySet(),
        sources = emptyList(),
        resources = emptyList(),
        dependenciesSources = emptyList(),
        javacOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModel.create(testMagicMetaModelProjectConfig, projectDetails)

      // then
      magicMetaModel::class shouldBe MagicMetaModelImpl::class
    }
  }
}
