@file:OptIn(ExperimentalTime::class)

package com.letstwinkle.sumbrella

import com.letstwinkle.TestScheduleSyncingClock
import com.letstwinkle.sumbrella.engine.IGameEngine
import com.letstwinkle.sumbrella.engine.IGameEngineFactory
import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.model.SumGameEffort
import com.letstwinkle.sumbrella.screens.game.SumGameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*
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

      override fun assignCell(row: Int, col: Int, plot: Byte) {
         (assignCellCalls as MutableList).add(Triple(row, col, plot))
      }

      override fun restorePlotSums() {}

      override val plotErrorsObservable: List<MutableStateFlow<Boolean>>
         get() = TODO("Not yet implemented")
      override val solvedObservable: MutableStateFlow<Boolean>
         get() = TODO("Not yet implemented")
   }

}

class SumGameViewModelTest {
   val game_2x2 = SumGame(
      "",
      arrayOf(byteArrayOf(1, 3), byteArrayOf(1, 1)),
      3,
      2
   )
   val testDispatcher = StandardTestDispatcher()

   @BeforeTest fun before() {
      Dispatchers.setMain(testDispatcher)
   }

   @AfterTest fun after(){
      Dispatchers.resetMain()
   }

   @Test fun testTapCell() = runTest {
      val gameEngineFactory = TestGameEngineFactory()
      val sut = SumGameViewModel(
         game_2x2,
         gameEngineFactory = gameEngineFactory,
         backgroundDispatcher = testDispatcher
      )

      sut.tapCell(0, 0)
      advanceUntilIdle()
      assertContentEquals(listOf(Triple(0, 0, 1.toByte())), gameEngineFactory.lastEngine!!.assignCellCalls)

      // test removing cell color : set coloration
      gameEngineFactory.lastEngine!!.effort.coloration[0][0] = 1
      sut.tapCell(0, 0)
      advanceUntilIdle()
      assertEquals(Triple(0, 0, 0.toByte()), gameEngineFactory.lastEngine!!.assignCellCalls.last())
      assertEquals(2, gameEngineFactory.lastEngine!!.assignCellCalls.size)
   }

   @Test fun testActivatePlot() = runTest {
      val gameEngineFactory = TestGameEngineFactory()
      val sut = SumGameViewModel(
         game_2x2,
         gameEngineFactory = gameEngineFactory,
         backgroundDispatcher = testDispatcher
      )
      sut.tapCell(0, 0)
      advanceUntilIdle()

      sut.activatePlot(2)

      sut.tapCell(0, 0)
      advanceUntilIdle()
      assertContentEquals(
         listOf(Triple(0, 0, 1.toByte()), Triple(0, 0, 2.toByte())),
         gameEngineFactory.lastEngine!!.assignCellCalls
      )
   }

   @Test fun testStopwatch() = runTest {
      val gameEngineFactory = TestGameEngineFactory()
      val sut = SumGameViewModel(
         game_2x2,
         gameEngineFactory = gameEngineFactory,
         backgroundDispatcher = testDispatcher,
         clock = TestScheduleSyncingClock(testDispatcher.scheduler)
      )
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
      assertEquals(twoSeconds + 1.milliseconds, gameEngineFactory.lastEngine!!.effort.elapsedTime)

   }
}
