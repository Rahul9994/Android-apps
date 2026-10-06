package com.proteinpulse.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LocalFoodEstimatorTest {

    private fun protein(text: String) = LocalFoodEstimator.estimate(text)!!.proteinG

    @Test
    fun `counts eggs and rotis`() {
        val a = LocalFoodEstimator.estimate("3 eggs and 2 rotis")
        assertNotNull(a)
        assertEquals(listOf("Egg", "Roti"), a!!.items.map { it.name })
        assertEquals(18.9, a.items[0].proteinG, 0.1) // 3 × 50 g × 12.6 %
        assertEquals(6.0, a.items[1].proteinG, 0.1) // 2 × 40 g × 7.5 %
    }

    @Test
    fun `grams are respected`() {
        assertEquals(62.0, protein("200 g chicken breast"), 0.1)
        assertEquals(62.0, protein("200g chicken breast"), 0.1)
    }

    @Test
    fun `dal is not read as a litre`() {
        // "dal" contains "a" + "l"; it must not be parsed as "1 litre".
        assertEquals(14.0, protein("dal"), 0.1)
    }

    @Test
    fun `bowls cups and scoops use serving sizes`() {
        assertEquals(14.0, protein("a bowl of dal"), 0.1)
        assertEquals(46.8, protein("2 scoops whey"), 0.1)
        assertEquals(8.5, protein("1 glass milk"), 0.1)
    }

    @Test
    fun `longest keyword wins`() {
        assertEquals("Egg white", LocalFoodEstimator.estimate("4 egg whites")!!.items.single().name)
        assertEquals("Peanut butter", LocalFoodEstimator.estimate("2 tbsp peanut butter")!!.items.single().name)
    }

    @Test
    fun `unknown food returns null`() {
        assertNull(LocalFoodEstimator.estimate("something mysterious"))
    }
}
