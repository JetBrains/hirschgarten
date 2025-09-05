load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "model",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bsp/protocol",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bzlmod",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
        "//third_party/bazel/src/main/protobuf:build_java_proto",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
    ],
)
