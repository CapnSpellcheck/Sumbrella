package com.letstwinkle.sumbrella.engine

import kotlinx.coroutines.flow.MutableStateFlow

interface IGameEngine {
   fun assignCell(row: Int, col: Int, plot: Byte): UndoCommand
   fun erase(): UndoCommand
   fun performUndo(command: UndoCommand)

   fun restorePlotSums()

   val plotErrorsObservable: List<MutableStateFlow<Boolean>>
   val plotSumsObservable: List<MutableStateFlow<Int>>
   val solvedObservable: MutableStateFlow<Boolean>
}
