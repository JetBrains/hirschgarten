load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "gson",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    exports = [
        "@maven//:com_google_code_gson_gson",
    ],
    deps = [
        "@maven//:com_google_code_gson_gson",
        "//commons/src/main/kotlin/org/jetbrains/bazel/label",
    ],
)
