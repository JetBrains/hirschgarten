load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//rules/testing:ide_starter.bzl", "IDE_STARTER_DEPS")

kt_jvm_library(
    name = "hirschgarten",
    testonly = True,
    srcs = ["IntegrationTestCompat.kt"],
    visibility = ["//visibility:public"],
    deps = [
        "//pluginTests:test_fixtures",
    ] + IDE_STARTER_DEPS,
)
