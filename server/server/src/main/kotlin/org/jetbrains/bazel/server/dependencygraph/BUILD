load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "dependencygraph",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    exports = [
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bsp/info",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/label",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
)
