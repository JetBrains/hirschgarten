load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_java//java:java_import.bzl", "java_import")

java_import(
    name = "annotations_import",
    jars = ["@annotations_http//file"],
    visibility = ["//visibility:public"],
)

java_library(
    name = "a-lib",
    srcs = glob(["src/main/java/**/*"]),
    deps = [":annotations_import"],
    visibility = ["//visibility:public"],
    resources = glob(["src/main/resources/**/*"], exclude = ["src/main/resources/messages/ExcludedBundle.properties"]),
    runtime_deps = [
      ":foo-res",
      ":bar-res"
    ],
)

java_library(
  name = "foo-res",
  resources = glob(["src/main/foo-res/**/*"], exclude = ["src/main/foo-res/messages/ExcludedFooBundle.properties"]),
  resource_strip_prefix = "module/src/main/foo-res",
)

java_library(
  name = "bar-res",
  resources = glob(["src/main/bar-res/**/*"], exclude = ["src/main/bar-res/messages/ExcludedBarBundle.properties"]),
  resource_strip_prefix = "module/src/main/bar-res",
)