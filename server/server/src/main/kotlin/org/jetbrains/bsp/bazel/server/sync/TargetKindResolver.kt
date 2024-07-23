package org.jetbrains.bsp.bazel.server.sync

import org.jetbrains.bsp.bazel.info.BspTargetInfo
import org.jetbrains.bsp.bazel.server.model.Tag

class TargetKindResolver {
    fun resolveTags(targetInfo: BspTargetInfo.TargetInfo): Set<Tag> {
        if (targetInfo.kind == "resources_union" ||
            targetInfo.kind == "java_import" ||
            targetInfo.kind == "aar_import"
        ) {
            return LIBRARY
        }
        val tag = ruleSuffixToTargetType.filterKeys {
            targetInfo.kind.endsWith("_$it") || targetInfo.kind == it
        }.values.firstOrNull() ?: NO_IDE
        return if (targetInfo.tagsList.contains("no-ide")) {
            tag + Tag.NO_IDE
        } else if (targetInfo.tagsList.contains("manual")) {
            tag + Tag.MANUAL
        } else tag
    }

    companion object {
        private val LIBRARY: Set<Tag> = setOf(Tag.LIBRARY)
        private val ruleSuffixToTargetType = mapOf(
            "library" to LIBRARY,
            "binary" to setOf(Tag.APPLICATION),
            "test" to setOf(Tag.TEST),
            "proc_macro" to LIBRARY,
            "intellij_plugin_debug_target" to setOf(Tag.INTELLIJ_PLUGIN, Tag.APPLICATION),
            "plugin" to LIBRARY,
        )
        private val NO_IDE: Set<Tag> = setOf(Tag.NO_IDE)
    }
}
