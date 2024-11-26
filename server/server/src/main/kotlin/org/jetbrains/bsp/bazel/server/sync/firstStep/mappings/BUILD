load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "mappings",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//protocol",
        "//server/executioncontext/workspacecontext",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/model",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/languages",
        "//server/server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/proto:build_java_proto",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_google_protobuf_protobuf_java",
    ],
)
