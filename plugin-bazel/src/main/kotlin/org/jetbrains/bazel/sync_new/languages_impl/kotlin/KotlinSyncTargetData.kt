package org.jetbrains.bazel.sync_new.languages_impl.kotlin

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.kryo.ClassTag
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import org.jetbrains.bazel.sync_new.graph.impl.BazelPath
import org.jetbrains.bazel.sync_new.lang.SyncClassTag
import org.jetbrains.bazel.sync_new.lang.SyncTargetData

@SyncClassTag(serialId = KotlinSyncLanguage.LANGUAGE_TAG)
@Tagged
@ClassTag(2000525142)
data class KotlinSyncTargetData(
  @field:Tag(1)
  val languageVersion: String,

  @field:Tag(2)
  val apiVersion: String,

  @field:Tag(3)
  val associates: List<Label>,

  @field:Tag(4)
  val kotlincOptions: List<String>,

  @field:Tag(5)
  val stdlibJars: List<BazelPath>,
) : SyncTargetData
