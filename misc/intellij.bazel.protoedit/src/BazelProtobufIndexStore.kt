package org.jetbrains.bazel.protobuf

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.util.io.mvstore.createOrResetMvStore
import com.intellij.util.io.mvstore.openOrResetMap
import org.h2.mvstore.MVMap
import org.h2.mvstore.type.StringDataType
import java.nio.file.Path

internal class BazelProtobufIndexStore(val project: Project) {
  private val store =
    createOrResetMvStore(
      file = project.getProjectDataPath("bazel-protobuf-index-v1.db"),
      readOnly = false,
      logSupplier = { logger<BazelProtobufIndexStore>() },
    )

  private val protoPathToFullPath: MVMap<String, String> =
    openStringToStringMap("protoPathToFullPath")

  private fun openStringToStringMap(name: String): MVMap<String, String> {
    val builder = MVMap.Builder<String, String>()
    builder.setKeyType(StringDataType.INSTANCE)
    builder.setValueType(StringDataType.INSTANCE)
    return openOrResetMap(store = store, name = name, mapBuilder = builder, logSupplier = { logger<BazelProtobufIndexStore>() })
  }

  fun putProtoFullPath(protoPath: String, fullPath: Path) {
    protoPathToFullPath[protoPath] = fullPath.toString()
  }

  fun getProtoFullPath(protoPath: String): Path? = protoPathToFullPath[protoPath]?.let { Path.of(it) }

  fun clearProtoIndexData() {
    protoPathToFullPath.clear()
  }

  fun save() {
    if (store.hasUnsavedChanges()) {
      store.commit()
    }
  }
}
