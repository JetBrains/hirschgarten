load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "parser",
    srcs = glob(["*.kt"]),
    resources = ["//server/log4j_config"],
    visibility = [
        "//server/executioncontext/projectview:__pkg__",
        "//server/executioncontext/projectview/src/test/kotlin/org/jetbrains/bsp/bazel/projectview/parser:__pkg__",
    ],
    exports = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/model",
    ],
    deps = [
        "//server/commons",
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/model",
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/parser/sections",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_logging_log4j_log4j_core",
    ],
)
