load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "symlinks",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = ["//commons/src/main/kotlin/org/jetbrains/bazel/commons/constants"],
)
