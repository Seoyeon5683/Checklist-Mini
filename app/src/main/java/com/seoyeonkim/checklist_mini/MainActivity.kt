package com.seoyeonkim.checklist_mini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seoyeonkim.checklist_mini.ui.theme.CheckListMiniTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

// 1. [데이터 모델] 체크리스트의 한 줄(항목)을 담당하는 설계도입니다.
data class CheckItem(
    val id: String = UUID.randomUUID().toString(),      // ID
    val title: String,                                  // 할 일 내용
    val isChecked: Boolean = false,                     // 완료 여부 (기본값은 false)
    val createdAt: Long = System.currentTimeMillis()    // 생성 시간
)

enum class Filter { All, Done, Active }

data class UiState(
    val items: List<CheckItem> = emptyList(),
    val input: String = "",
    val filter: Filter = Filter.All,
    val error: String? = null,
)

sealed interface UiEvent {
    data class InputChanged(val value: String) : UiEvent
    data object AddClicked : UiEvent
    data class ToggleDone(val id: String) : UiEvent
    data class Delete(val id: String) : UiEvent
    data class FilterChanged(val filter: Filter) : UiEvent
    data object ErrorConsumed : UiEvent
}

// 2. [ViewModel] 화면의 "두뇌" 역할입니다. 데이터를 저장하고 관리합니다.
// 화면이 회전되어도 이 안에 있는 데이터는 사라지지 않습니다.
class ChecklistViewModel : ViewModel() {
    // 체크리스트 목록 (초기값은 비어있는 리스트)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.InputChanged -> {
                _uiState.update { it.copy(input = event.value, error = null) }
            }

            is UiEvent.AddClicked -> {
                val text = uiState.value.input.trim()
                if (text.isBlank()) {
                    _uiState.update { it.copy(error = "할 일을 입력해줘.") }
                } else {
                    val newItem = CheckItem(
                        id = UUID.randomUUID().toString(),
                        title = text,
                        isChecked = false,
                        createdAt = System.currentTimeMillis()
                    )

                    _uiState.update { s ->
                        s.copy(
                            items = s.items + newItem,
                            input = "",
                            error = null
                        )
                    }
                }
            }

            is UiEvent.ToggleDone -> {
                _uiState.update { s ->
                    s.copy(
                        items = s.items.map {
                            if (it.id == event.id) it.copy(isChecked = !it.isChecked) else it
                        }
                    )
                }
            }

            is UiEvent.Delete -> {
                _uiState.update { s ->
                    s.copy(
                        items = s.items.filter { it.id != event.id }
                    )
                }
            }

            is UiEvent.FilterChanged -> {
                _uiState.update { s -> s.copy(filter = event.filter) }

            }

            is UiEvent.ErrorConsumed -> {
                _uiState.update { s -> s.copy(error = null) }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 전체 화면 모드 (상단 상태바까지 영역 확장)
        setContent {
            CheckListMiniTheme {
                // Scaffold는 앱의 기본적인 뼈대(흰 배경 등)를 만들어줍니다.
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // innerPadding: 상태바 등에 가려지지 않게 여백을 줍니다.
                    // 여기서 우리가 만든 ChecklistScreen을 화면에 보여줍니다.
                    Box(modifier = Modifier.padding(innerPadding)) {
                        ChecklistScreen()
                    }
                }
            }
        }
    }
}

// 3. [UI 화면] 실제 눈에 보이는 화면을 구성하는 함수들입니다.
@Composable
fun ChecklistScreen(
    // viewModel을 여기서 가져옵니다. (앱이 실행될 때 자동으로 생성됨)
    viewModel: ChecklistViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    CheckListContent (
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

// 4. Content: "UiState만 보고 렌더링" + "UiEvent만 올림"
@Composable
fun CheckListContent(
    uiState: UiState,
    onEvent: (UiEvent) -> Unit
) {
    // rememberLazyListState는 화면 회전 등의 구성 변경 시에도 스크롤 위치를 유지해줍니다.
    val listState = rememberLazyListState()

    val totalCount = uiState.items.size
    val doneCount = uiState.items.count { it.isChecked }
    val activeCount = totalCount - doneCount

    val visibleItems = remember(uiState.items, uiState.filter) {
        when (uiState.filter) {
            Filter.All -> uiState.items
            Filter.Done -> uiState.items.filter { it.isChecked }
            Filter.Active -> uiState.items.filterNot { it.isChecked }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        SummaryView(total = totalCount, done = doneCount, active = activeCount)

        Spacer(modifier = Modifier.height(12.dp))

        FilterRow(
            selected = uiState.filter,
            onSelected = { onEvent(UiEvent.FilterChanged(it)) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = visibleItems, key = { it.id }) { item ->
                ChecklistItemRow(
                    item = item,
                    onToggle = { onEvent(UiEvent.ToggleDone(item.id)) },
                    onDelete = { onEvent(UiEvent.Delete(item.id)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        InputArea(
            text = uiState.input,
            onTextChange = { onEvent(UiEvent.InputChanged(it)) },
            onAdd = { onEvent(UiEvent.AddClicked) }
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun FilterRow(
    selected: Filter,
    onSelected: (Filter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == Filter.All,
            onClick = { onSelected(Filter.All) },
            label = { Text("전체") }
        )
        FilterChip(
            selected = selected == Filter.Done,
            onClick = { onSelected(Filter.Done) },
            label = { Text("완료") }
        )
        FilterChip(
            selected = selected == Filter.Active,
            onClick = { onSelected(Filter.Active) },
            label = { Text("미완료") }
        )
    }
}

@Composable
fun SummaryView(total: Int, done: Int, active: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("전체: $total")
            Text("완료: $done", color = MaterialTheme.colorScheme.primary)
            Text("미완료: $active", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun ChecklistItemRow(
    item: CheckItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.isChecked, onCheckedChange = { onToggle() })

            Text(
                text = item.title,
                modifier = Modifier.weight(1f),
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                color = if (item.isChecked) Color.Gray else Color.Black
            )

            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun InputArea(
    text: String,
    onTextChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("할 일을 입력하세요") },
            singleLine = true
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onAdd) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "추가")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChecklistPreview() {
    CheckListMiniTheme {
        ChecklistScreen()
    }
}