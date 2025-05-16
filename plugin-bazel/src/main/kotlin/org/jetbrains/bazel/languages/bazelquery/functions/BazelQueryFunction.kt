package org.jetbrains.bazel.languages.bazelquery.functions

sealed class BazelQueryFunction {
  abstract val name: String
  abstract val description: String
  abstract val arguments: List<StandardFunctionArgument>
  abstract val varArguments: VarArgFunctionArgument?
  abstract val exampleUsage: String

  sealed interface BazelQueryFunctionArgument {
    val name: String
    val type: String
    val description: String
  }

  data class StandardFunctionArgument(
    override val name: String,
    override val type: String,
    val optional: Boolean,
    override val description: String,
  ) : BazelQueryFunctionArgument

  data class VarArgFunctionArgument(
    override val name: String,
    override val type: String,
    override val description: String,
  ) : BazelQueryFunctionArgument

  data class SimpleFunction(
    override val name: String,
    override val description: String,
    override val exampleUsage: String,
    override val arguments: List<StandardFunctionArgument> = emptyList(),
    override val varArguments: VarArgFunctionArgument? = null,
  ) : BazelQueryFunction()

  companion object {
    private val knownFunctions =
      listOf(
        SimpleFunction(
          name = "allpaths",
          description = "Computes all paths between two sets of targets.",
          exampleUsage = "allpaths(//foo:bar, //baz:qux)",
          arguments =
            listOf(
              StandardFunctionArgument("start", "expr", false, "Set of starting points."),
              StandardFunctionArgument("end", "expr", false, "Set of ending points."),
            ),
        ),
        SimpleFunction(
          name = "allrdeps",
          description = "Computes all reverse dependencies in the universe set.",
          exampleUsage = "allrdeps(//foo:bar, 2)",
          arguments =
            listOf(
              StandardFunctionArgument("expr", "expr", false, "The input set of targets."),
              StandardFunctionArgument("depth", "int", true, "Maximum depth for reverse dependencies."),
            ),
        ),
        SimpleFunction(
          name = "attr",
          description = "Filters targets by rule attribute values using regular expressions.",
          exampleUsage = "attr(\"tags\", \"[feature]\", deps(//foo:bar))",
          arguments =
            listOf(
              StandardFunctionArgument("name", "word", false, "The attribute name."),
              StandardFunctionArgument("pattern", "word", false, "Regular expression for attribute values."),
              StandardFunctionArgument("input", "expr", false, "The input set of targets."),
            ),
        ),
        SimpleFunction(
          name = "buildfiles",
          description = "Returns the BUILD files that define the packages for a given set of targets.",
          exampleUsage = "buildfiles(deps(//foo:bar))",
          arguments =
            listOf(
              StandardFunctionArgument("expr", "expr", false, "The input set of targets."),
            ),
        ),
        SimpleFunction(
          name = "deps",
          description = "Computes the transitive closure of dependencies of a given set of targets.",
          exampleUsage = "deps(//target:foo) or deps(//target:foo, 2)",
          arguments =
            listOf(
              StandardFunctionArgument("expr", "expr", false, "The input set of targets."),
              StandardFunctionArgument("depth", "int", true, "Maximum depth of dependency graph traversal."),
            ),
        ),
        SimpleFunction(
          name = "filter",
          description = "Filters a set of targets whose labels match a given pattern.",
          exampleUsage = "filter(\"//foo\", deps(//bar:baz))",
          arguments =
            listOf(
              StandardFunctionArgument("pattern", "word", false, "Regular expression for the target name."),
              StandardFunctionArgument("input", "expr", false, "The input set of targets to filter."),
            ),
        ),
        SimpleFunction(
          name = "kind",
          description = "Filters a set of targets by their kind (type).",
          exampleUsage = "kind(\"source file\", deps(//foo:bar))",
          arguments =
            listOf(
              StandardFunctionArgument("pattern", "word", false, "Regular expression for the target type."),
              StandardFunctionArgument("input", "expr", false, "The input set of targets."),
            ),
        ),
        SimpleFunction(
          name = "labels",
          description = "Returns the set of targets specified in a given attribute of type label.",
          exampleUsage = "labels(\"srcs\", deps(//foo:bar))",
          arguments =
            listOf(
              StandardFunctionArgument("attribute", "word", false, "The name of the attribute."),
              StandardFunctionArgument("input", "expr", false, "The input set of targets."),
            ),
        ),
        SimpleFunction(
          name = "loadfiles",
          description = "Returns the Starlark (.bzl) files needed to load the packages of a given set of targets.",
          exampleUsage = "loadfiles(deps(//foo:bar))",
          arguments =
            listOf(
              StandardFunctionArgument("expr", "expr", false, "The input set of targets."),
            ),
        ),
        SimpleFunction(
          name = "rbuildfiles",
          description = "Returns the BUILD files that transitively depend on given path fragments.",
          exampleUsage = "rbuildfiles(foo/BUILD, bar/file.bzl)",
          arguments = listOf(StandardFunctionArgument("path", "word", false, "A list of path fragments (variable number of arguments).")),
          varArguments =
            VarArgFunctionArgument(
              name = "path",
              type = "word",
              description = "A list of path fragments (variable number of arguments).",
            ),
        ),
        SimpleFunction(
          name = "rdeps",
          description = "Computes reverse dependencies of a set of targets within a universe set.",
          exampleUsage = "rdeps(//foo/..., //bar:baz, 1)",
          arguments =
            listOf(
              StandardFunctionArgument("universe", "expr", false, "The universe set."),
              StandardFunctionArgument("input", "expr", false, "The input set."),
              StandardFunctionArgument("depth", "int", true, "Maximum depth for reverse dependencies."),
            ),
        ),
        SimpleFunction(
          name = "same_pkg_direct_rdeps",
          description = "Returns the set of targets in the same package that directly depend on the input set.",
          exampleUsage = "same_pkg_direct_rdeps(//foo:bar)",
          arguments =
            listOf(
              StandardFunctionArgument("expr", "expr", false, "The input set of targets."),
            ),
        ),
        SimpleFunction(
          name = "siblings",
          description = "Returns the set of targets in the same package as the input set of targets.",
          exampleUsage = "siblings(//foo:bar)",
          arguments =
            listOf(
              StandardFunctionArgument("expr", "expr", false, "The input set of targets."),
            ),
        ),
        SimpleFunction(
          name = "some",
          description = "Selects at most k targets arbitrarily from the input set.",
          exampleUsage = "some(deps(//foo:bar), 2)",
          arguments =
            listOf(
              StandardFunctionArgument("expr", "expr", false, "The input set of targets."),
              StandardFunctionArgument("count", "int", true, "The maximum number of targets to select."),
            ),
        ),
        SimpleFunction(
          name = "somepath",
          description = "Computes a single path between two sets of targets.",
          exampleUsage = "somepath(//foo:bar, //baz:qux)",
          arguments =
            listOf(
              StandardFunctionArgument("start", "expr", false, "Set of starting points."),
              StandardFunctionArgument("end", "expr", false, "Set of ending points."),
            ),
        ),
        SimpleFunction(
          name = "tests",
          description = "Returns all test targets in the input set, expanding test_suite rules.",
          exampleUsage = "tests(deps(//foo:bar))",
          arguments =
            listOf(
              StandardFunctionArgument("expr", "expr", false, "The input set of targets."),
            ),
        ),
        SimpleFunction(
          name = "visible",
          description = "Filters a set of targets based on the visibility to a predicate target set.",
          exampleUsage = "visible(//foo, deps(//bar:qux))",
          arguments =
            listOf(
              StandardFunctionArgument("predicate", "expr", false, "The predicate target set."),
              StandardFunctionArgument("input", "expr", false, "The input set of targets."),
            ),
        ),
      )

    fun byName(name: String): BazelQueryFunction? = knownFunctions.firstOrNull { it.name == name }

    fun getAll(): List<BazelQueryFunction> = knownFunctions
  }
}
