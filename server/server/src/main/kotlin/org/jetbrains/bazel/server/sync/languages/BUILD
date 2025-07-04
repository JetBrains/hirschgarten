load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "languages",
    srcs = glob(["**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/dependencygraph",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/model",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/paths",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
)
