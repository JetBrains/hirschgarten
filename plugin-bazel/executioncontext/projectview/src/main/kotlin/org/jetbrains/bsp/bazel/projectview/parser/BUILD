load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "parser",
    srcs = glob(["*.kt"]),
    resources = ["//plugin-bazel/log4j_config"],
    visibility = [
        "//plugin-bazel/executioncontext/projectview:__pkg__",
        "//plugin-bazel/executioncontext/projectview/src/test/kotlin/org/jetbrains/bsp/bazel/projectview/parser:__pkg__",
    ],
    exports = [
        "//plugin-bazel/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/model",
    ],
    deps = [
        "//plugin-bazel/commons",
        "//plugin-bazel/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/model",
        "//plugin-bazel/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/parser/sections",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_logging_log4j_log4j_core",
    ],
)
