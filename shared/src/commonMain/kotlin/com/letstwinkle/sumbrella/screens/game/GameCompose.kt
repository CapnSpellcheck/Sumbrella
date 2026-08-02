@file:OptIn(ExperimentalTime::class)

package com.letstwinkle.sumbrella.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.letstwinkle.sumbrella.model.SumGame
import com.letstwinkle.sumbrella.toHHMMSS
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.ExperimentalTime

private val DigitFontSize = 36.dp
private val cellBorderColor = Color(228, 228, 231)

@Composable fun Cell(value: Byte, color: State<Color>, fontSize: TextUnit, modifier: Modifier = Modifier) {
   val cornerShape = RoundedCornerShape(14.dp)
   val mmodifier = modifier.clip(cornerShape).border(2.dp, cellBorderColor, cornerShape)
      .background(color.value)
   Box(mmodifier, contentAlignment = Alignment.Center) {
      Text(
         value.toString(),
         Modifier.padding().clip(cornerShape),
         fontSize = fontSize,
         fontWeight = FontWeight.Bold
      )
   }
}

@Composable fun SumGame(viewModel: SumGameViewModel) {
   Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
      GameHeader(viewModel)
      Board(viewModel, Modifier.padding(horizontal = 12.dp))
   }
}

@Composable fun GameHeader(viewModel: SumGameViewModel) {
   val elapsedTime = viewModel.elapsedTimeObservable.collectAsStateWithLifecycle()
   val game = viewModel.game

   LifecycleStartEffect(viewModel) {
      viewModel.resumeStopwatch()
      onStopOrDispose {
         viewModel.pauseStopwatch()
         viewModel.saveEffort()
      }
   }
   Row(Modifier.padding(bottom = 12.dp)) {
      Text(buildAnnotatedString {
         append("Split into ")
         withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("${game.plots} plots of ${game.sum}")
         }
      }, Modifier.alignBy(FirstBaseline), style = MaterialTheme.typography.bodyLarge)
      Spacer(Modifier.weight(1f))
      Text(elapsedTime.value.toHHMMSS(), Modifier.alignBy(FirstBaseline), style = MaterialTheme.typography.bodyMedium)
   }
}

@Composable fun Board(viewModel: SumGameViewModel, modifier: Modifier) {
   val game = viewModel.game
   val digitSize = with(LocalDensity.current) { DigitFontSize.toSp() }
   val boardWidth = game.cells[0].size
   repeat(game.cells.size) { row ->
      val rowValues = game.cells[row]
      Row(modifier) {
         val modifier = Modifier.weight(1f).aspectRatio(1f)
         repeat(boardWidth) { col ->
            val colorState = viewModel.cellColorsObservable[row][col].collectAsStateWithLifecycle()
            val interactionSource = remember { MutableInteractionSource() }
            Cell(rowValues[col], colorState, digitSize, modifier.clickable(interactionSource, null) {
               viewModel.tapCell(row, col)
            })
            if (col != boardWidth - 1)
               Spacer(Modifier.width(8.dp))
         }
      }
      Spacer(Modifier.height(8.dp))
   }
}

@Composable
@Preview(showBackground = true) fun boardPreview() {
   val game = SumGame("", arrayOf(byteArrayOf(1, 2, 3, 4), byteArrayOf(3, 2, 1, 4), byteArrayOf(2, 3, 1, 4)), 7, 4)
   val viewModel = SumGameViewModel(game)
   SumGame(viewModel)
}
