load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "target",
    srcs = glob(["*.kt"]),
    visibility = ["//plugin-bazel:__subpackages__"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/gson",
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/annotations",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/config",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/magicmetamodel/impl/workspacemodel",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/workspacemodel/entities",
        "//protocol",
    ],
)
