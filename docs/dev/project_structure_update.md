# Project Structure Update

During sync, we obviously need to update the project structure, which usually is not a big deal. But in our case we need
to support multiple languages (JVM, Python, Go, etc.) across multiple JetBrains products (IntelliJ IDEA, CLion, etc.)
what (as you may imagine) can create multiple challenges.

## Challenges:

1. **All language plugins have to be optional,** so the plugin should be able to work completely fine if
   the user does not install supported plugins. On top of that (like mentioned before), we want to support multiple
   JetBrains products, and some of them don't support all the languages we support.
   For example, PyCharm doesn't support Java at all, so it's not possible to release the plugin for it if the
   dependency to Java Plugin is not optional.
2. **Multiple Project Structures need to be supported.** Since we want to support multiple JetBrains products, and since
   they use different Project Structures under the hood (`WorkspaceModel` for IntelliJ IDEA, something else for CLion)
   so we need to be able to update any of the PS.
3. **Language support (extensions) should be able to call server for additional data.** BSP allows clients to query
   server for more language-specific info, which might be necessary to make it work, for example:
   [`buildTarget/javacOptions`](https://build-server-protocol.github.io/docs/extensions/java#buildtargetjavacoptions-request).
4. **There has to be a way to override "default" sync flow**. In some cases (well in our case) it's possible that
   default flow needs to be overridden. In bazel-bsp we have a separate endpoint for libraries to collect
   all the libraries at once instead of combining results of multiple endpoints (there are a few more such things).
   It is our own mechanism, and it's not compatible with official BSP specification, so we need to keep both flows
   available because we want to support other build tools as well (like SBT).

## Solution proposition:

**Project Sync Hooks!** So the idea is to split the Project Structure update part into 3 stages:

1. **Base Project Sync** which is responsible for querying the server for very basic infos which will be necessary
   almost always, that is: query for all the targets using [`workspace/buildTargets`](https://build-server-protocol.github.io/docs/specification#workspacebuildtargets-request),
   sources using [`buildTarget/sources`](https://build-server-protocol.github.io/docs/specification#buildtargetsources-request)
   and resources using [`buildTarget/resources`](https://build-server-protocol.github.io/docs/specification#buildtargetresources-request).
   The results of these queries will be passed to all the other hooks, so they can use them to make further calls
   (targets ids) or just put them as sources and resources.
2. **Default / BSP Project Syncs** which is responsible for the "official" flow, so they can be used also with 
   other build tools (like SBT). Some language support is simple enough and doesn't require any special custom 
   bazel-specific endpoints so they can be handled here (like Python).
3. **Additional Project Syncs** for handling all the other things, like build-tool-specific flows (we have a lot of that).

On top of that, there is an extension point that allows disabling Default Syncs, which is useful in cases when
build-tool-specific sync covers also a default flow. E.g., our custom endpoint `workspace/directories` covers 
[`buildTarget/outputPaths`](https://build-server-protocol.github.io/docs/specification#buildtargetoutputpaths-request) 
and more. Also, our java support uses `workspace/libraries` instead of [`buildTarget/dependencySources`](https://build-server-protocol.github.io/docs/specification#buildtargetdependencysources-request)
what would break the default flow as well.
