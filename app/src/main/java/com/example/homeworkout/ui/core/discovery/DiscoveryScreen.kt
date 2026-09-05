package com.example.homeworkout.ui.core.discovery

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeworkout.R
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.buttons.AppButtonVariant

private val DiscoveryBackground = Color(0xFFF8F9FB)
private val SearchSurface = Color(0xFFF1F2F5)
private val DiscoveryBlue = Color(0xFF0057FF)
private val PrimaryText = Color(0xFF15171B)
private val MutedText = Color(0xFF8B8D98)
private val InactiveTab = Color(0xFF8B8D98)
private val SecondaryText = Color(0xFFD1D5DB)
private val DiscoveryCardShape = RoundedCornerShape(22.dp)

private enum class DiscoveryCategory(val label: String) {
    AT_HOME("At Home"),
    GYM("Gym"),
    WALK_RUN("Walk & Run")
}

private data class TrainingPlanVisual(
    val programId: String,
    val weeks: Int,
    val title: String,
    @param:DrawableRes val imageRes: Int
)

private val walkRunPlans = listOf(
    TrainingPlanVisual("walking-weight-loss-20w", 20, "WALKING FOR\nWEIGHT LOSS", R.drawable.discovery_walking_plan),
    TrainingPlanVisual("beginner-running-12w", 12, "BEGINNER\nRUNNING", R.drawable.discovery_running_plan)
)

@Composable
fun DiscoveryScreen(
    onOpenFoodScanner: () -> Unit,
    onOpenRunning: () -> Unit,
    onOpenTrainingPlan: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(DiscoveryCategory.WALK_RUN) }
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(DiscoveryBackground)) {
        CategoryTabHeader(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )
        SearchInputBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        when (selectedCategory) {
            DiscoveryCategory.WALK_RUN -> WalkRunContent(searchQuery, onOpenRunning, onOpenTrainingPlan)
            DiscoveryCategory.AT_HOME -> AtHomeContent(searchQuery, onOpenFoodScanner)
            DiscoveryCategory.GYM -> EmptyCategoryContent("Gym plans are coming soon.")
        }
    }
}

@Composable
private fun CategoryTabHeader(
    selectedCategory: DiscoveryCategory,
    onCategorySelected: (DiscoveryCategory) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DiscoveryCategory.entries.forEachIndexed { index, category ->
            if (index > 0) Spacer(Modifier.width(24.dp))
            val selected = category == selectedCategory
            Column(
                modifier = Modifier.clickable { onCategorySelected(category) }.padding(horizontal = 2.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = category.label,
                    color = if (selected) PrimaryText else InactiveTab,
                    fontSize = if (selected) 20.sp else 17.sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier.width(40.dp).height(3.dp).clip(CircleShape)
                        .background(if (selected) DiscoveryBlue else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun SearchInputBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().height(46.dp).clip(CircleShape).background(SearchSurface),
        singleLine = true,
        textStyle = TextStyle(color = PrimaryText, fontSize = 15.sp),
        decorationBox = { input ->
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MutedText, modifier = Modifier.size(19.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search workouts, plans...", color = MutedText, fontSize = 15.sp)
                    input()
                }
            }
        }
    )
}

@Composable
private fun WalkRunContent(
    searchQuery: String,
    onOpenRunning: () -> Unit,
    onOpenTrainingPlan: (String) -> Unit
) {
    val visiblePlans = remember(searchQuery) {
        walkRunPlans.filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { FreeModeHeroCard(onStart = onOpenRunning) }
        item {
            Text(
                "Training plans",
                color = PrimaryText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        items(visiblePlans.size, key = { visiblePlans[it].programId }) { index ->
            TrainingPlanCard(visiblePlans[index], onClick = { onOpenTrainingPlan(visiblePlans[index].programId) })
        }
        if (visiblePlans.isEmpty()) item { EmptySearchResult(searchQuery) }
    }
}

@Composable
private fun FreeModeHeroCard(onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(232.dp).clip(DiscoveryCardShape)) {
        Image(
            painter = painterResource(R.drawable.discovery_free_mode_map),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f)))
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                "FREE MODE",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Text("Free running & walking with map", color = SecondaryText, fontSize = 15.sp)
            AppButton(
                text = "START",
                onClick = onStart,
                modifier = Modifier.width(190.dp),
                variant = AppButtonVariant.Primary
            )
        }
    }
}

@Composable
private fun TrainingPlanCard(plan: TrainingPlanVisual, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(192.dp).clip(DiscoveryCardShape).clickable(onClick = onClick)) {
        Image(
            painter = painterResource(plan.imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.78f),
                    0.62f to Color.Black.copy(alpha = 0.28f),
                    1f to Color.Transparent
                )
            )
        )
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "${plan.weeks} WEEKS PROGRAM",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
            Text(
                plan.title,
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AtHomeContent(searchQuery: String, onOpenFoodScanner: () -> Unit) {
    val matches = searchQuery.isBlank() || "Food calorie scanner".contains(searchQuery, ignoreCase = true)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("At-home tools", color = PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        if (matches) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(192.dp)
                        .clip(DiscoveryCardShape)
                        .clickable(onClick = onOpenFoodScanner)
                ) {
                    Image(
                        painter = painterResource(R.drawable.discovery_food_scanner),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(
                                0f to Color.Black.copy(alpha = 0.82f),
                                0.62f to Color.Black.copy(alpha = 0.3f),
                                1f to Color.Transparent
                            )
                        )
                    )
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(26.dp).background(DiscoveryBlue, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                "SMART NUTRITION",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                        }
                        Text(
                            "FOOD CALORIE\nSCANNER",
                            color = Color.White,
                            fontSize = 24.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Estimate calories and macros from a photo",
                            color = SecondaryText,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            item { EmptySearchResult(searchQuery) }
        }
    }
}

@Composable
private fun EmptyCategoryContent(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MutedText, fontSize = 15.sp)
    }
}

@Composable
private fun EmptySearchResult(query: String) {
    Text(
        text = "No results for \"$query\"",
        color = MutedText,
        fontSize = 15.sp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
    )
}
