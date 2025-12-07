package org.jetbrains.bazel.sync_new.flow.vfs_diff

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.sync_new.codec.kryo.ClassTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTagged
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged

@Tagged
@ClassTag(1353878240)
data class SyncVFSState(
  @field:Tag(1)
  val listenState: SyncVFSListenState
)

@EnumTagged
@ClassTag(1080086589)
enum class SyncVFSListenState {

  @EnumTag(1)
  WAITING_FOR_FIRST_SYNC,

  @EnumTag(2)
  LISTENING_VFS
}

@EnumTagged
@ClassTag(1899009378)
enum class SyncFileState {

  @EnumTag(1)
  ADDED,

  @EnumTag(2)
  REMOVED,

  @EnumTag(3)
  CHANGED
}
