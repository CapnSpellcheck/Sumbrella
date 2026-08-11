package com.letstwinkle.sumbrella

import com.letstwinkle.sumbrella.engine.SumGameEngine
import com.letstwinkle.sumbrella.engine.SumGameEngine.Companion.NULL_PLOT
import com.letstwinkle.sumbrella.engine.UndoCommand
import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.model.SumGameEffort
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SumGameEngineTest {
   @Test fun testRestorePlotSums1() {
      val game = SumGame(
         "",
         cells = CELLS_1,
         sum = 3,
         plots = 2
      )
      val effort = SumGameEffort(
         game.id,
         Array(game.cells.size) { ByteArray(game.cells[0].size) },
         game.plots
      )
      effort.coloration[0][1] = 1

      val sut = SumGameEngine(game, effort)

      sut.restorePlotSums()
      assertContentEquals(intArrayOf(3, 0), effort.plotSums, "CELLS_1 game, 1 cell colored")

      effort.coloration[0][0] = 2
      sut.restorePlotSums()
      assertContentEquals(intArrayOf(3, 1), effort.plotSums, "CELLS_1 game, 2 cell colored")
   }

   @Test fun testRestorePlotSums2() {
      val game = SumGame(
         "",
         cells = CELLS_2,
         sum = 6,
         plots = 3
      )
      val effort = SumGameEffort(
         game.id,
         Array(game.cells.size) { ByteArray(game.cells[0].size) },
         game.plots
      )
      effort.coloration[0][1] = 2
      effort.coloration[0][2] = 2
      effort.coloration[0][0] = 2

      val sut = SumGameEngine(game, effort)

      sut.restorePlotSums()
      assertContentEquals(intArrayOf(0, 6, 0), effort.plotSums, "CELLS_2 game, 1 cell colored")

      effort.coloration[1][1] = 3
      effort.coloration[1][2] = 3
      sut.restorePlotSums()
      assertContentEquals(intArrayOf(0, 6, 3), effort.plotSums, "CELLS_2 game, 2 cell colored")

      effort.coloration[2][0] = 1
      sut.restorePlotSums()
      assertContentEquals(intArrayOf(2, 6, 3), effort.plotSums, "CELLS_2 game, 3 cell colored")
   }

   @Test fun testUpdateSolved() {
      val game = SumGame(
         "",
         cells = CELLS_1,
         sum = 3,
         plots = 2
      )
      val effort = SumGameEffort(
         game.id,
         Array(game.cells.size) { ByteArray(game.cells[0].size) },
         game.plots
      )
      effort.coloration[0][0] = 2
      val sut = SumGameEngine(game, effort)
      sut.restorePlotSums()

      sut.assignCell(0, 1, 1)
      assertContentEquals(intArrayOf(3, 1), effort.plotSums, "CELLS_1 game, assign (0, 1)")
      assertFalse(effort.solved, "CELLS_1 game, assign (0, 1) not solved")
      assertFalse(sut.solvedObservable.value)

      sut.assignCell(0, 1, 2)
      assertContentEquals(intArrayOf(0, 4), effort.plotSums, "CELLS_1 game, change assign (0, 1)")
      assertFalse(effort.solved, "CELLS_1 game, change assign (0, 1) not solved")
      assertFalse(sut.solvedObservable.value)

      sut.assignCell(0, 1, 1)
      sut.assignCell(1, 0, 2)
      sut.assignCell(1, 1, 2)
      assertTrue(effort.solved, "CELLS_1 game, solved")
      assertTrue(sut.solvedObservable.value)
   }

   @Test fun testAssignCell() {
      val game = SumGame(
         "",
         cells = CELLS_2,
         sum = 6,
         plots = 3
      )
      val effort = SumGameEffort(
         game.id,
         Array(game.cells.size) { ByteArray(game.cells[0].size) },
         game.plots
      )
      val sut = SumGameEngine(game, effort)
      var command: UndoCommand

      command = sut.assignCell(0, 0, 1)
      assertEquals(UndoCommand.AssignCell(0, 0, 0), command)
      command = sut.assignCell(0, 1, 2)
      assertEquals(UndoCommand.AssignCell(0, 1, 0), command)
      command = sut.assignCell(0, 2, 3)
      assertEquals(UndoCommand.AssignCell(0, 2, 0), command)

      assertEquals(1, effort.plotSums[0])
      assertEquals(1, sut.plotSumsObservable[1].value)
      assertEquals(2, effort.plotSums[1])
      assertEquals(2, sut.plotSumsObservable[2].value)
      assertEquals(3, effort.plotSums[2])
      assertEquals(3, sut.plotSumsObservable[3].value)

      command = sut.assignCell(0, 0, 2)
      assertEquals(UndoCommand.AssignCell(0, 0, 1), command)

      assertEquals(0, effort.plotSums[0])
      assertEquals(0, sut.plotSumsObservable[1].value)
      assertEquals(3, effort.plotSums[1])
      assertEquals(3, sut.plotSumsObservable[2].value)

      command = sut.assignCell(0, 2, 0)

      assertEquals(UndoCommand.AssignCell(0, 2, 3), command)
      assertEquals(0, effort.plotSums[2])
      assertEquals(0, sut.plotSumsObservable[3].value)
   }
   
   @Test fun testErase() {
      val game = SumGame(
         "",
         cells = CELLS_1,
         sum = 3,
         plots = 2
      )
      val initialColoration = {
         Array(game.cells.size) { ByteArray(game.cells[0].size) { (it + 1).toByte() } }
      }
      val effort = SumGameEffort(
         game.id,
         initialColoration(),
         game.plots
      )
      val effortCopy = effort.copy()
      val sut = SumGameEngine(game, effort)

      val command = sut.erase()

      assertTrue(effort.coloration.contentDeepEquals(Array(game.cells.size) { ByteArray(game.cells[0].size) } ))
      assertIs<UndoCommand.AssignAll>(command)
      assertTrue(command.coloration.contentDeepEquals(initialColoration()))
      assertContentEquals(effortCopy.plotSums, command.plotSums)
      assertContentEquals(effortCopy.plotInError, command.plotInError)
   }
   
   @Test fun testPerformUndo() {
      val game = SumGame(
         "",
         cells = CELLS_1,
         sum = 3,
         plots = 2
      )
      val effort = SumGameEffort(
         game.id,
         Array(game.cells.size) { ByteArray(game.cells[0].size) },
         game.plots
      )
      val sut = SumGameEngine(game, effort)
      sut.assignCell(0, 0, 1)
      var command = sut.assignCell(1, 1, 2)

      sut.performUndo(command)
      assertEquals(NULL_PLOT, effort.coloration[1][1])

      sut.assignCell(0, 1, 2)
      command = sut.erase()

      sut.performUndo(command)
      assertTrue(effort.coloration.contentDeepEquals(arrayOf(byteArrayOf(1, 2), byteArrayOf(0, 0))))
   }

   @Test fun testCheckError() {
      val game = SumGame(
         "",
         cells = CELLS_1,
         sum = 3,
         plots = 2
      )
      val effort = SumGameEffort(
         game.id,
         Array(game.cells.size) { ByteArray(game.cells[0].size) },
         game.plots
      )
      val sut = SumGameEngine(game, effort)

      sut.assignCell(0, 0, 1)
      assertFalse(effort.plotInError.contains(true), "CELLS_1 game, assign (0, 0) no plot in error")
      assertFalse(sut.plotErrorsObservable.any { it.value })
      sut.assignCell(1, 1, 2)
      assertFalse(effort.plotInError.contains(true), "CELLS_1 game, assign (1, 1) no plot in error")
      assertFalse(sut.plotErrorsObservable.any { it.value })
      sut.assignCell(1, 1, 1)
      assertTrue(effort.plotInError[0], "CELLS_1 game, assign (1, 1) plot 1 in error")
      assertTrue(sut.plotErrorsObservable[1].value)
      assertFalse(effort.plotInError[1], "CELLS_1 game, assign (1, 1) plot 2 not in error")
      assertFalse(sut.plotErrorsObservable[2].value)
      sut.assignCell(1, 0, 1)
      assertFalse(effort.plotInError.contains(true), "CELLS_1 game, assign (1, 0) no plot in error")
      assertFalse(sut.plotErrorsObservable.any { it.value })
      sut.assignCell(0, 1, 2)
      assertFalse(effort.plotInError.contains(true), "CELLS_1 game, assign (0, 1) no plot in error")
      assertFalse(sut.plotErrorsObservable.any { it.value })
      sut.assignCell(0, 0, 2)
      assertFalse(effort.plotInError.contains(true), "CELLS_1 game, assign (0, 0) no plot in error")
      assertFalse(sut.plotErrorsObservable.any { it.value })
   }

   companion object {
      val CELLS_1 = arrayOf(byteArrayOf(1, 3), byteArrayOf(1, 1))
      val CELLS_2 = arrayOf(byteArrayOf(1, 2, 3), byteArrayOf(3, 2, 1), byteArrayOf(2, 3, 1))
   }
}
