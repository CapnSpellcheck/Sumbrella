package com.letstwinkle.sumbrella.engine

import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.model.SumGameEffort
import kotlinx.coroutines.flow.MutableStateFlow

class SumGameEngine(val game: SumGame, effort: SumGameEffort) : IGameEngine {
   override val plotErrorsObservable = List(game.plots + 1) { i ->
      if (i == 0) MutableStateFlow(false) else MutableStateFlow(effort.plotInError[i - 1])
   }
   override val solvedObservable = MutableStateFlow(effort.solved)
   override val plotSumsObservable = List(game.plots + 1) { i ->
      if (i == 0) MutableStateFlow(0) else MutableStateFlow(effort.plotSums[i - 1])
   }

   var effort: SumGameEffort = effort; private set

   private val lastColumn = game.cells[0].lastIndex
   private val lastRow = game.cells.lastIndex

   // restore the plotSums in the effort
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

   override fun assignCell(row: Int, col: Int, plot: Byte): UndoCommand {
      val cellValue = game.cells[row][col]
      val oldPlot = effort.coloration[row][col]
      val undoAssign = UndoCommand.AssignCell(row, col, oldPlot)

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
      return undoAssign
   }

   override fun erase(): UndoCommand {
      val undo = UndoCommand.AssignAll(effort)
      for (row in effort.coloration) {
         row.fill(NULL_PLOT)
      }
      effort.plotSums.fill(0)
      effort.plotInError.fill(false)
      return undo
   }

   override fun performUndo(command: UndoCommand) {
      when (command) {
         is UndoCommand.AssignCell -> assignCell(command.row, command.col, command.plot)
         is UndoCommand.AssignAll -> {
            command.coloration.copyInto(effort.coloration)
            for (plot in 1 .. game.plots) {
               setSum(plot.toByte(), command.plotSums[plot - 1])
               setPlotError(plot.toByte(), effort.plotInError[plot - 1])
            }
         }
      }
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

      setPlotError(plot, error)
   }

   private fun updateSolved() {
      effort.solved = effort.plotSums.all { it == game.sum } && !effort.plotInError.contains(true)
      solvedObservable.value = effort.solved
   }

   private fun setPlotError(plot: Byte, error: Boolean) {
      effort.plotInError[plot - 1] = error
      plotErrorsObservable[plot + 0].value = error
   }

   private fun setSum(plot: Byte, sum: Int) {
      effort.plotSums[plot - 1] = sum
      println("setSum($plot, $sum)")
      plotSumsObservable[plot + 0].value = sum
   }

   companion object {
      const val NULL_PLOT: Byte = 0
   }
}
