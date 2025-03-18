load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "label",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//:kotlin_serialization",
        "@maven//:org_jetbrains_kotlinx_kotlinx_serialization_core_jvm",
        "@maven//:org_jetbrains_kotlinx_kotlinx_serialization_json",
    ],
)
