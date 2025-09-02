package org.jetbrains.bazel.protobuf

import com.intellij.configurationStore.SettingsSavingComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.ProtobufBuildTarget
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Path
import kotlin.io.path.absolutePathString

@Service(Service.Level.PROJECT)
class BazelProtobufSyncService(val project: Project) : SettingsSavingComponent {

  val store = BazelProtobufIndexStore(project)

  fun getRealProtoFile(importPath: String): VirtualFile? {
    val data = store.getProtoIndexData(importPath) ?: return null
    val vfsManager = project.service<WorkspaceModel>().getVirtualFileUrlManager()
    return data.absolutePath.toRealPath()
      .toVirtualFileUrl(vfsManager)
      .virtualFile
  }

  override suspend fun save() {
    store.saveIfNeeded()
  }

}
