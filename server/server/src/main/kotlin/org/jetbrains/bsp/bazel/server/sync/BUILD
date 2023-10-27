load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sync",
    srcs = glob([
        "**/*.java",
        "**/*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//bazelrunner",
        "//bazelrunner/src/main/kotlin/org/jetbrains/bsp/bazel/bazelrunner/utils",
        "//commons",
        "//executioncontext/workspacecontext",
        "//logger",
        "//server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bep",
        "//server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/info",
        "//server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/managers",
        "//server/src/main/kotlin/org/jetbrains/bsp/bazel/server/bsp/utils",
        "//server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/dependencytree",
        "//server/src/main/kotlin/org/jetbrains/bsp/bazel/server/sync/proto:bsp_target_info_java_proto",
        "@com_google_protobuf//:protobuf_java",
        "@io_bazel//src/main/protobuf:deps_java_proto",
        "@maven//:ch_epfl_scala_bsp4j_2_13",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:org_eclipse_lsp4j_org_eclipse_lsp4j_jsonrpc",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
    ],
)