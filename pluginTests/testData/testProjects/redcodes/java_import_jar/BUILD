load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_java//java:java_import.bzl", "java_import")

java_import(
  name = "jar_target",
  jars = ["imported.jar"],
)

java_library(
  name = "jar_usage",
  srcs = ["JarUsage.java"],
  deps = [
    ":jar_target"
  ],
)
