package com.proteinpulse.app.data

import kotlin.math.roundToInt

/**
 * Rough, offline protein estimates for common foods, used until an AI key is added
 * (or when the network call fails). Values are per 100 g of the food as eaten.
 */
object LocalFoodEstimator {

    private data class Food(
        val keys: List<String>,
        val label: String,
        val proteinPer100: Double,
        val kcalPer100: Double,
        /** Grams in one bowl / cup / plate / glass. */
        val servingG: Double,
        /** Grams in one counted piece (an egg, a roti, a slice). Null when the food isn't counted. */
        val pieceG: Double? = null,
    )

    private val foods = listOf(
        Food(listOf("egg white"), "Egg white", 11.0, 52.0, 99.0, 33.0),
        Food(listOf("bhurji", "scrambled egg"), "Egg bhurji", 11.0, 180.0, 150.0),
        Food(listOf("omelette", "omelet"), "Omelette", 11.0, 154.0, 120.0),
        Food(listOf("egg"), "Egg", 12.6, 155.0, 100.0, 50.0),
        Food(listOf("chicken breast"), "Chicken breast", 31.0, 165.0, 150.0),
        Food(listOf("chicken biryani", "biryani"), "Biryani", 8.0, 180.0, 300.0),
        Food(listOf("chicken curry", "butter chicken"), "Chicken curry", 14.0, 170.0, 250.0),
        Food(listOf("chicken", "tikka", "tandoori"), "Chicken", 27.0, 210.0, 150.0),
        Food(listOf("mutton", "lamb", "goat"), "Mutton", 25.0, 294.0, 150.0),
        Food(listOf("tuna"), "Tuna", 26.0, 130.0, 100.0),
        Food(listOf("salmon"), "Salmon", 20.0, 208.0, 150.0),
        Food(listOf("prawn", "shrimp"), "Prawns", 24.0, 99.0, 100.0),
        Food(listOf("fish"), "Fish", 22.0, 130.0, 150.0),
        Food(listOf("soya chunk", "soya", "soy chunk"), "Soya chunks (dry)", 52.0, 345.0, 50.0),
        Food(listOf("paneer"), "Paneer", 18.0, 265.0, 100.0, 25.0),
        Food(listOf("tofu"), "Tofu", 12.0, 120.0, 150.0),
        Food(listOf("tempeh"), "Tempeh", 19.0, 192.0, 100.0),
        Food(listOf("greek yogurt", "greek yoghurt", "hung curd"), "Greek yogurt", 10.0, 59.0, 150.0),
        Food(listOf("curd", "dahi", "yogurt", "yoghurt"), "Curd", 3.5, 61.0, 150.0),
        Food(listOf("lassi"), "Lassi", 3.0, 75.0, 250.0),
        Food(listOf("buttermilk", "chaas"), "Buttermilk", 1.5, 40.0, 250.0),
        Food(listOf("milk"), "Milk", 3.4, 62.0, 250.0),
        Food(listOf("whey", "protein shake", "protein powder"), "Whey protein", 78.0, 390.0, 30.0, 30.0),
        Food(listOf("protein bar"), "Protein bar", 33.0, 350.0, 60.0, 60.0),
        Food(listOf("cheese"), "Cheese", 25.0, 400.0, 40.0, 20.0),
        Food(listOf("rajma", "kidney bean"), "Rajma", 8.7, 127.0, 200.0),
        Food(listOf("chole", "chana", "chickpea"), "Chana", 8.9, 164.0, 200.0),
        Food(listOf("sprout"), "Sprouts", 4.0, 40.0, 100.0),
        Food(listOf("dal", "daal", "lentil", "sambar"), "Dal", 7.0, 116.0, 200.0),
        Food(listOf("sattu"), "Sattu", 20.0, 400.0, 40.0),
        Food(listOf("peanut butter"), "Peanut butter", 25.0, 588.0, 32.0, 16.0),
        Food(listOf("peanut"), "Peanuts", 26.0, 567.0, 30.0),
        Food(listOf("almond"), "Almonds", 21.0, 579.0, 28.0, 1.2),
        Food(listOf("chia"), "Chia seeds", 17.0, 486.0, 15.0),
        Food(listOf("oats", "oatmeal"), "Oats", 13.0, 389.0, 40.0),
        Food(listOf("quinoa"), "Quinoa", 4.4, 120.0, 185.0),
        Food(listOf("paratha"), "Paratha", 7.0, 300.0, 80.0, 80.0),
        Food(listOf("roti", "chapati", "chapathi", "phulka"), "Roti", 7.5, 297.0, 40.0, 40.0),
        Food(listOf("bread", "toast"), "Bread", 9.0, 265.0, 28.0, 28.0),
        Food(listOf("idli"), "Idli", 4.0, 100.0, 40.0, 40.0),
        Food(listOf("dosa"), "Dosa", 4.0, 168.0, 100.0, 100.0),
        Food(listOf("upma"), "Upma", 4.0, 150.0, 200.0),
        Food(listOf("poha"), "Poha", 2.5, 130.0, 200.0),
        Food(listOf("rice"), "Rice", 2.7, 130.0, 180.0),
        Food(listOf("pasta", "noodle", "maggi"), "Pasta / noodles", 5.8, 158.0, 200.0),
        Food(listOf("pizza"), "Pizza", 11.0, 266.0, 107.0, 107.0),
        Food(listOf("burger"), "Burger", 13.0, 250.0, 220.0, 220.0),
        Food(listOf("sandwich"), "Sandwich", 10.0, 250.0, 150.0, 150.0),
        Food(listOf("banana"), "Banana", 1.1, 89.0, 120.0, 120.0),
        Food(listOf("apple"), "Apple", 0.3, 52.0, 180.0, 180.0),
        Food(listOf("broccoli"), "Broccoli", 2.8, 34.0, 100.0),
        Food(listOf("potato", "aloo"), "Potato", 2.0, 77.0, 150.0),
    )

    private val numberWords = mapOf(
        "a" to 1.0, "an" to 1.0, "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0,
        "five" to 5.0, "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0, "ten" to 10.0,
        "half" to 0.5, "dozen" to 12.0, "couple" to 2.0,
    )

    private val quantityRegex = Regex(
        """\b(\d+(?:\.\d+)?|${numberWords.keys.joinToString("|")})\s*""" +
            """(kg|kgs|g|gm|gms|gram|grams|ml|l|litre|liter|cups?|bowls?|katori|plates?|glass(?:es)?|scoops?|slices?|pieces?|pcs|tbsp|tsp)?\b""",
        RegexOption.IGNORE_CASE,
    )

    /** Returns null when nothing recognisable was found. */
    fun estimate(text: String): MealAnalysis? {
        val segments = text.lowercase()
            .split(Regex(""",|\band\b|\bwith\b|\+|&|\n|;"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val items = segments.mapNotNull { segment -> estimateSegment(segment) }
        if (items.isEmpty()) return null
        return MealAnalysis(items = items, tip = tipFor(items), source = AnalysisSource.OFFLINE)
    }

    private fun estimateSegment(segment: String): FoodItem? {
        val food = foods
            .flatMap { f -> f.keys.map { key -> key to f } }
            .filter { (key, _) -> segment.contains(key) }
            .maxByOrNull { (key, _) -> key.length }
            ?.second ?: return null

        val match = quantityRegex.find(segment)
        val amount = match?.groupValues?.get(1)?.let { it.toDoubleOrNull() ?: numberWords[it.lowercase()] } ?: 1.0
        val unit = match?.groupValues?.get(2)?.lowercase().orEmpty()

        val grams = when {
            unit in setOf("g", "gm", "gms", "gram", "grams", "ml") -> amount
            unit in setOf("kg", "kgs", "l", "litre", "liter") -> amount * 1000
            unit.startsWith("scoop") -> amount * (food.pieceG ?: 30.0)
            unit == "tbsp" -> amount * 15
            unit == "tsp" -> amount * 5
            unit.startsWith("slice") || unit.startsWith("piece") || unit == "pcs" ->
                amount * (food.pieceG ?: food.servingG)
            unit.isNotEmpty() -> amount * food.servingG // bowl, cup, plate, glass, katori
            else -> amount * (food.pieceG ?: food.servingG)
        }

        val quantityLabel = when {
            unit.isEmpty() && food.pieceG != null -> "${amount.pretty()} pc"
            unit.isEmpty() -> "${amount.pretty()} serving"
            else -> "${amount.pretty()} $unit"
        } + " (~${grams.roundToInt()} g)"

        return FoodItem(
            name = food.label,
            quantity = quantityLabel,
            proteinG = (food.proteinPer100 * grams / 100).round1(),
            calories = (food.kcalPer100 * grams / 100).roundToInt().toDouble(),
        )
    }

    private fun tipFor(items: List<FoodItem>): String {
        val protein = items.sumOf { it.proteinG }
        return when {
            protein >= 30 -> "Strong protein hit — great for muscle repair."
            protein >= 15 -> "Decent protein. A side of curd or eggs would round it out."
            else -> "Light on protein. Pair it with paneer, eggs, dal or a shake."
        }
    }

    private fun Double.pretty() = if (this % 1.0 == 0.0) toInt().toString() else toString()
    private fun Double.round1() = (this * 10).roundToInt() / 10.0
}
