# 1단계 앱 설계서: Checklist Mini (Compose State/UDF)
## 1) 목표
Compose의 상태/재구성(recomposition), state hoisting(UDF), rememberSaveable 감각을 “기능으로 강제” 

## 2) 화면
ChecklistScreen (단일 화면)

## 3) 기능 요구사항 (Must)
- 항목 추가: 텍스트 입력 → “추가” → 리스트에 즉시 반영
- 완료 토글: 체크박스/스위치로 완료 상태 변경
- 삭제: 항목 삭제
- 요약: “전체/완료/미완료 카운트” 표시
- 구성 변경(회전 등) 후에도 입력 중 텍스트/스크롤/체크 상태가 합리적으로 유지 

## 4) 데이터 모델
ChecklistItem(id: String, title: String, isDone: Boolean, createdAt: Long)

## 5) UI 상태/이벤트
- UiState(items: List<ChecklistItem>, input: String, filter: Filter, error: String?)
- UiEvent(AddClicked, InputChanged, ToggleDone(id), Delete(id), FilterChanged)

## 6) 규칙 (Must)
- mutableListOf()를 state로 직접 쓰지 말 것(관측 안 돼서 UI가 틀어질 수 있음). 
- 상태는 위에서 아래로, 이벤트는 아래에서 위로(UDF). 
- remember vs rememberSaveable을 의도적으로 구분 적용(저장해야 하는 UI 상태만 save). 

## 7) 완료 기준
- “추가/토글/삭제” 모두 즉시 UI 반영 + 회전해도 UX가 망가지지 않음
- Screen은 UiState만으로 렌더링(임의 전역/싱글톤 상태 금지)
