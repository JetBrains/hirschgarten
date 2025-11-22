package org.jetbrains.bazel.sync_new.connector

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import kotlinx.coroutines.flow.Flow
import java.nio.file.Path

interface BuildArgs : Args

fun BuildArgs.nobuild(): Unit = add("nobuild", argValueOf(true))
fun BuildArgs.aspects(aspects: List<String>): Unit = add("aspects", argValueOf(aspects.joinToString(separator = ",")))
fun BuildArgs.targetPatterns(patterns: List<String>): Unit = add("target_patterns", argValueOf(patterns.joinToString(separator = "\n")))

sealed interface QueryResult {
  data class Proto(val result: Build.QueryResult) : QueryResult
  data class StreamedProto(val flow: Flow<Build.Target>) : QueryResult
}

interface QueryArgs : Args

fun QueryArgs.output(output: String): Unit = add("output", argValueOf(output))
fun QueryArgs.query(query: String): Unit = add("query", argValueOf(query))

