load("@contrib_rules_jvm//java:defs.bzl", "JUNIT5_DEPS", "java_test_suite")
load("@rules_jvm_external//:defs.bzl", "artifact")

java_test_suite(
    name = "SimpleJavaTest",
    size = "small",
    srcs = glob(["*.java"]),
    runner = "junit5",
    runtime_deps = JUNIT5_DEPS,
    deps = [
        artifact("org.junit.jupiter:junit-jupiter-api"),
        artifact("org.junit.jupiter:junit-jupiter-params"),
    ],
)
