load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "languages",
    srcs = glob(["**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//protocol",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/dependencygraph",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/model",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/paths",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
        "@maven//:ch_epfl_scala_bsp4j",
    ],
)
