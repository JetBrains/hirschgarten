package org.jetbrains.bazel.sync_new.connector

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import java.nio.file.Path

interface BuildArgs : Args

fun BuildArgs.nobuild(): Unit = add("nobuild", argValueOf(true))
fun BuildArgs.aspects(aspects: List<String>): Unit = add("aspects", argValueOf(aspects.joinToString(separator = ",")))
fun BuildArgs.targetPatterns(patterns: List<String>): Unit = add("target_patterns", argValueOf(patterns.joinToString(separator = "\n")))

sealed interface QueryResult {
  data class Proto(val result: Build.QueryResult) : QueryResult
  data class StreamedProto(val flow: Flow<Build.Target>) : QueryResult
}

suspend fun QueryResult.toProtoTargets(): List<Build.Target> = when (this) {
  is QueryResult.Proto -> result.targetList
  is QueryResult.StreamedProto -> flow.toList()
}

enum class QueryOutput {
  PROTO,
  STREAMED_PROTO
}

// TODO: API for easy bazel query creation from level of code - avoid stupid string concat
interface QueryArgs : Args

fun QueryArgs.output(output: QueryOutput): Unit = add("output", argValueOf(output))
fun QueryArgs.query(query: String): Unit = add("query", argValueOf(query))

