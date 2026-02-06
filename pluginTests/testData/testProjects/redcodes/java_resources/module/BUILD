load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_java//java:java_import.bzl", "java_import")

java_import(
    name = "annotations_import",
    jars = ["@annotations_http//file"],
    visibility = ["//visibility:public"],
)

java_library(
    name = "a-lib",
    srcs = glob(["src/main/java/com/**/*"]),
    deps = [":annotations_import"],
    visibility = ["//visibility:public"],
    resources = glob(["src/main/resources/**/*", "src/main/java/messages/**"]),
    runtime_deps = [
      ":foo-res",
      ":bar-res"
    ],
)

java_library(
  name = "foo-res",
  resources = glob(["src/main/foo-res/**/*"]),
  resource_strip_prefix = "module/src/main/foo-res",
)

java_library(
  name = "bar-res",
  resources = glob(["src/main/bar-res/**/*"]),
  resource_strip_prefix = "module/src/main/bar-res",
)