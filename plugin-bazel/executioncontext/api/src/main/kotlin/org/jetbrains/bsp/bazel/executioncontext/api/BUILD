load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "api",
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bazel/executioncontext/api:__pkg__"],
    exports = [
        "//plugin-bazel/executioncontext/projectview:model",
    ],
    deps = [
        "//plugin-bazel/executioncontext/projectview:model",
    ],
)
