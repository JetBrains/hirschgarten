load("//rules/kotlin:macros.bzl", "kt_library")

kt_library(
    name = "FlatMapUtils",
    srcs = ["FlatMapUtils.kt"],
)

kt_library(
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
