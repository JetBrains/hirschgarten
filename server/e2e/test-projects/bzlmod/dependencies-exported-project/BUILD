load("@rules_java//java:java_library.bzl", "java_library")

java_library(
   name = "A",
   srcs = ["A.java"],
   deps = [":B", ":C"],
   exports = [":B", ":D", ":E"],
)

java_library(
   name = "B",
   srcs = ["B.java"],
)

java_library(
   name = "C",
   srcs = ["C.java"],
)

java_library(
   name = "D",
   srcs = ["D.java"],
)
java_library(
   name = "E",
   srcs = ["E.java"],
)
