package org.jetbrains.bazel.sync_new.connector

interface BazelConnector {
  suspend fun build(startup: StartupOptions.() -> Unit = {}, args: BuildArgs.() -> Unit = {}): BazelResult<Unit>
  suspend fun query(startup: StartupOptions.() -> Unit = {}, args: QueryArgs.() -> Unit = {}): BazelResult<QueryResult>
}

sealed interface BazelResult<T> {
  class Success<T>(val value: T) : BazelResult<T>
  class Failure(val error: Throwable) : BazelResult<Nothing>
}

fun <T> BazelResult<T>.getOrThrow(): T {
  return when (this) {
    is BazelResult.Success -> value
    is BazelResult.Failure -> throw error
  }
}
