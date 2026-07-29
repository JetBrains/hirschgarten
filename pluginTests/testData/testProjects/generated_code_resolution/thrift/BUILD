load("@rules_scala//scala:scala.bzl", "scala_library")
load("@rules_scala//thrift:thrift.bzl", "thrift_library")
load("@rules_scala//twitter_scrooge:twitter_scrooge.bzl", "scrooge_scala_library")

thrift_library(
    name = "schema",
    srcs = ["schema.thrift"],
)

scrooge_scala_library(
    name = "thriftscala",
    visibility = ["//visibility:public"],
    deps = [":schema"],
)

scala_library(
    name = "consumer",
    srcs = ["ThriftConsumer.scala"],
    deps = [":thriftscala"],
)
