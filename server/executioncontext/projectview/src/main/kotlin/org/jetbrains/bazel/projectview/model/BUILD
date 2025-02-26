load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "model",
    srcs = glob(["*.kt"]),
    visibility = [
        "//server/executioncontext/projectview:__pkg__",
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview:__subpackages__",
        "//server/executioncontext/projectview/src/test/kotlin/org/jetbrains/bazel/projectview/model:__pkg__",
    ],
    exports = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/model/sections",
    ],
    deps = [
        "//server/commons",
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/model/sections",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_logging_log4j_log4j_core",
    ],
)
