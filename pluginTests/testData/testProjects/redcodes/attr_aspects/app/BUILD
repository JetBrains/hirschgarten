load("@rules_java//java:defs.bzl", "java_binary")
load(":wrapper.bzl", "wrapper")

# IDE-visible target that only *runs with* the hidden target (data edge).
java_binary(
    name = "app_with_data",
    srcs = ["App.java"],
    data = ["//hidden"],
    main_class = "app.App",
    deps = ["//lib"],
)

# IDE-visible target that holds the hidden target in a custom attribute.
wrapper(
    name = "app_with_custom_attr",
    inner = "//hidden",
)
