load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_import", "kt_jvm_library")
load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_java//java:java_import.bzl", "java_import")

kt_jvm_import(
    name = "annotations_import",
    jars = ["@annotations_http//file"],
    visibility = ["//visibility:public"],
)

kt_jvm_library(
    name = "a-lib",
    srcs = glob(["src/main/kotlin/com/**/*"]),
    visibility = ["//visibility:public"],
    resource_jars = [":foo-res", ":bar-res"],
    deps = [":annotations_import"],
)

java_library(
  name = "foo-res",
  resources = glob(["src/main/foo-res/**/*"]),
  resource_strip_prefix = "module/src/main/foo-res",
)

java_import(
  name = "bar-res",
  jars = ["bar-res.jar"],
)
