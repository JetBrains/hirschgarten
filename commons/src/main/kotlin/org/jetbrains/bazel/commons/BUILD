load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "commons",
    srcs = glob(["*.kt", "*.java"], allow_empty = True),
    visibility = ["//visibility:public"],
    exports = ["//commons/src/main/kotlin/org/jetbrains/bazel/label"],
)
