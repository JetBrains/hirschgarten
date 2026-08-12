load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load(":generator.bzl", "animals_library_source")

kt_jvm_library(
  name = "main", 
  srcs = ["Main.kt"],
  deps = [":addition"],
)

kt_jvm_library(
  name = "addition",
  srcs = ["my_addition.kt", ":generated"],
)


animals_library_source(
    name = "generated",
    visibility = ["//visibility:public"],
)

