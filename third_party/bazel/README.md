These files were copied from https://github.com/bazelbuild/bazel,
the same way as the Google plugin did https://github.com/bazelbuild/intellij/tree/master/third_party/bazel.

Why copy instead of introducing a source dependency on Bazel?
1. It's impossible to depend on Bazel as a bzlmod module currently
2. That would be a big overhead

Why not use some kind of precompiled libraries for the protos?
1. That's exactly what we were doing before copying the files!
2. It caused problems with bringing in protobuf as a transitive dependency
3. No official libraries exist
