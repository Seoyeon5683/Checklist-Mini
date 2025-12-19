package com.seoyeonkim.checklist_mini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

// 1. [데이터 모델] 체크리스트의 한 줄(항목)을 담당하는 설계도입니다.
data class CheckItem(
    val id: Long = System.currentTimeMillis(), // 각 항목을 구별하는 고유 번호 (시간으로 생성)
    val title: String,                         // 할 일 내용
    val isChecked: Boolean = false             // 완료 여부 (기본값은 false)
)

// 2. [ViewModel] 화면의 "두뇌" 역할입니다. 데이터를 저장하고 관리합니다.
// 화면이 회전되어도 이 안에 있는 데이터는 사라지지 않습니다.
class ChecklistViewModel : ViewModel() {
    // 체크리스트 목록 (초기값은 비어있는 리스트)
    private val _items = MutableStateFlow<List<CheckItem>>(emptyList())
    val items: StateFlow<List<CheckItem>> = _items.asStateFlow()

    // 입력창에 쓰고 있는 텍스트
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // 사용자가 글자를 칠 때마다 호출되어 값을 업데이트합니다.
    fun onInputChange(newText: String) {
        _inputText.value = newText
    }

    // [추가] 버튼을 누르면 호출됩니다.
    fun addItem() {
        if (_inputText.value.isNotBlank()) { // 내용이 있을 때만
            _items.update { currentList ->
                // 기존 리스트 뒤에 새로운 항목을 붙입니다.
                currentList + CheckItem(id = System.currentTimeMillis(), title = _inputText.value)
            }
            _inputText.value = "" // 입력창을 다시 비워줍니다.
        }
    }

    // [체크/해제] 항목을 누르면 완료 상태를 뒤집습니다.
    fun toggleItem(item: CheckItem) {
        _items.update { currentList ->
            currentList.map {
                // ID가 같은 항목을 찾아서 isChecked를 반대로 바꿉니다.
                if (it.id == item.id) it.copy(isChecked = !it.isChecked) else it
            }
        }
    }

    // [삭제] 휴지통 버튼을 누르면 호출됩니다.
    fun deleteItem(item: CheckItem) {
        _items.update { currentList ->
            // ID가 같지 않은 것들만 남깁니다 (즉, 삭제할 항목을 제외함).
            currentList.filter { it.id != item.id }
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
    // ViewModel에 있는 데이터를 "구독"합니다. 데이터가 바뀌면 화면이 자동으로 다시 그려집니다.
    val items by viewModel.items.collectAsState()
    val inputText by viewModel.inputText.collectAsState()

    // 요약 정보를 계산합니다.
    val totalCount = items.size
    val doneCount = items.count { it.isChecked }
    val activeCount = totalCount - doneCount

    // Column: 요소를 세로로 배치합니다.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 상단 요약 카드
        SummaryView(total = totalCount, done = doneCount, active = activeCount)

        Spacer(modifier = Modifier.height(16.dp)) // 여백

        // 리스트 (스크롤 가능한 영역)
        LazyColumn(
            modifier = Modifier.weight(1f), // 남은 공간을 꽉 채웁니다.
            verticalArrangement = Arrangement.spacedBy(8.dp) // 항목 간 간격
        ) {
            items(items = items, key = { it.id }) { item ->
                ChecklistItemRow(
                    item = item,
                    onToggle = { viewModel.toggleItem(item) },
                    onDelete = { viewModel.deleteItem(item) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 하단 입력창
        InputArea(
            text = inputText,
            onTextChange = viewModel::onInputChange,
            onAdd = viewModel::addItem
        )
    }
}

// [UI 컴포넌트] 요약 정보를 보여주는 카드
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
            horizontalArrangement = Arrangement.SpaceEvenly // 가로로 균등 배치
        ) {
            Text("전체: $total")
            Text("완료: $done", color = MaterialTheme.colorScheme.primary) // 파란색 계열
            Text("미완료: $active", color = MaterialTheme.colorScheme.error) // 빨간색 계열
        }
    }
}

// [UI 컴포넌트] 리스트의 한 줄(체크박스 + 텍스트 + 삭제버튼)
@Composable
fun ChecklistItemRow(
    item: CheckItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 체크박스
            Checkbox(checked = item.isChecked, onCheckedChange = { onToggle() })

            // 할 일 텍스트
            Text(
                text = item.title,
                modifier = Modifier.weight(1f), // 중간 공간 차지
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null, // 완료 시 취소선
                color = if (item.isChecked) Color.Gray else Color.Black
            )

            // 삭제 아이콘 버튼
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray)
            }
        }
    }
}

// [UI 컴포넌트] 입력창과 추가 버튼
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