load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "installationcontext",
    srcs = glob(["*.kt"]),
    visibility = [
        "//server/executioncontext/installationcontext:__pkg__",
        "//server/executioncontext/installationcontext/src/test/kotlin/org/jetbrains/bsp/bazel/installationcontext:__pkg__",
    ],
    exports = [
        "//server/executioncontext/api",
    ],
    deps = [
        "//server/executioncontext/api",
        "//server/executioncontext/projectview:parser",
    ],
)
