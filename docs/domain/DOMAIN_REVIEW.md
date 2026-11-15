# 자동차 도메인 검토 (content/quests)

검토 대상: `docs/SPEC.md` 4장 안전 규칙과 5장 퀘스트 스키마, 제출 기획서, `app/src/main/assets/quests/*.json`.

기준: Vehicle SDK 2.3.2 javap 시그니처, 현대차와 기아의 온라인 취급설명서, Pleos Virtual Console 카드 목록(사용자 확인: Wipers Off/Auto/Low/High, Gear P/R/N/D, Steering Wheel Heat, Exterior Lights Mode, Seat Occupancy, Defroster). 콘솔 조작이 앱에 어떤 값으로 읽히는지는 Pleos Connect v2.1.0 에뮬레이터에서 확인했다(5장, SPEC 1장).

표시: [확인]은 출처로 확인한 것, [추정]은 확인하지 못해 방어적으로 설계한 것, [요청]은 다른 팀에 요청하는 것이다.

## 1. 확인한 차량 사실

| 항목 | 내용 | 근거 |
|---|---|---|
| 와이퍼 칸 순서 | 우적 센서가 있는 트림은 MIST, OFF, AUTO, LO, HI 순서다. 센서가 없는 트림은 MIST, OFF, INT(다이얼), LO, HI 순서다. MIST는 항상 LO, HI의 반대쪽에 있다. | [확인] 현대 LX3 2026 국문 매뉴얼, MEa 2026 영문 매뉴얼 |
| 레버를 올리나 내리나 | 차종과 연식마다 다르다. 최근 매뉴얼은 MIST가 아래, 2010~13년식 일부는 위다. | [확인] 현대 매뉴얼 여러 연식 비교. 그래서 퀘스트 문구는 위, 아래를 쓰지 않고 칸 이름과 반대쪽이라는 말만 쓴다 |
| 와셔액 | 레버를 운전자 쪽으로 당기면 분사되고 와이퍼가 함께 움직인다. | [확인] LX3 국문 매뉴얼 |
| 세차 | "세차 전에는 와이퍼를 끄십시오(스위치 위치: OFF)." AUTO에서는 센서가 물을 감지해 와이퍼가 움직인다. | [확인] LX3 국문, MEa 영문 매뉴얼 |
| 언 유리 | "앞유리가 언 경우 와이퍼 스위치를 OFF 위치에 두십시오." 눈이나 얼음이 많으면 디프로스터로 먼저 녹인다. | [확인] LX3 국문, MEa 영문 매뉴얼 |
| 마른 유리 | 마른 앞유리에서는 와이퍼를 작동하지 않는다. | [확인] MEa 영문 매뉴얼 |
| 라이트 다이얼 | 왼쪽 레버 끝을 돌린다. 순서는 OFF, AUTO, 미등, 전조등이고 전조등이 맨 끝 칸이다. 2026년 9월 이후 신차는 자동 점등이 의무라 OFF 칸이 없을 수 있다. | [확인] 기아 국문 매뉴얼 요약, 기획서 인용 규정. [추정] 그랜저와 아반떼도 같음 |
| 와이퍼 레버 위치 | 아반떼(CN7)는 오른쪽 레버다. 그랜저(GN7)는 칼럼식 변속 레버가 오른쪽에 있어서 와이퍼 배치를 확인하지 못했다. | [추정] 그래서 문구는 핸들 옆 와이퍼 레버로 중립적으로 쓴다 |
| 앞유리 디프로스터 | 공조 패널에 있고, 휘어진 앞유리 그림에 위로 올라가는 물결 화살표가 그려져 있다(ISO 기호). 그랜저는 터치 공조 패널이다. | [확인] 일반 ISO 7000 기호. 문구는 "공조 패널, 앞유리 그림 버튼" |
| 뒷유리 열선 | 네모난 뒷유리 그림에 물결 화살표가 있다. 일부 차종은 사이드미러 열선이 함께 켜지고, 일정 시간 뒤 자동으로 꺼진다. | [확인] 일반 기호, 기획서. 자동으로 꺼지는 시간은 차종마다 다르다 |
| 핸들 열선 | 버튼 위치가 차종마다 다르다(핸들 스포크, 공조 패널, 화면). | [추정] 그래서 문구는 "핸들 그림에 물결 있는 버튼", "차마다 자리가 달라요"로 쓴다 |
| 시동 상태 | SDK 2.3.2에 `IgnitionState` enum은 있지만 이 값을 돌려주는 API가 없다(javap 전수 검색). 앱은 시동 여부를 알 수 없다. | [확인] SDK javap |
| 벨트 | `Seat.getCurrentSeatBeltBuckledState(SeatArea, (Boolean)->Unit, …)`, `registerSeatBeltBuckledState`. 콘솔에는 착석 카드만 있다. | [확인] SDK javap, 사용자 콘솔 확인 |
| 서울시 공회전 | 제한 시간은 5~25℃에서 2분, 0~5℃와 25~30℃에서 5분이다. 0℃ 이하와 30℃ 초과는 허용한다. 과태료는 5만 원. | [확인] 서울시 환경분야 누리집. 시행일이 최신인지는 조례 원문으로 확인해야 한다 |

## 2. SPEC 4장 안전 규칙 검토

### 2.1 기어 의미: 대체로 맞고, 두 군데를 고친다

- `DRIVE`, `REVERSE`, `GEAR_1..9`를 L0으로 보는 규칙은 맞다. D에 두고 서 있는 상태(신호 대기)도 L0이라는 기획서 원칙과 맞는다. 앱은 속도를 읽지 않으므로 이 규칙이 유일한 방어선이다. 그대로 둔다.
- 고칠 점 1. N(중립)은 확인을 받으면 여는 상태가 아니다. 기어를 읽었는데 N이면 앱은 차가 P에 있지 않다는 것을 이미 안다. 이때 "P에 두셨나요?"라고 물으면 운전자가 사실과 다른 확인을 누르게 된다. 경사로라면 차가 굴러갈 수 있다.
  - [요청 feat/core] 값을 읽은 `GEAR_NEUTRAL`은 L1 잠금으로 둔다. 확인 대화 대신 "P로 옮기면 열려요" 안내만 띄운다. 확인 대화는 기어가 `Unreadable`, `Unknown`, `GEAR_UNKNOWN`일 때만 띄운다.
- 고칠 점 2. 확인을 지우는 조건. SPEC에는 P에서 D로 바뀔 때만 적혀 있다. [요청 feat/core] L0으로 가는 모든 전환(R 포함, uxRestricted=true 포함)과, 앱이 백그라운드로 갔다가 돌아올 때 운전자 확인을 지운다.
- [요청 feat/core] 기어 흔들림 방지: L0에서 L2로 돌아가는 것은 `GEAR_PARK`가 2초 연속 읽힐 때만 인정하고, 그 뒤에 "이어서 할까요?"를 띄운다. L0 진입은 지연 없이 즉시 한다.
- [확인] Pleos Connect v2.1.0 콘솔에서 Gear P는 `GEAR_PARK`로 읽힌다.

### 2.2 UxRestrictions

- `CarUxRestrictions.isRequiresDistractionOptimization == true`이면 L0이다. AAOS는 차 설정에 따라 정차 중 D(idling)에서는 제한을 걸지 않을 수 있다. 그래서 기어 규칙과 함께 써야 한다. SPEC대로 OR 조건을 유지한다.
- [확인] Pleos Connect v2.1.0에서 콘솔 Gear를 D로 바꾸면 Pleos가 앱 화면을 가리고 "주행 중에는 사용할 수 없습니다"를 띄운다. 그래도 앱 안의 기어 규칙은 그대로 둔다.
- [요청 feat/core] Car 서비스 연결이 끊기거나 리스너가 예외를 내면 `uxRestricted = null`로 두고, 기어도 못 읽으면 L1(확인 필요)로 내린다. L2로 추정해서는 안 된다.
- [요청 feat/core, feat/screens] L0에 들어가면 TTS를 문장 중간이라도 바로 끊는다. 기획서에 L0에서는 소리와 알림도 내지 않는다고 적었다. 다시 주차해도 자동으로 말을 이어 가지 않는다.

### 2.3 공회전 안내 (서울시 2분)

- 앱은 시동 여부를 읽을 수 없다(1장). 그래서 시동이 켜져 있으면 2분마다 알려 주는 기능은 판정이 아니라 시간에 따른 권고로만 만든다.
- [요청 feat/screens] 앱이 앞에 떠 있는 동안 2분마다 홈이나 완료 화면에 "시동이 켜져 있다면 꺼 두세요." 한 줄 카드를 띄운다. 헤즈업 알림과 음성은 쓰지 않는다. 드릴 진행 중에는 띄우지 않고 드릴이 끝난 화면에 띄운다.
- 문구에 법 위반이나 과태료를 쓰지 않는다. 0℃ 이하는 서울시 기준상 허용이라, 성에 드릴을 하는 아침에는 사실과 맞지 않는다. 근거는 기름과 배출가스를 아끼려는 것으로 둔다. 성에 드릴 완료 음성에는 예열은 짧게라고만 적었다.

### 2.4 와이퍼 마모와 마른 유리

- 실제 차에서 폭우 드릴을 하면 마른 유리 위로 와이퍼가 움직인다. 매뉴얼은 마른 유리에서 작동하지 말라고 한다. 그래서 이렇게 대응했다.
  1. 처음에는 기획서의 "와이퍼는 1단까지만"에 따라 HI를 오조작 힌트로 두었다. Pleos 콘솔 High가 `CONTINUOUS_LEVEL_3`으로 읽히는 것을 확인한 뒤로는 폭우 드릴이 LO와 HI를 모두 정답으로 받는다(`CONTINUOUS_LEVEL_1`~`CONTINUOUS_LEVEL_3`). 폭우에서는 HI도 맞는 조작이다.
  2. 와이퍼 단계 음성에 "유리가 말라 있으면 레버를 당겨 워셔액부터 뿌려요"를 넣었다.
  3. 뒤에 `restore`(OFF) 단계가 있다.
- [요청 feat/core] `role: "restore"` 단계는 시간 초과, 그만하기, 일시정지(L0)로 끝나도 남아 있어야 한다. 드릴이 끝난 화면에서 `Wiper.switch`가 OFF가 아니면 "와이퍼를 OFF로 돌려 두세요" 한 줄을 띄운다. L0 일시정지 중에는 띄우지 않는다. 주행 중에 비가 와서 켰을 수 있기 때문이다.
- [요청 feat/core] 와이퍼 단계의 안정 시간은 1000ms보다 늘리지 않는다. 와이퍼가 움직이는 시간을 줄이기 위해서다.

### 2.5 언 유리

- 성에 드릴 `guards`: `Wiper.switch`가 `OFF`, `OTHER`, `SERVICE`가 아니면 경고한다. MIST(한 번 닦기)와 AUTO도 경고 대상이다. AUTO는 녹은 물에 반응해 움직일 수 있다.
- [요청 feat/core] guard는 이렇게 구현해 주세요. guard 조건이 참인 동안 Hint를 띄우고 현재 단계의 안정 시간을 세지 않는다(통과 보류). 남은 시간은 계속 흐른다. 조건이 풀리면 Hint를 내리고 판정을 이어 간다. guard는 드릴을 시작하는 순간에도 검사한다. 시작할 때부터 AUTO일 수 있기 때문이다.
- SPEC 5장 예시의 `"notAnyOf": ["OFF"]`는 이 값들이 아니면 경고한다는 뜻으로 읽었다. 엔진이 반대로 해석하면 알려 주세요.

### 2.6 빨간 경고는 놀이로 만들지 않는다

- 이번 퀘스트는 경고등을 읽지 않는다(`PRIVATE_CAR_SAFETY` 특권 권한이 필요하고, 기획서에서 쓰지 않기로 한 것). 맞는 판단이다.
- [요청 feat/core] 퀘스트 로더 검증: `read`가 경고등, Telltale, 오일, 브레이크 계열이면 로드를 거부한다. 나중에 소모품 퀘스트를 붙여도 엔진오일과 브레이크액은 퀘스트로 만들지 않고, 고정 안내 화면("안전한 곳에 세우고 긴급출동이나 정비소에 문의")으로만 보여 준다.
- [요청 design/system] 경고 성격의 Hint(언 유리 guard)에는 빨간색과 경고등 아이콘을 쓰지 않는다. 주황이나 중립 톤에 문장으로 보여 준다. 빨간색은 실제 차량 경고를 위해 남겨 둔다.

### 2.7 달리기를 보상하지 않는다

- 퀘스트 4개 어디에서도 `Gear.selection`, 속도, 주행거리를 판정 대상으로 쓰지 않았다. 기어는 안전 단계에만 쓴다.
- [요청 feat/core] 로더 검증: 퀘스트의 `read`나 `guards.read`에 `Gear.selection`이 오면 거부한다. 기어 변경을 퀘스트로 만들지 않는다는 기획서의 약속을 코드로 못 박는 것이다.
- [요청 feat/screens] 완료 화면의 "지난번보다 3.1초 빨라요"는 주행 속도가 아니라 조작 속도다. 순위표나 다른 사람과의 비교는 넣지 않는다.

### 2.8 오조작 힌트 타이밍 (중간 칸 문제)

- 라이트 다이얼은 OFF, AUTO, 미등, 전조등 칸을 차례로 지나며 돌린다. 와이퍼도 OFF, AUTO, LO처럼 중간 칸을 지난다. 지나가는 순간마다 `wrong` 힌트가 뜨면, 정답으로 가고 있는 사람에게 틀렸다고 말하게 된다.
- [요청 feat/core] `wrong` 힌트는 그 값이 600ms 이상 유지될 때만 띄운다. 예외로 `MIST`는 손을 떼면 OFF로 돌아오는 순간 칸이라 읽히는 즉시 띄우고 3초간 유지한다.
- [요청 feat/core] 와이퍼 스위치는 250ms 폴링이라 짧은 MIST를 놓칠 수 있다. `WiperState`가 `ON`으로 바뀌었는데 스위치가 계속 `OFF`로 읽히면, 로그에 "와이퍼가 한 번 움직였어요(MIST로 보임)"를 남기고 MIST 힌트를 띄운다.

### 2.9 라이트 단계에서 AUTO가 정답이 아닌 이유

- AUTO는 조도 센서만 본다. 낮에 폭우가 오면 바깥이 밝다고 판단해 전조등이 켜지지 않을 수 있고, 주간주행등만 켜지면 후미등이 꺼져 뒤차가 잘 보지 못한다. 그래서 정답은 스위치 `ON`이다.
- 판정은 `LowBeam.switch`(스위치 위치)로 한다. `LowBeam.state`(실제 점등)는 AUTO에서도 ON일 수 있어서 판정에 쓰지 않는다.
- 2026년 9월 이후 신차에는 OFF 칸이 없을 수 있다. 그 차에서는 OFF 힌트가 나오지 않을 뿐 문제는 없다.
- [확인] Pleos 콘솔 Exterior Lights Mode에서는 Low beam만 `ON`으로 읽히고, Auto, Tail lamp, Off는 모두 `OFF`로 읽힌다. 그래서 AUTO(`AUTOMATIC`) 힌트는 콘솔로 만들 수 없고 앱 안 시뮬레이터에서만 볼 수 있다.

## 3. 퀘스트 파일 요약

| 파일 | 열림 | 단계(순서대로) | 제한 | 비고 |
|---|---|---|---|---|
| `rain_drill.json` | open, L1 | 전조등 ON, 와이퍼 LO 또는 HI, 앞유리 디프로스터 ON, 와이퍼 OFF(restore) | 45초 | 오조작 힌트: 라이트 AUTO, 주간등, OFF. 와이퍼 MIST, AUTO, 간헐 2~5단, 연속 4~5단 |
| `carwash_drill.json` | open, L1 | 와이퍼 OFF (`alreadyOn: pass`) | 30초 | 시작할 때 이미 OFF면 1초 뒤 통과하고 로그에 이미 켜져 있음을 남긴다 |
| `frost_drill.json` | open, L1 | 앞유리 디프로스터, 뒷유리 열선, 핸들 열선(`notAnyOf ["0"]`) | 60초 | guard: 와이퍼가 OFF가 아니면 경고 |
| `departure_drill.json` | coming_soon, L1 | 운전석 벨트, 조수석 벨트, 뒷좌석과 카시트 | 60초 | 시작하지 않고 카드만 보인다 |

`zone` 값은 `windshield`(폭우, 세차), `heating`(성에), `seat`(출발 준비)이다. 도면 칸 그룹과 맞춰 쓰면 된다.

### 와이퍼 단계의 `toAnyOf` 근거

- [확인] Pleos Connect v2.1.0 에뮬레이터에서 콘솔 Wipers를 하나씩 바꿔 보았다. Low는 `CONTINUOUS_LEVEL_1`, High는 `CONTINUOUS_LEVEL_3`, Auto는 `AUTO`, Off는 `OFF`로 읽힌다.
- 그래서 폭우 드릴의 와이퍼 단계는 LO와 HI를 모두 정답으로 받는다. `toAnyOf`는 `INTERMITTENT_LEVEL_1`, `CONTINUOUS_LEVEL_1`, `CONTINUOUS_LEVEL_2`, `CONTINUOUS_LEVEL_3`이다.
- `INTERMITTENT_LEVEL_1`은 콘솔 Low가 어떤 값을 보내는지 모르던 때 데모가 막히지 않도록 넣은 값이다. 콘솔에서는 나오지 않지만 지금도 정답 목록에 남아 있다. 간헐 2~5단에는 "띄엄띄엄 닦아요" 힌트를 띄운다.
- 오조작 장면은 Auto로 보여 준다. 콘솔에는 MIST가 없고 High는 정답이기 때문이다.

### 출발 준비 드릴의 `read` 키

- SPEC 4장의 `Signal` enum에는 벨트 키가 없다. 2차용으로 `SeatBelt.buckled.ROW_1_LEFT`, `SeatBelt.buckled.ROW_1_RIGHT`, `SeatBelt.buckled.ROW_2`를 미리 적었다. 한국 좌핸들 기준으로 ROW_1_LEFT가 운전석이다.
- [요청 feat/core] 로더는 모르는 `read` 키를 만나도 멈추지 말고 그 신호를 `Unreadable`로 다룬다. 그러면 `ifUnreadable: selfCheck`가 적용된다. `availability: coming_soon`이면 단계 검증을 건너뛰어도 된다. kotlinx.serialization은 `ignoreUnknownKeys = true`로 둔다.
- 2차에서는 착석 센서가 없는 자리의 단계를 건너뛰어야 한다(기획서: 착석 센서가 없는 차는 운전석만). 스키마에 `"onlyIf": {"read": "Seat.occupied.ROW_1_RIGHT", "to": "ON"}` 같은 조건을 넣자는 SPEC 변경 제안이다(2차 범위).

## 4. 다른 팀에 요청 (모아 보기)

feat/core

1. 값을 읽은 `GEAR_NEUTRAL`은 L1 잠금. 확인 대화는 기어를 못 읽을 때만 띄운다. (2.1)
2. L0으로 가는 모든 전환과 백그라운드 복귀 때 운전자 확인을 지운다. (2.1)
3. L2 복귀는 P가 2초 연속 읽힌 뒤에. L0 진입은 즉시. (2.1)
4. Car 서비스 오류 때는 `uxRestricted=null`로 두고 L1로. L2로 추정하지 않는다. (2.2)
5. L0에 들어가면 TTS를 바로 멈춘다. (2.2)
6. restore 단계를 마치지 못하고 끝나면 와이퍼 OFF 안내 데이터를 상태에 실어 준다. (2.4)
7. guard 의미: 참인 동안 Hint를 띄우고 안정 시간을 멈춘다. 시작하는 순간에도 검사한다. (2.5)
8. 로더 검증: `Gear.selection`과 경고등 계열 `read`는 거부, 모르는 키는 Unreadable, `ignoreUnknownKeys`. (2.6, 2.7, 3)
9. `wrong` 힌트는 600ms 유지된 뒤 표시. MIST는 즉시 띄우고 3초 유지. WiperState가 ON인데 스위치가 OFF면 MIST로 추정. (2.8)

feat/screens

1. 2분 공회전 카드: 시간 기준으로, 드릴이 끝난 뒤에만, 알림과 음성 없이, 과태료라는 말 없이. (2.3)
2. 완료 화면 비교는 본인 조작 시간만. 순위표는 없다. (2.7)
3. 드릴 종료 화면에 와이퍼 OFF 알림 한 줄. (2.4)

design/system

1. guard와 오조작 Hint에는 빨간색과 경고등 아이콘을 쓰지 않는다(주황이나 중립 톤). (2.6)
2. 위치 그림: 라이트는 왼쪽 레버 끝 다이얼(맨 끝 칸 강조). 와이퍼는 핸들 옆 레버로 그리되 좌우를 단정하지 않는 그림으로 하거나, 아반떼 기준 오른쪽에 두고 "차종마다 다를 수 있어요" 캡션을 단다. 레버 이동 화살표(위, 아래)는 그리지 않는다. 대신 칸 이름(MIST, OFF, AUTO, LO, HI)을 줄지어 보여 주고 목표 칸을 강조한다. (1장)
3. 앞유리 디프로스터, 뒷유리 열선, 핸들 열선은 ISO 기호 모양으로 그린다. 휘어진 앞유리와 위로 향한 물결, 네모와 물결, 핸들과 물결이다.

chore/compliance (DEMO_SCRIPT)

1. 영상의 라이트 단계: 처음에는 콘솔 Exterior Lights Mode 카드가 `getLowBeamLightSwitch` 값을 바꾸는지 몰랐다. Pleos Connect v2.1.0에서 Low beam이 `ON`으로 읽히는 것을 확인했으므로 콘솔 Low beam으로 시연한다. Auto, Tail lamp, Off는 `OFF`로 읽힌다. 콘솔로 판정되지 않는 일이 생기면 켰어요 확인으로 보여 주고, 자막에 콘솔에 라이트 신호가 없어 운전자 확인으로 넘어간다고 밝힌다.
2. 퀘스트 `label`은 모두 `null`이다. 현대차 안내 라벨은 실제 현대차가 올린 퀘스트가 아니므로 1차 영상에서 쓰지 않는다. 쓰려면 예시라고 표시한다.

## 5. 시연 영상용 매핑 (퀘스트 단계, SDK 호출, Virtual Console 조작)

콘솔 값은 Pleos Connect v2.1.0 에뮬레이터에서 확인한 것이다. 전체 대응표는 SPEC 1장에 있다.

녹화 전 콘솔 초기값: Gear P, Wipers Off, Defroster Off, Steering Wheel Heat 0(Off), Exterior Lights Mode Auto 또는 Off(둘 다 `OFF`로 읽힌다).

### 폭우 드릴 (45초)

| 번호 | 단계 | SDK 호출 (읽기만) | 콘솔 조작 | 기대 판정 |
|---|---|---|---|---|
| 0 | 안전 단계 | `Gear.getCurrentGearSelectionState`, `registerGearSelection`, `CarUxRestrictionsManager` | Gear P | `GEAR_PARK`로 읽혀 L2 주차 확정 칩 |
| 1 | 전조등 켜기 | `Light.getLowBeamLightSwitch` 폴링, `registerLowBeamLightState` | Exterior Lights Mode Low beam | `ON`이 1초 유지되면 통과. Auto와 Tail lamp는 `OFF`로 읽혀서 AUTO 힌트는 나오지 않는다 |
| 2 | 와이퍼 켜기 | `Wiper.getCurrentWiperSwitch(FRONT_WINDSHIELD)` 250ms 폴링, `registerWiperState` | Wipers Auto(오조작), 그다음 Low 또는 High | Auto에서 "AUTO는 센서에 맡겨요". Low(`CONTINUOUS_LEVEL_1`)나 High(`CONTINUOUS_LEVEL_3`)가 1초 유지되면 통과 |
| 3 | 앞유리 김서림 | `HVAC.getHvacDefrosterState(FRONT_WINDSHIELD)`, `registerHvacDefrosterState` | Defroster Front 켜기 | 1초 뒤 통과 |
| 4 | 와이퍼 끄기 | `Wiper.getCurrentWiperSwitch` | Wipers Off | 1초 뒤 통과하고 완료 화면에서 지난 기록과 비교 |
| 추가 | 일시정지 장면(선택) | `registerGearSelection` | 드릴 중 Gear D, 다시 P | D가 되면 바로 Paused(SAFETY)로 바뀌고 소리가 끊긴다. Pleos가 앱 화면을 가리고 "주행 중에는 사용할 수 없습니다"를 띄운다. P로 돌아와 2초가 지나면 "이어서 할까요?" |

MIST는 콘솔에서 만들 수 없다. 기획서 로그 예시의 MIST 오조작은 영상에서 Auto 오조작으로 대신 보여 준다. High는 정답이라 오조작 장면에 쓰지 않는다. 시간 초과 장면이 필요하면 3번에서 디프로스터를 누르지 않고 기다린다. 그러면 "괜찮아요. 여기 있어요"와 "공조 패널, 앞유리 그림 버튼"이 나온다.

### 세차 대비 드릴 (30초)

| 번호 | 단계 | SDK 호출 | 콘솔 조작 | 기대 판정 |
|---|---|---|---|---|
| 1 | 와이퍼 OFF | `Wiper.getCurrentWiperSwitch` | 시작 전 Wipers Auto, 시작한 뒤 Low(오조작, "반대쪽, LO로 갔어요"), 그다음 Off | Off가 1초 유지되면 통과 |
| 1' | 이미 OFF | 같음 | 시작 전 Wipers Off | 1초 뒤 통과, 로그에 이미 켜져 있음 |

High는 `CONTINUOUS_LEVEL_3`으로 읽혀 "지금은 HI예요" 힌트가 뜬다.

### 성에 낀 아침 드릴 (60초)

| 번호 | 단계 | SDK 호출 | 콘솔 조작 | 기대 판정 |
|---|---|---|---|---|
| g | guard | `Wiper.getCurrentWiperSwitch` | 드릴 중 Wipers Low 또는 Auto | "언 유리엔 와이퍼를 꺼 두세요"가 뜨고 단계 판정이 멈춘다. Off로 바꾸면 풀린다 |
| 1 | 앞유리 디프로스터 | `HVAC.registerHvacDefrosterState(FRONT_WINDSHIELD)` | Defroster Front 켜기 | 통과 |
| 2 | 뒷유리 열선 | `HVAC.registerHvacDefrosterState(REAR_WINDSHIELD)` | Defroster Rear 켜기 | 통과 |
| 3 | 핸들 열선 | `SteeringWheel.getHvacSteeringWheelHeatState`, `registerHvacSteeringWheelHeatState` | Steering Wheel Heat 1 이상 | 콘솔 0~3이 `0`~`3`으로 그대로 읽힌다. `0`이 아니면 1초 뒤 통과 |

[확인] 콘솔 Defroster의 Front와 Rear는 각각 앞유리, 뒷유리 구역으로 읽힌다(Pleos Connect v2.1.0). 그래서 뒷유리 단계도 콘솔로 판정된다. 확인 전에는 뒷유리 값이 계속 `OFF`로 남아 단계가 막힐까 걱정해서, 시뮬레이션 패널에서 REAR를 바꿀 수 있게 하고 실차 신호 모드에서 값이 오래 그대로면 켰어요 버튼을 보조로 띄우자고 feat/core에 요청했었다. 콘솔 확인으로 이 걱정은 풀렸다.

### 출발 준비 드릴

- 준비 중 카드만 있다. 2차에서 `Seat.registerSeatBeltBuckledState(SeatArea(ROW_1_LEFT))` 등을 쓴다. 콘솔에는 착석 카드만 있어서 벨트는 녹화 재생 어댑터로 시연한다.

## 6. 출처

- 현대 LX3 2026 국문 매뉴얼, 앞유리 와이퍼/와셔 작동하기: https://ownersmanual.hyundai.com/full_webhelp/LX3/2026/ko_KR/id23C6B060A4Y.html
- 현대 MEa 2026 영문 매뉴얼, Front Windshield Wipers: https://ownersmanual.hyundai.com/full_webhelp/MEa/2026/en_US/id6c0e8dde9d6.html
- 현대 구형 매뉴얼(MIST 방향 비교): https://www.manualsdir.com/manuals/772763/hyundai-2012-sonata.html?page=159
- 기아 국문 매뉴얼(라이트 스위치, 세차 시 와이퍼): https://ownersmanual.kia.com/full_webhelp/MQ4/2026/ko_KR/topics/chapter4_26_1.html
- 서울시 공회전 제한(환경분야 누리집): https://news.seoul.go.kr/env/archives/39671
- Vehicle SDK 2.3.2 javap (`Seat`, `SteeringWheel`, `IgnitionState`, `WiperSwitch`, `LightSwitch`, `WindowValue`): 저장소 밖 로컬 추출본으로 확인했고 AAR은 커밋하지 않았다.
