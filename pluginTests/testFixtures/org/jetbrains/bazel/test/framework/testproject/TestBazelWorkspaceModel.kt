package org.jetbrains.bazel.test.framework.testproject

import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.module.JavaModuleType.JAVA_MODULE_ENTITY_TYPE_ID_NAME
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SdkId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager

/** Returns the workspace model of the project. */
val TestBazelProject.workspaceModel: WorkspaceModel
  get() = WorkspaceModel.getInstance(project)

/** Returns the URL manager of the workspace model. */
val TestBazelProject.virtualFileUrlManager: VirtualFileUrlManager
  get() = workspaceModel.getVirtualFileUrlManager()

/** Changes the workspace model in a write action. Call it from any thread, but not from a read action. */
fun TestBazelProject.updateWorkspaceModel(updater: (MutableEntityStorage) -> Unit) {
  runWriteActionAndWait {
    workspaceModel.updateProjectModel("Change the test Bazel project model", updater)
  }
}

/** Adds an empty Java module with the name [name]. The module depends on the Java 11 SDK and on its own sources. */
fun MutableEntityStorage.addJavaModule(name: String): ModuleEntity =
  addEntity(
    ModuleEntity(
      name = name,
      dependencies = listOf(
        SdkDependency(SdkId("11", "JavaSDK")),
        ModuleSourceDependency,
      ),
      entitySource = object : EntitySource {},
    ) {
      this.type = ModuleTypeId(JAVA_MODULE_ENTITY_TYPE_ID_NAME)
    },
  )
