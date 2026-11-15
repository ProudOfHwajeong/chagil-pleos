# 보안과 법률 검토 (scaffold f90863b, SPEC)

검토 범위: `main` f90863b(scaffold), `docs/SPEC.md`, 기획서 본문, Pleos SDK 2.3.2 AAR(매니페스트와 `@RequiresPermission` 어노테이션), 42dot SDK License Agreement 전문, 공모전 GitHub 제출 안내.

이 문서는 공모전 준비를 위한 실무 검토이고 법률 자문이 아니다. 출시 단계에서는 전문가의 확인을 받는다.

## 0. 요약: 먼저 고칠 것

| 번호 | 심각도 | 내용 | 담당 |
|---|---|---|---|
| 1 | 치명 | 매니페스트에 기어와 와이퍼를 읽는 데 필요한 `CAR_DRIVE`, `CAR_WINDOWS` 권한이 없다. 실제 Pleos 에뮬레이터에서 기어와 와이퍼 읽기가 권한 오류로 실패하고, 안전 단계가 L2에 가지 못해 드릴이 열리지 않는다(2장) | feat/core |
| 2 | 높음 | release 서명 설정이 없으면 `*-release-unsigned.apk`가 나와 실행 가능한 release라는 요건을 채우지 못한다(5장) | 통합(release) |
| 3 | 높음 | 시뮬레이션 표시나 예시 표시가 빠지면 약관 제9조 제2항(작품 오인 금지)을 어길 소지가 있다(7장) | design/system, feat/screens |
| 4 | 중간 | 현대차 안내 라벨이 실제 제조사 게시물처럼 보인다. 예시라고 표시한다(6장) | content/quests, design/system |
| 5 | 중간 | 커밋 작성자의 실명과 개인 이메일이 공개 전환 때 영구히 노출된다. 팀 결정이 필요하다(9장) | 팀 |
| 6 | 낮음 | 필요 없는 권한 `CAR_POWERTRAIN`, `CAR_INFO`를 지운다(최소 권한)(2장) | feat/core |

## 1. LICENSE 선택: 모든 권리 보유와 심사용 이용 허락

- 선택: 오픈소스 라이선스를 붙이지 않는다. 저작권은 팀이 모두 갖고, 주최 측과 심사위원에게는 읽기, 빌드, 설치, 실행, 심사용 인용만 허락하는 자체 라이선스(`LICENSE`)를 쓴다.
- 이유
  - 공모전은 저장소 공개(Public)를 요구하지만 오픈소스화를 요구하지는 않는다. MIT나 Apache-2.0을 붙이면 누구나 기획과 코드를 상업적으로 다시 쓸 수 있다. 2차 PT나 시범 사업 협상을 하기도 전에 아이디어를 지킬 수단을 스스로 버리게 된다.
  - 라이선스 파일이 아예 없어도 법적으로는 모든 권리를 보유한 상태다. 하지만 심사자가 빌드하고 설치해도 되는지가 불분명해진다. 심사용 이용을 명시적으로 허락해 두면 심사 행위가 확실히 적법해진다.
  - 저장소에는 독점 라이선스인 Pleos SDK를 쓰는 코드가 있다. 앱 코드를 GPL 같은 카피레프트로 내면 SDK와 결합하는 문제가 생기고, 허용형으로 내도 SDK까지 자유롭게 쓸 수 있다는 오해를 줄 수 있다. 그래서 자체 라이선스 4항에서 제3자 구성 요소를 따로 떼어 두었다.
  - GitHub 이용약관은 다른 이용자에게 공개 저장소를 GitHub 안에서 보고 포크할 권리를 준다. 이것은 막을 수 없으므로 LICENSE 3항에 공개가 곧 오픈소스라는 뜻은 아니라고 분명히 적었다.
- 한계: 아이디어 자체는 저작권으로 보호되지 않는다. 보호되는 것은 표현뿐이다. 기획을 실제로 지키는 수단은 접수 기록(제출 일시, 커밋 해시)과 저장소 이력이다. 접수 메일에 커밋 해시를 남기는 것 자체가 선행 증거가 된다.
- 공모전 약관에 수상작 권리 귀속이나 이용 허락 조항이 있으면 그 조항이 우선한다. 팀이 약관의 지식재산권 조항을 한 번 확인한다.

## 2. 매니페스트 권한: 최소 권한

SDK 2.3.2의 각 API에 붙은 `@RequiresPermission` 값을 javap로 확인했다. AAR의 `AndroidManifest.xml`은 `ai.pleos.playground.service.vehicle.VEHICLE_BINDING`만 선언하며, 이 권한은 매니페스트 병합으로 자동으로 들어온다.

| 앱이 쓰는 API | SDK가 요구하는 권한 | 현재 매니페스트 |
|---|---|---|
| `Gear.getCurrentGearSelectionState`, `registerGearSelection` | `pleos.car.permission.CAR_DRIVE` | 없음, 추가 필요 |
| `Wiper.getCurrentWiperSwitch`, `getCurrentWiperState`, `registerWiperState`, `check*Capabilities` | `pleos.car.permission.CAR_WINDOWS` | 없음, 추가 필요 |
| `HVAC.getHvacDefrosterState`, `registerHvacDefrosterState`, `checkHvacDefrosterCapabilities` | `pleos.car.permission.CAR_CLIMATE` | 있음 |
| `SteeringWheel.getHvacSteeringWheelHeatState`, `register…` | `pleos.car.permission.CAR_CLIMATE` | 있음 |
| `Light.getLowBeamLightSwitch`, `registerLowBeamLightState`, `check…` | `pleos.car.permission.CAR_LIGHTS` | 있음 |
| `CarUxRestrictionsManager` (android.car) | 없음 | 해당 없음 |

feat/core가 반영할 권고 매니페스트는 다음과 같다.

```xml
<!-- Read-only. SDK 2.3.2 @RequiresPermission 기준. CONTROL_* 권한은 선언하지 않는다. -->
<uses-permission android:name="pleos.car.permission.CAR_DRIVE" />    <!-- Gear -->
<uses-permission android:name="pleos.car.permission.CAR_WINDOWS" />  <!-- Wiper -->
<uses-permission android:name="pleos.car.permission.CAR_LIGHTS" />   <!-- LowBeam -->
<uses-permission android:name="pleos.car.permission.CAR_CLIMATE" />  <!-- Defroster, Steering heat -->
<!-- 임시: SDK 어노테이션에는 없으나 공식 샘플이 선언함. 실제 에뮬레이터에서 와이퍼 읽기가
     이것 없이 되는지 확인되면 지운다. 읽기 전용이라 위험은 낮다. -->
<uses-permission android:name="pleos.car.permission.READ_WINDSHIELD_WIPERS" />
```

- 지울 것: `CAR_POWERTRAIN`(SDK 2.3.2 어디에도 쓰이지 않는다. 기어는 `CAR_DRIVE`다), `CAR_INFO`(`CarInfo` API를 쓰지 않는다).
- 절대 넣지 말 것: `CONTROL_CAR_*`, `CONTROL_WINDSHIELD_WIPERS`, `PRIVATE_CAR_*`(특권), `INTERNET`, 위치 권한.
- 공식 샘플은 읽기 권한과 제어 권한을 30여 개 모두 선언한다. 샘플은 API 카탈로그라서 그런 것이고, 우리 앱이 따라 하면 안 된다.
- 이 권한들은 Vehicle Service에서 `protectionLevel="normal"`(설치할 때 부여)이라 런타임 요청 대화상자는 필요 없다.
- 실제 에뮬레이터 확인(팀이 사용자 기기에서 직접): `adb -s <Pleos 에뮬레이터> logcat | grep -iE "SecurityException|permission"`을 켜 두고 드릴을 한 번 실행하는 동안 권한 오류가 없는지 본다. 오류가 나면 메시지에 적힌 권한만 추가한다.
- 빌드한 뒤 최종 권한 확인:
  ```sh
  ~/Library/Android/sdk/build-tools/35.0.0/aapt2 dump permissions app/build/outputs/apk/release/*.apk
  # 기대: CAR_DRIVE, CAR_WINDOWS, CAR_LIGHTS, CAR_CLIMATE, (READ_WINDSHIELD_WIPERS), VEHICLE_BINDING,
  #       com.chagil.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION(AndroidX 자동 추가, 정상) 만. CONTROL_ / INTERNET 없음.
  ```
  f90863b debug APK 실측 결과는 `CAR_CLIMATE, CAR_LIGHTS, CAR_POWERTRAIN, CAR_INFO, READ_WINDSHIELD_WIPERS, VEHICLE_BINDING, DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`이었다. `CAR_DRIVE`와 `CAR_WINDOWS`가 빠져 있는 것을 확인했다.

## 3. `set*` 호출 금지 확인

이 앱이 쓰는 5개 도메인에서 SDK의 제어 함수는 `setWiperSwitch, setHvacDefroster, setHvacMaxDefrost, setHvacSteeringWheelHeat, setLowBeamLightSwitch, setHighBeamLightSwitch, setFrontFogLightSwitch, setRearFogLightSwitch, setTailLampLightSwitch, setCabinLightSwitch, setReadingLightSwitch, setAmbient*, setHvac*`이다. Gear에는 set 함수가 없다.

```sh
# 1) 소스: SDK 제어 함수 이름으로 호출 찾기 → "OK"가 나와야 통과
P='\.set(Wiper|Hvac|LowBeam|HighBeam|FrontFog|RearFog|TailLamp|Cabin|Reading|Ambient)[A-Za-z]*[[:space:]]*\('
grep -rnE --include='*.kt' --include='*.java' "$P" app/src && echo "FAIL: set* 호출 있음" || echo "OK: set* 호출 없음"

# 2) 바이트코드: com.chagil.* 클래스가 SDK api 인터페이스의 set*을 부르는지 → "OK"가 나와야 통과
~/Library/Android/sdk/build-tools/35.0.0/dexdump -d app/build/outputs/apk/debug/app-debug.apk \
  | LC_ALL=C awk '/Class descriptor/ {own = ($0 ~ /Lcom\/chagil\//)}
      own && /invoke-.*Lai\/pleos\/playground\/vehicle\/api\/[A-Za-z]+;\.set/ {n++; print}
      END {print (n ? "FAIL" : "OK: dex에 set* 호출 없음")}'
```

f90863b scaffold에서 두 명령 모두 OK였다. 2번 패턴은 SDK 자체 클래스에서 78건이 잡혀, 패턴이 제대로 동작하는 것도 확인했다.

- `SimulatedVehicleSource.set(signal, raw)`은 앱 안의 가짜 신호를 바꾸는 함수라 SDK 호출이 아니다. 위 패턴에도 걸리지 않는다.
- 권고: feat/core가 1번 검사를 단위 테스트로 넣어 CI처럼 빌드할 때마다 확인한다.

## 4. 매니페스트의 다른 항목

| 항목 | 상태 | 판단 |
|---|---|---|
| `android:allowBackup="false"` | 있음 | 좋다. Android 12 이상에서 기기 간 이전까지 막으려면 `android:dataExtractionRules`에 빈 규칙을 추가한다. 저장 데이터가 기록뿐이라 선택 사항이고 중요도도 낮다 |
| exported 컴포넌트 | `MainActivity`(LAUNCHER)만 `exported="true"` | 적절하다. Service, Receiver, Provider를 추가하면 기본을 `exported="false"`로 한다 |
| 인텐트 extras | 시뮬레이션 조작을 `am start` extras로 받게 만든다면 | debug 빌드에서만 받게 한다(`BuildConfig.DEBUG` 확인). release에서 외부 앱이 판정 값을 넣지 못하게 하려는 것이다 |
| 인터넷 | 권한 없음 | 좋다. PRIVACY.md에 보내는 것이 없다고 쓴 근거다. 의존성을 추가할 때 이 권한이 병합되지 않는지 release마다 2장 명령으로 확인한다 |
| `debuggable` | release 기본값 false | 직접 지정하지 않는다 |
| `isMinifyEnabled=false` | 그대로 | 1차에는 문제없다. 켜면 SDK consumer proguard 규칙을 확인해야 한다 |
| 주행 중 동작 | DO(주행 최적화) 앱이 아니므로 AAOS가 주행 중에 차단 | 도로교통법 제49조 제1항(운전 중 영상표시장치 시청과 조작 금지)의 취지와 맞다. 앱 자체 L0 정지 화면도 유지한다. Pleos Connect v2.1.0에서 기어를 D로 바꾸면 Pleos가 앱 화면을 가리고 "주행 중에는 사용할 수 없습니다"를 띄우는 것을 확인했다 |

## 5. 서명 키

- 지금 `app/build.gradle.kts`는 루트의 `keystore.properties`(git-ignored)를 읽어 release에 서명한다. `.gitignore`에 `keystore.properties`, `*.jks`, `*.keystore`가 들어 있다. 좋다.
- 키스토어 파일은 저장소 밖 `~/.chagil-signing/`에 두고, `keystore.properties`의 `storeFile`은 반드시 절대 경로로 쓴다. 상대 경로로 쓰면 키스토어를 저장소 안에 두고 싶어진다.
- `keystore.properties`가 없으면 release가 서명되지 않은 채 빌드된다. 통합 담당은 release 전에 파일이 있는지 보고 `apksigner verify`로 검증한다(체크리스트 B).
- 비밀번호는 커밋 메시지, PR 본문, README에 쓰지 않는다. 키를 잃으면 같은 패키지명으로 업데이트 설치를 할 수 없으니 키스토어를 따로 백업한다.

## 6. 상표와 현대차 안내 라벨

- Pleos, Pleos Connect, 42dot(42dot Inc.)과 현대차, 현대자동차, Hyundai(현대자동차)라는 이름은 대상 플랫폼과 공모전을 설명하는 데만 쓴다(설명적, 지명적 사용). SDK License 3.6에 따라 42dot 상표와 로고를 쓸 권리는 없다.
- 앱 이름, 아이콘, 런처 라벨, 스플래시에는 위 이름과 로고, 차종명(그랜저 등), 현대차 고유 서체와 색(현대 블루 조합 등)을 쓰지 않는다. 차종명은 설명에만 쓴다는 기획서 원칙과 같다.
- 현대차 안내 퀘스트 라벨: 프로토타입에서는 제조사가 실제로 올린 것처럼 보이면 안 된다(표시광고법상 기만적 표시 우려, 약관 제9조 제2항). 권장 표기는 다음과 같다.
  - 칩(짧게): `제조사 안내 · 예시` (현대차 이름을 쓰고 싶으면 `현대차 안내 · 예시`)
  - 카드 아래 한 줄: `시연용 예시예요. 현대자동차가 실제로 올린 퀘스트가 아닙니다.`
  - 담당자 화면(2차 PT)을 만들 때는 제목 옆에 `제조사 화면 예시 (시연용)`.
- README와 영상 끝에 넣을 한 줄: `이 앱은 공모전 출품작이며 42dot·현대자동차그룹이 만들거나 보증한 앱이 아닙니다.`

## 7. 시뮬레이션 표시 규칙 (앱과 영상 공통)

공모전 약관 제9조 제2항(작품 오인 금지)과 표시광고법의 기만 표시 금지 취지를 따른다. 보는 사람이 차가 판정했다고 믿을 만한 장면에서는, 실제로 차(Vehicle SDK)가 판정했는지가 늘 보여야 한다.

앱 (SPEC 6장과 같다):

1. 홈의 신호 출처 칩은 `차 신호`와 `시뮬레이션` 두 상태만 둔다. SPEC의 실차 신호라는 말은 에뮬레이터 녹화에서 실제 차로 읽힐 수 있어서 `차 신호`(보조 문구 `Vehicle SDK`)를 권한다.
2. 시뮬레이션 모드에서는 로그 패널뿐 아니라 홈, 드릴, 완료 화면 모두 위쪽에 `시뮬레이션` 배지가 보여야 한다.
3. 시뮬레이터 조작 패널 위쪽에 고정하는 문구는 `시뮬레이션 중이에요. 실제 차 신호가 아니에요`이다(SPEC 6장 9번).
4. 판정 로그의 시뮬레이션 항목은 앞에 `SIM ·`을 붙인다. 예: `SIM · 00:04.0 · Wiper 스위치 OFF → AUTO · 오조작`.
5. 운전자 확인(켰어요 버튼)으로 통과한 단계는 로그에 `운전자 확인`으로 남겨, 차가 판정한 것과 구분한다.
6. 음성(TTS)이 구현되지 않았으면 화면의 음성 문구를 `음성 안내 문구(예정)`처럼 표시한다. 영상에서도 소리가 나는 것처럼 말하지 않는다.

영상은 [DEMO_SCRIPT.md](../DEMO_SCRIPT.md)의 자막 규칙을 따른다. 첫 화면에 녹화 환경을 알리고, 시뮬레이션 장면에는 화면 배지와 자막을 둘 다 넣는다. Virtual Console에는 MIST가 없으므로 기획서의 MIST 오조작 예시는 영상에서 AUTO로 보여 주고, 그 사실을 자막에 적는다.

## 8. 게임산업법, 개인정보, 위치정보

- 게임산업법: 등급분류 의무(제21조 제1항)는 게임물을 "유통시키거나 이용에 제공하게 할 목적"일 때 생긴다. 1차 제출은 심사용이라 일반 이용자에게 유통하지 않으므로, 지금은 등급분류가 필요 없다고 본다. 다만 이 앱에는 시간 기록, 배지, 진행 지도가 있어서 출시하면 게임물로 볼 여지가 있다.
  - 출시 전에 할 일: Pleos 앱 마켓이 자체등급분류사업자(제21조의2)인지 확인한 뒤, 마켓의 등급 절차를 따르거나 게임물관리위원회에 문의한다. 교육용, 학습용 예외(제21조 제1항 단서)는 시행령에서 요건과 절차를 확인한다.
  - 지금 할 일: 화면, 영상, README에서 게임이라는 말보다 드릴, 익히기, 실습이라는 말을 쓴다(기획서 방침과 같다). 무작위 보상, 유료 아이템, 순위표는 계속 넣지 않는다.
- 개인정보 보호법: 이름, 연락처, 계정, 식별자를 받지 않고, 차량 상태값은 기기 안에서만 쓰고 버린다. 팀이 개인정보를 처리하지 않는 구조이며, SDK License 4.3에 따라 `PRIVACY.md`로 처리 내용을 공개했다. 저장 항목이 바뀌면 PRIVACY.md 표를 먼저 고친다.
- SDK License 8.1.2(이용자 동의를 받은 경우에만 데이터 조회): 앱 첫 화면이나 홈 아래쪽에 한 줄 고지를 둔다. 문구는 `연습 판정을 위해 기어·와이퍼·전조등·공조 상태를 읽기만 해요. 기기 밖으로 보내지 않아요.`이다. 드릴 시작 버튼을 누르는 것을 그 드릴 판정에 대한 동의로 본다.
- 위치정보법: 위치를 읽지 않는다. 기어나 와이퍼 상태는 위치정보가 아니다. 기획서의 장거리 휴식 알림을 붙일 때 다시 검토한다. 서버와 연동하려면 위치기반서비스사업 신고를 먼저 해야 한다.
- 아동: 1차 범위에는 아이 프로필이 없다. 만 14세 미만의 정보를 수집하지 않는다.

## 9. 커밋 작성자와 계정 노출: 팀 결정 필요

- 지금 모든 커밋의 작성자가 실명과 개인 Gmail 주소이고, 이후 에이전트 커밋도 같은 git 설정을 쓴다. 저장소가 11/16에 공개되면 누구나 이 정보를 볼 수 있고, 포크와 아카이브로 영구히 남는다. 저장소는 팀 조직(ProudOfHwajeong)으로 옮겨서 개인 계정은 주소에 보이지 않는다.
- 블라인드 심사인지: 받은 제출 안내에는 블라인드 심사 규정이 없다. 오히려 접수 메일과 신청서로 팀을 밝히고, 저장소 주소(계정명 포함)를 직접 내라고 한다. 그래서 저장소에 신원이 보이는 것 자체는 감점 사유로 보이지 않는다. 약관에 작품에 소속이나 실명을 표기하지 말라는 조항이 있는지만 팀이 확인한다.
- 남는 문제는 개인 이메일이 공개적으로 노출되는 것(스팸, 개인정보)이다. 선택지는 셋이다.
  1. 그대로 둔다. 가장 안전하게 마감할 수 있지만 노출은 감수해야 한다.
  2. 앞으로의 커밋만 바꾼다. 이 저장소에서만 `git config user.email "<숫자ID>+<계정명>@users.noreply.github.com"`을 설정한다(주소는 GitHub 설정의 Emails에서 확인). 첫 scaffold 커밋 1개는 그대로 남는다. 브랜치 충돌은 없다.
  3. 공개 전에 이력을 다시 쓴다. 모든 커밋의 작성자를 바꾼 뒤 force-push한다. 진행 중인 브랜치와 PR이 모두 깨지므로 마감 당일에는 권하지 않는다. 하려면 모든 PR을 머지한 뒤, 접수 메일을 보내기 전에 한 번만 하고, 새 해시를 메일에 적는다.
  - 권장은 2번이다. 마감 전 아무 때나 사람이 직접 한다. GitHub 설정 Emails의 "Keep my email addresses private"와 "Block command line pushes that expose my email"도 함께 켤 수 있다. 다만 이것을 켜면 지금 설정으로는 push가 거부되므로 2번을 먼저 한다.
- 이 PR은 git 설정을 바꾸지 않았다.

## 10. 비밀 정보 스캔 (f90863b 기준)

- 추적 파일 17개: `.gitignore, apk/.gitkeep, app/*, build.gradle.kts, docs/SPEC.md, gradle/*, gradlew(.bat), README.md, settings.gradle.kts`. AAR, 키스토어, `local.properties`, 이미지는 없다.
- `git log --all -p`에서 비밀번호, 토큰, 개인키 패턴은 나오지 않았다. 서명 설정의 변수 이름만 있다. 실명과 이메일은 커밋 메타데이터(9장)에만 있다.
- 다시 검사하는 명령(최종 커밋 직전, 통합 담당):
  ```sh
  git log --all -p | grep -nEi "storePassword *= *['\"]?[^k ]|keyPassword *= *['\"]?[^k ]|BEGIN .*PRIVATE KEY|ghp_[A-Za-z0-9]{20,}|AIza[0-9A-Za-z_-]{30,}|@gmail\.com|@naver\.com|01[016789]-?[0-9]{3,4}-?[0-9]{4}" | grep -v "^[0-9]*:Author:"
  git ls-files | grep -Ei "\.(aar|jks|keystore|p12|pem)$|keystore\.properties|local\.properties|\.(png|jpg|mp4)$"
  # 이미지가 나오면 계정·이메일·알림이 찍혔는지 눈으로 확인
  ```
  `gitleaks`는 설치돼 있지 않다. 원하면 `brew install gitleaks && gitleaks detect --log-opts="--all"`로 쓸 수 있다.

## 11. SDK 라이선스와 참고 자료 다루기

- SDK License 3.4는 복제(백업 제외), 재배포, 역컴파일, 디스어셈블을 금지한다. 개발 중 로컬에서 javap로 공개 API 시그니처와 어노테이션을 본 것은 앱 개발(3.1에서 허락한 범위)을 위한 최소한의 확인이었다. 그래도 추출한 클래스 파일과 javap 출력은 저장소, PR, 영상에 넣지 않는다. 공모전이 끝나면 로컬 사본을 지운다.
- SPEC 2장의 API 목록은 IDE 자동완성 수준의 공개 인터페이스 정보다. 구현 코드나 내부 클래스 이름(`utils.f` 같은 난독화 이름)은 문서에 옮기지 않는다.
- APK에는 SDK 코드가 빌드 결과로 들어간다. 주최 측이 APK 제출을 요구했고 SDK가 이 공모전용으로 제공되었으므로 3.1 범위의 이용으로 본다. 그래도 APK를 공모전 밖(다른 마켓이나 사이트)에 올리지 않는다.
- 공식 샘플 앱(`pleos-sample-vehicle-app`)에는 라이선스가 명시되어 있지 않다. 그러니 코드는 복사하지 않고 호출 순서 같은 패턴만 참고한다. PR 리뷰 때 샘플과 같은 주석이나 이름이 그대로 들어 있지 않은지 본다.
- SDK License 6.1: 42dot이 사용 통계를 모을 때는 개발자에게 알리고 동의를 받는다. 우리 앱은 따로 통계를 보내지 않는다.

## 12. 다른 팀에 요청 (PR 본문에도 같은 목록)

- feat/core: 2장 권고 매니페스트 반영(`CAR_DRIVE`, `CAR_WINDOWS` 추가, `CAR_POWERTRAIN`, `CAR_INFO` 삭제). 3장 `set*` 검사를 단위 테스트로. 화면이 꺼지면(`onStop`) 폴링과 리스너 해제. 판정 로그는 메모리에만. SharedPreferences 항목은 PRIVACY.md 2장 표 범위 안에서. 시뮬레이션 주입 extras는 debug 빌드에서만. 로그 항목에 출처(SDK, SIM, 운전자 확인) 필드.
- content/quests: 와이퍼 단계 `wrong`에 `AUTO`, `CONTINUOUS_LEVEL_2`, `MIST` 각각에 맞는 문구(예: AUTO는 `지금은 '자동'이에요. 한 단계만 올려 보세요`). 콘솔 High는 Pleos Connect v2.1.0에서 `CONTINUOUS_LEVEL_3`으로 확인됐고, 폭우 드릴은 이 값을 정답으로 받는다. `toAnyOf`를 고칠 때는 High가 계속 정답으로 잡히는지 확인한다. 현대차 안내 예시 퀘스트의 `label`은 `제조사 안내 · 예시`.
- design/system: 출처 칩 `차 신호`와 `시뮬레이션`, 시뮬레이션 배지(모든 화면 위쪽), 예시 라벨 컴포넌트. 글꼴이나 아이콘을 추가하면 이름과 라이선스(OFL 등)를 PR에 적어 주면 NOTICE.md에 넣는다. 현대차와 Pleos 로고, 현대 블루 계열 브랜드 색 조합은 쓰지 않는다.
- feat/screens: README 필수 5항목(체크리스트 C)과 프로토타입 범위, 면책 한 줄. 홈 아래쪽 데이터 고지 한 줄(8장). 앱 안의 오픈소스 라이선스 보기(2차 전까지. Apache-2.0 고지 의무에 대비한 것이고, 1차는 NOTICE.md로 대신한다).
- 통합(release): 서명 확인, APK 파일명, 비밀 재검사, 해시 확정 순서(체크리스트 B, F).
