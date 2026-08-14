load("@rules_java//java:java_library.bzl", "java_library")
load(":junit_test.bzl", "kt_test")

kt_test(
    name = "SimpleKotlinTest",
    size = "large",
    srcs = ["SimpleKotlinTest.kt"],
    classname = "SimpleKotlinTest",
)

java_library(
    name = "hello",
    srcs = ["nested_src/Hello.java"],
    deps = [
        ":junit_test.bzl",
        "//src/package_with_macros:my_java_library",
    ],
)

java_library(
    name = "junit_test.bzl",
    srcs = ["//src/package_with_macros:Hello.java"],
)
