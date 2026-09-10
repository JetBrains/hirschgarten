package org.jetbrains.bazel.sync

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.BazelQueryOutput
import org.jetbrains.bazel.server.BazelQueryParams
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bsp.protocol.TaskId

@ApiStatus.Internal
object FileToTargetQuery {

  /**
   * Find targets that own specific source file. Source files are identified by labels.
   */
  suspend fun findDependantTargetsFromFiles(
    server: BazelServerFacade,
    fileLabels: Set<Label>,
    taskId: TaskId? = null,
  ): Map<Label, List<Label>> {
    if (fileLabels.isEmpty()) {
      return emptyMap()
    }
    val result =
      server.query(
        BazelQueryParams(
          expression = expressionOf(fileLabels),
          output = BazelQueryOutput.Targets,
          taskId = taskId,
          keepGoing = true,
          // we need canonical labels here
          flags = listOf(BazelFlag.consistentLabels(true)),
        ),
      )
    return fileLabels.associateWith { emptyList<Label>() } + result.result.targetsByFile(fileLabels)
  }

  private fun expressionOf(fileLabels: Set<Label>): String =
    fileLabels.joinToString(separator = " + ", prefix = "same_pkg_direct_rdeps(", postfix = ")") { "\"$it\"" }

  // here we build map of file to owning targets in bulk
  // `targetsByFile` scan all output attributes for presence of requested file labels (with respect to `nodep`)
  // and combine it into single map, by that we can query multiple files -> target mappings at once
  private fun List<Build.Target>.targetsByFile(fileLabels: Set<Label>): Map<Label, List<Label>> {
    val targetsByFile = mutableMapOf<Label, MutableList<Label>>()
    for (target in this) {
      if (target.type != Build.Target.Discriminator.RULE) continue
      val ruleLabel = Label.parseOrNull(target.rule.name) ?: continue
      for (fileLabel in target.rule.usedFileLabels(fileLabels)) {
        targetsByFile.getOrPut(fileLabel) { mutableListOf() }.add(ruleLabel)
      }
    }
    return targetsByFile
  }

  private fun Build.Rule.usedFileLabels(fileLabels: Set<Label>): Set<Label> =
    attributeList.asSequence()
      .filterNot { it.nodep }
      .flatMap { it.labelValues() }
      .mapNotNull { Label.parseOrNull(it) }
      .filterTo(hashSetOf()) { it in fileLabels }

  private fun Build.Attribute.labelValues(): List<String> =
    when (type) {
      Build.Attribute.Discriminator.LABEL, Build.Attribute.Discriminator.OUTPUT -> listOf(stringValue)
      Build.Attribute.Discriminator.LABEL_LIST, Build.Attribute.Discriminator.OUTPUT_LIST -> stringListValueList
      Build.Attribute.Discriminator.LABEL_KEYED_STRING_DICT -> labelKeyedStringDictValueList.map { it.key }
      else -> emptyList()
    }
}
