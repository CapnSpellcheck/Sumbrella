package com.letstwinkle.sumbrella.model

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class SumGame(
   val id: String,
   val cells: Array<CharArray>,
   val sum: Int,
   val plots: Short
) {
   override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as SumGame

      if (sum != other.sum) return false
      if (plots != other.plots) return false
      if (id != other.id) return false
      if (!cells.contentDeepEquals(other.cells)) return false

      return true
   }

   override fun hashCode(): Int {
      var result: Int = sum + 0
      result = 31 * result + plots
      result = 31 * result + id.hashCode()
      result = 31 * result + cells.contentDeepHashCode()
      return result
   }
}

// to be Entity
class SumGameEffort(
   val gameId: String,
   var elapsedTime: Duration,
   val coloration: Array<CharArray>,
   val plotSums: IntArray,
   var solved: Boolean,
) {
   constructor(gameId: String, coloration: Array<CharArray>, numberOfPlots: Short)
       : this(gameId, Duration.ZERO, coloration, IntArray(numberOfPlots + 0), false)
}

