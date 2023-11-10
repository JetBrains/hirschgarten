load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")

def _scala_deps(_ctx):
    scala_repositories()

scala_deps = module_extension(implementation = _scala_deps)
