# Bazel Transitive Proto Dependencies Example

Minimal reproducible example demonstrating transitive proto dependencies behavior in Bazel with Scala.

## Problem Description

This project demonstrates the dependency chain:

```
scala_binary → scala_proto_library → proto_library → proto_library (transitive)
```

When using types from transitive proto dependencies, they appear as unresolved in IntelliJ IDEA despite successful Bazel builds.

## Project Structure

```
app/
  Main.scala                 # uses ServiceRequest, Environment, and Metadata
proto/
  requests/
    requests.proto           # depends on common.proto, defines ServiceRequest
  common/
    common.proto             # defines Environment enum and Metadata message
```

## Dependency Chain

```
scala_binary (//app:app)
  → scala_proto_library (//proto/requests/scala:requests_scala_proto)
    → proto_library (//proto/requests:requests_proto)
      → proto_library (//proto/common:common_proto)  ← transitive dependency
```

## The IDE Issue

In `app/Main.scala`, there are two imports:

```scala
import example.service.ServiceRequest
import example.common.{Environment, Metadata}
```

**The Problem:**
- `import example.service.ServiceRequest` resolves correctly in IntelliJ IDEA
- `import example.common.{Environment, Metadata}` does NOT resolve in IntelliJ IDEA

Both types come through the same dependency chain, but IDE fails to resolve the transitive proto types from `//proto/common:common_proto`, even though:
- Bazel builds successfully
- The types are available at classpath
- `ServiceRequest` from the direct proto dependency resolves fine
