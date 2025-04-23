load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "sections",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//server/server/src/main/kotlin/org/jetbrains/bazel/server/label",
    ],
)
