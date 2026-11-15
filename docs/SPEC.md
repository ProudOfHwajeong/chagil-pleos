# 차 길들이기 — 프로토타입 개발 계약서 (SPEC)

모든 에이전트가 이 문서를 기준으로 병렬 작업한다. 이 문서와 다르게 만들고 싶으면 PR 설명에 "SPEC 변경 제안"으로 적고, 임의로 다른 팀의 영역을 고치지 않는다.

## 0. 제품 한 줄
차를 세워 두고 진짜 레버와 버튼을 움직이면, 차(Vehicle SDK 값)가 직접 정답을 판정하는 차 기능 실습 앱. 1차 제출 범위: **폭우 드릴**이 Pleos Connect 에뮬레이터 + Virtual Console에서 실제로 판정되고, 세차 대비·성에 낀 아침 드릴은 같은 엔진에 데이터로 붙는다. 출발 준비(벨트) 드릴은 "준비 중" 카드로만 보인다.

## 1. 대상 환경
- Pleos Connect v2.1.0 에뮬레이터 (Android Automotive OS, API 34, arm64, Vehicle Service 2.3.22). 실측: 화면 2560x1440 가로, density 160(mdpi). 즉 화면이 2560x1440dp로 매우 넓다. 레이아웃 기준 캔버스는 2560x1440dp이고, 1280x720dp에서도 깨지지 않게 반응형으로 만든다. 글자와 버튼이 작아 보이지 않게 이 큰 캔버스 기준으로 크기를 정한다.
- 미리보기 AVD `chagil_preview`(emulator-5580)도 `wm density 160`으로 맞춰 두었다. 사용자의 Pleos 에뮬레이터는 emulator-5554이며 에이전트는 건드리지 않는다.
- Vehicle SDK `ai.pleos.playground:Vehicle:2.3.2` (Maven 의존성. AAR을 저장소에 커밋하지 않는다 — SDK 라이선스 3.4 재배포 금지).
- 미리보기용 일반 에뮬레이터 `chagil_preview` (2560x1440, density 160으로 덮어씀, serial `emulator-5580` 고정). Vehicle Service가 없으므로 앱은 자동으로 시뮬레이션 모드로 떨어져야 한다. **다른 adb 기기(사용자의 Pleos 에뮬레이터)에는 절대 접근하지 않는다. 모든 adb 명령은 `adb -s emulator-5580`.**
- 빌드: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"; ./gradlew assembleDebug testDebugUnitTest`

## 2. 실제 SDK API (2.3.2 AAR에서 javap로 확인함)
모두 콜백형. `Vehicle(context)` → `initialize()` → `getWiper()/getGear()/getHVAC()/getLight()/getSteeringWheel()` → `release()`.
- Gear: `getCurrentGearSelectionState(onComplete:(VehicleGear)->Unit, onFailed)`, `registerGearSelection(GearSelectionListener{onSelected(VehicleGear); onFailed(e)})`. VehicleGear: GEAR_UNKNOWN, GEAR_NEUTRAL, GEAR_REVERSE, GEAR_PARK, GEAR_DRIVE, GEAR_1..9.
- Wiper: `getCurrentWiperSwitch(WindowArea, onComplete:(WiperSwitch)->Unit, onFailed)` — **스위치 리스너 없음 → 250ms 폴링**. `registerWiperState(WindowArea, WiperStateListener{onWiperStateUpdated(WiperState)})`. WiperSwitch: OTHER, OFF, MIST, INTERMITTENT_LEVEL_1..5, CONTINUOUS_LEVEL_1..5, AUTO, SERVICE. WiperState: OTHER, OFF, ON, SERVICE. 구역 `WindowArea(WindowValue.FRONT_WINDSHIELD)`.
- HVAC 디프로스터: `getHvacDefrosterState(WindowArea, (Boolean)->Unit, onFailed)`, `registerHvacDefrosterState(WindowArea, HvacDefrosterStateListener{onHvacDefrosterStateUpdated(Boolean)})`. 구역 FRONT_WINDSHIELD / REAR_WINDSHIELD.
- Light: `getLowBeamLightSwitch((LightSwitch)->Unit, onFailed)` (폴링), `registerLowBeamLightState(LightStateListener{onLightStateUpdated(LightState)})`. LightSwitch: OFF, ON, DAYTIME_RUNNING, AUTOMATIC.
- SteeringWheel: `getHvacSteeringWheelHeatState((Int)->Unit, onFailed)`, `registerHvacSteeringWheelHeatState(listener{onHvacSteeringWheelHeatStateUpdated(Int)})`.
- 패키지: `ai.pleos.playground.vehicle.{Vehicle, api.*, listener.*, constant.control.*, constant.area.WindowValue, api.model.area.WindowArea}`.
- **set* 함수는 절대 호출하지 않는다.** 앱은 읽기만 한다 (기획서 약속).
- 주행 제한: `android.car.Car` + `CarUxRestrictionsManager` (`useLibrary("android.car")` 적용됨). 일반 에뮬레이터에서는 Car 서비스가 없으니 null/예외를 안전하게 처리.

## 3. 패키지와 소유권 (충돌 방지 규칙)
| 경로 | 소유 브랜치 / 역할 |
|---|---|
| `app/src/main/java/com/chagil/app/vehicle/**` | `feat/core` (개발자) |
| `.../safety/**`, `.../engine/**`, `.../data/**`, `app/src/test/**` | `feat/core` (개발자) |
| `app/src/main/assets/quests/*.json`, `docs/domain/**` | `content/quests` (자동차 전문가) |
| `.../ui/theme/**`, `.../ui/components/**`, `docs/design/**`, `app/src/main/res/drawable*/**`, `res/mipmap*/**` | `design/system` (UI 디자이너) |
| `LICENSE`, `NOTICE.md`, `PRIVACY.md`, `docs/compliance/**`, `docs/DEMO_SCRIPT.md` | `chore/compliance` (보안·법률) |
| `.../ui/screens/**`, `MainActivity.kt`, `.../AppViewModel.kt`, `AndroidManifest.xml`, `app/build.gradle.kts`, `README.md` | `feat/screens` (2단계, 통합) |

다른 영역을 고쳐야 하면 고치지 말고 PR 설명의 "다른 팀에 요청" 칸에 적는다. 1단계에서 `AndroidManifest.xml`/`build.gradle.kts` 변경이 꼭 필요하면 개발자(`feat/core`)만 최소한으로 한다.

## 4. 핵심 타입 계약 (1단계에서 개발자가 이 이름 그대로 구현, 디자이너·화면은 이 이름을 가정)
```kotlin
package com.chagil.app.vehicle
enum class Signal(val key: String) {
  GEAR("Gear.selection"), WIPER_SWITCH("Wiper.switch"), WIPER_STATE("Wiper.state"),
  LOW_BEAM_SWITCH("LowBeam.switch"), LOW_BEAM_STATE("LowBeam.state"),
  DEFROST_FRONT("HVAC.defroster.FRONT"), DEFROST_REAR("HVAC.defroster.REAR"),
  STEERING_HEAT("SteeringWheel.heat") }
sealed interface Reading { data class Value(val raw: String, val atMs: Long): Reading; data object Unreadable: Reading; data object Unknown: Reading }
// raw 값: enum 이름 그대로("MIST","GEAR_PARK","AUTOMATIC"), Boolean은 "ON"/"OFF", Int는 "0","1","2"
enum class SourceMode { PLEOS_SDK, SIMULATION }
interface VehicleSignalHub {
  val mode: StateFlow<SourceMode>
  val readings: StateFlow<Map<Signal, Reading>>
  val uxRestricted: StateFlow<Boolean?>   // null = 알 수 없음
  fun start(); fun stop()
}
class SimulatedVehicleSource : VehicleSignalHub { fun set(signal: Signal, raw: String) }  // 시뮬레이션 조작용
class PleosVehicleSource(context: Context) : VehicleSignalHub
class SwitchingVehicleHub(context): VehicleSignalHub  // SDK 시도 → 3초 안에 initialize/첫 값 실패 시 SIMULATION. 수동 전환 fun useSimulation(Boolean)

package com.chagil.app.safety
enum class SafetyLevel { L0_DRIVING, L1_STOPPED_UNCONFIRMED, L2_PARKED }
class SafetyMonitor(hub: VehicleSignalHub) { val level: StateFlow<SafetyLevel>; fun confirmParkedByDriver(); fun clearDriverConfirmation() }
// 규칙: uxRestricted==true 또는 GEAR∈{DRIVE,REVERSE,1..9} → L0. GEAR_PARK → L2. 그 외(UNKNOWN/NEUTRAL/읽기불가) → 운전자 확인 있으면 L1 아니면 L1(미확인, 확인 대화 필요).
// 기어가 P→D로 바뀌면 운전자 확인도 지운다.

package com.chagil.app.engine
// Quest/Step: §5 JSON을 kotlinx.serialization으로 그대로 매핑
sealed interface DrillState {
  data object Idle; data class Running(questId, stepIndex, remainingMs, stepStatus: StepStatus, lastRead: String?, hint: Hint?)
  data class Paused(reason: PauseReason, snapshot: Running); data class TimedOut(questId, stepIndex, passed: List<Boolean>)
  data class Completed(questId, elapsedMs, previousBestMs: Long?) }
enum class StepStatus { WAITING, STABILIZING, PASSED, SELF_CHECK_NEEDED }
data class Hint(text: String, voice: String?)
data class LogEntry(atMs: Long, text: String)   // 기획서 판정 로그 형식: "00:04.0 · Wiper 스위치 OFF → MIST · 오조작: …"
class DrillEngine(hub, safety, clock) { val state: StateFlow<DrillState>; val log: StateFlow<List<LogEntry>>; fun start(quest); fun selfCheck(); fun pause(); fun resume(); fun abort() }
```
정확한 시그니처 세부는 개발자가 정하되 위 이름·의미는 유지한다. 바꾸면 PR에 적는다.

## 5. 퀘스트 JSON 스키마 (`app/src/main/assets/quests/<id>.json`)
```json
{ "id": "rain_drill", "title": "폭우 드릴", "subtitle": "…", "level": "L1", "limitSec": 45,
  "zone": "windshield", "availability": "open | coming_soon", "label": null,
  "intro": {"text": "…", "voice": "…"},
  "steps": [
    {"id": "lowbeam", "label": "전조등 켜기", "instruction": "전조등을 켜 보세요", "voice": "…",
     "control": "LIGHT_STALK", "read": "LowBeam.switch", "to": "ON", "stableMs": 1000,
     "alreadyOn": "pass", "ifUnreadable": "selfCheck",
     "wrong": {"AUTOMATIC": {"text": "…", "voice": "…"}},
     "where": {"text": "…", "voice": "…"} },
    {"id": "wiper1", "read": "Wiper.switch", "toAnyOf": ["INTERMITTENT_LEVEL_1","CONTINUOUS_LEVEL_1"], "stableMs": 1000, "…": "…"},
    {"id": "restore", "read": "Wiper.switch", "to": "OFF", "role": "restore"}
  ],
  "guards": [ {"read": "Wiper.switch", "notAnyOf": ["OFF"], "text": "언 유리에서는 와이퍼를 꺼 두세요", "voice": "…"} ],
  "done": {"text": "…", "voice": "…"}
}
```
- 판정 조건: `to` | `toAnyOf` | `notAnyOf` 중 하나. 값이 조건을 만족한 채 `stableMs`(기본 1000) 동안 유지되면 PASSED.
- `alreadyOn: "pass"` — 단계 시작 시 이미 만족이면 안정 시간 뒤 통과 + 로그 "이미 켜져 있음".
- `ifUnreadable: "selfCheck"` — 신호가 Unreadable이면 "켰어요" 버튼(운전자 확인)으로 통과.
- `wrong` — 값이 이 키로 바뀌면 즉시 Hint. `where` — 시간 초과 화면에서 위치 안내.
- `control`: `LIGHT_STALK | WIPER_STALK | HVAC_FRONT_DEFROST | HVAC_REAR_DEFROST | STEERING_HEAT_BUTTON | GEAR_SELECTOR | SEAT_BELT` — 디자이너가 위치 그림을 그리는 키.
- `level`: 이 퀘스트를 여는 최소 안전 단계(L1 또는 L2). 현재 단계가 낮아지면 엔진은 Paused(SAFETY).

## 6. 화면 목록 (2단계 `feat/screens`가 구현, 디자이너가 `docs/design/SCREENS.md`에 배치 명세)
1. 홈: 안전 상태 칩, 신호 출처 칩(실차 신호 / 시뮬레이션), 드릴 카드(폭우·세차 대비·성에 낀 아침 열림, 출발 준비 "준비 중"), 진행 지도 미니.
2. 안전 확인(L1): "P에 두셨나요?" 확인 다이얼로그. L0이면 앱 화면 대신 "주행 중에는 쉬어요" 정지 화면(시스템이 가리지 않는 일반 에뮬레이터 대비).
3. 드릴 진행: 지시 한 줄(큰 글씨) + 음성 문구 + 위치 그림(조작부 깜빡임) + 단계 리스트 + 남은 시간 + "차가 확인하는 중"/읽은 값. 큰 버튼 최대 2개(일시정지·그만하기).
4. 오조작: 드릴 진행 화면 안에서 지시가 Hint로 바뀜("지금은 '한 번 닦기'예요"), 읽은 값 표시.
5. 시간 초과: "괜찮아요. 여기 있어요" + 못 찾은 조작부 위치를 크게, 버튼 2개(다시·나중에). "실패"라는 말 금지.
6. 완료: 통과 시간, 지난 기록 비교, 도면 미니에 방금 쓴 조작부 점등, 버튼 2개(도면 보기·다시).
7. 내 차 도면(진행 지도): 운전석에서 본 그림, 조작부별 칸(점등/미점등/퀴즈형).
8. 일시정지 카드: 주행으로 멈췄다가 다시 주차하면 "이어서 할까요?".
9. 판정 로그 패널 + (시뮬레이션 모드에서만) 시뮬레이터 조작 패널. 패널 상단에 항상 "시뮬레이션 — 실제 차 신호가 아닙니다" 표시. 실차 신호 모드에선 조작 패널 숨김.
- 낮/밤 테마 둘 다. 터치 타깃 ≥ 76dp(AAOS 가이드), 본문 ≥ 24sp, 지시 ≥ 48sp, 대비 WCAG AA 이상.

## 7. 브랜치 / PR 규칙
- 원격: GitHub 비공개 저장소 `origin`. `main`은 통합 담당만 머지한다.
- 각 에이전트는 자기 worktree에서만 작업: `git -C <repo> fetch origin && git -C <repo> worktree add <repo>/.worktrees/<짧은이름> -b <branch> origin/main`.
- 작은 단위로 커밋, 메시지는 `type(scope): 요약` (예: `feat(engine): stable-ms 판정`). 끝나면 `git push -u origin <branch>` 후 `gh pr create --base main --title … --body …` (본문에 한 일, 확인한 것, SPEC 변경 제안, 다른 팀에 요청).
- **커밋 금지**: 키스토어·비밀번호, SDK AAR, 개인정보(이름·이메일·전화), 사용자의 에뮬레이터 캡처 속 계정 정보.
- 1단계 PR 머지 순서: chore/compliance → feat/core → content/quests → design/system. 각 머지 뒤 main이 `assembleDebug testDebugUnitTest` 통과해야 한다.
