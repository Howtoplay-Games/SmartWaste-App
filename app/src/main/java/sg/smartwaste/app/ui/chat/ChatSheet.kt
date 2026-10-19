package sg.smartwaste.app.ui.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sg.smartwaste.app.AppContainer
import sg.smartwaste.app.data.ai.ChatTurn
import sg.smartwaste.app.ui.components.CircleIconButton
import sg.smartwaste.app.ui.components.appViewModel
import sg.smartwaste.app.ui.theme.SW

data class ChatUiState(
    val messages: List<ChatTurn> = listOf(
        ChatTurn(
            fromUser = false,
            text = "Hi! I'm the SmartWaste Assistant 👋 Ask me anything about recycling e-waste in Singapore or how to use the app.",
        )
    ),
    val sending: Boolean = false,
)

class ChatViewModel(private val c: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun send(text: String) {
        val q = text.trim()
        if (q.isEmpty() || _state.value.sending) return
        val history = _state.value.messages + ChatTurn(true, q)
        _state.update { it.copy(messages = history, sending = true) }
        viewModelScope.launch {
            // The greeting is UI-only, so don't send it as model context.
            val reply = c.ai.chat(history.drop(1))
            _state.update { it.copy(messages = it.messages + reply, sending = false) }
        }
    }
}

private val suggestions = listOf(
    "How do I earn more points?",
    "Where do I recycle batteries?",
    "How do I wipe my old phone?",
    "Can I sell a broken laptop?",
    "What goes in which compartment?",
    "How do I dispose of a fridge?",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSheet(onDismiss: () -> Unit) {
    // Activity-scoped so the conversation survives closing and reopening the sheet.
    val vm = appViewModel { ChatViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var input by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()

    LaunchedEffect(s.messages.size, s.sending) {
        val count = s.messages.size + if (s.sending) 1 else 0
        if (count > 0) list.animateScrollToItem(count - 1)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = SW.Bg,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.88f).imePadding()) {
            // Header
            Row(
                Modifier.fillMaxWidth().background(SW.Ink).padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(SW.Emerald, SW.Electric))),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Forum, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("SmartWaste Assistant", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(SW.Lime))
                        Spacer(Modifier.size(6.dp))
                        Text("AI support · e-waste & app help", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall)
                    }
                }
                CircleIconButton(Icons.Rounded.Close, "Close", onDismiss, container = Color.White.copy(alpha = 0.12f), content = Color.White, size = 38.dp, iconSize = 18.dp)
            }

            LazyColumn(
                state = list,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(s.messages) { _, m -> Bubble(m) }
                if (s.sending) item { TypingBubble() }
            }

            if (s.messages.size <= 2) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { q ->
                        Text(
                            q, color = SW.Ink, style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White).clickable { vm.send(q) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(26.dp)).background(Color.White).padding(horizontal = 18.dp, vertical = 15.dp),
                ) {
                    if (input.isEmpty()) Text("Ask about e-waste or the app…", color = SW.Muted, style = MaterialTheme.typography.bodyLarge)
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = SW.Ink),
                        cursorBrush = SolidColor(SW.Emerald),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { vm.send(input); input = "" }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.size(10.dp))
                CircleIconButton(
                    Icons.AutoMirrored.Rounded.Send, "Send", { vm.send(input); input = "" },
                    container = if (input.isBlank()) SW.Line else SW.Ink, content = Color.White, size = 52.dp,
                )
            }
        }
    }
}

@Composable
private fun Bubble(m: ChatTurn) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start) {
        Text(
            m.text,
            color = if (m.fromUser) Color.White else SW.Ink,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp, topEnd = 22.dp,
                        bottomStart = if (m.fromUser) 22.dp else 6.dp,
                        bottomEnd = if (m.fromUser) 6.dp else 22.dp,
                    )
                )
                .background(if (m.fromUser) SW.Ink else Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun TypingBubble() {
    val t = rememberInfiniteTransition(label = "typing")
    Row(
        Modifier.clip(RoundedCornerShape(22.dp)).background(Color.White).padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(3) { i ->
            val a by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(600, delayMillis = i * 150), RepeatMode.Reverse), label = "dot$i")
            Box(Modifier.size(8.dp).alpha(a).clip(CircleShape).background(SW.Emerald))
        }
    }
}
