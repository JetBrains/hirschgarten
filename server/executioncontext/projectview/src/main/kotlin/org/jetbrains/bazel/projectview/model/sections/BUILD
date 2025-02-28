load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sections",
    srcs = glob(["*.kt"]),
    visibility = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bazel/projectview/model:__pkg__",
        "//server/executioncontext/projectview/src/test/kotlin/org/jetbrains/bazel/projectview/model/sections:__pkg__",
    ],
    exports = [
        "//protocol",
    ],
    deps = [
        "//protocol",
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/label",
    ],
)
