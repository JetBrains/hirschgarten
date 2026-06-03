load("@rules_java//java:java_import.bzl", "java_import")
load("@rules_java//java:java_library.bzl", "java_library")

java_import(
    name = "annotations_import",
    jars = ["@annotations_http//file"],
    visibility = ["//visibility:public"],
)

java_library(
    name = "a-lib",
    srcs = glob(["src/main/java/com/**/*"]),
    resources = glob([
        "src/main/resources/**/*",
        "src/main/java/messages/**",
    ]),
    visibility = ["//visibility:public"],
    runtime_deps = [
        ":bar-res",
        ":foo-res",
    ],
    deps = [":annotations_import"],
)

java_library(
    name = "foo-res",
    resource_strip_prefix = "module/src/main/foo-res",
    resources = glob(["src/main/foo-res/**/*"]),
)

java_library(
    name = "bar-res",
    resource_strip_prefix = "module/src/main/bar-res",
    resources = glob(["src/main/bar-res/**/*"]),
)
