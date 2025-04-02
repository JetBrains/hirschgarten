package org.jetbrains.bazel.languages.bazelquery.options

class BazelqueryCommonOptions {

  data class BazelqueryOption(
    val name: String,
    val values: List<String> = emptyList()
  )

  companion object {
    // Default value is always first in the list
    private val knownFlags = listOf(
      BazelqueryOption(
        name = "output",
        values = listOf(
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
          "streamed_proto"
        )
      ),
      BazelqueryOption(
        name = "keep_going",
      ),
      BazelqueryOption(
        name = "aspect_deps",
        values = listOf("conservative", "precise", "off")
      ),
      BazelqueryOption(
        name = "nodep_deps",
      ),
      BazelqueryOption(
        name = "consistent_labels",
      ),
      BazelqueryOption(
        name = "relative_locations"
      ),
      BazelqueryOption(
        name = "order_output",
        values = listOf("auto", "no", "deps", "full")
      ),
      BazelqueryOption(
        name = "implicit_deps",
      ),
      BazelqueryOption(
        name = "fetch",
      ),
      BazelqueryOption(
        name = "experimental_spawn_scheduler"
      ),
      BazelqueryOption(
        name = "host_action_env"
      ),
      BazelqueryOption(
        name = "graph:conditional_edges_limit"
      ),
    )

    fun byName(name: String): BazelqueryOption? = knownFlags.firstOrNull { it.name == name }
    fun getAll(): List<BazelqueryOption> = knownFlags
  }

}
