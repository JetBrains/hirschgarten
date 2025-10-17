load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "protocol",
    srcs = glob(["**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
        "//commons/src/main/kotlin/org/jetbrains/bazel/workspacecontext",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/testing",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
        "//third_party/bazel/src/main/protobuf:build_java_proto",
    ],
)
