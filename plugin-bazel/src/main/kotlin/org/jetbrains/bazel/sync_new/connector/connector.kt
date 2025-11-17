package org.jetbrains.bazel.sync_new.connector

interface BazelConnector {
  fun query(startup: StartupOptions.() -> Unit = {}, args: QueryArgs.() -> Unit = {}): BazelResult<QueryResult>
}

sealed interface BazelResult<T> {
  class Success<T>(val value: T) : BazelResult<T>
  class Failure(val error: Throwable) : BazelResult<Nothing>
}
