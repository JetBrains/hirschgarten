package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.esotericsoftware.kryo.kryo5.Serializer
import org.jetbrains.annotations.ApiStatus

/**
 * Base class for handwritten Kryo serializers for workspace snapshot.
 * Serializer versioning allow us to correctly reason about schema changes.
 *
 * @property binaryFormatVersion Bump after each serializer modification
 */
@ApiStatus.Internal
abstract class VersionedKryoSerializer<T> : Serializer<T>() {

  // TODO: currently only used inside test
  //  think of how to enforce it inside
  abstract val binaryFormatVersion: Int
}
