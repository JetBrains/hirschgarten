load("@rules_jvm_external//:defs.bzl", "artifact")

java_test(
    name = "basename",
    srcs = ["SimpleTest.java"],
    test_class = "org.example.basenameConflict.SimpleTest",
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "nested/basename",
    srcs = ["nested/SimpleTest.java"],
    test_class = "org.example.basenameConflict.nested.SimpleTest",
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:junit_junit",
    ],
)
