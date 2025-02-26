load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "benchmark",
    srcs = glob([
        "*.kt",
    ]),
    visibility = ["//visibility:public"],
    deps = [
        "//commons/src/main/kotlin/org/jetbrains/bazel/commons/constants",
        "//server/logger",
        "@maven//:io_opentelemetry_opentelemetry_exporter_otlp",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
    ],
)
