load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "workspacecontext",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/executioncontext/api",
        "//rules_intellij/intellij_platform_sdk:plugin_api",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/label",
        "@maven//:commons_io_commons_io",
        "@maven//:org_slf4j_slf4j_api",
    ],
)
