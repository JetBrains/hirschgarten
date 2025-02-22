package org.jetbrains.bazel.commons

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Duration
import java.util.Locale
import kotlin.math.roundToInt

object Format {
  fun duration(duration: Duration): String {
    if (duration.toSeconds() == 0L) {
      return duration.toMillis().toString() + "ms"
    }
    if (duration.toMinutes() == 0L) {
      val df = DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.US))
      return df.format((duration.toMillis().toDouble()) / 1000) + "s"
    }
    val minutes = duration.toMinutes().toString() + "m"

    if (duration.toSecondsPart() > 0 || duration.toMillisPart() > 0) {
      val millisAsSeconds = (duration.toMillisPart().toDouble()) / 1000
      val seconds = millisAsSeconds + duration.toSecondsPart()
      return minutes + " " + (seconds.roundToInt().toString() + "s")
    }

    return minutes
  }
}
