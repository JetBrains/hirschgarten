load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_import")

kt_jvm_import(
    name = "annotations_import",
    jars = ["@annotations_http//file"],
    visibility = ["//visibility:public"],
)

kt_jvm_library(
    name = "a-lib",
    srcs = glob(["src/main/kotlin/com/**/*"]),
    visibility = ["//visibility:public"],
    resources = glob(["src/main/resources/**/*", "src/main/kotlin/messages/**"]),
    deps = [
      ":annotations_import",
      ":foo-res",
      ":bar-res"
    ],
)

kt_jvm_library(
  name = "foo-res",
  resources = glob(["src/main/foo-res/**/*"]),
  resource_strip_prefix = "module/src/main/foo-res",
)

kt_jvm_library(
  name = "bar-res",
  resources = glob(["src/main/bar-res/**/*"]),
  resource_strip_prefix = "module/src/main/bar-res",
)