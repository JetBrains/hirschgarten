java_library(
    name = "tests",
    testonly = True,
    srcs = glob(["*Test.java"]),
    deps = [
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "AddTest",
    test_class = "org.example.sharedTestLib.AddTest",
    runtime_deps = [":tests"],
)

# Bazel derives the test class `org.example.sharedTestLib.SubtractTest` from the label
java_test(
    name = "SubtractTest",
    runtime_deps = [":tests"],
)

# Bazel derives the test class `org.example.sharedTestLib.other.MultiplyTest` from `srcs`, not from the label
java_test(
    name = "MultiplyTest",
    srcs = ["other/MultiplyTest.java"],
    runtime_deps = [":tests"],
    deps = [
        "@maven//:junit_junit",
    ],
)

java_test(
    name = "SharedMultiplyTest",
    test_class = "org.example.sharedTestLib.MultiplyTest",
    runtime_deps = [":tests"],
)

java_library(
    name = "apps",
    srcs = ["FooApp.java"],
)

java_binary(
    name = "FooApp",
    main_class = "org.example.sharedTestLib.FooApp",
    runtime_deps = [":apps"],
)
