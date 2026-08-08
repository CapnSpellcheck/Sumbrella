package com.letstwinkle.sumbrella.engine

import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.model.SumGameEffort
import kotlinx.coroutines.flow.MutableStateFlow

class SumGameEngine(val game: SumGame, val effort: SumGameEffort) : IGameEngine {
   override val plotErrorsObservable = List(game.plots + 1) { i ->
      if (i == 0) MutableStateFlow(false) else MutableStateFlow(effort.plotInError[i - 1])
   }
   override val solvedObservable = MutableStateFlow(effort.solved)
   override val plotSumsObservable = List(game.plots + 1) { i ->
      if (i == 0) MutableStateFlow(0) else MutableStateFlow(effort.plotSums[i - 1])
   }

   private val lastColumn = game.cells[0].lastIndex
   private val lastRow = game.cells.lastIndex

   // restore the plotSums in the effort
   // TODO: don't need it, might as well persist plot sums
   override fun restorePlotSums() {
      effort.plotSums.fill(0)
      effort.coloration.forEachIndexed { row, rowColoration ->
         rowColoration.forEachIndexed { col, cellColoration ->
            if (cellColoration != NULL_PLOT) {
               effort.plotSums[cellColoration - 1] += game.cells[row][col]
            }
         }
      }
   }

   override fun assignCell(row: Int, col: Int, plot: Byte) {
      val cellValue = game.cells[row][col]
      val oldPlot = effort.coloration[row][col]
      if (oldPlot != NULL_PLOT) {
         setSum(oldPlot, effort.plotSums[oldPlot - 1] - cellValue)
      }

      effort.coloration[row][col] = plot

      if (plot != NULL_PLOT) {
         setSum(plot, effort.plotSums[plot - 1] + cellValue)
      }

      // is this plot in error?
      checkError(oldPlot)
      checkError(plot)
      updateSolved()
   }

   private fun checkError(plot: Byte) {
      if (plot == NULL_PLOT) return
      var error = false
      val plotSum = effort.plotSums[plot - 1]

      scan@ for (row in 0 .. lastRow) {
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
                  error = true
                  break@scan
               }
            }
         }
      }

      effort.plotInError[plot - 1] = error
      plotErrorsObservable[plot + 0].value = error
   }

   private fun updateSolved() {
      effort.solved = effort.plotSums.all { it == game.sum } && !effort.plotInError.contains(true)
      solvedObservable.value = effort.solved
   }

   private fun setSum(plot: Byte, sum: Int) {
      effort.plotSums[plot - 1] = sum
      print("setSum($plot, $sum)")
      plotSumsObservable[plot + 0].value = sum
   }

   companion object {
      const val NULL_PLOT: Byte = 0
   }
}
