load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "dependencygraph",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    exports = [
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
    deps = [
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/info",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/label",
        "//plugin-bazel/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
)
