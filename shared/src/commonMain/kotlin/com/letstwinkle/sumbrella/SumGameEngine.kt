package com.letstwinkle.sumbrella.screens.game

import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.model.SumGameEffort

private val CHAR_ZERO = 0.toChar()

class SumGameEngine(val game: SumGame, val effort: SumGameEffort) {
   // restore the plotSums in the effort
   fun restorePlotSums() {
      effort.plotSums.fill(0)
      effort.coloration.forEachIndexed { row, rowColoration ->
         rowColoration.forEachIndexed { col, cellColoration ->
            if (cellColoration != CHAR_ZERO) {
               effort.plotSums[cellColoration.toInt() - 1] += game.cells[row][col].toInt()
            }
         }
      }
   }

   fun assignCell(row: Int, col: Int, plot: Char) {
      val cellValue = game.cells[row][col].toInt()
      effort.coloration[row][col].let { currentPlot ->
         if (currentPlot != CHAR_ZERO) {
            effort.plotSums[currentPlot.toInt() - 1] -= cellValue
         }
      }
      effort.coloration[row][col] = plot
      effort.plotSums[plot.toInt() - 1] += cellValue
      effort.solved = effort.plotSums.all { it == game.sum }
   }
}
