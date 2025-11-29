package org.jetbrains.bazel.sync_new.connector

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.jetbrains.bazel.label.Label

interface BuildArgs : Args

fun BuildArgs.nobuild(): Unit = add("nobuild", argValueOf(true))
fun BuildArgs.aspects(aspects: List<String>): Unit = add("aspects", argValueOf(aspects.joinToString(separator = ",")))
fun BuildArgs.targetPatterns(patterns: List<String>): Unit = add("target_patterns", argValueOf(patterns.joinToString(separator = "\n")))

sealed interface QueryResult : UnwrappableResult {
  data class Proto(val result: Build.QueryResult) : QueryResult
  data class StreamedProto(val flow: Flow<Build.Target>) : QueryResult
  data class Labels(val labels: List<Label>) : QueryResult
}

suspend fun QueryResult.unwrapProtos(): List<Build.Target> = when (this) {
  is QueryResult.Proto -> result.targetList
  is QueryResult.StreamedProto -> flow.toList()
  else -> error("Query output does not contain proto target definitions")
}

enum class QueryOutput {
  PROTO,
  STREAMED_PROTO,
  LABEL
}

// TODO: QueryBuilder dsl
interface QueryArgs : Args

fun QueryArgs.output(output: QueryOutput): Unit = add("output", argValueOf(output))
fun QueryArgs.query(query: String): Unit = add("query", argValueOf(query))
fun QueryArgs.consistentLabels(): Unit = add("consistent_labels", argValueOf(true))

// TODO: use query expr instead of string
fun QueryArgs.universeScope(scope: List<String>) = add("universe_scope", argValueOf(scope.joinToString(separator = ",")))

