package com.letstwinkle.sumbrella

import com.letstwinkle.sumbrella.model.SumGame
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import sumbrella.shared.generated.resources.Res
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SumGameTest {
   @Test fun testLoadGame3() = runTest {
      val bytes = Res.readBytes("3.json")
      val game = Json.decodeFromString<SumGame>(bytes.decodeToString())
      assertEquals("1", game.id)
      assertEquals(20, game.sum)
      assertEquals(4, game.plots)
      assertContentEquals(arrayOf(
         charArrayOf(1.char, 2.char, 2.char, 4.char, 1.char),
         charArrayOf(6.char, 1.char, 1.char, 1.char, 1.char),
         charArrayOf(2.char, 1.char, 1.char, 4.char, 1.char),
         charArrayOf(4.char, 6.char, 5.char, 1.char, 7.char),
         charArrayOf(1.char, 9.char, 6.char, 7.char, 5.char),
      ), game.cells)
   }

}
