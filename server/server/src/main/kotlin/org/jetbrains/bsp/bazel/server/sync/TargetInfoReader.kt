package org.jetbrains.bsp.bazel.server.sync

import com.google.protobuf.Message
import com.google.protobuf.TextFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.bsp.bazel.info.BspTargetInfo.AndroidAarImportInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.AndroidTargetInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.CppTargetInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.JavaRuntimeInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.JavaToolchainInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.JvmTargetInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.KotlinTargetInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.PythonTargetInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.RustCrateInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.ScalaTargetInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name

class TargetInfoReader {
    fun readTargetMapFromAspectOutputs(files: Set<Path>): Map<Label, TargetInfo> {
        return runBlocking(Dispatchers.Default) {
            files.map { file -> async { readFromFile(file) } }.awaitAll()
        }.groupBy { it.id }
            // If any aspect has already been run on the build graph, it created shadow graph
            // containing new nodes of the same labels as the original ones. In particular,
            // this happens for all protobuf targets, for which a built-in aspect "bazel_java_proto_aspect"
            // is run. In order to correctly address this issue, we would have to provide separate
            // entities (TargetInfos) for each target and each ruleset (or language) instead of just
            // entity-per-label. As long as we don't have it, in case of a conflict we just take the entity
            // that contains JvmTargetInfo as currently it's the most important one for us. Later, we sort by
            // shortest size to get a stable result, which should be the default config.
            .mapValues {
                it.value.filter(TargetInfo::hasJvmTargetInfo).minByOrNull { targetInfo -> targetInfo.serializedSize } ?: it.value.first()
            }
            .mapKeys { Label.parse(it.key) }
    }

    private fun readFromFile(file: Path): TargetInfo {
        val builder = TargetInfo.newBuilder()
        val parser = TextFormat.Parser.newBuilder().setAllowUnknownFields(true).build()
        file.reader().use {
            parser.merge(it, builder)
        }
        return builder.build()
    }
}
