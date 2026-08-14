load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//broken:broken.bzl", "broken")
load(":junit_test.bzl", "kt_test")

kt_test(
    name = "SimpleKotlinTest",
    size = "large",
    srcs = ["SimpleKotlinTest.kt"],
    classname = "SimpleKotlinTest",
)

kt_jvm_library(
    name = "associates_example",
    srcs = ["A.kt"],
    associates = [":B"],
)

kt_jvm_library(
    name = "B",
    srcs = ["B.kt"],
    exports = [":C"],
)

kt_jvm_library(
    name = "C",
    srcs = ["C.kt"],
)

kt_jvm_library(
    name = "not_building",
    srcs = ["X.kt"],
    tags = ["no-ide"],
    deps = ["//broken:should_be_ignored"],
)

broken(
    name = "ignore-me",
    tags = ["broken"],
)

kt_jvm_library(
    name = "requires-no-ide",
    srcs = ["Y.kt"],
    deps = [":not-ignored"],
)

kt_jvm_library(
    name = "not-ignored",
    srcs = ["Z.kt"],
    tags = ["no-ide"],
)
