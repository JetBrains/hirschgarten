load("@rules_java//java:defs.bzl", "java_binary", "java_library", "java_test")

java_library(
    name = "greeting",
    srcs = glob(["src/main/java/**/*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:org_springframework_spring_beans",
        "@maven//:org_springframework_spring_context",
    ],
)

java_binary(
    name = "demo",
    main_class = "com.example.greeting.DemoApplication",
    runtime_deps = [
        ":greeting",
        "@maven//:org_springframework_spring_context",
    ],
)

java_test(
    name = "wiring_test",
    srcs = ["src/test/java/com/example/greeting/SpringWiringTest.java"],
    main_class = "com.example.greeting.SpringWiringTest",
    use_testrunner = False,
    deps = [
        ":greeting",
        "@maven//:org_springframework_spring_context",
    ],
)
