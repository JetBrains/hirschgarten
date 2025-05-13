package org.jetbrains.bazel.languages.bazelquery.options

data class BazelQueryOption(val name: String, val values: List<String> = emptyList())

object BazelQueryCommonOptions {
  // Default value is always first in the list
  private val knownFlags =
    listOf(
      BazelQueryOption(
        name = "output",
        values =
          listOf(
            "label",
            "label_kind",
            "build",
            "minrank",
            "maxrank",
            "package",
            "location",
            "graph",
            "xml",
            "proto",
            "streamed_jsonproto",
            "streamed_proto",
          ),
      ),
      BazelQueryOption(
        name = "keep_going",
      ),
      BazelQueryOption(
        name = "aspect_deps",
        values = listOf("conservative", "precise", "off"),
      ),
      BazelQueryOption(
        name = "nonodep_deps",
      ),
      BazelQueryOption(
        name = "consistent_labels",
      ),
      BazelQueryOption(
        name = "relative_locations",
      ),
      BazelQueryOption(
        name = "order_output",
        values = listOf("auto", "no", "deps", "full"),
      ),
      BazelQueryOption(
        name = "noimplicit_deps",
      ),
      BazelQueryOption(
        name = "graph:conditional_edges_limit",
      ),
      BazelQueryOption(
        name = "universe_scope",
      ),
      BazelQueryOption(
        name = "nofetch",
      ),
    )

  fun byName(name: String): BazelQueryOption? = knownFlags.firstOrNull { it.name == name }

  fun getAll(): List<BazelQueryOption> = knownFlags
}
