load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sharding",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//server/executioncontext/workspacecontext",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/paths",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/proto:bsp_target_info_java_proto",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/utils",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)
