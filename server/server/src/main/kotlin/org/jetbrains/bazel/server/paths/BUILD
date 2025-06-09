load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "paths",
    srcs = glob(["*.kt"]),
    visibility = ["//server/server:__subpackages__"],
    deps = [
        "//server/bazelrunner",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/bzlmod",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/label",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/sync/proto:bsp_target_info_java_proto",
    ],
)
