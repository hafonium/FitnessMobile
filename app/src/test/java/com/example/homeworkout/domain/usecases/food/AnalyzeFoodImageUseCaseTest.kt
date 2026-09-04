package com.example.homeworkout.domain.usecases.food

import com.example.homeworkout.domain.models.FoodAnalysis
import com.example.homeworkout.domain.models.NutritionEstimate
import com.example.homeworkout.domain.repositories.FoodAnalysisRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AnalyzeFoodImageUseCaseTest {
    @Test
    fun `rejects an empty image`() {
        val useCase = AnalyzeFoodImageUseCase(FakeFoodAnalysisRepository())

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(byteArrayOf()) }
        }
    }

    @Test
    fun `passes image to repository and returns analysis`() = runBlocking {
        val repository = FakeFoodAnalysisRepository()
        val useCase = AnalyzeFoodImageUseCase(repository)
        val image = byteArrayOf(1, 2, 3)

        val result = useCase(image)

        assertArrayEquals(image, repository.receivedImage)
        assertEquals("burger", result.category)
        assertEquals(508.0, result.calories.value, 0.0)
    }
}

private class FakeFoodAnalysisRepository : FoodAnalysisRepository {
    var receivedImage: ByteArray? = null

    override suspend fun analyzeFoodImage(image: ByteArray): FoodAnalysis {
        receivedImage = image
        return FoodAnalysis(
            category = "burger",
            categoryProbability = 0.99,
            calories = NutritionEstimate(508.0, "calories", 429.0, 572.0),
            fat = NutritionEstimate(21.0, "g", 21.0, 33.0),
            protein = NutritionEstimate(29.0, "g", 23.0, 34.0),
            carbohydrates = NutritionEstimate(40.0, "g", 30.0, 45.0),
            recipesUsed = 25
        )
    }
}
