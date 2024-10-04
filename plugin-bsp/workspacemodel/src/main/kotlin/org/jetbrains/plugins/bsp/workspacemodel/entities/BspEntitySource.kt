package org.jetbrains.plugins.bsp.workspacemodel.entities

import com.intellij.openapi.roots.ProjectModelExternalSource
import com.intellij.platform.workspace.storage.EntitySource

sealed interface BspEntitySource : EntitySource

data object BspProjectEntitySource : BspEntitySource

class BspModuleEntitySource(val moduleName: String) : BspEntitySource

data object BspDummyEntitySource : BspEntitySource

object BspProjectModelExternalSource : ProjectModelExternalSource {
  override fun getDisplayName() = "BSP"

  override fun getId() = "BSP"
}
