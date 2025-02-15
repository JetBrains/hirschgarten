load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "installationcontext",
    srcs = glob(["*.kt"]),
    visibility = [
        "//plugin-bazel/executioncontext/installationcontext:__pkg__",
        "//plugin-bazel/executioncontext/installationcontext/src/test/kotlin/org/jetbrains/bsp/bazel/installationcontext:__pkg__",
    ],
    exports = [
        "//plugin-bazel/executioncontext/api",
    ],
    deps = [
        "//plugin-bazel/executioncontext/api",
        "//plugin-bazel/executioncontext/projectview:parser",
    ],
)
