package com.letstwinkle.sumbrella

import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.model.SumGameEffort
import com.letstwinkle.sumbrella.screens.game.SumGameEngine
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
         Array(game.cells.size) { CharArray(game.cells[0].size) },
         game.plots
      )
      effort.coloration[0][1] = 1.char

      val engine = SumGameEngine(game, effort)

      engine.restorePlotSums()
      assertContentEquals(intArrayOf(3, 0), effort.plotSums, "CELLS_1 game, 1 cell colored")

      effort.coloration[0][0] = 2.char
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
         Array(game.cells.size) { CharArray(game.cells[0].size) },
         game.plots
      )
      effort.coloration[0][1] = 2.char
      effort.coloration[0][2] = 2.char
      effort.coloration[0][0] = 2.char

      val engine = SumGameEngine(game, effort)

      engine.restorePlotSums()
      assertContentEquals(intArrayOf(0, 6, 0), effort.plotSums, "CELLS_2 game, 1 cell colored")

      effort.coloration[1][1] = 3.char
      effort.coloration[1][2] = 3.char
      engine.restorePlotSums()
      assertContentEquals(intArrayOf(0, 6, 3), effort.plotSums, "CELLS_2 game, 2 cell colored")

      effort.coloration[2][0] = 1.char
      engine.restorePlotSums()
      assertContentEquals(intArrayOf(2, 6, 3), effort.plotSums, "CELLS_2 game, 3 cell colored")
   }

   @Test fun testAssignCell() {
      val game = SumGame(
         "",
         cells = CELLS_1,
         sum = 3,
         plots = 2
      )
      val effort = SumGameEffort(
         game.id,
         Array(game.cells.size) { CharArray(game.cells[0].size) },
         game.plots
      )
      effort.coloration[0][0] = 2.char
      val engine = SumGameEngine(game, effort)
      engine.restorePlotSums()

      engine.assignCell(0, 1, 1.char)
      assertContentEquals(intArrayOf(3, 1), effort.plotSums, "CELLS_1 game, assign (0, 1)")
      assertFalse(effort.solved, "CELLS_1 game, assign (0, 1) not solved")

      engine.assignCell(0, 1, 2.char)
      assertContentEquals(intArrayOf(0, 4), effort.plotSums, "CELLS_1 game, change assign (0, 1)")
      assertFalse(effort.solved, "CELLS_1 game, change assign (0, 1) not solved")

      engine.assignCell(0, 1, 1.char)
      engine.assignCell(1, 0, 2.char)
      engine.assignCell(1, 1, 2.char)
      assertTrue(effort.solved, "CELLS_1 game, solved")
   }

   companion object {
      val CELLS_1 = arrayOf(charArrayOf(1.char, 3.char), charArrayOf(1.char, 1.char))
      val CELLS_2 = arrayOf(charArrayOf(1.char, 2.char, 3.char), charArrayOf(3.char, 2.char, 1.char), charArrayOf(2.char, 3.char, 1.char))
   }
}
