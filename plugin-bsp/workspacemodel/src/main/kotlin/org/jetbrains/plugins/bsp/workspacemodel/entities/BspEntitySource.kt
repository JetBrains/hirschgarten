package org.jetbrains.plugins.bsp.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource

sealed interface BspEntitySource : EntitySource

data object BspProjectEntitySource : BspEntitySource

data class BspModuleEntitySource(val moduleName: String) : BspEntitySource

data object BspDummyEntitySource : BspEntitySource
