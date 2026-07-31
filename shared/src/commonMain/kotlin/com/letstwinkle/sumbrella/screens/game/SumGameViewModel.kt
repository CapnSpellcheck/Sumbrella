@file:OptIn(ExperimentalTime::class)

package com.letstwinkle.sumbrella.screens.game

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letstwinkle.sumbrella.PlotColorProvider
import com.letstwinkle.sumbrella.StandardPlotColorProvider
import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.model.SumGameEffort
import com.letstwinkle.sumbrella.screens.game.SumGameEngine.Companion.NULL_PLOT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.*

class SumGameViewModel(
   val game: SumGame,
   val plotColorProvider: PlotColorProvider = StandardPlotColorProvider(game.plots + 0)
) : ViewModel() {
   val cellColorsObservable: List<List<MutableStateFlow<Color>>>
   val plotStatusesObservable: List<MutableStateFlow<Boolean>>

   private var stopwatchBase = Instant.fromEpochMilliseconds(0)
   private var selectedPlot: Byte = 0
   private val effort: SumGameEffort
   private val gameEngine: SumGameEngine

   init {
      activatePlot(1)
      effort = SumGameEffort(
         gameId = "3",
         elapsedTime = Duration.ZERO,
         coloration = Array(game.cells.size) { ByteArray(game.cells[0].size) },
         plotSums = IntArray(game.plots.toInt()),
         plotInError = BooleanArray(game.plots.toInt()),
         false
      )
      cellColorsObservable = List(game.cells.size) { i ->
         List(game.cells[i].size) { j ->
            MutableStateFlow(plotColorProvider.cellColorForPlot(effort.coloration[i][j] + 0))
         }
      }
      plotStatusesObservable = List(game.plots.toInt()) { i -> MutableStateFlow(effort.plotInError[i]) }
      gameEngine = SumGameEngine(game, effort)
   }

   private fun loadEffort() {

   }

   fun activatePlot(plotNumber: Byte) {
      selectedPlot = plotNumber
   }

   fun tapCell(row: Int, col: Int) {
      val newPlot = if (effort.coloration[row][col] == selectedPlot) NULL_PLOT else selectedPlot
      viewModelScope.launch(Dispatchers.Default) {
         gameEngine.assignCell(row, col, newPlot)
         plotStatusesObservable[newPlot + 0].value = effort.plotInError[newPlot + 0]
         if (effort.solved) {
            // TODO
         }
      }
      val color = plotColorProvider.cellColorForPlot(newPlot + 0)
      cellColorsObservable[row][col].value = color
   }

   fun saveEffort() {
      pauseTimer()
      // TODO: save effort
   }

   fun pauseTimer() {
      effort.elapsedTime += Clock.System.now() - stopwatchBase
      stopwatchBase = Instant.fromEpochMilliseconds(0)
   }

   fun resumeTimer() {
      stopwatchBase = Clock.System.now()
   }

}
