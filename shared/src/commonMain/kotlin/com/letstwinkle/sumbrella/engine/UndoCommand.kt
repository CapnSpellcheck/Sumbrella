package com.letstwinkle.sumbrella.engine

import com.letstwinkle.sumbrella.model.SumGameEffort

sealed class UndoCommand {
   data class AssignCell(val row: Int, val col: Int, val plot: Byte) : UndoCommand()
   class AssignAll(effort: SumGameEffort): UndoCommand(){
      val coloration = Array(effort.coloration.size) { i -> effort.coloration[i].copyOf() }
      val plotSums = effort.plotSums.copyOf()
      val plotInError = effort.plotInError.copyOf()
   }
}
