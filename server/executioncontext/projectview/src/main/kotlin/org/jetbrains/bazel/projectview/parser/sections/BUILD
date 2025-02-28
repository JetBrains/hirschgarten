load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sections",
    srcs = glob(["*.kt"]),
    visibility = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/parser:__pkg__",
        "//server/executioncontext/projectview/src/test/kotlin/org/jetbrains/bazel/projectview/parser/sections:__pkg__",
    ],
    exports = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/model",
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/parser/splitter",
    ],
    deps = [
        "//server/commons",
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/model",
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/parser/splitter",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_logging_log4j_log4j_core",
    ],
)
