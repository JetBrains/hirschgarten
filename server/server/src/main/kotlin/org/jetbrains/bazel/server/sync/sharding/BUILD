load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sharding",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/symlinks",
        "//protocol",
        "//server/executioncontext/workspacecontext",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/paths",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/firstPhase",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
)
