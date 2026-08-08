package com.letstwinkle.sumbrella

import androidx.compose.ui.graphics.Color

class TestPlotColorProvider : IPlotColorProvider {
   override fun cellColorForPlot(number: Int): Color = Color(number*10, 0, 0)

   override fun colorWellColorForPlot(number: Int): Color = Color(0, number*10, 0)
}
