load("@rules_java//java:java_library.bzl", "java_library")

java_library(
    name = "foo-res",
    resource_strip_prefix = "res/foo-res",
    resources = glob(["foo-res/**/*"]),
    visibility = ["//visibility:public"],
)

java_library(
    name = "bar-res",
    resource_strip_prefix = "res/bar-res",
    resources = glob(["bar-res/**/*"]),
    visibility = ["//visibility:public"],
)

java_library(
    name = "standard-res",
    resources = glob(["src/main/resources/**/*"]),
    visibility = ["//visibility:public"],
)
