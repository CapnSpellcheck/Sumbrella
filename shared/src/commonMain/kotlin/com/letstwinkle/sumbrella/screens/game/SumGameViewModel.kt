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
import com.letstwinkle.sumbrella.engine.UndoCommand
import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.model.SumGameEffort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.*
import kotlin.collections.removeLast as removeLastKt

private val plotSuccessTextColor = Color(0xFF2E7D32)

@OptIn(ExperimentalTime::class)
class SumGameViewModel(
   val game: SumGame,
   val plotColorProvider: IPlotColorProvider = StandardPlotColorProvider(game.plots.toInt()),
   val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
   val gameEngineFactory: IGameEngineFactory = GameEngineFactory(),
   val clock: Clock = Clock.System,
) : ViewModel() {
   val cellColorsObservable: List<List<StateFlow<Color>>>
   val plotStatusesObservable: List<StateFlow<Boolean>>
      get() = gameEngine.plotErrorsObservable
   val plotTalliesObservable: List<StateFlow<String>>
   val plotTallyColorsObservable: List<StateFlow<Color>>
   val elapsedTimeObservable: MutableStateFlow<Duration>
   val selectedPlotObservable = MutableStateFlow<Byte>(0)
   val isUndoEnabledObservable = MutableStateFlow(false)
   val solvedEventObservable = MutableSharedFlow<Unit>()

   // these don't change, so not flows
   val colorWellColors: List<Color> = IntRange(1, game.plots.toInt())
      .map { plotColorProvider.colorWellColorForPlot(it) }

   private var stopwatchBase = Instant.fromEpochMilliseconds(0)
   private val effort: SumGameEffort
   private val gameEngine: IGameEngine
   private var elapsedTimerJob: Job? = null
   private var undoCommandStack = mutableListOf<UndoCommand>()

   init {
      effort = SumGameEffort(
         gameId = game.id,
         elapsedTime = Duration.ZERO,
         coloration = Array(game.cells.size) { ByteArray(game.cells[0].size) },
         plotSums = IntArray(game.plots.toInt()),
         plotInError = BooleanArray(game.plots.toInt()),
         solved = false
      )
      if (!effort.solved)
         activatePlot(1)
      gameEngine = gameEngineFactory.createGameEngine(game, effort)
      cellColorsObservable = gameEngine.colorationsObservable.map { colorationRow ->
         colorationRow.map { cellColorationFlow ->
            cellColorationFlow.mapState { cellColoration ->
               plotColorProvider.cellColorForPlot(cellColoration.toInt())
            }
         }
      }

         List(game.cells.size) { i ->
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
               0 -> plotSuccessTextColor
               -1 -> Color.Red
               else -> error(Unit)
            }
         }
      }
      elapsedTimeObservable = MutableStateFlow(effort.elapsedTime)
   }

   val gameIsSolved: Boolean
      get() = effort.solved

   fun activatePlot(plotNumber: Int) {
      selectedPlotObservable.value = plotNumber.toByte()
   }

   fun getPlot(row: Int, col: Int): Byte = effort.coloration[row][col]

   fun tapCell(row: Int, col: Int) {
      val selectedPlot = selectedPlotObservable.value
      val newPlot = if (effort.coloration[row][col] == selectedPlot) NULL_PLOT else selectedPlot
      viewModelScope.launch(backgroundDispatcher) {
         val undoCommand = gameEngine.assignCell(row, col, newPlot)
         undoCommandStack.add(undoCommand)
         isUndoEnabledObservable.value = true
         if (effort.solved) {
            pauseStopwatch()
            solvedEventObservable.emit(Unit)
            activatePlot(0)
         }
      }
   }

   fun eyedropCell(row: Int, col: Int) {
      val cellPlot = effort.coloration[row][col]
      activatePlot(cellPlot.toInt())
   }

   fun saveEffort() {
      // TODO: save `effort`
   }

   fun pauseStopwatch() {
      elapsedTimerJob?.let {
         it.cancel()
         elapsedTimerJob = null
         effort.elapsedTime += clock.now() - stopwatchBase
         stopwatchBase = Instant.DISTANT_FUTURE
      }
      println("clock.now=${clock.now()} stopwatchBase=$stopwatchBase")
   }

   fun resumeStopwatch() {
      if (!effort.solved) {
         stopwatchBase = clock.now()
         elapsedTimerJob = viewModelScope.launch {
            while (true) {
               elapsedTimeObservable.value = clock.now() - stopwatchBase + effort.elapsedTime
               delay(1000)
            }
         }
      }
   }

   fun erase() {
      viewModelScope.launch {
         val undoCommand = gameEngine.erase()
         undoCommandStack.add(undoCommand)
      }
   }


   fun undo() {
      val command = undoCommandStack.removeLastKt()
      viewModelScope.launch {
         gameEngine.performUndo(command)
      }
      isUndoEnabledObservable.value = undoCommandStack.isNotEmpty()
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
