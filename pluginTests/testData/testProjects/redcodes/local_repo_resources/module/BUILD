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
    resources = glob(["src/main/resources/**/*"]),
    resource_strip_prefix = "module/src/main/resources",
    runtime_deps = [
      "@local_repo//res:foo-res",
      "@local_repo//res:bar-res",
      "@local_repo//res:standard-res",
    ],
)
