# 자동차 도메인 검토 (content/quests)

검토 대상: `docs/SPEC.md` §4 안전 규칙·§5 퀘스트 스키마, 제출 기획서, `app/src/main/assets/quests/*.json`.
기준: Vehicle SDK 2.3.2 javap 시그니처, 현대차·기아 온라인 취급설명서, Pleos Virtual Console 카드 목록(사용자 확인: Wipers Off/Auto/Low/High, Gear P/R/N/D, Steering Wheel Heat, Exterior Lights Mode, Seat Occupancy, Defroster).

표시: **[확인]** 출처로 확인함 · **[추정]** 확인 못 함, 방어적으로 설계함 · **[요청]** 다른 팀에 요청.

---

## 1. 확인한 차량 사실

| 항목 | 내용 | 근거 |
|---|---|---|
| 와이퍼 칸 순서 | 우적 센서 트림: `MIST · OFF · AUTO · LO · HI`. 센서 없는 트림: `MIST · OFF · INT(다이얼) · LO · HI`. **MIST는 항상 LO·HI의 반대쪽이다.** | [확인] 현대 LX3 2026 국문 매뉴얼, MEa 2026 영문 매뉴얼 |
| 레버를 올리나 내리나 | 차종·연식마다 다르다(최근 매뉴얼은 MIST가 아래, 2010~13년식 일부는 위). | [확인] 현대 매뉴얼 여러 연식 비교 → 퀘스트 문구는 "위/아래"를 쓰지 않고 "칸 이름"과 "반대쪽"만 쓴다 |
| 와셔액 | 레버를 운전자 쪽으로 당기면 분사되고 와이퍼가 함께 움직인다. | [확인] LX3 국문 매뉴얼 |
| 세차 | "세차 전에는 와이퍼를 끄십시오(스위치 위치: OFF)." AUTO에서는 센서가 물을 감지해 와이퍼가 움직인다. | [확인] LX3 국문, MEa 영문 매뉴얼 |
| 언 유리 | "앞유리가 언 경우 와이퍼 스위치를 OFF 위치에 두십시오." 눈·얼음이 많으면 디프로스터로 먼저 녹인다. | [확인] LX3 국문, MEa 영문 매뉴얼 |
| 마른 유리 | 마른 앞유리에서 와이퍼를 작동하지 않는다. | [확인] MEa 영문 매뉴얼 |
| 라이트 다이얼 | 왼쪽 레버 끝을 돌린다. 순서 `OFF · AUTO · 미등 · 전조등` — 전조등이 맨 끝 칸. 2026.9 이후 신차는 자동점등 의무라 OFF 칸이 없을 수 있다. | [확인] 기아 국문 매뉴얼 요약, 기획서 인용 규정 / [추정] 그랜저·아반떼 동일 |
| 와이퍼 레버 위치 | 아반떼(CN7)는 오른쪽 레버. **그랜저(GN7)는 칼럼식 변속 레버가 오른쪽에 있어 와이퍼 배치를 확인하지 못했다.** | [추정] → 문구는 "핸들 옆 와이퍼 레버"로 중립 표기 |
| 앞유리 디프로스터 | 공조 패널, 휘어진 앞유리 그림 + 위로 올라가는 물결 화살표(ISO 기호). 그랜저는 터치 공조 패널. | [확인] 일반 ISO 7000 기호 / 문구는 "공조 패널, 앞유리 그림 버튼" |
| 뒷유리 열선 | 네모난 뒷유리 그림 + 물결 화살표. 일부 차종은 사이드미러 열선이 함께 켜지고, 일정 시간 뒤 자동으로 꺼진다. | [확인] 일반 기호, 기획서 / 자동 꺼짐 시간은 차종별 |
| 핸들 열선 | 버튼 위치가 차종마다 다르다(핸들 스포크, 공조 패널, 화면). | [추정] → 문구는 "핸들 그림에 물결 있는 버튼"과 "차마다 자리가 달라요" |
| 시동 상태 | SDK 2.3.2에 `IgnitionState` enum은 있지만 **이를 돌려주는 API가 없다**(javap 전수 검색). 앱은 시동 여부를 알 수 없다. | [확인] SDK javap |
| 벨트 | `Seat.getCurrentSeatBeltBuckledState(SeatArea, (Boolean)->Unit, …)`, `registerSeatBeltBuckledState`. 콘솔에는 착석 카드만 있다. | [확인] SDK javap, 사용자 콘솔 확인 |
| 서울시 공회전 | 5~25℃ 2분, 0~5℃·25~30℃ 5분, 0℃ 이하·30℃ 초과는 허용. 과태료 5만 원. | [확인] 서울시 환경분야 누리집 (시행일 최신 여부는 조례 원문 확인 필요) |

---

## 2. SPEC §4 안전 규칙 검토

### 2.1 기어 의미 — 대체로 맞다, 두 군데 고친다
- `DRIVE · REVERSE · GEAR_1..9 → L0`는 맞다. **D에 두고 선 상태(신호 대기)도 L0**라는 기획서 원칙과 일치한다. 속도를 읽지 않으므로 이 규칙이 유일한 방어선이다. 유지한다.
- **고칠 점 1 — N(중립)은 "확인 받으면 열림"이 아니다.** 기어를 *읽었는데* N이면 차는 P가 아니라는 것을 앱이 이미 안다. 이 상태에서 "P에 두셨나요?"를 물으면 운전자가 사실과 다른 확인을 누르게 된다(경사로에서 차가 굴러갈 수 있음).
  - [요청 feat/core] `GEAR_NEUTRAL`(값을 읽은 경우) → L1 잠금. 확인 대화 대신 "P로 옮기면 열려요" 안내만. 확인 대화는 기어가 `Unreadable`/`Unknown`/`GEAR_UNKNOWN`일 때만 띄운다.
- **고칠 점 2 — 확인을 지우는 조건.** SPEC은 "P→D"만 적었다. [요청 feat/core] **L0으로 가는 모든 전환**(R 포함, uxRestricted=true 포함)과 앱이 백그라운드로 간 뒤 다시 올 때 운전자 확인을 지운다.
- [요청 feat/core] 기어 흔들림 방지: L0 → L2 복귀는 `GEAR_PARK`가 **2초 연속** 읽힐 때만 인정하고, 그 뒤에 "이어서 할까요?"를 띄운다. L0 진입은 즉시(지연 없음).

### 2.2 UxRestrictions
- `CarUxRestrictions.isRequiresDistractionOptimization == true → L0`. AAOS는 차 설정에 따라 "정차·D(idling)"에서도 제한을 안 걸 수 있다 → **기어 규칙과 함께 써야 한다.** SPEC대로 OR 조건 유지.
- [요청 feat/core] Car 서비스 연결이 끊기거나 리스너가 예외를 내면 `uxRestricted = null`로 두고, 기어도 못 읽으면 L1(확인 필요)로 떨어뜨린다. 절대 L2로 추정하지 않는다.
- [요청 feat/core·feat/screens] **L0 진입 시 TTS를 즉시 끊는다**(문장 중간이라도). 기획서 "소리와 알림도 내지 않는다". 다시 주차해도 자동으로 말을 이어 하지 않는다.

### 2.3 공회전 안내 (서울시 2분)
- 앱은 시동 여부를 읽을 수 없다(1장). 따라서 "시동이 켜져 있으면 2분마다"는 **판정이 아니라 시간 기반 권고**로만 구현한다.
- [요청 feat/screens] 앱이 앞에 떠 있는 동안 2분마다 홈/완료 화면에 카드 한 줄: "시동이 켜져 있다면 꺼 두세요." 헤즈업 알림·음성 금지, 드릴 진행 중에는 띄우지 않고 드릴이 끝난 화면에 띄운다.
- 문구에 "법 위반·과태료"를 쓰지 않는다. 0℃ 이하에서는 서울시 기준상 허용이라 성에 드릴 아침에는 사실과 다르다. 근거는 "기름과 배출가스를 아끼려고"로 둔다. 성에 드릴 완료 음성은 "예열은 짧게"로만 적었다.

### 2.4 와이퍼 마모 · 마른 유리
- 실제 차에서 폭우 드릴을 하면 **마른 유리 위로 와이퍼가 움직인다.** 매뉴얼은 마른 유리 작동을 피하라고 한다. 대응:
  1. HI는 목표가 아니라 오조작 힌트다(기획서 "와이퍼는 1단까지만").
  2. 와이퍼 단계 음성에 "유리가 말라 있으면 레버를 당겨 워셔액부터 뿌려요"를 넣었다.
  3. 바로 뒤에 `restore`(OFF) 단계가 있다.
- [요청 feat/core] `role: "restore"` 단계는 **시간 초과·그만하기·일시정지(L0)로 끝나도** 남아 있어야 한다. 드릴이 끝난 화면에서 `Wiper.switch ≠ OFF`이면 "와이퍼를 OFF로 돌려 두세요" 한 줄을 띄운다. (L0 일시정지 중에는 띄우지 않는다 — 주행 중 비가 와서 켰을 수 있음.)
- [요청 feat/core] 로그 문구에서 와이퍼 단계 안정 시간을 1000ms보다 늘리지 않는다(작동 시간 최소화).

### 2.5 언 유리
- 성에 드릴 `guards`: `Wiper.switch`가 `OFF·OTHER·SERVICE`가 아니면 경고. MIST(한 번 닦기)와 AUTO도 경고 대상이다(AUTO는 녹은 물에 반응해 움직일 수 있음).
- [요청 feat/core] guard 의미를 이렇게 구현해 주세요: **guard 조건이 참인 동안** Hint를 띄우고, 현재 단계의 안정 시간을 세지 않는다(통과 보류). 남은 시간은 계속 흐른다. 조건이 풀리면 Hint를 내리고 판정을 이어 간다. guard는 드릴 시작 순간에도 검사한다(시작부터 AUTO일 수 있음).
- SPEC §5 예시의 `"notAnyOf": ["OFF"]`는 "이 값들이 **아니면** 경고"로 읽었다. 엔진이 반대로 해석하면 알려 주세요.

### 2.6 빨간 경고는 놀이로 만들지 않는다
- 이번 퀘스트는 경고등을 읽지 않는다(`PRIVATE_CAR_SAFETY` 특권 권한, 기획서 "쓰지 않는 것"). 맞다.
- [요청 feat/core] 퀘스트 로더 검증: `read`가 경고등·Telltale·오일·브레이크 계열이면 로드를 거부한다. 앞으로 소모품 퀘스트를 붙여도 엔진오일·브레이크액은 퀘스트가 아니라 고정 안내 화면("안전한 곳에 세우고 긴급출동이나 정비소에 문의")으로만.
- [요청 design/system] 경고 성격 Hint(언 유리 guard)는 빨간색·경고등 아이콘을 쓰지 않는다. 주황/중립 톤 + 문장으로. 빨간색은 실제 차량 경고에만 남겨 둔다.

### 2.7 달리기를 보상하지 않는다
- 퀘스트 4개 어디에도 `Gear.selection`, 속도, 주행거리를 판정 대상으로 쓰지 않았다. 기어는 안전 단계에만 쓴다.
- [요청 feat/core] 로더 검증: 퀘스트의 `read`나 `guards.read`에 `Gear.selection`이 오면 거부(기어 변경을 퀘스트로 만들지 않는다는 기획서 약속을 코드로 고정).
- [요청 feat/screens] 완료 화면의 "지난번보다 3.1초 빨라요"는 **조작 속도**이지 주행 속도가 아니다. 순위표·다른 사람과 비교는 넣지 않는다.

### 2.8 오조작 힌트 타이밍 (중간 칸 문제)
- 라이트 다이얼은 `OFF → AUTO → 미등 → 전조등`으로 **지나가며** 돌린다. 와이퍼도 `OFF → AUTO → LO`처럼 중간 칸을 지난다. 지나가는 순간마다 `wrong` 힌트가 뜨면 정답으로 가는 사람에게 "틀렸다"고 말하게 된다.
- [요청 feat/core] `wrong` 힌트는 그 값이 **600ms 이상 유지될 때만** 띄운다. 예외: `MIST`는 손을 떼면 OFF로 돌아오는 순간 칸이라 읽히는 즉시 띄우고 3초간 유지한다.
- [요청 feat/core] 와이퍼 스위치는 250ms 폴링이라 짧은 MIST를 놓칠 수 있다. `WiperState`가 `ON`으로 바뀌었는데 스위치가 계속 `OFF`로 읽히면 로그에 "와이퍼가 한 번 움직였어요(MIST로 보임)"를 남기고 MIST 힌트를 띄운다.

### 2.9 라이트 단계 — 왜 AUTO가 정답이 아닌가
- AUTO는 조도 센서만 본다. 낮의 폭우에서는 바깥이 밝다고 판단해 전조등이 안 켜질 수 있고, 주간주행등만 켜지면 후미등이 꺼져 뒤차에게 안 보인다. 그래서 정답은 스위치 `ON`.
- 판정은 `LowBeam.switch`(스위치 위치)로 한다. `LowBeam.state`(실제 점등)는 AUTO에서도 ON일 수 있어 판정에 쓰지 않는다.
- 2026.9 이후 신차는 OFF 칸이 없을 수 있다 → OFF 힌트는 그 차에서 안 나올 뿐 문제없음.

---

## 3. 퀘스트 파일 요약

| 파일 | 열림 | 단계 | 제한 | 비고 |
|---|---|---|---|---|
| `rain_drill.json` | open, L1 | 전조등 ON → 와이퍼 LO → 앞유리 디프로스터 ON → 와이퍼 OFF(restore) | 45초 | 오조작 힌트: 라이트 AUTO/주간등/OFF, 와이퍼 MIST/AUTO/HI/간헐 |
| `carwash_drill.json` | open, L1 | 와이퍼 OFF (`alreadyOn: pass`) | 30초 | 시작이 이미 OFF면 1초 뒤 통과 + "이미 켜져 있음" 로그 |
| `frost_drill.json` | open, L1 | 앞유리 디프로스터 → 뒷유리 열선 → 핸들 열선(`notAnyOf ["0"]`) | 60초 | guard: 와이퍼가 OFF가 아니면 경고 |
| `departure_drill.json` | coming_soon, L1 | 운전석 벨트 → 조수석 벨트 → 뒷좌석·카시트 | 60초 | 시작 안 함. 카드만 |

`zone` 값: `windshield`(폭우·세차), `heating`(성에), `seat`(출발 준비). 도면 칸 그룹과 맞춰 쓰면 된다.

### 와이퍼 LO 단계의 `toAnyOf` 근거
- `CONTINUOUS_LEVEL_1` = 현대차 LO로 본다(AAOS 표준 매핑에서 연속 1단). **[추정]**
- `INTERMITTENT_LEVEL_1`도 통과로 둔 이유: Virtual Console "Low"가 어느 enum을 내보내는지 아직 확인하지 않았다. 콘솔 Low가 간헐 1단으로 나와도 데모가 막히지 않게 한다. 간헐 2~5단은 "띄엄띄엄 닦아요" 힌트.
- **확인 방법(사용자 Pleos 에뮬레이터에서 1분)**: 앱 판정 로그는 raw enum을 그대로 찍는다. 콘솔 Wipers를 Low, High, Auto로 하나씩 바꿔 로그에 찍힌 값을 확인한다. Low가 `CONTINUOUS_LEVEL_1`이면 `INTERMITTENT_LEVEL_1`은 지워도 된다. High가 `CONTINUOUS_LEVEL_1`로 나오면(Low=간헐 1) 이 파일의 HI 힌트 키를 바꿔야 한다 → 저에게 알려 주세요.

### 출발 준비 드릴의 `read` 키
- `Signal` enum(SPEC §4)에 벨트 키가 없다. 2차용으로 `SeatBelt.buckled.ROW_1_LEFT`, `SeatBelt.buckled.ROW_1_RIGHT`, `SeatBelt.buckled.ROW_2`를 미리 적었다(한국 좌핸들: ROW_1_LEFT = 운전석).
- [요청 feat/core] 로더는 **모르는 `read` 키를 만나도 죽지 않고** 그 신호를 `Unreadable`로 다룬다(→ `ifUnreadable: selfCheck`). `availability: coming_soon`이면 단계 검증을 건너뛰어도 된다. kotlinx.serialization은 `ignoreUnknownKeys = true`로.
- 2차에서 착석 센서가 없는 자리는 단계를 건너뛰어야 한다(기획서 "착석 센서 없는 차는 운전석만"). 스키마에 `"onlyIf": {"read": "Seat.occupied.ROW_1_RIGHT", "to": "ON"}` 같은 조건을 넣자는 **SPEC 변경 제안**(2차 범위).

---

## 4. 다른 팀에 요청 (모아 보기)

**feat/core**
1. `GEAR_NEUTRAL`(읽힘) → L1 잠금, 확인 대화는 기어를 못 읽을 때만. (2.1)
2. L0으로 가는 모든 전환·백그라운드 복귀 때 운전자 확인 삭제. (2.1)
3. L2 복귀는 P 2초 연속 후. L0 진입은 즉시. (2.1)
4. Car 서비스 오류 시 `uxRestricted=null` → L1, L2로 추정 금지. (2.2)
5. L0 진입 시 TTS 즉시 중단. (2.2)
6. restore 단계 미완료 상태로 끝나면 "와이퍼 OFF" 안내 데이터를 상태에 실어 줌. (2.4)
7. guard 의미: 참인 동안 Hint + 안정 시간 정지, 시작 순간에도 검사. (2.5)
8. 로더 검증: `Gear.selection`·경고등 계열 `read` 거부, 모르는 키는 Unreadable, `ignoreUnknownKeys`. (2.6, 2.7, 3)
9. `wrong` 힌트 600ms 유지 후 표시, MIST는 즉시 + 3초 유지. WiperState ON & 스위치 OFF → MIST 추정. (2.8)

**feat/screens**
1. 2분 공회전 카드: 시간 기반, 드릴 끝난 뒤에만, 알림·음성 없음, "과태료" 문구 금지. (2.3)
2. 완료 화면 비교는 본인 조작 시간만. 순위표 없음. (2.7)
3. 드릴 종료 화면에 와이퍼 OFF 리마인더 한 줄. (2.4)

**design/system**
1. guard·오조작 Hint는 빨간색·경고등 아이콘 금지(주황/중립). (2.6)
2. 위치 그림: 라이트 = 왼쪽 레버 끝 다이얼(맨 끝 칸 강조). 와이퍼 = "핸들 옆 레버"로 그리되 좌우를 단정하지 않는 그림, 또는 아반떼 기준 오른쪽 + "차종마다 다를 수 있어요" 캡션. 레버 이동 화살표(위/아래)는 그리지 않고 칸 이름(MIST·OFF·AUTO·LO·HI)을 줄지어 보여 주고 목표 칸을 강조. (1장)
3. 앞유리 디프로스터·뒷유리 열선·핸들 열선은 ISO 기호 모양(휘어진 앞유리+위로 물결, 네모+물결, 핸들+물결)으로.

**chore/compliance (DEMO_SCRIPT)**
1. 영상의 라이트 단계: 콘솔 Exterior Lights Mode 카드가 `getLowBeamLightSwitch` 값을 바꾸는지 미확인. 바뀌면 콘솔로 시연, 안 바뀌면 "켰어요" 확인으로 보여 주고 자막에 "콘솔에 라이트 신호 없음 → 운전자 확인"을 밝힌다.
2. 퀘스트 `label`은 모두 `null`. "현대차 안내" 라벨은 실제 현대차가 올린 퀘스트가 아니므로 1차 영상에서 쓰지 않는다(쓰려면 "예시" 표기).

---

## 5. 시연 영상용 매핑 (퀘스트 단계 → SDK 호출 → Virtual Console 조작)

**녹화 전 콘솔 초기값**: Gear **P**, Wipers **Off**, Defroster **Off**, Steering Wheel Heat **0(Off)**, Exterior Lights Mode **Auto**(카드가 로우빔과 연결될 때).

### 폭우 드릴 (45초)
| # | 단계 | SDK 호출 (읽기만) | 콘솔 조작 | 기대 판정 |
|---|---|---|---|---|
| 0 | 안전 단계 | `Gear.getCurrentGearSelectionState` + `registerGearSelection`, `CarUxRestrictionsManager` | Gear **P** | L2 주차 확정 칩 |
| 1 | 전조등 켜기 | `Light.getLowBeamLightSwitch` 폴링 + `registerLowBeamLightState` | Exterior Lights Mode **Auto → (잠깐 유지) → On/Low Beam** | Auto에서 600ms 머물면 "AUTO 말고 전조등이에요" → On 1초 → 통과. 카드가 연결 안 되면 `Unreadable` → "켰어요" 버튼 |
| 2 | 와이퍼 LO | `Wiper.getCurrentWiperSwitch(FRONT_WINDSHIELD)` 250ms 폴링 + `registerWiperState` | Wipers **Auto**(오조작 1) → **High**(오조작 2) → **Low** | "AUTO는 센서에 맡겨요" → "지금은 HI, 가장 빨라요" → Low 1초 → 통과 |
| 3 | 앞유리 김서림 | `HVAC.getHvacDefrosterState(FRONT_WINDSHIELD)` + `registerHvacDefrosterState` | Defroster **On**(앞유리) | 1초 → 통과 |
| 4 | 와이퍼 끄기 | `Wiper.getCurrentWiperSwitch` | Wipers **Off** | 1초 → 통과 → 완료 화면, 지난 기록 비교 |
| + | 일시정지 장면(선택) | `registerGearSelection` | 드릴 중 Gear **D** → **P** | D 즉시 Paused(SAFETY), 소리 끊김 → P 2초 뒤 "이어서 할까요?" |

MIST는 콘솔에서 만들 수 없다. 기획서 로그 예시의 MIST 오조작은 영상에서 **Auto와 High 오조작**으로 대신 보여 준다. 시간 초과 장면이 필요하면 3번에서 디프로스터를 누르지 않고 기다린다 → "괜찮아요. 여기 있어요" + "공조 패널, 앞유리 그림 버튼".

### 세차 대비 드릴 (30초)
| # | 단계 | SDK 호출 | 콘솔 조작 | 기대 판정 |
|---|---|---|---|---|
| 1 | 와이퍼 OFF | `Wiper.getCurrentWiperSwitch` | 시작 전 Wipers **Auto** → 시작 → **Low**(오조작, "반대쪽, LO로 갔어요") → **Off** | Off 1초 → 통과 |
| 1' | 이미 OFF | 같음 | 시작 전 Wipers **Off** | 1초 뒤 통과, 로그 "이미 켜져 있음" |

### 성에 낀 아침 드릴 (60초)
| # | 단계 | SDK 호출 | 콘솔 조작 | 기대 판정 |
|---|---|---|---|---|
| g | guard | `Wiper.getCurrentWiperSwitch` | 드릴 중 Wipers **Low** 또는 **Auto** | "언 유리엔 와이퍼를 꺼 두세요", 단계 판정 멈춤 → **Off**로 풀림 |
| 1 | 앞유리 디프로스터 | `HVAC.registerHvacDefrosterState(FRONT_WINDSHIELD)` | Defroster **On** | 통과 |
| 2 | 뒷유리 열선 | `HVAC.registerHvacDefrosterState(REAR_WINDSHIELD)` | Defroster 카드에 뒷유리 구역이 있으면 **On**, 없으면 "켰어요" 버튼 | 통과 / selfCheck |
| 3 | 핸들 열선 | `SteeringWheel.getHvacSteeringWheelHeatState` + `registerHvacSteeringWheelHeatState` | Steering Wheel Heat **1 이상** | `0`이 아니면 1초 → 통과 |

[추정] 콘솔 Defroster 카드가 앞·뒤 구역을 따로 바꾸는지 미확인. 뒷유리가 콘솔에서 안 바뀌고 값이 계속 `OFF`로 읽히면 selfCheck로 넘어가지 않아 막힌다 → [요청 feat/core] 시뮬레이션 패널에서 REAR를 바꿀 수 있게 하고, 실차 신호 모드에서 한 단계가 20초 이상 그대로면 "켰어요" 버튼을 보조로 띄우는 것을 검토.

### 출발 준비 드릴
- 준비 중 카드만. 2차에 `Seat.registerSeatBeltBuckledState(SeatArea(ROW_1_LEFT))` 등을 쓴다. 콘솔에는 착석 카드만 있어 벨트는 녹화 재생 어댑터로 시연.

---

## 6. 출처
- 현대 LX3 2026 국문 매뉴얼, 앞유리 와이퍼/와셔 작동하기: https://ownersmanual.hyundai.com/full_webhelp/LX3/2026/ko_KR/id23C6B060A4Y.html
- 현대 MEa 2026 영문 매뉴얼, Front Windshield Wipers: https://ownersmanual.hyundai.com/full_webhelp/MEa/2026/en_US/id6c0e8dde9d6.html
- 현대 구형 매뉴얼(MIST 방향 비교): https://www.manualsdir.com/manuals/772763/hyundai-2012-sonata.html?page=159
- 기아 국문 매뉴얼(라이트 스위치, 세차 시 와이퍼): https://ownersmanual.kia.com/full_webhelp/MQ4/2026/ko_KR/topics/chapter4_26_1.html
- 서울시 공회전 제한(환경분야 누리집): https://news.seoul.go.kr/env/archives/39671
- Vehicle SDK 2.3.2 javap (`Seat`, `SteeringWheel`, `IgnitionState`, `WiperSwitch`, `LightSwitch`, `WindowValue`) — 저장소 밖 로컬 추출본으로 확인, AAR은 커밋하지 않음.
