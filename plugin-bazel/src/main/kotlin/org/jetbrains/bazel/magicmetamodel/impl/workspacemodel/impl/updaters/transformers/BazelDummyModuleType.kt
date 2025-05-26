package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.icons.AllIcons
import com.intellij.ide.util.projectWizard.EmptyModuleBuilder
import com.intellij.openapi.module.InternalModuleType
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleTypeManager
import javax.swing.Icon

class BazelDummyModuleType : InternalModuleType<BazelDummyModuleBuilder>(ID) {
  companion object {
    const val ID: String = "BAZEL_DUMMY_MODULE_TYPE"
  }

  override fun createModuleBuilder(): BazelDummyModuleBuilder = BazelDummyModuleBuilder()

  override fun getName(): String = "Bazel synthetic module"

  override fun getDescription(): String = name

  override fun getNodeIcon(isOpened: Boolean): Icon = AllIcons.Nodes.Package
}

class BazelDummyModuleBuilder : EmptyModuleBuilder() {
  override fun getModuleType(): ModuleType<*> = ModuleTypeManager.getInstance().findByID(BazelDummyModuleType.ID)
}
