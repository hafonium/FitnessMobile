package com.example.homeworkout.ui.core.trainingplan

import android.content.Intent
import android.graphics.Color.parseColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.R
import com.example.homeworkout.domain.models.training.StructuredProgramProgress
import com.example.homeworkout.domain.models.training.StructuredTrainingProgram
import com.example.homeworkout.domain.models.training.StructuredTrainingSession
import com.example.homeworkout.domain.models.training.StructuredTrainingWeek
import com.example.homeworkout.domain.models.training.TrainingEnrollmentStatus
import com.example.homeworkout.domain.models.training.TrainingProgramKind
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.buttons.AppButtonVariant
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.CardWhite
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.PageBackground
import com.example.homeworkout.ui.theme.SlateGray
import com.example.homeworkout.ui.theme.SuccessGreen

private val Canvas = PageBackground
private val SurfaceCard = CardWhite
private val SurfaceSubcard = CloudGray
private val Border = HairlineGray
private val Accent = BrandBlue
private val Primary = InkBlack
private val Secondary = SlateGray
private val Gold = Color(0xFFFBBF24)
private val Success = SuccessGreen

@Composable
fun StructuredTrainingPlanScreen(
    viewModel: StructuredTrainingPlanViewModel,
    onNavigateBack: () -> Unit,
    onStartSession: (String, String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val program = state.program
    val progress = state.progress
    val context = LocalContext.current
    var bookmarked by remember { mutableStateOf(false) }
    var showProgramSummary by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize().background(Canvas), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent)
        }
        return
    }
    if (program == null) {
        Box(Modifier.fillMaxSize().background(Canvas), contentAlignment = Alignment.Center) {
            Text(state.errorMessage ?: "Program unavailable", color = Primary)
        }
        return
    }

    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            StickyProgramCta(
                program = program,
                progress = progress,
                onEnroll = { viewModel.enroll(onStartSession) },
                onStart = { session, week -> viewModel.start(session.id, week, onStartSession) },
                onShowSummary = { showProgramSummary = true }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "hero", contentType = "hero") {
                ProgramHero(
                    program = program,
                    bookmarked = bookmarked,
                    onBack = onNavigateBack,
                    onBookmark = { bookmarked = !bookmarked },
                    onShare = {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, "${program.title} — ${program.durationWeeks} week training program"),
                                "Share program"
                            )
                        )
                    }
                )
            }
            item(key = "goal", contentType = "info") {
                InfoCard("PROGRAM GOAL", program.goal)
            }
            program.startingAbility?.let { ability ->
                item(key = "ability", contentType = "info") { InfoCard("RECOMMENDED STARTING ABILITY", ability) }
            }
            item(key = "intensity", contentType = "info") { IntensityGuide(program) }
            items(program.weeks, key = { it.id }, contentType = { "week" }) { week ->
                val phase = program.phases.firstOrNull { it.weekStart == week.weekNumber }
                if (phase != null) PhaseHeader(phase.index, phase.title, phase.goal)
                WeekAccordion(
                    week = week,
                    progress = progress,
                    expanded = week.weekNumber in state.expandedWeeks,
                    onToggle = { viewModel.toggleWeek(week.weekNumber) },
                    onStart = { session -> viewModel.start(session.id, week.weekNumber, onStartSession) }
                )
            }
            item(key = "maintenance", contentType = "info") { MaintenanceCard(program) }
            if (program.rules.isNotEmpty()) {
                item(key = "rules-title") { SectionTitle("Important training rules") }
                items(program.rules, key = { "rule-${it.title}" }, contentType = { "rule" }) { rule ->
                    InfoCard(rule.title.uppercase(), rule.description)
                }
            }
            if (progress?.status == TrainingEnrollmentStatus.ACTIVE) {
                item(key = "repeat") {
                    AppButton(
                        text = "REPEAT WEEK ${progress.currentWeekNumber}",
                        onClick = viewModel::repeatCurrentWeek,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        variant = AppButtonVariant.Outlined
                    )
                }
            }
        }
    }

    if (showProgramSummary) {
        val requiredSessions = program.weeks.sumOf { week -> week.sessions.count { !it.isOptional } }
        AlertDialog(
            onDismissRequest = { showProgramSummary = false },
            containerColor = SurfaceCard,
            title = { Text("PROGRAM COMPLETE", color = Primary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("You completed ${program.title}.", color = Primary)
                    Text(
                        "${program.durationWeeks} weeks • $requiredSessions required sessions",
                        color = Secondary
                    )
                    Text(program.maintenanceGuidance, color = Secondary)
                }
            },
            confirmButton = {
                AppButton(text = "DONE", onClick = { showProgramSummary = false })
            }
        )
    }
}

@Composable
private fun ProgramHero(
    program: StructuredTrainingProgram,
    bookmarked: Boolean,
    onBack: () -> Unit,
    onBookmark: () -> Unit,
    onShare: () -> Unit
) {
    val image = if (program.kind == TrainingProgramKind.WALKING) R.drawable.discovery_walking_plan else R.drawable.discovery_running_plan
    Box(Modifier.fillMaxWidth().height(350.dp)) {
        Image(painterResource(image), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = .25f), .55f to Color.Black.copy(alpha = .55f), 1f to Color.Black
                )
            )
        )
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CircleAction(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircleAction(if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Bookmark", onBookmark)
                CircleAction(Icons.Default.Share, "Share", onShare)
            }
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("${program.durationWeeks} WEEKS PROGRAM", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(program.title, color = Color.White, fontSize = 28.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MetaChip("${program.durationWeeks} Weeks")
                MetaChip(program.sessionsPerWeek)
                MetaChip(program.level)
            }
        }
    }
}

@Composable
private fun CircleAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = .55f), CircleShape)) {
        Icon(icon, contentDescription = description, tint = Color.White)
    }
}

@Composable private fun MetaChip(text: String) {
    Text(
        text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(Color.Black.copy(alpha = .55f), CircleShape).padding(horizontal = 9.dp, vertical = 6.dp)
    )
}

@Composable private fun InfoCard(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(SurfaceCard, RoundedCornerShape(18.dp))
            .border(1.dp, Border, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(title, color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = .4.sp)
        Text(body, color = Primary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun IntensityGuide(program: StructuredTrainingProgram) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(SurfaceCard, RoundedCornerShape(18.dp))
            .border(1.dp, Border, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("Intensity guide", horizontalPadding = 0.dp)
        program.intensities.forEach { intensity ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 4.dp).size(10.dp).background(Color(parseColor(intensity.colorHex)), CircleShape))
                Column(Modifier.weight(1f)) {
                    Text("${intensity.name}  •  RPE ${intensity.rpe}", color = Primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(intensity.description, color = Secondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable private fun PhaseHeader(index: Int, title: String, goal: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text("PHASE $index", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(title, color = Primary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(goal, color = Secondary, fontSize = 13.sp)
    }
}

@Composable
private fun WeekAccordion(
    week: StructuredTrainingWeek,
    progress: StructuredProgramProgress?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onStart: (StructuredTrainingSession) -> Unit
) {
    val completed = week.sessions.filterNot { it.isOptional }.all { it.id in progress?.completedSessions.orEmpty() }
    val current = progress?.status == TrainingEnrollmentStatus.ACTIVE && week.weekNumber == progress.currentWeekNumber
    val duration = durationLabel(week.durationMinMinutes, week.durationMaxMinutes)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(SurfaceCard, RoundedCornerShape(18.dp))
            .border(if (current) 1.5.dp else 1.dp, if (current) Accent else Border, RoundedCornerShape(18.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("WEEK ${week.weekNumber}", color = if (current) Accent else Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(when { completed -> "COMPLETED"; current -> "CURRENT"; else -> "UPCOMING" }, color = if (completed) Success else Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(week.title, color = Primary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("$duration • ${week.sessions.size} sessions", color = Secondary, fontSize = 12.sp)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (expanded) "Collapse week" else "Expand week", tint = Primary)
        }
        if (expanded) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                week.sessions.forEach { session ->
                    SessionRow(
                        session,
                        completed = session.id in progress?.completedSessions.orEmpty(),
                        active = progress?.activeSessionId == session.id,
                        enabled = progress?.status == TrainingEnrollmentStatus.ACTIVE && week.weekNumber <= progress.currentWeekNumber,
                        onStart = { onStart(session) }
                    )
                }
                week.goal?.let { SupportingNote("GOAL", it, Accent) }
                week.coachTip?.let { SupportingNote("COACH TIP", it, Accent, showBulb = true) }
                week.milestone?.let { SupportingNote("MILESTONE", it, Gold, showStar = true) }
                week.note?.let { SupportingNote("NOTE", it, Secondary) }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: StructuredTrainingSession,
    completed: Boolean,
    active: Boolean,
    enabled: Boolean,
    onStart: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().background(SurfaceSubcard, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled && !completed, onClick = onStart).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(30.dp).background(Accent.copy(alpha = .18f), CircleShape), contentAlignment = Alignment.Center) {
                Text(session.sessionIndex.toString(), color = Accent, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(session.title, color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(durationLabel(session.durationMinMinutes, session.durationMaxMinutes), color = Secondary, fontSize = 12.sp)
                    if (session.isOptional) Text("OPTIONAL", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Icon(
                when { completed -> Icons.Default.CheckCircle; else -> Icons.Default.PlayArrow },
                contentDescription = when { completed -> "Completed"; active -> "Resume session"; else -> "Start session" },
                tint = when { completed -> Success; active -> Accent; enabled -> Primary; else -> Secondary }
            )
        }
        if (session.steps.size > 1) Timeline(session, session.steps.sumOf { it.durationSeconds })
    }
}

@Composable private fun Timeline(session: StructuredTrainingSession, totalSeconds: Int) {
    Row(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        session.steps.forEach { step ->
            val color = when (step.intensity) {
                "EASY", "EASY_RUN" -> Success
                "BRISK", "STEADY_RUN" -> Accent
                "POWER", "MODERATE" -> Color(0xFFF59E0B)
                else -> Color(0xFF6B7280)
            }
            Box(Modifier.weight(step.durationSeconds.toFloat() / totalSeconds.coerceAtLeast(1)).fillMaxSize().background(color))
        }
    }
}

@Composable private fun SupportingNote(label: String, text: String, color: Color, showBulb: Boolean = false, showStar: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().background(color.copy(alpha = .09f), RoundedCornerShape(10.dp)).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showBulb) Icon(Icons.Default.Lightbulb, null, tint = color, modifier = Modifier.size(18.dp))
        if (showStar) Icon(Icons.Default.Star, null, tint = color, modifier = Modifier.size(18.dp))
        Column {
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text, color = Primary, fontSize = 12.sp)
        }
    }
}

@Composable private fun MaintenanceCard(program: StructuredTrainingProgram) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(SurfaceCard, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(program.maintenanceTitle, horizontalPadding = 0.dp)
        Text(program.maintenanceGuidance, color = Secondary, fontSize = 13.sp)
        program.maintenanceDays.forEach { Text("• $it", color = Primary, fontSize = 13.sp) }
    }
}

@Composable private fun SectionTitle(text: String, horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp) {
    Text(text, color = Primary, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = horizontalPadding))
}

@Composable
private fun StickyProgramCta(
    program: StructuredTrainingProgram,
    progress: StructuredProgramProgress?,
    onEnroll: () -> Unit,
    onStart: (StructuredTrainingSession, Int) -> Unit,
    onShowSummary: () -> Unit
) {
    val next = program.weeks.asSequence().flatMap { week -> week.sessions.asSequence().map { week.weekNumber to it } }
        .firstOrNull { (_, session) -> session.id !in progress?.completedSessions.orEmpty() && !session.isOptional }
    val label = when (progress?.status) {
        null, TrainingEnrollmentStatus.NOT_ENROLLED -> "START PROGRAM"
        TrainingEnrollmentStatus.COMPLETED -> "VIEW PROGRAM SUMMARY"
        TrainingEnrollmentStatus.ACTIVE -> if (progress.activeSessionId != null) "CONTINUE SESSION" else "START SESSION ${next?.second?.sessionIndex ?: 1}"
    }
    Column(Modifier.fillMaxWidth().background(Canvas.copy(alpha = .96f)).navigationBarsPadding().padding(12.dp)) {
        AppButton(
            text = label,
            onClick = {
                when (progress?.status) {
                    null, TrainingEnrollmentStatus.NOT_ENROLLED -> onEnroll()
                    TrainingEnrollmentStatus.ACTIVE -> {
                        val active = progress.activeSessionId?.let { id ->
                            program.weeks.asSequence().flatMap { w -> w.sessions.asSequence().map { w.weekNumber to it } }.firstOrNull { it.second.id == id }
                        }
                        (active ?: next)?.let { onStart(it.second, it.first) }
                    }
                    TrainingEnrollmentStatus.COMPLETED -> onShowSummary()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun durationLabel(min: Int, max: Int): String = if (min == max) "$min min" else "$min–$max min"
