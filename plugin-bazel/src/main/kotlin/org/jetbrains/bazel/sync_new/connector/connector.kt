package org.jetbrains.bazel.sync_new.connector

interface BazelConnector {
  suspend fun build(startup: StartupOptions.() -> Unit = {}, args: BuildArgs.() -> Unit = {}): BazelResult<Unit>
  suspend fun query(startup: StartupOptions.() -> Unit = {}, args: QueryArgs.() -> Unit = {}): BazelResult<QueryResult>
}

sealed interface BazelResult<T> {
  class Success<T>(val value: T) : BazelResult<T>
  class Failure<T>(val reason: BazelFailureReason) : BazelResult<T>
}

enum class BazelFailureReason {
  SUCCESS,
  COMMAND_LINE_ERROR,
  BUILD_ERROR,
  BUILD_CANCELLED,
  OOM_ERROR,
  FATAL_ERROR,
  PARTIAL_FAILURE;

  val isRecoverable: Boolean
    get() = this == SUCCESS || this == BUILD_ERROR || this == PARTIAL_FAILURE

  companion object {
    fun fromExitCode(exitCode: Int): BazelFailureReason = when (exitCode) {
      0 -> SUCCESS
      2 -> COMMAND_LINE_ERROR
      1 -> BUILD_ERROR
      8 -> BUILD_CANCELLED
      33 -> OOM_ERROR
      3 -> PARTIAL_FAILURE
      else -> FATAL_ERROR
    }
  }
}

fun <T> BazelResult<T>.unwrap(): T {
  return when (this) {
    is BazelResult.Success -> value
    is BazelResult.Failure -> throw RuntimeException("Bazel command failed: $reason")
  }
}

interface UnwrappableResult

inline fun <reified T> UnwrappableResult.unwrap(): T = this as? T ?: error("expected ${T::class.java.name}, got ${this.javaClass.name}")
