load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "targetKinds",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons",
        "//plugin-bazel/src/main/kotlin/org/jetbrains/bazel/sync/targetKind",
        "//rules_intellij/third_party/go",
    ],
)
