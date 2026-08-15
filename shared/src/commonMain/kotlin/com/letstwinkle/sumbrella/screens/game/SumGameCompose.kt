@file:OptIn(ExperimentalTime::class)

package com.letstwinkle.sumbrella.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.letstwinkle.sumbrella.SymmetricInOutEasing
import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.toHHMMSS
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import sumbrella.shared.generated.resources.*
import kotlin.time.ExperimentalTime

private val digitFontSize = 36.dp
private val cellBorderColor = Color(228, 228, 231)

@Composable fun SumGame(viewModel: SumGameViewModel) {
   val gameIsNotSolved = remember { mutableStateOf(!viewModel.gameIsSolved) }
   LaunchedEffect(viewModel) {
      viewModel.solvedEventObservable.collect {
         gameIsNotSolved.value = false
      }
   }

   Box {
      Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
         GameHeader(viewModel, Modifier.padding(bottom = 8.dp))
         Board(viewModel, gameIsNotSolved, Modifier.padding(horizontal = 12.dp, vertical = 16.dp))
         PlotWellsAndStatuses(viewModel, gameIsNotSolved)
         if (gameIsNotSolved.value) {
            Buttons(viewModel)
         } else {
            Text(
               "Game is solved!",
               Modifier.align(CenterHorizontally).padding(top = 12.dp),
               style = typography.displaySmall
            )
         }
      }
      if (!gameIsNotSolved.value) {
         val composition by rememberLottieComposition {
            LottieCompositionSpec.JsonString(
               Res.readBytes("files/lf20_u4yrau.json").decodeToString()
            )
         }
         val progress by animateLottieCompositionAsState(composition)

         Image(
            painter = rememberLottiePainter(
               composition = composition,
               progress = { progress },
            ),
            contentDescription = "Congratulations effect",
            Modifier.matchParentSize()
         )
      }
   }
}

@Composable private fun GameHeader(viewModel: SumGameViewModel, modifier: Modifier = Modifier) {
   val elapsedTime = viewModel.elapsedTimeObservable.collectAsStateWithLifecycle()
   val game = viewModel.game

   LifecycleStartEffect(viewModel) {
      viewModel.resumeStopwatch()
      onStopOrDispose {
         viewModel.pauseStopwatch()
         viewModel.saveEffort()
      }
   }
   Row(modifier) {
      Text(buildAnnotatedString {
         append("Split into ")
         withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("${game.plots} plots of ${game.sum}")
         }
      }, Modifier.alignBy(FirstBaseline), style = typography.bodyLarge)
      Spacer(Modifier.weight(1f))
      Text(elapsedTime.value.toHHMMSS(), Modifier.alignBy(FirstBaseline), style = typography.bodyMedium)
   }
}

@Composable private fun Board(viewModel: SumGameViewModel, enabledState: State<Boolean>, modifier: Modifier) {
   val game = viewModel.game
   val digitSize = with(LocalDensity.current) { digitFontSize.toSp() }
   val boardWidth = game.cells[0].size
   Column(modifier) {
      repeat(game.cells.size) { row ->
         val rowValues = game.cells[row]
         Row {
            val baseM = Modifier.weight(1f).aspectRatio(1f)
            repeat(boardWidth) { col ->
               val colorState = viewModel.cellColorsObservable[row][col]
               val interactionSource = remember { MutableInteractionSource() }
               val onClick = {
                  viewModel.tapCell(row, col)
               }
               val modifier =
                  if (viewModel.getPlot(row, col) == viewModel.selectedPlotObservable.value)
                     baseM.clickable(interactionSource, null, enabledState.value, onClick = onClick)
                  else baseM.combinedClickable(
                     interactionSource,
                     null,
                     enabledState.value,
                     onLongClick = { viewModel.eyedropCell(row, col) },
                     onClick = onClick
                  )
               Cell(rowValues[col], colorState, digitSize, modifier)
               if (col != boardWidth - 1)
                  Spacer(Modifier.width(8.dp))
            }
         }
         Spacer(Modifier.height(8.dp))
      }
   }
}

@Composable private fun Cell(
   value: Byte,
   color: StateFlow<Color>,
   fontSize: TextUnit,
   modifier: Modifier = Modifier
) {
   val cornerShape = RoundedCornerShape(14.dp)
   val modifier = modifier.clip(cornerShape).border(2.dp, cellBorderColor, cornerShape)
   val backColor = remember { mutableStateOf(color.value) }
   val frontColor = remember { mutableStateOf(Color.Transparent) }
   val frontScaleAnimator = remember { Animatable(0.5f) }

   LaunchedEffect(frontScaleAnimator) {
      color.drop(1).collect {
         if (it == Color.White) {
            frontColor.value = backColor.value
            backColor.value = it
            frontScaleAnimator.snapTo(1f)
            frontScaleAnimator.animateTo(0.5f, tween(easing = SymmetricInOutEasing()))
            frontColor.value = Color.Transparent
         } else {
            frontColor.value = it
            frontScaleAnimator.snapTo(0.5f)
            frontScaleAnimator.animateTo(1f, tween(easing = SymmetricInOutEasing()))
            backColor.value = frontColor.value
            frontColor.value = Color.Transparent
         }
      }
   }
   Box(modifier, contentAlignment = Alignment.Center) {
      Box(Modifier.background(backColor.value).matchParentSize())
      Box(Modifier.graphicsLayer {
         shape = cornerShape
         scaleX = frontScaleAnimator.value; scaleY = frontScaleAnimator.value
         clip = true
      }.background(frontColor.value).matchParentSize())
      Text(
         value.toString(),
         fontSize = fontSize,
         fontWeight = Bold
      )
   }
}

private val colorWellWidth = 56.dp

@Composable private fun PlotWellsAndStatuses(
   viewModel: SumGameViewModel,
   enabledState: State<Boolean>,
   modifier: Modifier = Modifier
) {
   val cornerShape = RoundedCornerShape(14.dp)
   val selectedPlot = viewModel.selectedPlotObservable.collectAsStateWithLifecycle()
   Row(modifier.fillMaxWidth(), spacedBy(16.dp, CenterHorizontally), CenterVertically) {
      val modifier =
         Modifier.height(48.dp).width(colorWellWidth).padding(horizontal = 4.dp).clip(cornerShape)
      val selectedModifier =
         Modifier.size(colorWellWidth).clip(cornerShape).border(4.dp, Color.Black, cornerShape)
      for (i in 0..<viewModel.game.plots) {
         val bgcolor = viewModel.colorWellColors[i]
         val plot = (i + 1)
         val plotError = viewModel.plotStatusesObservable[plot].collectAsStateWithLifecycle()
         Column() {
            Box(
               (if (selectedPlot.value.toInt() == plot) selectedModifier else modifier)
                  .background(bgcolor)
                  .clickable(enabledState.value) { viewModel.activatePlot(plot) }
            )
            // TODO: this plot path error indication sucks. Make something custom.
            if (plotError.value) {
               Text(
                  "- -",
                  color = Color(239,	68,	68),
                  fontSize = 25.sp,
                  fontWeight = Bold
               )
            }
         }
      }
   }
   Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), spacedBy(16.dp, CenterHorizontally)) {
      for (plot in 1..viewModel.game.plots) {
         Text(
            viewModel.plotTalliesObservable[plot].collectAsStateWithLifecycle().value,
            Modifier.width(colorWellWidth).testTag("plotSum_$plot"),
            fontSize = 20.sp,
            fontWeight = Bold,
            textAlign = TextAlign.Center,
            color = viewModel.plotTallyColorsObservable[plot].collectAsStateWithLifecycle().value,
         )
      }
   }
}

@Composable private fun Buttons(viewModel: SumGameViewModel, modifier: Modifier = Modifier) {
   Row(modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = spacedBy(12.dp, CenterHorizontally)) {
      val undoEnabled = viewModel.isUndoEnabledObservable.collectAsStateWithLifecycle()
      OutlinedButton({ viewModel.undo() }, enabled = undoEnabled.value) {
         Icon(painterResource(Res.drawable.undo), "undo")
         Spacer(Modifier.width(8.dp))
         Text("Undo")
      }
      OutlinedButton({ viewModel.erase() }) {
         Icon(painterResource(Res.drawable.clear), "clear")
         Spacer(Modifier.width(8.dp))
         Text("Clear")
      }
   }
}

@Composable
@Preview(showBackground = true) fun boardPreview() {
   val game = SumGame(
      "",
      arrayOf(
         byteArrayOf(1, 1, 1, 1),
         byteArrayOf(2, 2, 2, 2),
         byteArrayOf(3, 3, 3, 3),
         byteArrayOf(4, 4, 4, 4)
      ),
      10,
      4
   )
   val viewModel = SumGameViewModel(game)
   SumGame(viewModel)
}
