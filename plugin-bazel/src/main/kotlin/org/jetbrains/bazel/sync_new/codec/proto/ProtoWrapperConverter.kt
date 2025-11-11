package org.jetbrains.bazel.sync_new.codec.proto

import com.google.protobuf.Message
import org.jetbrains.bazel.sync_new.codec.CodecConverter

class ProtoWrapperConverter<U : Message, V : ProtoWrapper<U>>(
  private val obj : () -> V
) : CodecConverter<U, V> {
  override fun to(value: U): V {
    val instance = obj()
    instance.fromProto(value)
    return instance
  }

  override fun from(value: V): U = value.toProto()

}

interface ProtoWrapper<V : Message> {
  fun fromProto(proto: V)
  fun toProto(): V
}
