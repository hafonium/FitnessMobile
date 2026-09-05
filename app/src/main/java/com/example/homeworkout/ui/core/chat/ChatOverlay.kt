package com.example.homeworkout.ui.core.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.homeworkout.R
import com.example.homeworkout.domain.models.chat.ChatMessage
import com.example.homeworkout.domain.models.chat.ChatSession
import com.example.homeworkout.domain.models.enums.ChatMessageRole
import com.example.homeworkout.ui.App
import com.example.homeworkout.ui.theme.AppGradients
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.CardWhite
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.PageBackground
import com.example.homeworkout.ui.theme.SheetTopShape
import com.example.homeworkout.ui.theme.SlateGray
import com.example.homeworkout.ui.theme.SuccessGreen
import com.mikepenz.markdown.m3.Markdown
import kotlin.math.roundToInt

private const val BUBBLE_SIZE_DP = 64

/**
 * Global, draggable, translucent floating chat bubble + popup for the in-app Groq-backed fitness
 * assistant. Mount once at the top of HomeWorkoutApp (outside ScreenNavigator) so it floats above
 * every screen. See docs/chatbot-feature.md for the full design.
 */
@Composable
fun ChatOverlay() {
    val appInstance = LocalContext.current.applicationContext as App
    val viewModel: ChatViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ChatViewModel(
                    getChatSessionsUseCase = appInstance.getChatSessionsUseCase,
                    getChatMessagesUseCase = appInstance.getChatMessagesUseCase,
                    createChatSessionUseCase = appInstance.createChatSessionUseCase,
                    sendChatMessageUseCase = appInstance.sendChatMessageUseCase,
                    deleteChatSessionUseCase = appInstance.deleteChatSessionUseCase,
                    chatPanelController = appInstance.chatPanelController
                )
            }
        }
    )

    // Open/closed state lives on ChatPanelController (not local remember) so a chat-triggered
    // navigation to Create Workout can reopen the panel on return - see docs/chatbot-feature.md.
    val isOpen by appInstance.chatPanelController.isOpen.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxXPx = with(density) { (maxWidth - BUBBLE_SIZE_DP.dp).toPx() }.coerceAtLeast(0f)
        val maxYPx = with(density) { (maxHeight - BUBBLE_SIZE_DP.dp).toPx() }.coerceAtLeast(0f)

        ChatBubble(
            initialOffset = Offset(x = (maxXPx - 12f).coerceAtLeast(0f), y = maxYPx * 0.6f),
            bounds = Offset(maxXPx, maxYPx),
            onClick = { appInstance.chatPanelController.open() }
        )

        if (isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { appInstance.chatPanelController.close() }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                ChatPanel(
                    viewModel = viewModel,
                    onDismiss = { appInstance.chatPanelController.close() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.94f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { /* consume taps so they don't fall through to the scrim */ }
                )
            }
        }
    }
}

/** Small circular avatar image reused for the FAB, the panel header, message bubbles and the
 * empty-state illustration, so the assistant has one consistent "face" throughout the feature. */
@Composable
private fun PilotAvatar(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.chatbot_avatar),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
private fun ChatBubble(initialOffset: Offset, bounds: Offset, onClick: () -> Unit) {
    var offset by remember { mutableStateOf(initialOffset) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .size(BUBBLE_SIZE_DP.dp)
            // Two independent detectors: detectDragGestures only fires onDragStart/onDragEnd once
            // the platform touch slop is crossed, so a plain tap (which stays inside that slop)
            // never reaches those callbacks at all - it has to be recognized separately via
            // detectTapGestures, or the bubble would never open.
            .pointerInput(bounds) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offset = Offset(
                        x = (offset.x + dragAmount.x).coerceIn(0f, bounds.x),
                        y = (offset.y + dragAmount.y).coerceIn(0f, bounds.y)
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .shadow(elevation = 10.dp, shape = CircleShape, ambientColor = BrandBlue, spotColor = BrandBlue)
            .clip(CircleShape)
            .border(width = 2.5.dp, color = Color.White, shape = CircleShape)
    ) {
        PilotAvatar(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ChatPanel(viewModel: ChatViewModel, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var showSessionList by remember { mutableStateOf(viewModel.activeSessionId.value == null) }

    Surface(modifier = modifier, color = PageBackground, shape = SheetTopShape, shadowElevation = 12.dp) {
        Column(modifier = Modifier.fillMaxSize()) {
            val onNewChat: () -> Unit = {
                viewModel.startNewSession()
                showSessionList = false
            }
            ChatPanelHeader(
                showBack = !showSessionList,
                onBack = { showSessionList = true },
                onNewChat = onNewChat,
                onClose = onDismiss
            )
            if (showSessionList) {
                SessionListView(
                    viewModel = viewModel,
                    onSessionSelected = { sessionId ->
                        viewModel.openSession(sessionId)
                        showSessionList = false
                    },
                    onNewChat = onNewChat
                )
            } else {
                MessageThreadView(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun ChatPanelHeader(showBack: Boolean, onBack: () -> Unit, onNewChat: () -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppGradients.PromoCard)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.History, contentDescription = "Chat history", tint = Color.White)
            }
        } else {
            PilotAvatar(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(width = 1.5.dp, color = Color.White.copy(alpha = 0.85f), shape = CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Fitness Assistant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Online",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
        IconButton(onClick = onNewChat) {
            Icon(Icons.Filled.Add, contentDescription = "New chat", tint = Color.White)
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Close chat", tint = Color.White)
        }
    }
}

@Composable
private fun SessionListView(viewModel: ChatViewModel, onSessionSelected: (Long) -> Unit, onNewChat: () -> Unit) {
    val sessions by viewModel.sessions.collectAsState()

    if (sessions.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PilotAvatar(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(elevation = 10.dp, shape = CircleShape, ambientColor = BrandBlue, spotColor = BrandBlue)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ask me anything about your workouts, form or recovery.",
                style = MaterialTheme.typography.bodyMedium,
                color = SlateGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            NewChatButton(onClick = onNewChat)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    NewChatButton(onClick = onNewChat)
                }
            }
            items(sessions, key = { it.sessionId }) { session ->
                SessionRow(
                    session = session,
                    onClick = { onSessionSelected(session.sessionId) },
                    onDelete = { viewModel.deleteSession(session.sessionId) }
                )
            }
        }
    }
}

@Composable
private fun NewChatButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppGradients.PrimaryButton)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("New chat", color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SessionRow(session: ChatSession, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.bodyLarge,
                color = InkBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = relativeTimeLabel(session.updatedAt), style = MaterialTheme.typography.bodySmall, color = SlateGray)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete chat", tint = SlateGray)
        }
    }
}

@Composable
private fun MessageThreadView(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(messages.size, isSending) {
        val lastIndex = messages.size - 1 + if (isSending) 1 else 0
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.messageId }) { message -> MessageBubble(message) }
            if (isSending) {
                item(key = "typing-indicator") { TypingIndicatorBubble() }
            }
        }
        ChatInputBar(
            text = inputText,
            onTextChange = { inputText = it },
            enabled = !isSending,
            onSend = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                }
            }
        )
    }
}

private fun bubbleShape(isUser: Boolean) = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = if (isUser) 16.dp else 4.dp,
    bottomEnd = if (isUser) 4.dp else 16.dp
)

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatMessageRole.USER
    val shape = bubbleShape(isUser)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            PilotAvatar(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(width = 1.dp, color = HairlineGray, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = shape,
                    ambientColor = if (isUser) BrandBlue else Color.Black,
                    spotColor = if (isUser) BrandBlue else Color.Black
                )
                .clip(shape)
                .background(if (isUser) AppGradients.PrimaryButton else SolidColor(CloudGray))
        ) {
            if (isUser) {
                val userColorScheme = MaterialTheme.colorScheme.copy(
                    onSurface = Color.White,
                    onBackground = Color.White,
                    primary = Color.White
                )
                MaterialTheme(colorScheme = userColorScheme) {
                    CompositionLocalProvider(LocalContentColor provides Color.White) {
                        Markdown(
                            message.content,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            } else {
                Markdown(
                    message.content,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun TypingIndicatorBubble() {
    val shape = bubbleShape(false)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        PilotAvatar(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(width = 1.dp, color = HairlineGray, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .shadow(elevation = 2.dp, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(shape)
                .background(CloudGray)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            TypingDots(color = SlateGray)
        }
    }
}

@Composable
private fun TypingDots(color: Color) {
    val transition = rememberInfiniteTransition()
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val dotScale by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, delayMillis = index * 150, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(7.dp)
                    .scale(dotScale)
                    .alpha(0.4f + 0.6f * dotScale)
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
private fun ChatInputBar(text: String, onTextChange: (String) -> Unit, enabled: Boolean, onSend: () -> Unit) {
    Column(modifier = Modifier.background(CardWhite)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(HairlineGray)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about your workout...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                enabled = enabled,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CloudGray,
                    unfocusedContainerColor = CloudGray,
                    disabledContainerColor = CloudGray,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            val canSend = enabled && text.isNotBlank()
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (canSend) AppGradients.PrimaryButton else SolidColor(HairlineGray))
                    .clickable(enabled = canSend, onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

private fun relativeTimeLabel(timestampMillis: Long): String {
    val minutes = (System.currentTimeMillis() - timestampMillis) / 60_000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60}h ago"
        else -> "${minutes / (24 * 60)}d ago"
    }
}
