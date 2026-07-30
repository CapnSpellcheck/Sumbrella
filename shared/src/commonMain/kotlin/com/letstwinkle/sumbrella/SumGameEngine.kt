package com.letstwinkle.sumbrella.screens.game

import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.model.SumGameEffort

private const val NULL_PLOT: Byte = 0

class SumGameEngine(val game: SumGame, val effort: SumGameEffort) {
   private val lastColumn = game.cells[0].lastIndex
   private val lastRow = game.cells.lastIndex

   // restore the plotSums in the effort
   // TODO: don't need it, might as well persist plot sums
   fun restorePlotSums() {
      effort.plotSums.fill(0)
      effort.coloration.forEachIndexed { row, rowColoration ->
         rowColoration.forEachIndexed { col, cellColoration ->
            if (cellColoration != NULL_PLOT) {
               effort.plotSums[cellColoration - 1] += game.cells[row][col]
            }
         }
      }
   }

   fun assignCell(row: Int, col: Int, plot: Byte) {
      val cellValue = game.cells[row][col]
      val oldPlot = effort.coloration[row][col]
      if (oldPlot != NULL_PLOT) {
         effort.plotSums[oldPlot - 1] -= cellValue
      }

      effort.coloration[row][col] = plot

      if (plot != NULL_PLOT) {
         effort.plotSums[plot - 1] += cellValue
      }

      // is this plot in error?
      checkError(oldPlot)
      checkError(plot)
      updateSolved()
   }

   private fun checkError(plot: Byte) {
      if (plot == NULL_PLOT) return
      val plotSum = effort.plotSums[plot - 1]
      for (row in 0 .. lastRow) {
         for (col in 0 .. lastColumn) {
            if (effort.coloration[row][col] == plot) {
               if (plotSum != game.cells[row][col] + 0 &&
                  !(
                      (row != 0 && effort.coloration[row - 1][col] == plot) ||
                      (row != lastRow && effort.coloration[row + 1][col] == plot) ||
                      (col != 0 && effort.coloration[row][col - 1] == plot) ||
                      (col != lastColumn && effort.coloration[row][col + 1] == plot)
                      )
                  )
               {
                  effort.plotInError[plot - 1] = true
                  return
               }
            }
         }
      }
      effort.plotInError[plot - 1] = false
   }

   private fun updateSolved() {
      effort.solved = effort.plotSums.all { it == game.sum } && !effort.plotInError.contains(true)
   }
}
