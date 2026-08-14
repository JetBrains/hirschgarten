package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.esotericsoftware.kryo.kryo5.util.Pool
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
inline fun <T, R> Pool<T>.use(fn: (obj: T) -> R): R {
  val obj = this.obtain()
  return try {
    fn(obj)
  }
  finally {
    this.free(obj)
  }
}
