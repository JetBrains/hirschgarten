load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sharding",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/symlinks",
        "//plugin-bazel/executioncontext/workspacecontext",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/paths",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/firstPhase",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/proto:bsp_target_info_java_proto",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)
