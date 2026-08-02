package com.letstwinkle.sumbrella

import kotlin.time.Duration

fun Duration.toHHMMSS(): String {
   return toComponents { hours, minutes, seconds, _ ->
      val mmSS = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
      if (hours > 0)
         "$hours:$mmSS"
      else
         mmSS
   }
}
