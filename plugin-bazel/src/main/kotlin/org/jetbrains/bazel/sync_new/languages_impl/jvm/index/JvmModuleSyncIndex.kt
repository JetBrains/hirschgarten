package org.jetbrains.bazel.sync_new.languages_impl.jvm.index

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sync_new.codec.ofHash128
import org.jetbrains.bazel.sync_new.codec.ofLabel
import org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.legacy.LegacyImportData
import org.jetbrains.bazel.sync_new.storage.DefaultStorageHints
import org.jetbrains.bazel.sync_new.storage.createKVStore
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.storage.storageContext

@Service(Service.Level.PROJECT)
internal class JvmModuleSyncIndex(
  private val project: Project,
) {
  private val libraryId2VertexId =
    project.storageContext.createKVStore<HashValue128, Label>("bazel.sync.jvm.libraryId2VertexId", DefaultStorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofHash128() }
      .withValueCodec { ofLabel() }
      .build()

  private val moduleId2VertexId =
    project.storageContext.createKVStore<HashValue128, Label>("bazel.sync.jvm.moduleId2VertexId", DefaultStorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofHash128() }
      .withValueCodec { ofLabel() }
      .build()

  fun reset(data: LegacyImportData) {
    libraryId2VertexId.clear()
    moduleId2VertexId.clear()
    for (library in data.libraries) {
      val name = library.id.formatAsModuleName(project)
      libraryId2VertexId.put(hash { putString(name) }, library.id)
    }
    for (target in data.targets) {
      val name = target.id.formatAsModuleName(project)
      moduleId2VertexId.put(hash { putString(name) }, target.id)
    }
  }

  fun getTargetForModuleId(moduleId: String): Label? = moduleId2VertexId[hash { putString(moduleId) }]
  fun getTargetForLibraryId(libraryId: String): Label? = libraryId2VertexId[hash { putString(libraryId) }]
}
