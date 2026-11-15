# 차 길들이기 화면 배치 명세 (SCREENS)

`feat/screens`는 이 문서대로 만든다. 컴포넌트와 토큰 이름은 [DESIGN.md](DESIGN.md) 5장을 보고, 코드는 `ui/components`에 있다.

기준 캔버스는 가로 1707×960dp(2560×1440 @240dpi)다. 1280×720dp와 1920×1080dp에서도 깨지지 않아야 한다. Pleos 홈 화면에서는 앱이 오른쪽 창(약 1707x1210 px)에서 돌아간다. Pleos Connect v2.1.0 에뮬레이터에서 확인했다.

참고 구현은 `ComponentGallery`(page 0 드릴, 1 홈, 2 상태 모음)이고 캡처는 `img/`에 있다.

## 0. 공통 프레임

```
┌──────────────────────────────────────────────── W ────────────────────────────────────────────────┐
│ G │ TopBar 104dp  (왼쪽 제목 · 오른쪽 칩/버튼)                                                │ G │
│ G │ SimBanner 56dp  (SourceMode == SIMULATION 일 때만)                                          │ G │
│ G │ 32dp                                                                                         │ G │
│ G │ Body (남는 높이 전부)                                                                        │ G │
│ G │ G (아래 여백)                                                                                │ G │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

- G(페이지 여백)는 `ChagilSpacing.pageGutter` 48dp. 폭이 1400dp보다 좁으면 `pageGutterCompact` 32dp.
- 폭 구간: Compact는 1400dp 미만(1280), Regular는 1400~1799(1707), Wide는 1800 이상(1920). Wide는 Regular와 같되 본문 최대 폭을 1824dp로 두고 가운데 정렬한다.
- 배경은 `colors.background`, 카드는 `surface`에 `ChagilShapes.large`.
- 테마는 `ChagilTheme(night = isSystemInDarkTheme())`. 앱이 낮과 밤을 직접 정하지 않고 AAOS 시스템 모드를 따른다.
- 시뮬레이션: `SimBanner()`는 TopBar 바로 아래에 전체 폭으로 둔다. 문구는 `SIM_BANNER_TEXT`로 고정돼 있고 내용은 "시뮬레이션 중이에요. 실제 차 신호가 아니에요"이다. TopBar 오른쪽에는 `SourceChip(simulation)`을 항상 둔다.
- 큰 버튼 규칙: 한 화면에 큰 버튼(Primary, Secondary, TopBarButton을 합쳐서)은 최대 2개다. 카드(DrillCard)는 버튼으로 세지 않는다.
- 쓰지 않는 말: 실패, 틀렸어요, 오답. 대신 "지금은 …예요", "괜찮아요"를 쓴다.
- 모든 `Text`는 `ChagilTheme.type.*` 스타일을 쓴다. 한국어 어절 줄바꿈이 스타일에 들어 있다.

## 1. 홈

```
TopBar: [차 길들이기 (headline)] [24] [차를 세워 두고, 진짜 레버로 연습해요 (bodyLarge, textSecondary, 1줄, weight 1)] [SafetyChip] [12] [SourceChip]
SimBanner (시뮬레이션일 때)
24dp
Row 카드 4장 (height 330dp, 간격 24, 각 weight 1)
24dp
도면 미니 카드 (남는 높이, surface, padding 32)
  ├ 왼쪽 Column 420dp: "내 차 도면"(headline) / "3 / 7칸에 불이 들어왔어요"(bodyLarge, textSecondary) / 12 / TopBarButton("도면 보기", MAP)
  └ 오른쪽 CockpitMap(weight 1, fillMaxHeight, showLabels=false)
```

카드 4장 (`DrillCard`):

| 퀘스트 | title | subtitle | meta | glyph | state |
|---|---|---|---|---|---|
| rain_drill | 폭우 드릴 | 전조등, 와이퍼, 앞유리 김서림 | 60초 · 4단계 | RAIN | 열림 |
| (세차) | 세차 대비 | 세차 전에 와이퍼 AUTO 끄기 | `{limitSec}초 · {n}단계` | CAR_WASH | 열림 |
| (성에) | 성에 낀 아침 | 앞유리, 뒷유리 열선, 핸들 열선 | `{limitSec}초 · {n}단계` | FROST | 열림 |
| (벨트) | 출발 준비 | 탄 사람 모두 벨트 확인 | 곧 열려요 | BELT | COMING_SOON |

- title과 subtitle은 퀘스트 JSON의 `title`, `subtitle`을 먼저 쓰고, 없으면 위 문구를 쓴다.
- state: `availability == coming_soon`이면 COMING_SOON. 현재 안전 단계가 퀘스트 `level`보다 낮으면 LOCKED_BY_SAFETY(칩은 "P에 두면 열려요", 누르면 2장의 안전 확인). 그 밖에는 OPEN.
- `best`: 저장된 최고 기록이 있으면 `"최고 {초}초"`(예: 최고 11.2초), 없으면 null.
- `label`: JSON에 `label`이 있으면 그대로 쓴다(예: 현대차 안내).
- 도면 미니 문구는 `"{켜진 칸} / {전체 칸}칸에 불이 들어왔어요"`. 0칸이면 "첫 드릴을 해 보세요".
- Compact(1280×720): 카드 높이 260dp, 카드 subtitle 1줄. 도면 미니 카드의 왼쪽 Column 폭 320dp. TopBar 부제 문구는 숨긴다.

## 2. 안전 확인

### 2-a. L1 확인 대화

L1_STOPPED_UNCONFIRMED 상태에서 OPEN이 아닌 카드를 눌렀을 때, 또는 드릴을 시작할 때 띄운다.

- 현재 화면 위에 scrim(`background` 80% 불투명)을 깔고 가운데에 `PromptCard`(최대 폭 880dp)를 둔다.
- icon `PARK`, iconTint `attention`, iconContainer `attentionContainer`
- title: P에 두셨나요?
- body: 기어를 P에 두고 시작해요. 차가 P를 읽을 수 없어서 한 번만 확인할게요.
- 버튼: Secondary "아직이에요"(닫기), Primary "P에 뒀어요"(`confirmParkedByDriver()`를 부른 뒤 드릴 시작)
- 음성: 기어를 P에 두셨나요?

### 2-b. L0 휴식 화면

L0_DRIVING 상태에서 보여 준다. 플랫폼이 앱 화면을 가리지 않는 일반 에뮬레이터를 위한 화면이다. Pleos 에뮬레이터에서는 기어가 D이면 Pleos가 앱 화면을 가리고 "주행 중에는 사용할 수 없습니다"를 띄운다(Pleos Connect v2.1.0에서 확인).

- TopBar와 카드 없이 전체 화면 `background` 위 가운데에 `PromptCard`를 둔다. 버튼은 없다.
- icon `PAUSE`, iconTint `rest`, iconContainer `restContainer`
- title: 주행 중에는 쉬어요
- body: 차를 세우고 P에 두면 이어서 할 수 있어요.
- 오른쪽 위에는 `SafetyChip(L0_DRIVING)`만 둔다. 소리와 음성은 내지 않는다(기획서: L0에서는 소리와 알림도 내지 않는다). 진행 중이던 드릴은 Paused(SAFETY)로 저장한다.

## 3. 드릴 진행 (Running)

```
TopBar: [폭우 드릴 · 2/4단계]                      [SourceChip] [TopBarButton 일시정지(PAUSE)] [TopBarButton 그만하기(CLOSE)]
SimBanner
32dp
Row(간격 32)
 ├ 왼쪽 Column (weight 1, fillMaxHeight)
 │   InstructionPanel(headline, voice, checking, readValue?)
 │   24dp
 │   ControlLocator(point = ControlPoint.fromKey(step.control), fillMaxWidth, weight 1)
 └ 오른쪽 단계 패널 (폭 460dp, fillMaxHeight, surface, large, padding 32)
     "할 일" (label, textSecondary)
     12dp
     StepList(steps)
     Spacer(weight 1)
     CountdownRing(remainingSec, limitSec) 가운데 정렬
```

- TopBar 제목은 `"{quest.title}"`, progress는 `"{현재 단계 번호}/{전체}단계"`(restore 단계도 센다).
- InstructionPanel
  - `headline` = step.instruction (예: 와이퍼를 한 단계 올려 보세요). 한 줄을 권장하고 최대 2줄.
  - `voice` = step.voice (예: 오른쪽 레버예요. 비 오는 날 쓰는 단계로 옮겨 보세요.). TTS와 같은 문장이다.
  - `checking` = true (StepStatus가 WAITING 또는 STABILIZING). STABILIZING에서도 표시를 그대로 둔다. 1초 안정 시간 동안 문구를 바꾸면 깜빡여 보이기 때문이다.
  - `readValue`: 값이 한 번이라도 바뀐 뒤에만 `"{부품 짧은 이름} {raw}"`(예: 와이퍼 INTERMITTENT_LEVEL_1). 처음에는 null.
  - tone NORMAL.
- 단계 상태 매핑: 지난 단계(PASSED)는 DONE, 현재 단계는 ACTIVE, 이후 단계는 PENDING.
- 단계를 통과하는 순간 해당 StepRow가 DONE으로 바뀐다. ControlLocator tone은 600ms 동안 SUCCESS로 보였다가 다음 단계 point로 바뀐다.
- `SELF_CHECK_NEEDED`(신호를 읽을 수 없음): InstructionPanel eyebrow는 "이 차는 이 신호를 읽을 수 없어요", headline은 그대로, `checking=false`. 단계 패널의 CountdownRing 자리에 PrimaryButton "켰어요"(`selfCheck()` 호출)를 둔다. 이때 TopBar의 일시정지 버튼은 숨겨서 큰 버튼 2개(켰어요, 그만하기) 규칙을 지킨다.
- 남은 시간이 10초 이하가 되면 CountdownRing이 스스로 호박색으로 바뀐다. 문구는 더하지 않는다.
- Compact(1280×720): 오른쪽 패널 폭 400dp, padding 24. CountdownRing 대신 `CountdownBar`를 StepList 바로 아래 24dp에 둔다. InstructionPanel headline은 최대 2줄. 음성 문구가 1줄을 넘으면 2줄까지.
- Wide(1920×1080): 오른쪽 패널 폭 520dp, CountdownRing size 200dp.

## 4. 오조작 (Running이고 hint가 있을 때)

드릴 진행 화면은 그대로 두고 InstructionPanel만 바꾼다.

- `tone = HINT`, `eyebrow = "지금은"`
- `headline` = hint.text (예: “한 번 닦기”예요). 기획서 문구 "지금은 '한 번 닦기'예요"를 eyebrow와 headline으로 나눈 것이다. JSON의 wrong.text가 "지금은 …"으로 시작하면 eyebrow를 빼고 그대로 headline에 쓴다.
- `voice` = hint.voice (예: 반대쪽으로 한 칸 옮겨 보세요.)
- `readValue` = "와이퍼 MIST" (방금 읽은 틀린 값. attention 색으로 표시된다)
- ControlLocator tone = ATTENTION (같은 부품을 호박색으로)
- 단계 목록과 남은 시간은 그대로 둔다. 오조작은 단계 상태를 바꾸지 않으므로 ACTIVE가 유지된다.
- 값이 다시 바뀌면 정답 쪽이든 아니든 hint를 지우고 tone NORMAL로 돌아간다(250ms 색 전환).

## 5. 시간 초과 (TimedOut)

```
TopBar: [폭우 드릴 · 시간 종료]                                        [SourceChip]   (버튼 없음)
SimBanner
Row(간격 32)
 ├ 왼쪽 Column (weight 1)
 │   InstructionPanel(tone CALM, headline "괜찮아요. 여기 있어요", voice = 못 찾은 단계 where.voice)
 │   24dp
 │   ControlLocator(point = 못 찾은 단계 control, tone ATTENTION, weight 1)   ← 드릴 화면보다 크게(같은 영역 전부)
 └ 단계 패널(460dp): StepList: 통과한 단계 DONE, 시간 안에 못 한 첫 단계 ATTENTION(!), 그 뒤 PENDING. CountdownRing 없음.
     아래(Spacer weight 1 뒤): PrimaryButton "다시"(RETRY), 24, SecondaryButton "나중에", 둘 다 fillMaxWidth (6장 완료 화면과 같은 자리)
```

- headline: 괜찮아요. 여기 있어요 (마침표 뒤 줄바꿈을 허용한다. 어절 줄바꿈이 적용돼 있다)
- voice 예: 공조 패널, 앞유리 그림이 있는 버튼이에요. JSON `where.voice`를 쓰고, 없으면 `ControlPoint.where`를 쓴다.
- 큰 버튼은 정확히 2개다. 다시 버튼은 같은 퀘스트를 처음부터 시작하고, 나중에 버튼은 홈으로 간다. TopBar에는 버튼을 두지 않는다.
- 실패라는 말과 빨강은 쓰지 않는다. 단계의 ! 표시는 attention 색이다.

## 6. 완료 (Completed)

```
TopBar: [폭우 드릴 · 완료]                                             [SourceChip]   (버튼 없음)
SimBanner
Row(간격 32)
 ├ 왼쪽 Column (weight 1)
 │   ResultHero(timeText "11.2초", deltaText, improved)
 │   24dp
 │   VoiceLine(done.voice)   ← InstructionPanel 대신 VoiceLine 단독 (success 색)
 │   32dp
 │   도면 미니: CockpitMap(방금 쓴 control들 = JUST_LIT, 이전에 켜진 칸 = LIT, 나머지 UNLIT, weight 1)
 │   아래 캡션 "앞유리·시야 구역 {켜진}/{전체}" (label, textSecondary)
 └ 단계 패널(460dp): StepList 모두 DONE, 아래(Spacer weight 1 뒤) Column: PrimaryButton "도면 보기"(MAP), 24, SecondaryButton "다시"(RETRY), 둘 다 fillMaxWidth
```

- 큰 버튼은 정확히 2개(도면 보기, 다시)다. 홈으로는 시스템 뒤로가기로 간다. TopBar에 닫기 버튼을 두지 않아서 2개 규칙을 지킨다.
- deltaText 규칙
  - 지난 기록이 없으면 "첫 기록이에요", improved = null
  - 더 빠르면 "지난번보다 {d}초 빨라요", improved = true
  - 같거나 느리면 "지난 기록 {best}초", improved = false (부정적인 표현은 쓰지 않는다)
- voice 예: 와이퍼도 잘 꺼 두셨어요. 지난번보다 3.1초 빨라요. JSON `done.voice` 뒤에 비교 문구를 붙인 것이다.
- 통과 순간 효과: 도면의 JUST_LIT 칸이 맥박 2회(ChagilMotion.pulse) 뒤 LIT로 바뀐다. 소리는 엔진과 TTS가 맡는다.

## 7. 내 차 도면 (진행 지도)

```
TopBar: [내 차 도면]                                                    [SourceChip] [TopBarButton 닫기(CLOSE)]
SimBanner
Row(간격 32)
 ├ CockpitMap(zones, showLabels = true, weight 1, fillMaxHeight)  (surface 카드 안, padding 24)
 └ 오른쪽 목록 (폭 460dp, surface 카드, padding 32)
     "{켜진} / {전체}칸" (headline)  · "이 차가 지원하는 기능 기준" (body, textSecondary)
     24dp
     구역 행 반복(행 높이 76dp): [StatusDot(success|outline)] [구역 이름 title] [Spacer] ["{n}/{m}" label]
       1 라이트 레버 · 2 와이퍼 레버 · 3 공조·김서림 · 4 열선 (핸들·뒷유리) · 5 벨트·좌석 · Q 운전 보조 (퀴즈형)
```

- zones 값: 드릴로 통과한 control은 LIT, 앱이 읽을 수 없는 control(신호가 Unreadable이거나 지원하지 않음)은 QUIZ, 나머지는 UNLIT.
- 1차 제출 범위에서 Q(운전 보조) 행은 준비 중 칩으로 표시한다.

## 8. 일시정지 카드 (Paused)

- reason이 SAFETY이고 안전 단계가 다시 퀘스트 level 이상이 되면, 드릴 화면 위에 scrim을 깔고 가운데에 `PauseCard`를 띄운다.
- title: 이어서 할까요? (고정)
- progressText: "{quest.title} {n}/{전체}단계에서 멈췄어요. 차가 다시 P에 있어요." L1 확인으로 돌아온 경우에는 마지막 문장 대신 "다시 멈춰 있어요."를 쓴다.
- 버튼: Secondary "그만하기"(abort 후 홈), Primary "이어서 하기"(resume)
- 사용자가 TopBar의 일시정지를 누른 경우(reason USER)에도 같은 카드를 쓴다. progressText는 "{quest.title} {n}/{전체}단계에서 멈췄어요."
- 멈춘 동안 CountdownRing은 `paused = true`로 멈춤을 보여 준다.

## 9. 판정 로그와 시뮬레이터 패널

- 위치: 드릴 진행 화면의 오른쪽 가장자리에서 밀려 나오는 패널(폭 720dp, 높이는 Body 전체, surface, 왼쪽 모서리만 large).
- 여는 방법: TopBar의 `SourceChip`을 누른다. 모든 화면에서 같다(심사와 시연용). 칩에 `Modifier.clickable`과 위아래 padding 10dp를 줘서 터치 영역 76dp를 맞춘다. 닫을 때는 패널 바깥을 누르거나 뒤로가기를 누른다.
- 패널 내용 순서
  1. `SimBanner`: 시뮬레이션일 때 항상 맨 위에 둔다.
  2. `LogPanel(lines, simulation)`: 시간은 00:02.1 형식(mono). 점 색은 PASS 초록, HINT 호박, INFO 회색, DONE 파랑. 본문은 엔진 LogEntry.text를 그대로 쓴다. 최신 줄이 아래에 오고 자동으로 스크롤한다.
  3. SIMULATION일 때만 구분선 24dp 뒤에 시뮬레이터(title)와 `SimSwitchRow`를 줄마다 둔다.
     - 전조등 스위치: OFF, ON, AUTOMATIC, DAYTIME_RUNNING. 표시는 끔, 켬, AUTO, 미등
     - 와이퍼 스위치: OFF, AUTO, CONTINUOUS_LEVEL_1, CONTINUOUS_LEVEL_3. 표시는 끔, AUTO, LO, HI. Pleos 콘솔의 Low, High와 같은 값을 보낸다.
     - 앞유리 김서림: OFF, ON. 표시는 끔, 켬
     - 뒷유리 열선: OFF, ON
     - 핸들 열선: 0, 1, 2. 표시는 끔, 1단, 2단
     - 기어: GEAR_PARK, GEAR_NEUTRAL, GEAR_DRIVE, GEAR_REVERSE. 표시는 P, N, D, R
     - 읽기 불가 만들기(신호별 Unreadable 토글): 선택지는 정상, 읽기 불가
- 실차 신호(PLEOS_SDK) 모드에서는 시뮬레이터 부분을 모두 숨긴다. LogPanel은 그대로 두고 SourceChip은 차 신호로 보인다.
- Compact: 패널 폭 560dp. SimSwitchRow는 FlowRow라 자동으로 줄을 바꾼다.

## 부록 A. 문구 모음 (그대로 사용)

| 키 | 문구 |
|---|---|
| sim.banner | 시뮬레이션 중이에요. 실제 차 신호가 아니에요 |
| source.real / source.sim | 차 신호 / 시뮬레이션 |
| safety.l0 / l1 / l1.confirmed / l2 | 주행 중 · 쉬어요 / 정지 · P 확인 필요 / 정지 · P 확인함 / 주차 확인 · P |
| home.tagline | 차를 세워 두고, 진짜 레버로 연습해요 |
| card.locked / card.soon | P에 두면 열려요 / 준비 중 |
| drill.checking | 차가 확인하는 중 |
| drill.read | 차가 읽은 값 |
| drill.steps | 할 일 |
| drill.timer | 남은 시간 / 멈춤 |
| drill.selfcheck.eyebrow / button | 이 차는 이 신호를 읽을 수 없어요 / 켰어요 |
| hint.eyebrow | 지금은 |
| timeout.headline | 괜찮아요. 여기 있어요 |
| timeout.buttons | 다시 / 나중에 |
| done.eyebrow | 통과 |
| done.first / faster / notFaster | 첫 기록이에요 / 지난번보다 {d}초 빨라요 / 지난 기록 {best}초 |
| done.buttons | 도면 보기 / 다시 |
| pause.title | 이어서 할까요? |
| pause.buttons | 이어서 하기 / 그만하기 |
| confirm.title | P에 두셨나요? |
| confirm.body | 기어를 P에 두고 시작해요. 차가 P를 읽을 수 없어서 한 번만 확인할게요. |
| confirm.buttons | 아직이에요 / P에 뒀어요 |
| rest.title / body | 주행 중에는 쉬어요 / 차를 세우고 P에 두면 이어서 할 수 있어요. |
| map.title / caption | 내 차 도면 / 이 차가 지원하는 기능 기준 |
| map.empty | 첫 드릴을 해 보세요 |
| topbar.pause / quit / close | 일시정지 / 그만하기 / 닫기 |

## 부록 B. 상태와 컴포넌트 매핑 (화면 계층에서 변환)

| 엔진, 안전 상태 | 컴포넌트 |
|---|---|
| `SafetyLevel.L0_DRIVING / L1_STOPPED_UNCONFIRMED / L2_PARKED` | 같은 이름의 `SafetyChipLevel` |
| `SourceMode.SIMULATION` | `SourceChip(true)`, `SimBanner()`, `LogPanel(simulation = true)` |
| `StepStatus.WAITING / STABILIZING` | ACTIVE, `checking = true` |
| `StepStatus.PASSED` | DONE |
| `StepStatus.SELF_CHECK_NEEDED` | ACTIVE, eyebrow, 켰어요 버튼 |
| `DrillState.Running.hint != null` | `InstructionTone.HINT`, `LocatorTone.ATTENTION` |
| `DrillState.TimedOut` | 5장. 못 한 첫 단계는 ATTENTION |
| `DrillState.Completed` | 6장 |
| `DrillState.Paused` | 8장 |
| step JSON `control` | `ControlPoint.fromKey(control)`. null이면 ControlLocator에 null을 넘겨 강조 없이 그림만 보인다 |
