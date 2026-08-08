package com.letstwinkle.sumbrella.screens.game

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letstwinkle.sumbrella.IPlotColorProvider
import com.letstwinkle.sumbrella.StandardPlotColorProvider
import com.letstwinkle.sumbrella.engine.GameEngineFactory
import com.letstwinkle.sumbrella.engine.IGameEngine
import com.letstwinkle.sumbrella.engine.IGameEngineFactory
import com.letstwinkle.sumbrella.engine.SumGameEngine.Companion.NULL_PLOT
import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.model.SumGameEffort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.*

@OptIn(ExperimentalTime::class)
class SumGameViewModel(
   val game: SumGame,
   val plotColorProvider: IPlotColorProvider = StandardPlotColorProvider(game.plots.toInt()),
   val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
   val gameEngineFactory: IGameEngineFactory = GameEngineFactory(),
   val clock: Clock = Clock.System,
) : ViewModel() {
   val cellColorsObservable: List<List<MutableStateFlow<Color>>>
   val solvedObservable: StateFlow<Boolean>
      get() = gameEngine.solvedObservable
   val plotStatusesObservable: List<StateFlow<Boolean>>
      get() = gameEngine.plotErrorsObservable
   val plotTalliesObservable: List<StateFlow<String>>
   val plotTallyColorsObservable: List<StateFlow<Color>>
   val elapsedTimeObservable: MutableStateFlow<Duration>
   val selectedPlotObservable = MutableStateFlow<Byte>(0)

   // these don't change, so not flows
   val colorWellColors: List<Color> = IntRange(1, game.plots.toInt())
      .map { plotColorProvider.colorWellColorForPlot(it) }

   private var stopwatchBase = Instant.fromEpochMilliseconds(0)
   private val effort: SumGameEffort
   private val gameEngine: IGameEngine
   private var elapsedTimerJob: Job? = null

   init {
      activatePlot(1)
      effort = SumGameEffort(
         gameId = game.id,
         elapsedTime = Duration.ZERO,
         coloration = Array(game.cells.size) { ByteArray(game.cells[0].size) },
         plotSums = IntArray(game.plots.toInt()),
         plotInError = BooleanArray(game.plots.toInt()),
         solved = false
      )
      gameEngine = gameEngineFactory.createGameEngine(game, effort)
      cellColorsObservable = List(game.cells.size) { i ->
         List(game.cells[i].size) { j ->
            MutableStateFlow(plotColorProvider.cellColorForPlot(effort.coloration[i][j].toInt()))
         }
      }
      plotTalliesObservable = gameEngine.plotSumsObservable.map { plotSumFlow ->
         plotSumFlow.mapState { plotSum -> (game.sum - plotSum).toString() }
      }
      plotTallyColorsObservable = gameEngine.plotSumsObservable.map { plotSumFlow ->
         plotSumFlow.mapState { plotSum ->
            when (game.sum.compareTo(plotSum)) {
               1 -> Color.Black // TODO: Color promises
               0 -> Color.Green
               -1 -> Color.Red
               else -> error(Unit)
            }
         }
      }
      elapsedTimeObservable = MutableStateFlow(effort.elapsedTime)
   }

   fun activatePlot(plotNumber: Int) {
      selectedPlotObservable.value = plotNumber.toByte()
   }

   fun getPlot(row: Int, col: Int): Byte = effort.coloration[row][col]

   fun tapCell(row: Int, col: Int) {
      val selectedPlot = selectedPlotObservable.value
      val newPlot = if (effort.coloration[row][col] == selectedPlot) NULL_PLOT else selectedPlot
      viewModelScope.launch(backgroundDispatcher) {
         gameEngine.assignCell(row, col, newPlot)
      }
      val color = plotColorProvider.cellColorForPlot(newPlot.toInt())
      cellColorsObservable[row][col].value = color
   }

   fun eyedropCell(row: Int, col: Int) {
      val cellPlot = effort.coloration[row][col]
      activatePlot(cellPlot.toInt())
   }

   fun saveEffort() {
      // TODO: save `effort`
   }

   fun pauseStopwatch() {
      elapsedTimerJob?.cancel()
      elapsedTimerJob = null
      println("clock.now=${clock.now()} stopwatchBase=$stopwatchBase")
      effort.elapsedTime += clock.now() - stopwatchBase
      stopwatchBase = Instant.DISTANT_FUTURE
   }

   fun resumeStopwatch() {
      stopwatchBase = clock.now()
      elapsedTimerJob = viewModelScope.launch {
         while (true) {
            elapsedTimeObservable.value = clock.now() - stopwatchBase + effort.elapsedTime
            delay(1000)
         }
      }
   }

   private fun <T, K> StateFlow<T>.mapState(
      transform: (data: T) -> K
   ): StateFlow<K> {
      return map {
         transform(it)
      }
         .stateIn(viewModelScope, SharingStarted.Eagerly, transform(value))
   }
}
