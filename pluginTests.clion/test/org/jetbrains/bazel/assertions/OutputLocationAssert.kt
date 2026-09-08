package org.jetbrains.bazel.assertions

import org.assertj.core.api.AbstractIterableAssert
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Condition
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection

internal fun workspace(relativePath: String): Condition<OutputLocation> = condition("workspace '$relativePath'") {
  it == OutputLocation.Workspace(relativePath)
}

internal fun external(repoName: String, relativePath: String): Condition<OutputLocation> = condition("external '$repoName/$relativePath'") {
  it == OutputLocation.External(repoName, relativePath)
}

internal fun host(absolutePath: String): Condition<OutputLocation> = condition("host '$absolutePath'") {
  it == OutputLocation.Host(absolutePath)
}

internal fun bazelBin(relativePath: String): Condition<OutputLocation> = condition("bazel-bin '$relativePath'") {
  it is OutputLocation.Output && it.root.segments.lastOrNull() == "bin" && it.relativePath == relativePath
}

internal fun assertThat(actual: OutputLocation?): OutputLocationAssert = OutputLocationAssert(actual)

internal class OutputLocationAssert(actual: OutputLocation?) :
  AbstractObjectAssert<OutputLocationAssert, OutputLocation>(actual, OutputLocationAssert::class.java) {

  fun isWorkspace(relativePath: String): OutputLocationAssert = has(workspace(relativePath))

  fun isExternal(repoName: String, relativePath: String): OutputLocationAssert = has(external(repoName, relativePath))

  fun isHost(absolutePath: String): OutputLocationAssert = has(host(absolutePath))

  fun isBazelBin(relativePath: String): OutputLocationAssert = has(bazelBin(relativePath))
}

internal fun assertThat(actual: OutputLocationCollection?): OutputLocationsAssert =
  OutputLocationsAssert(actual?.getOutputLocations()?.toList())

internal class OutputLocationsAssert(actual: List<OutputLocation>?) :
  AbstractIterableAssert<OutputLocationsAssert, List<OutputLocation>, OutputLocation, OutputLocationAssert>(
    actual, OutputLocationsAssert::class.java,
  ) {

  override fun toAssert(value: OutputLocation?, description: String?): OutputLocationAssert =
    OutputLocationAssert(value).describedAs(description)

  override fun newAbstractIterableAssert(iterable: MutableIterable<OutputLocation>): OutputLocationsAssert =
    OutputLocationsAssert(iterable.toList())
}
