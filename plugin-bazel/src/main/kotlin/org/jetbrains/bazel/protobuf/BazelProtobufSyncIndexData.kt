package org.jetbrains.bazel.protobuf

import org.h2.mvstore.DataUtils
import org.h2.mvstore.WriteBuffer
import org.h2.mvstore.type.DataType
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.absolutePathString

data class BazelProtobufSyncIndexData(val protoPath: String, val root: Path, val realPaths: MutableList<Path> = mutableListOf())

internal fun WriteBuffer.putString(str: String) {
  if (str.isEmpty()) {
    putVarInt(0)
    return
  }
  putVarInt(str.length)
  put(str.encodeToByteArray())
}

internal fun ByteBuffer.getString(): String {
  val length = DataUtils.readVarInt(this)
  if (length == 0) {
    return "";
  }
  return ByteArray(length).also { get(it) }.decodeToString()
}

object BazelProtobufSyncIndexDataType : DataType<BazelProtobufSyncIndexData> {
  override fun getMemory(obj: BazelProtobufSyncIndexData): Int = 0

  override fun isMemoryEstimationAllowed(): Boolean = true

  override fun write(buffer: WriteBuffer, data: BazelProtobufSyncIndexData) {
    buffer.putString(data.protoPath)
    buffer.putString(data.root.toString())

    buffer.putVarInt(data.realPaths.size)
    for (path in data.realPaths) {
      buffer.putString(path.absolutePathString())
    }
  }

  override fun write(
    buffer: WriteBuffer,
    storage: Any,
    len: Int,
  ) {
    @Suppress("UNCHECKED_CAST")
    storage as Array<BazelProtobufSyncIndexData>
    for (item in storage) {
      write(buffer, item)
    }
  }

  override fun read(buffer: ByteBuffer): BazelProtobufSyncIndexData {
    val protoPath = buffer.getString()
    val root = Path.of(buffer.getString())
    val realPathsLength = DataUtils.readVarInt(buffer)
    val realPaths = mutableListOf<Path>()
    for (n in 0..<realPathsLength) {
      realPaths.add(Path.of(buffer.getString()))
    }
    return BazelProtobufSyncIndexData(protoPath, root, realPaths)
  }

  override fun read(
    buffer: ByteBuffer,
    storage: Any,
    size: Int,
  ) {
    @Suppress("UNCHECKED_CAST")
    storage as Array<BazelProtobufSyncIndexData>
    for (i in 0..<size) {
      storage[i] = read(buffer)
    }
  }

  override fun createStorage(size: Int): Array<BazelProtobufSyncIndexData?> = arrayOfNulls(size)

  override fun compare(one: BazelProtobufSyncIndexData, two: BazelProtobufSyncIndexData): Int = throw UnsupportedOperationException()

  override fun binarySearch(
    keyObj: BazelProtobufSyncIndexData,
    storageObj: Any,
    size: Int,
    initialGuess: Int,
  ): Int = throw UnsupportedOperationException()

}
