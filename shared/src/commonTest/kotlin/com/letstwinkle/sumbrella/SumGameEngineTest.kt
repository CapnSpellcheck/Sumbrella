package com.letstwinkle.sumbrella

import com.letstwinkle.sumbrella.engine.SumGameEngine
import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.model.SumGameEffort
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
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

      val engine = SumGameEngine(game, effort)

      engine.restorePlotSums()
      assertContentEquals(intArrayOf(3, 0), effort.plotSums, "CELLS_1 game, 1 cell colored")

      effort.coloration[0][0] = 2
      engine.restorePlotSums()
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

      val engine = SumGameEngine(game, effort)

      engine.restorePlotSums()
      assertContentEquals(intArrayOf(0, 6, 0), effort.plotSums, "CELLS_2 game, 1 cell colored")

      effort.coloration[1][1] = 3
      effort.coloration[1][2] = 3
      engine.restorePlotSums()
      assertContentEquals(intArrayOf(0, 6, 3), effort.plotSums, "CELLS_2 game, 2 cell colored")

      effort.coloration[2][0] = 1
      engine.restorePlotSums()
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
      val engine = SumGameEngine(game, effort)
      engine.restorePlotSums()

      engine.assignCell(0, 1, 1)
      assertContentEquals(intArrayOf(3, 1), effort.plotSums, "CELLS_1 game, assign (0, 1)")
      assertFalse(effort.solved, "CELLS_1 game, assign (0, 1) not solved")

      engine.assignCell(0, 1, 2)
      assertContentEquals(intArrayOf(0, 4), effort.plotSums, "CELLS_1 game, change assign (0, 1)")
      assertFalse(effort.solved, "CELLS_1 game, change assign (0, 1) not solved")

      engine.assignCell(0, 1, 1)
      engine.assignCell(1, 0, 2)
      engine.assignCell(1, 1, 2)
      assertTrue(effort.solved, "CELLS_1 game, solved")
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
      val engine = SumGameEngine(game, effort)

      engine.assignCell(0, 0, 1)
      assertFalse(effort.plotInError.contains(true), "CELLS_1 game, assign (0, 0) no plot in error")
      engine.assignCell(1, 1, 2)
      assertFalse(effort.plotInError.contains(true), "CELLS_1 game, assign (1, 1) no plot in error")
      engine.assignCell(1, 1, 1)
      assertTrue(effort.plotInError[0], "CELLS_1 game, assign (1, 1) plot 1 in error")
      assertFalse(effort.plotInError[1], "CELLS_1 game, assign (1, 1) plot 2 not in error")
      engine.assignCell(1, 0, 1)
      assertFalse(effort.plotInError.contains(true), "CELLS_1 game, assign (1, 0) no plot in error")
      engine.assignCell(0, 1, 2)
      assertFalse(effort.plotInError.contains(true), "CELLS_1 game, assign (0, 1) no plot in error")
      engine.assignCell(0, 0, 2)
      assertFalse(effort.plotInError.contains(true), "CELLS_1 game, assign (0, 0) no plot in error")
   }

   companion object {
      val CELLS_1 = arrayOf(byteArrayOf(1, 3), byteArrayOf(1, 1))
      val CELLS_2 = arrayOf(byteArrayOf(1, 2, 3), byteArrayOf(3, 2, 1), byteArrayOf(2, 3, 1))
   }
}
