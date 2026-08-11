package com.letstwinkle.sumbrella

import androidx.compose.ui.graphics.Color

interface IPlotColorProvider {
   fun cellColorForPlot(number: Int): Color
   fun colorWellColorForPlot(number: Int): Color
}

class StandardPlotColorProvider(val numberOfPlots: Int) : IPlotColorProvider {
   override fun cellColorForPlot(number: Int): Color {
      return when (numberOfPlots) {
         4 -> when (number) {
            1 -> Color(142, 217, 175)
            2 -> Color(245, 214, 131)
            3 -> Color(206, 178, 234)
            4 -> Color(156, 199, 243)
            // 0 (no plot) falls here
            else -> Color.White
         }
         6 -> when (number) {
            1 -> Color(142, 217, 175)
            2 -> Color(245, 214, 131)
            3 -> Color(206, 178, 234)
            4 -> Color(156, 199, 243)
            5 -> Color(245, 185, 142)
            6 -> Color(243, 178, 205)
            // 0 (no plot) falls here
            else -> Color.White
         }
         7 -> when (number) {
            1 -> Color(142, 217, 175)
            2 -> Color(245, 214, 131)
            3 -> Color(206, 178, 234)
            4 -> Color(156, 199, 243)
            5 -> Color(245, 185, 142)
            6 -> Color(243, 178, 205)
            7 -> TODO("Forget to sample color")
            // 0 (no plot) falls here
            else -> Color.White
         }
         else -> error("Unexpected number of plots: $numberOfPlots")
      }
   }

   override fun colorWellColorForPlot(number: Int): Color {
      return when (numberOfPlots) {
         4 -> when (number) {
            1 -> Color(46, 158, 98)
            2 -> Color(217, 166, 45)
            3 -> Color(139, 95, 191)
            4 -> Color(61, 125, 216)
            // 0 (no plot) falls here
            else -> Color.White
         }
         6 -> when (number) {
            1 -> Color(46, 158, 98)
            2 -> Color(217, 166, 45)
            3 -> Color(139, 95, 191)
            4 -> Color(61, 125, 216)
            5 -> Color(221, 122, 52)
            6 -> Color(214, 90, 147)
            // 0 (no ple
            else -> Color.White
         }
         7 -> when (number) {
            1 -> Color(46, 158, 98)
            2 -> Color(217, 166, 45)
            3 -> Color(139, 95, 191)
            4 -> Color(61, 125, 216)
            5 -> Color(221, 122, 52)
            6 -> Color(214, 90, 147)
            7 -> Color(46, 150, 158)
            // 0 (no ple
            else -> Color.White
         }
         else -> error("Unexpected number of plots: $numberOfPlots")
      }
   }
}
