load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "dependencygraph",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    exports = [
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/info",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/label",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
)
