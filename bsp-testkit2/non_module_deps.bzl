load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")


IO_BAZEL_RULES_SCALA_TAG = "6.2.1"

IO_BAZEL_RULES_SCALA_SHA = "71324bef9bc5a885097e2960d5b8effed63399b55572219919d25f43f468c716"

def _rules_scala(_ctx):
    http_archive(
        name = "io_bazel_rules_scala",
        sha256 = IO_BAZEL_RULES_SCALA_SHA,
        strip_prefix = "rules_scala-{}".format(IO_BAZEL_RULES_SCALA_TAG),
        url = "https://github.com/bazelbuild/rules_scala/archive/refs/tags/v{}.tar.gz".format(IO_BAZEL_RULES_SCALA_TAG),
    )

rules_scala = module_extension(implementation = _rules_scala)
