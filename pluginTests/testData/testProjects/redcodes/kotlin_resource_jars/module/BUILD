load("@rules_java//java:java_import.bzl", "java_import")
load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_import", "kt_jvm_library")

kt_jvm_import(
    name = "annotations_import",
    jars = ["@annotations_http//file"],
    visibility = ["//visibility:public"],
)

kt_jvm_library(
    name = "a-lib",
    srcs = glob(["src/main/kotlin/com/**/*"]),
    resource_jars = [
        ":foo-res",
        ":bar-res",
    ],
    visibility = ["//visibility:public"],
    deps = [":annotations_import"],
)

java_library(
    name = "foo-res",
    resource_strip_prefix = "module/src/main/foo-res",
    resources = glob(["src/main/foo-res/**/*"]),
)

java_import(
    name = "bar-res",
    jars = ["bar-res.jar"],
)
