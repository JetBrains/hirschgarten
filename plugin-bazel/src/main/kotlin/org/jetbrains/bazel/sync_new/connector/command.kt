package org.jetbrains.bazel.sync_new.connector

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import kotlinx.coroutines.flow.Flow
import java.nio.file.Path

// TODO: add required options on-demand
sealed interface QueryResult {
  class Proto(result: Build.QueryResult)
  class StreamedProto(flow: Flow<Build.Target>) : QueryResult
}

interface QueryArgs : Args
fun QueryArgs.output(output: String): Unit = add("output", valueOf(output))
fun QueryArgs.queryFile(file: Path): Unit = add("query_file", valueOf(file))
fun QueryArgs.keepGoing(): Unit = add("keep_going", valueOf(true))

