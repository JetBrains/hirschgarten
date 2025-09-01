package org.jetbrains.bazel.protobuf

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.util.io.mvstore.createOrResetMvStore
import com.intellij.util.io.mvstore.openOrResetMap
import org.h2.mvstore.MVMap
import org.h2.mvstore.type.DataType
import org.h2.mvstore.type.StringDataType

class BazelProtobufIndexStore(val project: Project) {
  private val store = createOrResetMvStore(
    file = project.getProjectDataPath("bazel-protobuf-target-index.db"),
    readOnly = false,
    logSupplier = { logger<BazelProtobufIndexStore>() },
  )

  private val protoPathToIndexData: MVMap<String, BazelProtobufSyncIndexData> = openStringToObjectMap(
    "protoPathToIndexData",
    BazelProtobufSyncIndexDataType,
  )

  private fun <V> openStringToObjectMap(name: String, dataType: DataType<V>): MVMap<String, V> {
    val builder = MVMap.Builder<String, V>()
    builder.setKeyType(StringDataType.INSTANCE)
    builder.setValueType(dataType)
    return openOrResetMap(store = store, name = name, mapBuilder = builder, logSupplier = { logger<BazelProtobufIndexStore>() })
  }

  fun putProtoIndexData(data: BazelProtobufSyncIndexData) {
    protoPathToIndexData[data.protoPath] = data
  }

  fun getProtoIndexData(protoPath: String): BazelProtobufSyncIndexData? = protoPathToIndexData[protoPath]

  fun saveIfNeeded() {
    if (store.hasUnsavedChanges()) {
      store.commit()
    }
  }
}
