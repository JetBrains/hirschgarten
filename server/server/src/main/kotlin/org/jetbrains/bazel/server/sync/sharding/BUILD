load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "sharding",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/symlinks",
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext",
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/paths",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/firstPhase",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
)
