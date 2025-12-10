package org.jetbrains.bazel.sync_new.graph.impl

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.Bind
import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.LongSet
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.kryo.ClassTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTag
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTagged
import org.jetbrains.bazel.sync_new.graph.EMPTY_ID
import org.jetbrains.bazel.sync_new.graph.ID
import org.jetbrains.bazel.sync_new.graph.TargetVertex
import org.jetbrains.bazel.sync_new.lang.SyncTargetData
import org.jetbrains.bazel.sync_new.lang.kryo.SyncTargetDataSerializer
import java.nio.file.Path
import java.util.EnumSet

const val PRIORITY_LOW: Int = 0
const val PRIORITY_NORMAL: Int = 100
const val PRIORITY_HIGH: Int = 200

@Tagged
@ClassTag(1225691729)
data class BazelTargetVertex(
  @field:Tag(1)
  override var vertexId: ID = EMPTY_ID,

  @field:Tag(2)
  override val label: Label,

  @field:Tag(5)
  val genericData: BazelGenericTargetData,

  @field:Tag(6)
  val languageTags: LongSet,

  @field:Bind(serializer = SyncTargetDataSerializer::class)
  @field:Tag(10)
  val targetData: Long2ObjectMap<SyncTargetData>,

  @field:Tag(7)
  val baseDirectory: Path,

  @field:Tag(8)
  val kind: String
) : TargetVertex

@Tagged
@ClassTag(1053755131)
data class BazelGenericTargetData(
  @field:Tag(1)
  val tags: EnumSet<BazelTargetTag>,

  @field:Tag(2)
  val directDependencies: List<BazelTargetDependency>,

  @field:Tag(3)
  val sources: List<BazelTargetSourceFile>,

  @field:Tag(4)
  val resources: List<BazelTargetResourceFile>,

  @field:Tag(5)
  val isUniverseTarget: Boolean,
)

@EnumTagged
@ClassTag(1152363487)
enum class BazelTargetTag {
  @field:EnumTag(1)
  LIBRARY,

  @field:EnumTag(2)
  EXECUTABLE,

  @field:EnumTag(3)
  TEST,

  @field:EnumTag(4)
  INTELLIJ_PLUGIN,

  @field:EnumTag(5)
  NO_IDE,

  @field:EnumTag(6)
  NO_BUILD,

  @field:EnumTag(7)
  MANUAL,
}

@Tagged
@ClassTag(1955827292)
data class BazelTargetDependency(
  @field:Tag(1)
  val label: Label,
)

@Tagged
@ClassTag(107370715)
data class BazelTargetSourceFile(
  @field:Tag(1)
  val path: BazelPath,

  @field:Tag(2)
  val priority: Int,
)

@Tagged
@ClassTag(666027055)
data class BazelTargetResourceFile(
  @field:Tag(1)
  val path: BazelPath,
)
