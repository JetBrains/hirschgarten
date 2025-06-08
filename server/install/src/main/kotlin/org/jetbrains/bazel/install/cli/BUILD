load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "cli",
    srcs = glob(["*.kt"]),
    visibility = [
        "//server/install/src/main/kotlin/org/jetbrains/bazel/install:__subpackages__",
        "//server/install/src/test/kotlin/org/jetbrains/bazel/install/cli:__pkg__",
    ],
    deps = [
        "//server/commons",
    ],
)
