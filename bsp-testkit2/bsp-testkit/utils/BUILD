load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "FlatMapUtils",
    srcs = ["FlatMapUtils.kt"],
)

kt_jvm_library(
    name = "JsonComparator",
    srcs = ["JsonComparator.kt"],
    visibility = ["//visibility:public"],
    deps = [
        "FlatMapUtils",
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/gson",
        "@maven//:com_google_guava_guava",
        "@maven//:org_junit_jupiter_junit_jupiter_api",
    ],
)
