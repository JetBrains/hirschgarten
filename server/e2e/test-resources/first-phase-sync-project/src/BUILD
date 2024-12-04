load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_binary", "kt_jvm_library")

java_library(
    name = "java-lib",
    srcs = ["Lib.java"],
)

java_binary(
    name = "java-binary",
    srcs = ["Main.java"],
    deps = [":java-lib"],
    main_class = "Main"
)

kt_jvm_library(
    name = "kt-lib",
    srcs = ["Lib.kt"]
)

kt_jvm_binary(
    name = "kt-binary",
    srcs = ["Main.kt"],
    deps = [":kt-lib"],
    main_class = "Main"
)
