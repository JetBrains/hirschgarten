load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "commons",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:com_google_code_gson_gson",
    ],
)