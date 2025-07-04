load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "provider",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    exports = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/executioncontext/api",
    ],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/executioncontext/api",
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/generator",
        "//commons/src/main/kotlin/org/jetbrains/bazel/projectview/parser",
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//server/bazelrunner:params",
        "@maven//:commons_io_commons_io",
        "@maven//:org_slf4j_slf4j_api",
    ],
)
