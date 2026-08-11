@file:OptIn(ExperimentalTime::class)

package com.letstwinkle.sumbrella

import androidx.compose.ui.graphics.Color
import com.letstwinkle.TestScheduleSyncingClock
import com.letstwinkle.sumbrella.engine.IGameEngine
import com.letstwinkle.sumbrella.engine.IGameEngineFactory
import com.letstwinkle.sumbrella.engine.UndoCommand
import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.model.SumGameEffort
import com.letstwinkle.sumbrella.screens.game.SumGameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

class TestGameEngineFactory : IGameEngineFactory {
   var lastEngine: TestGameEngine? = null

   override fun createGameEngine(game: SumGame, effort: SumGameEffort): TestGameEngine {
      lastEngine = TestGameEngine(game, effort)
      return lastEngine!!
   }

   class TestGameEngine(val game: SumGame, val effort: SumGameEffort) : IGameEngine {
      val assignCellCalls: List<Triple<Int, Int, Byte>> = mutableListOf()
      var eraseCalls: Int = 0; private set
      var undoCalls: Int = 0; private set

      override fun assignCell(row: Int, col: Int, plot: Byte): UndoCommand {
         (assignCellCalls as MutableList).add(Triple(row, col, plot))
         return FAKE_UNDO_COMMAND
      }

      override fun erase(): UndoCommand {
         eraseCalls += 1
         return FAKE_UNDO_COMMAND
      }

      override fun performUndo(command: UndoCommand) {
         assertEquals(command, FAKE_UNDO_COMMAND)
         undoCalls += 1
      }

      override fun restorePlotSums() {}

      override val plotErrorsObservable = List(game.plots + 1) { MutableStateFlow(false) }
      override val plotSumsObservable = List(game.plots + 1) { MutableStateFlow(0) }
      override val solvedObservable = MutableStateFlow(false)

      companion object {
         val FAKE_UNDO_COMMAND = UndoCommand.AssignCell(0, 0, 0)
      }
   }

}

class SumGameViewModelTest {
   val game_2x2 = SumGame(
      "",
      arrayOf(byteArrayOf(1, 3), byteArrayOf(1, 1)),
      3,
      2
   )
   val game_3x3 = SumGame(
      "",
      arrayOf(byteArrayOf(1, 2, 3), byteArrayOf(3, 2, 1), byteArrayOf(2, 3, 1)),
      6,
      3
   )
   val testDispatcher = StandardTestDispatcher()
   lateinit var gameEngineFactory: TestGameEngineFactory

   @BeforeTest fun before() {
      Dispatchers.setMain(testDispatcher)
      gameEngineFactory = TestGameEngineFactory()
   }

   @AfterTest fun after(){
      Dispatchers.resetMain()
   }

   fun makeSUT(paramOverrides: Map<String, Any> = mapOf()): SumGameViewModel =
      SumGameViewModel(
         game = paramOverrides["game"] as SumGame? ?: game_2x2,
         plotColorProvider = paramOverrides["plotColorProvider"] as IPlotColorProvider? ?: TestPlotColorProvider(),
         backgroundDispatcher = testDispatcher,
         gameEngineFactory = gameEngineFactory,
         clock = paramOverrides["clock"] as Clock? ?: Clock.System,
      )

   @Test fun testTapCell() = runTest {
      val sut = makeSUT()

      sut.tapCell(0, 0)
      advanceUntilIdle()
      val gameEngine = gameEngineFactory.lastEngine!!
      assertEquals(Triple(0, 0, 1.toByte()), gameEngine.assignCellCalls.first())
      assertEquals(1, gameEngine.assignCellCalls.size)

      // test removing cell color : set coloration
      gameEngine.effort.coloration[0][0] = 1
      sut.tapCell(0, 0)
      advanceUntilIdle()
      assertEquals(Triple(0, 0, 0.toByte()), gameEngine.assignCellCalls.last())
      assertEquals(2, gameEngine.assignCellCalls.size)
   }

   @Test fun testEyedropCel() {

   }

   @Test fun testActivatePlot() = runTest {
      val sut = makeSUT()
      sut.tapCell(0, 0)
      advanceUntilIdle()
      sut.activatePlot(2)
      sut.tapCell(0, 0)
      advanceUntilIdle()

      val gameEngine = gameEngineFactory.lastEngine!!
      assertContentEquals(
         listOf(Triple(0, 0, 1.toByte()), Triple(0, 0, 2.toByte())),
         gameEngine.assignCellCalls
      )
   }

   @Test fun testStopwatch() = runTest {
      val sut = makeSUT(mapOf("clock" to TestScheduleSyncingClock(testDispatcher.scheduler)))
      val twoSeconds = 2000.toDuration(DurationUnit.MILLISECONDS)

      sut.resumeStopwatch()
      advanceTimeBy(1001)
      try {
         assertEquals(1000.toDuration(DurationUnit.MILLISECONDS), sut.elapsedTimeObservable.value)
      } catch (e: AssertionError) {
         sut.pauseStopwatch()
         throw e
      }

      advanceTimeBy(1000)
      try {
         assertEquals(twoSeconds, sut.elapsedTimeObservable.value)
      } catch (e: AssertionError) {
         sut.pauseStopwatch()
         throw e
      }

      sut.pauseStopwatch()
      advanceTimeBy(2000)
      assertEquals(twoSeconds, sut.elapsedTimeObservable.value)
      val gameEngine = gameEngineFactory.lastEngine!!
      assertEquals(twoSeconds + 1.milliseconds, gameEngine.effort.elapsedTime)

   }

   @Test fun testPlotTallies() = runTest {
      val sut = makeSUT(mapOf("game" to game_3x3))

      assertContentEquals(listOf("6", "6", "6"), sut.plotTalliesObservable.drop(1).map { it.value })

      gameEngineFactory.lastEngine!!.plotSumsObservable[1].value = 4
      gameEngineFactory.lastEngine!!.plotSumsObservable[2].value = 6
      advanceUntilIdle()
      assertContentEquals(listOf("2", "0", "6"), sut.plotTalliesObservable.drop(1).map { it.value })
   }

   @Test fun testPlotTallyColors() = runTest {
      val sut = makeSUT(mapOf("game" to game_3x3))

      assertContentEquals(listOf(Color.Black, Color.Black, Color.Black), sut.plotTallyColorsObservable.drop(1).map { it.value })

      gameEngineFactory.lastEngine!!.plotSumsObservable[1].value = 8
      gameEngineFactory.lastEngine!!.plotSumsObservable[2].value = 6
      advanceUntilIdle()
      assertContentEquals(listOf(Color.Red, Color.Green, Color.Black), sut.plotTallyColorsObservable.drop(1).map { it.value })
   }

   @Test fun testCellColors() = runTest {
      val sut = makeSUT()
      val color_0_0_0 = Color(0, 0, 0)
      val color_10_0_0 = Color(10, 0, 0)
      val color_20_0_0 = Color(20, 0, 0)

      assertContentEquals(
         listOf(listOf(color_0_0_0, color_0_0_0), listOf(color_0_0_0, color_0_0_0)),
         sut.cellColorsObservable.map { it.map { it.value } }
      )

      sut.tapCell(0, 0)
      assertContentEquals(
         listOf(listOf(color_10_0_0, color_0_0_0), listOf(color_0_0_0, color_0_0_0)),
         sut.cellColorsObservable.map { it.map { it.value } }
      )

      sut.activatePlot(2)
      sut.tapCell(1, 1)
      assertContentEquals(
         listOf(listOf(color_10_0_0, color_0_0_0), listOf(color_0_0_0, color_20_0_0)),
         sut.cellColorsObservable.map { it.map { it.value } }
      )
   }

   @Test fun testErase() {
      val sut = makeSUT()
      assertEquals(0, gameEngineFactory.lastEngine!!.eraseCalls)

      sut.erase()
      assertEquals(1, gameEngineFactory.lastEngine!!.eraseCalls)
   }

   @Test fun testUndo() = runTest {
      val sut = makeSUT()

      val gameEngine = gameEngineFactory.lastEngine!!
      assertEquals(0, gameEngine.undoCalls)
      assertEquals(false, sut.isUndoEnabledObservable.value)

      sut.tapCell(0, 1)
      advanceUntilIdle()
      assertEquals(true, sut.isUndoEnabledObservable.value)

      sut.undo()
      advanceUntilIdle()
      assertEquals(1, gameEngine.undoCalls)
      assertEquals(false, sut.isUndoEnabledObservable.value)
   }
}
