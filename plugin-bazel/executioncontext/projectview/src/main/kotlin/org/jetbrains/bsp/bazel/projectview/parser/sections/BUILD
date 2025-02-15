load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sections",
    srcs = glob(["*.kt"]),
    visibility = [
        "//plugin-bazel/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/parser:__pkg__",
        "//plugin-bazel/executioncontext/projectview/src/test/kotlin/org/jetbrains/bsp/bazel/projectview/parser/sections:__pkg__",
    ],
    exports = [
        "//plugin-bazel/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/model",
        "//plugin-bazel/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/parser/splitter",
    ],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/model",
        "//plugin-bazel/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/parser/splitter",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_logging_log4j_log4j_core",
    ],
)
