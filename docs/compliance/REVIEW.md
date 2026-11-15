# 보안·법률 검토 — scaffold(f90863b) + SPEC

검토 범위: `main` f90863b(scaffold), `docs/SPEC.md`, 기획서 본문, Pleos SDK 2.3.2 AAR(매니페스트·`@RequiresPermission` 어노테이션), 42dot SDK License Agreement 전문, 공모전 GitHub 제출 안내.
이 문서는 공모전 준비를 위한 실무 검토이며 법률 자문이 아닙니다. 출시 단계에서는 전문가 확인을 받으세요.

## 0. 요약 — 먼저 고칠 것

| # | 심각도 | 내용 | 담당 |
|---|---|---|---|
| 1 | **치명** | 매니페스트에 기어·와이퍼 읽기에 필요한 `CAR_DRIVE`·`CAR_WINDOWS` 권한이 없음 → 실제 Pleos 에뮬레이터에서 기어·와이퍼 읽기가 권한 오류로 실패하고, 안전 단계가 L2에 못 가 드릴이 안 열림 (§2) | feat/core |
| 2 | 높음 | release 서명 설정이 없으면 `*-release-unsigned.apk`가 나와 "실행 가능한 release" 요건 불충족 (§5) | 통합(release) |
| 3 | 높음 | 시뮬레이션·예시 표시가 빠지면 약관 제9조②(작품 오인 금지) 위반 소지 (§7) | design/system, feat/screens |
| 4 | 중간 | "현대차 안내" 라벨이 실제 제조사 게시물처럼 보임 → "예시" 표시 (§6) | content/quests, design/system |
| 5 | 중간 | 커밋 작성자 실명·개인 이메일이 공개 전환 시 영구 노출 → 팀 결정 필요 (§9) | 팀 |
| 6 | 낮음 | 불필요 권한 `CAR_POWERTRAIN`·`CAR_INFO` 제거(최소 권한) (§2) | feat/core |

## 1. LICENSE 선택 — "All rights reserved + 심사용 이용 허락"

- **선택**: 오픈소스 라이선스를 붙이지 않고, 저작권을 팀이 모두 보유하면서 주최 측·심사위원에게 **읽기·빌드·설치·실행·심사용 인용**만 허락하는 자체 라이선스(`LICENSE`).
- **이유**
  - 공모전은 저장소 **공개(Public)를 요구**하지만 오픈소스화를 요구하지 않는다. MIT·Apache-2.0을 붙이면 누구나 기획·코드를 상업적으로 재사용할 수 있어, 2차 PT·시범 사업 협상 전에 아이디어 보호 수단을 스스로 버리게 된다.
  - 라이선스 파일이 아예 없어도 법적으로는 "모든 권리 보유"지만, 심사자가 빌드·설치해도 되는지 불분명해진다. 명시적 심사용 허락이 있으면 심사 행위가 확실히 적법해진다.
  - 저장소에는 **독점 라이선스인 Pleos SDK를 쓰는 코드**가 있다. 앱 코드를 GPL 같은 카피레프트로 내면 SDK 결합 문제가 생기고, 허용형으로 내도 "SDK까지 자유롭게 쓸 수 있다"는 오해를 줄 수 있다. 자체 라이선스 4항에서 제3자 구성 요소를 분리했다.
  - GitHub 이용약관은 공개 저장소를 다른 이용자가 GitHub 안에서 보고 포크할 권리를 준다. 이를 막을 수는 없으므로 LICENSE 3항에서 "공개 ≠ 오픈소스"를 분명히 했다.
- **한계**: 아이디어 자체는 저작권으로 보호되지 않는다(표현만 보호). 기획을 지키는 실질 수단은 접수 기록(제출 일시·커밋 해시)과 저장소 이력이다. 커밋 해시를 접수 메일에 남기는 것 자체가 선행 증거가 된다.
- 공모전 약관에 수상작 권리 귀속·이용 허락 조항이 있으면 그 조항이 우선한다. 약관의 지식재산권 조항을 팀이 한 번 확인할 것.

## 2. 매니페스트 권한 — 최소 권한

SDK 2.3.2의 각 API에 붙은 `@RequiresPermission` 값을 javap로 확인했다 (AAR의 `AndroidManifest.xml`은 `ai.pleos.playground.service.vehicle.VEHICLE_BINDING`만 선언하며, 이 권한은 매니페스트 병합으로 자동 포함된다).

| 앱이 쓰는 API | SDK가 요구하는 권한 | 현재 매니페스트 |
|---|---|---|
| `Gear.getCurrentGearSelectionState`, `registerGearSelection` | `pleos.car.permission.CAR_DRIVE` | **없음 → 추가 필요** |
| `Wiper.getCurrentWiperSwitch`, `getCurrentWiperState`, `registerWiperState`, `check*Capabilities` | `pleos.car.permission.CAR_WINDOWS` | **없음 → 추가 필요** |
| `HVAC.getHvacDefrosterState`, `registerHvacDefrosterState`, `checkHvacDefrosterCapabilities` | `pleos.car.permission.CAR_CLIMATE` | 있음 |
| `SteeringWheel.getHvacSteeringWheelHeatState`, `register…` | `pleos.car.permission.CAR_CLIMATE` | 있음 |
| `Light.getLowBeamLightSwitch`, `registerLowBeamLightState`, `check…` | `pleos.car.permission.CAR_LIGHTS` | 있음 |
| `CarUxRestrictionsManager` (android.car) | 없음 | — |

권고 매니페스트 (feat/core가 반영):

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

- **지울 것**: `CAR_POWERTRAIN`(SDK 2.3.2 어디에도 쓰이지 않음 — 기어는 `CAR_DRIVE`), `CAR_INFO`(`CarInfo` API를 쓰지 않음).
- **절대 넣지 말 것**: `CONTROL_CAR_*`, `CONTROL_WINDSHIELD_WIPERS`, `PRIVATE_CAR_*`(특권), `INTERNET`, 위치 권한.
- 공식 샘플은 읽기·제어 권한을 30여 개 모두 선언한다. 샘플은 API 카탈로그라 그렇고, 우리 앱이 따라 하면 안 된다.
- 이 권한들은 Vehicle Service에서 `protectionLevel="normal"`(설치 시 부여)이라 런타임 요청 대화상자는 필요 없다.
- 실제 에뮬레이터 확인(팀이 직접, 사용자 기기): `adb -s <Pleos 에뮬레이터> logcat | grep -iE "SecurityException|permission"` 으로 드릴 한 번 실행 중 권한 오류가 없는지. 오류가 나면 메시지에 적힌 권한만 추가한다.
- 빌드 후 최종 권한 확인:
  ```sh
  ~/Library/Android/sdk/build-tools/35.0.0/aapt2 dump permissions app/build/outputs/apk/release/*.apk
  # 기대: CAR_DRIVE, CAR_WINDOWS, CAR_LIGHTS, CAR_CLIMATE, (READ_WINDSHIELD_WIPERS), VEHICLE_BINDING,
  #       com.chagil.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION(AndroidX 자동 추가, 정상) 만. CONTROL_ / INTERNET 없음.
  ```
  f90863b debug APK 실측: `CAR_CLIMATE, CAR_LIGHTS, CAR_POWERTRAIN, CAR_INFO, READ_WINDSHIELD_WIPERS, VEHICLE_BINDING, DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` — `CAR_DRIVE`·`CAR_WINDOWS`가 빠져 있음을 확인.

## 3. `set*` 호출 금지 확인

SDK의 제어 함수(이 앱이 쓰는 5개 도메인): `setWiperSwitch, setHvacDefroster, setHvacMaxDefrost, setHvacSteeringWheelHeat, setLowBeamLightSwitch, setHighBeamLightSwitch, setFrontFogLightSwitch, setRearFogLightSwitch, setTailLampLightSwitch, setCabinLightSwitch, setReadingLightSwitch, setAmbient*, setHvac*` (Gear에는 set 없음).

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
(f90863b scaffold에서 두 명령 모두 OK. 2번 패턴은 SDK 자체 클래스에서는 78건이 잡혀 패턴이 동작함을 확인했다.)

- `SimulatedVehicleSource.set(signal, raw)`은 앱 안 가짜 신호를 바꾸는 함수라 SDK 호출이 아니다(위 패턴에 걸리지 않음).
- 권고: feat/core가 1)번을 단위 테스트로 넣어 CI처럼 매 빌드마다 확인.

## 4. 매니페스트 기타

| 항목 | 상태 | 판단 |
|---|---|---|
| `android:allowBackup="false"` | 있음 | 좋음. Android 12+ 기기 간 이전까지 막으려면 `android:dataExtractionRules`에 빈 규칙을 추가(선택, 저장 데이터가 기록뿐이라 낮음) |
| exported 컴포넌트 | `MainActivity`(LAUNCHER)만 `exported="true"` | 적절. Service·Receiver·Provider를 추가하면 `exported="false"` 기본 |
| 인텐트 extras | 시뮬레이션 조작을 `am start` extras로 받게 만들면 | **debug 빌드에서만** 받게 한다(`BuildConfig.DEBUG` 확인). release에서 외부 앱이 판정 값을 주입하지 못하게 |
| 인터넷 | 권한 없음 | 좋음. PRIVACY.md "보내는 것 없음"의 근거. 의존성 추가로 병합되지 않는지 release마다 §2 명령으로 확인 |
| `debuggable` | release 기본 false | 직접 지정하지 말 것 |
| `isMinifyEnabled=false` | 그대로 | 1차엔 문제없음. 켜면 SDK consumer proguard 규칙 확인 필요 |
| 주행 중 동작 | DO(주행 최적화) 아님 → AAOS가 주행 중 차단 | 도로교통법 제49조 제1항(운전 중 영상표시장치 시청·조작 금지) 취지와 맞음. 앱 자체 L0 정지 화면도 유지 |

## 5. 서명 키

- 현재 `app/build.gradle.kts`는 루트 `keystore.properties`(git-ignored)를 읽어 release 서명. `.gitignore`에 `keystore.properties`, `*.jks`, `*.keystore` 있음 — 좋음.
- 키스토어 파일은 저장소 밖 `~/.chagil-signing/`에 두고 `keystore.properties`의 `storeFile`은 **절대 경로**로. (상대 경로면 저장소 안에 두고 싶어진다.)
- `keystore.properties`가 없으면 release가 **서명되지 않은 채** 빌드된다. 통합 담당은 release 전에 파일 존재를 확인하고, `apksigner verify`로 검증(체크리스트 B).
- 비밀번호를 커밋 메시지·PR 본문·README에 쓰지 않는다. 키를 잃으면 같은 패키지명으로 업데이트 설치가 안 되므로 키스토어를 따로 백업.

## 6. 상표·"현대차 안내"

- "Pleos", "Pleos Connect", "42dot"(42dot Inc.), "현대차/현대자동차/Hyundai"(현대자동차) 이름은 **대상 플랫폼·공모전을 설명하는 데만** 쓴다(설명적·지명적 사용). SDK License 3.6: 42dot 상표·로고 사용권 없음.
- 앱 이름·아이콘·런처 라벨·스플래시에 위 이름, 로고, 차종명(그랜저 등), 현대차 고유 서체·색(현대 블루 조합 등)을 쓰지 않는다. 기획서 원칙("차종명은 설명에만")과 같다.
- **"현대차 안내" 퀘스트 라벨** — 프로토타입에서는 제조사가 실제로 올린 것처럼 보이면 안 된다(표시광고법상 기만적 표시 우려, 약관 제9조②). 권장 표기:
  - 칩(짧게): **`제조사 안내 · 예시`** (현대차 이름을 쓰고 싶으면 `현대차 안내 · 예시`)
  - 카드 아래 한 줄: **`시연용 예시예요. 현대자동차가 실제로 올린 퀘스트가 아닙니다.`**
  - 담당자 화면(2차 PT)을 만들 때는 제목 옆에 `제조사 화면 예시 (시연용)`.
- README·영상 끝에 한 줄: `이 앱은 공모전 출품작이며 42dot·현대자동차그룹이 만들거나 보증한 앱이 아닙니다.`

## 7. 시뮬레이션 표시 규칙 (앱·영상 공통)

공모전 약관 제9조②(작품 오인 금지)와 표시광고법의 기만 표시 금지 취지에 따라, **보는 사람이 "차가 판정했다"고 믿을 장면에서 실제로 차(Vehicle SDK)가 판정했는지**가 늘 보여야 한다.

앱 (SPEC §6과 일치):
1. 홈 신호 출처 칩은 두 상태만: `차 신호` / `시뮬레이션`. SPEC의 "실차 신호"는 에뮬레이터 녹화에서 "실제 차"로 읽힐 수 있어 **`차 신호`(보조 문구 `Vehicle SDK`)를 권장**.
2. 시뮬레이션 모드에서는 홈·드릴·완료 화면 모두 상단에 `시뮬레이션` 배지가 보여야 한다(로그 패널만이 아니라).
3. 시뮬레이터 조작 패널 상단 고정 문구: `시뮬레이션 — 실제 차 신호가 아닙니다` (SPEC §6-9).
4. 판정 로그의 시뮬레이션 항목은 앞에 `SIM ·`을 붙인다. 예: `SIM · 00:04.0 · Wiper 스위치 OFF → AUTO · 오조작`.
5. 운전자 확인("켰어요")으로 통과한 단계는 로그에 `운전자 확인`으로 남겨, 차가 판정한 것과 구분.
6. 음성(TTS)이 구현되지 않았으면 화면의 "음성" 문구는 `음성 안내 문구(예정)`처럼 표시하거나, 영상에서 소리가 나는 것처럼 말하지 않는다.

영상: [DEMO_SCRIPT.md](../DEMO_SCRIPT.md)의 자막 규칙. 첫 화면에 녹화 환경 고지, 시뮬레이션 장면엔 화면 배지 + 자막 둘 다. 기획서의 "MIST 오조작" 예시는 Virtual Console에 MIST가 없으므로 영상에선 AUTO로 보여 주고 그 사실을 자막에 적는다.

## 8. 게임산업법·개인정보·위치정보

- **게임산업법**: 등급분류 의무(제21조 제1항)는 게임물을 "유통시키거나 이용에 제공하게 할 목적"일 때 생긴다. 1차 제출은 심사용 제출이라 일반 이용자에게 유통하지 않으므로 지금 등급분류는 필요 없다고 본다. 다만 이 앱은 시간 기록·배지·진행 지도가 있어 출시 시 "게임물"로 볼 여지가 있다.
  - 출시 전 할 일: Pleos 앱 마켓이 자체등급분류사업자인지(제21조의2) 확인 → 마켓 등급 절차를 따르거나 게임물관리위원회에 문의. 교육·학습용 예외(제21조 제1항 단서)는 요건·절차를 시행령으로 확인.
  - 지금 할 일: 화면·영상·README에서 "게임"보다 **"드릴", "익히기", "실습"**이라고 부른다(기획서 방침과 같음). 무작위 보상·유료 아이템·순위표 없음 유지.
- **개인정보 보호법**: 이름·연락처·계정·식별자를 받지 않고, 차량 상태값은 기기 안에서만 쓰고 버린다. 팀이 개인정보를 "처리"하지 않는 구조이며, SDK License 4.3에 따라 `PRIVACY.md`로 처리 내용을 공개했다. 저장 항목이 바뀌면 PRIVACY.md 표를 먼저 고친다.
- **SDK License 8.1.2**(이용자 동의 하에만 데이터 조회): 앱 첫 화면(또는 홈 하단)에 한 줄 고지를 둔다 — `연습 판정을 위해 기어·와이퍼·전조등·공조 상태를 읽기만 해요. 기기 밖으로 보내지 않아요.` 드릴 시작 버튼을 누르는 것을 그 드릴의 판정에 대한 동의로 본다.
- **위치정보법**: 위치를 읽지 않음. 기어·와이퍼 상태는 위치정보가 아니다. 기획서의 "장거리 휴식 알림"을 붙일 때 다시 검토(서버 연동은 위치기반서비스사업 신고 뒤).
- **아동**: 1차 범위에 아이 프로필 없음. 만 14세 미만 정보 수집 없음.

## 9. 커밋 작성자·계정 노출 — 팀 결정 필요

- 현재 모든 커밋의 작성자가 **실명 + 개인 Gmail 주소**이고, 이후 에이전트 커밋도 같은 git 설정을 쓴다. 저장소가 11/16 공개되면 이 정보는 누구나 볼 수 있고, 포크·아카이브로 영구히 남는다. GitHub 계정 `13JVV`도 저장소 주소에 보인다.
- **블라인드 심사인가?** 받은 제출 안내에는 블라인드 심사 규정이 없다. 오히려 접수 메일·신청서로 팀을 밝히고, 저장소 주소(계정명 포함)를 직접 내라고 한다. 따라서 **저장소에 신원이 보이는 것 자체는 감점 사유로 보이지 않는다.** (약관에 "작품에 소속·실명 표기 금지" 조항이 있는지만 팀이 확인.)
- 남는 문제는 **개인 이메일의 공개 노출**(스팸·개인정보)이다. 선택지:
  1. 그대로 둔다 — 가장 안전하게 마감. 노출을 감수.
  2. 앞으로의 커밋만 바꾼다 — 저장소에서만 `git config user.email "<숫자ID>+13JVV@users.noreply.github.com"` (GitHub 설정 › Emails에서 확인). 첫 scaffold 커밋 1개는 남는다. 브랜치 충돌 없음.
  3. 공개 전 이력을 다시 쓴다 — 모든 커밋의 작성자 교체 후 force-push. 진행 중인 모든 브랜치·PR이 깨지므로 **마감 당일엔 권하지 않음**. 하려면 모든 PR 머지 뒤, 접수 메일 발송 전에 한 번만, 그리고 새 해시를 메일에 적는다.
  - 권장: **2번** (마감 전 아무 때나, 사람이 직접). GitHub 설정 › Emails의 "Keep my email addresses private"와 "Block command line pushes that expose my email"도 함께 켤 수 있다(켜면 지금 설정으로는 push가 거부되므로 2번을 먼저).
- 이 PR은 git 설정을 바꾸지 않았다.

## 10. 비밀 정보 스캔 (f90863b 기준)

- 추적 파일 17개: `.gitignore, apk/.gitkeep, app/*, build.gradle.kts, docs/SPEC.md, gradle/*, gradlew(.bat), README.md, settings.gradle.kts`. AAR·키스토어·`local.properties`·이미지 없음.
- `git log --all -p`에서 비밀번호·토큰·개인키 패턴 없음(서명 설정의 **변수 이름**만 있음). 실명·이메일은 커밋 메타데이터(§9)에만 있음.
- 재검사 명령(최종 커밋 직전, 통합 담당):
  ```sh
  git log --all -p | grep -nEi "storePassword *= *['\"]?[^k ]|keyPassword *= *['\"]?[^k ]|BEGIN .*PRIVATE KEY|ghp_[A-Za-z0-9]{20,}|AIza[0-9A-Za-z_-]{30,}|@gmail\.com|@naver\.com|01[016789]-?[0-9]{3,4}-?[0-9]{4}" | grep -v "^[0-9]*:Author:"
  git ls-files | grep -Ei "\.(aar|jks|keystore|p12|pem)$|keystore\.properties|local\.properties|\.(png|jpg|mp4)$"
  # 이미지가 나오면 계정·이메일·알림이 찍혔는지 눈으로 확인
  ```
  (`gitleaks`는 설치돼 있지 않다. 원하면 `brew install gitleaks && gitleaks detect --log-opts="--all"`.)

## 11. SDK 라이선스·참고 자료 취급

- SDK License 3.4는 복제(백업 제외)·재배포·역컴파일·디스어셈블을 금지한다. 개발 중 시그니처 확인을 위해 로컬에서 javap로 공개 API 시그니처·어노테이션을 본 것은 앱 개발(3.1 허락 범위)을 위한 최소한의 확인이었지만, **추출한 클래스 파일·javap 출력은 저장소·PR·영상에 넣지 않는다.** 공모전 뒤 로컬 사본은 지운다.
- SPEC §2의 API 목록은 IDE 자동완성 수준의 공개 인터페이스 정보다. 구현 코드나 내부 클래스 이름(`utils.f` 같은 난독화 이름)은 문서에 옮기지 않는다.
- APK에는 SDK 코드가 빌드 결과로 들어간다. 주최 측이 APK 제출을 요구하고 SDK가 이 공모전용으로 제공되었으므로 3.1 범위의 이용으로 보지만, APK를 공모전 밖(다른 마켓·사이트)에 올리지 않는다.
- 공식 샘플 앱(`pleos-sample-vehicle-app`)은 라이선스가 명시되어 있지 않다 → **코드 복사 금지**, 호출 순서 같은 패턴만 참고. PR 리뷰 때 샘플과 같은 주석·이름이 그대로 있는지 본다.
- SDK License 6.1: 42dot이 사용 통계를 모을 때는 개발자에게 알리고 동의를 받는다. 우리 앱은 별도 통계를 보내지 않는다.

## 12. 다른 팀에 요청 (PR 본문에도 같은 목록)

- **feat/core**: §2 권고 매니페스트 반영(`CAR_DRIVE`·`CAR_WINDOWS` 추가, `CAR_POWERTRAIN`·`CAR_INFO` 삭제). §3 `set*` 검사 단위 테스트. 화면이 꺼지면(`onStop`) 폴링·리스너 해제. 판정 로그는 메모리에만. SharedPreferences 항목은 PRIVACY.md §2 표 범위로. 시뮬레이션 주입 extras는 debug 빌드에서만. 로그 항목에 출처(SDK/SIM/운전자 확인) 필드.
- **content/quests**: 와이퍼 단계 `wrong`에 `AUTO`·`CONTINUOUS_LEVEL_2`·`MIST` 각각 맞는 문구(예: AUTO → `지금은 '자동'이에요. 한 단계만 올려 보세요`). 콘솔 "High"가 어느 값인지 검증 전이라 `toAnyOf`를 고칠 땐 High가 정답으로 잡히지 않는지 확인. "현대차 안내" 예시 퀘스트의 `label`은 `제조사 안내 · 예시`.
- **design/system**: 출처 칩 `차 신호`/`시뮬레이션`, 시뮬레이션 배지(모든 화면 상단), 예시 라벨 컴포넌트. 폰트·아이콘을 추가하면 이름·라이선스(OFL 등)를 PR에 적어 주면 NOTICE.md에 넣는다. 현대차·Pleos 로고·현대 블루 계열 브랜드 색 조합 사용 금지.
- **feat/screens**: README 필수 5항목(체크리스트 C) + "프로토타입 범위"·면책 한 줄. 홈 하단 데이터 고지 한 줄(§8). 앱 안 "오픈소스 라이선스" 보기(2차 전, Apache-2.0 고지 의무 대비 — 1차는 NOTICE.md로 갈음).
- **통합(release)**: 서명 확인, APK 파일명, 비밀 재검사, 해시 확정 순서(체크리스트 B·F).
