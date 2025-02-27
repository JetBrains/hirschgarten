load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "diagnostics",
    srcs = glob(["*.kt"]),
    visibility = ["//server/server:__subpackages__"],
    deps = [
        "//protocol",
        "//server/bazelrunner",
        "//server/commons",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/label",
    ],
)
