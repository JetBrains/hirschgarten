package org.jetbrains.bazel.sync_new.flow.vfs_diff

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTagged
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged

@Tagged
data class SyncVFSState(
  @field:Tag(1)
  val listenState: SyncVFSListenState
)

@EnumTagged
enum class SyncVFSListenState {

  @EnumTag(1)
  WAITING_FOR_FIRST_SYNC,

  @EnumTag(2)
  LISTENING_VFS
}

@EnumTagged
enum class SyncFileState {

  @EnumTag(1)
  ADDED,

  @EnumTag(2)
  REMOVED,

  @EnumTag(3)
  CHANGED
}
