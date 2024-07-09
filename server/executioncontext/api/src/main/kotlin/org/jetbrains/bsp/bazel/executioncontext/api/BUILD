load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "api",
    srcs = glob(["*.kt"]),
    visibility = ["//server/executioncontext/api:__pkg__"],
    exports = [
        "//server/executioncontext/projectview:model",
    ],
    deps = [
        "//server/executioncontext/projectview:model",
    ],
)
