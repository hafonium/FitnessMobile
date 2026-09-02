package com.example.homeworkout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import kotlin.math.abs

private val ITEM_HEIGHT = 40.dp
private const val VISIBLE_COUNT = 5 // must be odd — the middle row is the selected value

/**
 * How many times [values] repeats in the virtual list backing [WheelColumn]'s infinite-loop
 * scrolling. The wheel starts centered in the middle of this range so there's ~1000 loops of
 * scroll room in either direction — not truly infinite, but far more than a user will ever reach
 * by scrolling, which is what "0->59 wraps to 0, 59->0 wraps to 59" means in practice.
 */
private const val CYCLES = 2000

/**
 * A single scrollable/snapping numeric wheel that loops (scrolling past the last value wraps to
 * the first, and vice versa) — one column of a `MM:SS` or `HH:mm` picker. Implemented as a huge
 * virtual list of `values.size * CYCLES` items, each mapped back to `values[index % values.size]`,
 * rather than any real circular-list API (Compose's `LazyColumn` has none) — this is the standard
 * trick for an "infinite" wheel picker.
 *
 * Performance notes (this is the fast path — every scroll frame runs through it):
 * - Snapping/momentum is entirely native (`rememberSnapFlingBehavior`) — no manual coroutine
 *   delays or spring animations fighting the fling.
 * - The centered item is tracked via [derivedStateOf], not read directly off `listState.layoutInfo`
 *   in the composition body. `layoutInfo` itself is a `State` that changes on literally every
 *   scroll frame, but the *derived* centered index only changes when the nearest item actually
 *   flips — usually far less often than every frame — so `derivedStateOf` is what keeps
 *   recomposition down to "the previous and new centered item redraw," not "every visible item
 *   redraws on every pixel of scroll."
 * - No callback/state write happens mid-scroll at all: [onSelect] fires only once scrolling has
 *   fully settled (`isScrollInProgress` transitions to `false`), read directly off the same
 *   derived-state snapshot rather than re-scanning `visibleItemsInfo` a second time.
 * - Items are a single `Text` at a fixed height with pre-formatted strings (no per-item
 *   `String.format` calls while flinging, no extra `Box` wrapper per row).
 *
 * The settle-watching coroutine is started once (`LaunchedEffect(listState)`, and `listState`'s
 * identity never changes across recompositions) and then runs for the wheel's whole lifetime.
 * [onSelect] and [selected] are read through [rememberUpdatedState] inside it rather than closed
 * over directly — otherwise the coroutine would keep calling the stale lambda/value captured at
 * first launch, which is what caused an earlier sibling-column desync bug (scrolling one wheel
 * could silently revert the other back to an old value). Every wheel column must read its
 * sibling's current value fresh, not the value from whenever this coroutine happened to start.
 */
@Composable
fun WheelColumn(
    values: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    format: (Int) -> String = { "%02d".format(it) }
) {
    val size = values.size
    val virtualCount = size * CYCLES
    val startIndex = remember(values) {
        val selectedOffset = values.indexOf(selected).coerceAtLeast(0)
        (virtualCount / 2 / size) * size + selectedOffset
    }
    val listState = rememberLazyListState(startIndex)
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val sidePadding = ITEM_HEIGHT * (VISIBLE_COUNT / 2)
    val formatted = remember(values, format) { values.map(format) }

    val centeredIndex: State<Int?> = remember(listState) {
        derivedStateOf { centeredVirtualIndex(listState) }
    }

    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { scrolling -> !scrolling }
            .collect {
                val value = centeredIndex.value?.let { values[it % size] } ?: return@collect
                if (value != currentSelected) currentOnSelect(value)
            }
    }

    Box(modifier = modifier.height(ITEM_HEIGHT * VISIBLE_COUNT), contentAlignment = Alignment.Center) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = sidePadding),
            modifier = Modifier.fillMaxHeight()
        ) {
            items(virtualCount) { index ->
                val isSelected = index == centeredIndex.value
                Text(
                    formatted[index % size],
                    textAlign = TextAlign.Center,
                    style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(ITEM_HEIGHT).wrapContentHeight(Alignment.CenterVertically)
                )
            }
        }
    }
}

/** Index (into the wheel's virtual item list) of whichever row's center is closest to the viewport center. */
private fun centeredVirtualIndex(listState: LazyListState): Int? {
    val info = listState.layoutInfo
    if (info.visibleItemsInfo.isEmpty()) return null
    val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index
}

/** The highlighted band behind the middle row, shared by every wheel-picker composition. */
@Composable
fun WheelSelectionHighlight(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(ITEM_HEIGHT)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
    )
}

/**
 * Two side-by-side looping wheels — minutes `00-59` and seconds `00-59`, each independently
 * infinite-scrolling — composed as `MM:SS`. Backs Rest timer and Prep timer.
 */
@Composable
fun DurationWheelPicker(
    totalSeconds: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minSeconds: Int = 0,
    maxSeconds: Int = 59 * 60 + 59,
    columnWidth: Dp = 64.dp
) {
    val clamped = totalSeconds.coerceIn(minSeconds, maxSeconds)
    val minutes = clamped / 60
    val seconds = clamped % 60

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        WheelSelectionHighlight(modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            WheelColumn(
                values = (0..59).toList(),
                selected = minutes,
                onSelect = { m -> onChange((m * 60 + seconds).coerceIn(minSeconds, maxSeconds)) },
                modifier = Modifier.width(columnWidth)
            )
            Text(":", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            WheelColumn(
                values = (0..59).toList(),
                selected = seconds,
                onSelect = { s -> onChange((minutes * 60 + s).coerceIn(minSeconds, maxSeconds)) },
                modifier = Modifier.width(columnWidth)
            )
        }
    }
}

/**
 * Two side-by-side looping wheels — hour `00-23` and minute `00-59`, 24-hour, no AM/PM — editing a
 * `"HH:mm"` string. Backs the daily reminder time.
 */
@Composable
fun ClockWheelPicker(
    time24: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    columnWidth: Dp = 64.dp
) {
    val (hour, minute) = remember(time24) {
        val parts = time24.split(":")
        (parts.getOrNull(0)?.toIntOrNull() ?: 7) to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        WheelSelectionHighlight(modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            WheelColumn(
                values = (0..23).toList(),
                selected = hour,
                onSelect = { h -> onChange("%02d:%02d".format(h, minute)) },
                modifier = Modifier.width(columnWidth)
            )
            Text(":", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            WheelColumn(
                values = (0..59).toList(),
                selected = minute,
                onSelect = { m -> onChange("%02d:%02d".format(hour, m)) },
                modifier = Modifier.width(columnWidth)
            )
        }
    }
}
