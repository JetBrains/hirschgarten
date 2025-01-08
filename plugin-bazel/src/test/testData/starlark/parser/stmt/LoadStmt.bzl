load("module.bzl", "a", "b", "c")
load("module.bzl", "a", bb = "b", "c")
load("module.bzl", "a", 5, "c")