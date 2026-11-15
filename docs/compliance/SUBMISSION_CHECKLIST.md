# 1차 제출 체크리스트 — GitHub 저장소·APK·영상

마감: **2026-11-15(일) 23:59 KST** · 공개 전환: **11-16(월)까지** · 공개 유지: **11-23(월) 1차 발표일까지**

"누가" 칸: 통합 = `feat/screens`·release 담당 에이전트, 팀 = 사람(사용자)이 직접.
각 줄의 확인 명령은 저장소 루트에서 실행합니다.

## A. 저장소·소스 코드

- [ ] **소스 코드 업로드** — `main`에 머지된 코드가 제출 기준. 누가: 통합 · 언제: 2단계 머지 후
  - 확인: `git status`가 깨끗하고 `git log origin/main -1` 이 제출할 커밋.
- [ ] **빌드가 깨끗한 클론에서 된다** — `local.properties` 없이도 Android Studio가 SDK 경로를 채우면 빌드되어야 함. 누가: 통합
  - 확인: `git clone <repo> /tmp/x && cd /tmp/x && ./gradlew assembleRelease` (JAVA_HOME = JDK 17)
- [ ] **SDK AAR·키스토어·비밀번호·개인정보가 없다** — 누가: 통합 · 언제: 최종 커밋 직전
  - 확인: `git ls-files | grep -Ei '\.(aar|jks|keystore)$|keystore\.properties|local\.properties'` → 결과 없음
  - 확인: `git log --all -p | grep -nEi 'storePassword *=|keyPassword *=|BEGIN .*PRIVATE KEY'` → 결과 없음 (변수 이름만 있는 build.gradle.kts 줄은 정상)
- [ ] **LICENSE·NOTICE.md·PRIVACY.md가 루트에 있다** — 누가: chore/compliance · 상태: 이 PR

## B. APK

- [ ] **`apk/` 폴더에 업로드** — 누가: 통합(release)
- [ ] **파일명 `팀명_앱이름_release.apk`** — 공백 없이. 권장: `<팀명>_차길들이기_release.apk`
  - 지금은 자리표시자 `TEAM`. **팀명 확정은 팀이 할 일** → 받으면 통합이 파일명 교체.
  - 팀명에 공백·`/`·`:`가 있으면 `_`로 바꾼다. 신청서에 적은 팀명과 같은 글자로.
- [ ] **실행 가능한 release 버전** — 누가: 통합
  - `./gradlew assembleRelease` 결과물. 서명 안 된 `*-release-unsigned.apk`는 설치가 안 되므로 **제출 불가**.
  - 서명 키는 저장소 밖(`~/.chagil-signing/`)에 두고, 루트의 git-ignored `keystore.properties`가 가리키게 한다.
  - 확인: `~/Library/Android/sdk/build-tools/35.0.0/apksigner verify --print-certs apk/*_release.apk` → `Verified`
  - 확인: `~/Library/Android/sdk/build-tools/35.0.0/aapt2 dump badging apk/*_release.apk | grep -E "package|debuggable"` → `application-debuggable` 줄이 없어야 함
  - 확인: `adb -s emulator-5580 install -r apk/*_release.apk && adb -s emulator-5580 shell am start -n com.chagil.app/.MainActivity` → 크래시 없이 시뮬레이션 모드로 뜸
  - 팀 확인: 사용자의 Pleos Connect 에뮬레이터에 같은 APK를 설치해 Virtual Console 와이퍼·기어로 한 번 판정되는지.
- [ ] **용량 확인** — GitHub 일반 업로드 한도는 파일당 100MB(웹 업로드 25MB). 이 앱은 수 MB 예상이라 저장소에 직접 넣음.
  - 확인: `ls -lh apk/`. 100MB를 넘으면 `gh release create v0.1.0 apk/<파일> --title "1차 제출"` 후 **Release 페이지 주소**를 제출.
- [ ] **APK와 제출 커밋이 같은 소스** — APK를 커밋한 그 커밋(또는 그 뒤 문서만 바뀐 커밋)을 제출 해시로 쓴다.

## C. README.md (소유: `feat/screens` — 아래 5개가 다 있어야 함)

- [x] 앱 이름 및 한 줄 소개 — "차 길들이기 — 차를 세워 두고 진짜 레버와 버튼을 움직이면, 차가 직접 정답을 판정하는 차 기능 실습 앱"
- [x] 실행 방법 — APK 설치 명령, 소스 빌드 명령(JDK 17, `./gradlew assembleRelease`), 앱 실행 후 첫 화면 설명
- [x] 테스트 환경 및 필요한 설정 — Android Automotive OS API 34, 가로 화면, 필요한 권한(아래 D), Vehicle Service가 없으면 자동 **시뮬레이션 모드**, 수동 전환 방법, 주차(P) 상태에서만 드릴이 열린다는 점
- [x] **Pleos Connect 에뮬레이터 버전과 화면 규격** — `Pleos Connect v2.1.0 (AAOS API 34, arm64), 2560×1440 가로, density 160`, Vehicle SDK 2.3.2, Virtual Console에서 쓴 카드(Wipers·Gear Selection·Defroster·Exterior Lights·Steering Wheel Heat)
- [ ] APK 파일 위치 또는 Release 링크 — `apk/<팀명>_차길들이기_release.apk`
- [x] (권장) "프로토타입 범위" 절 — 무엇이 Vehicle SDK로 실제 판정되고 무엇이 시뮬레이션인지 표로. 공모전 약관 제9조② 오인 방지.
- [x] (권장) LICENSE·PRIVACY·NOTICE 링크, "42dot·현대자동차그룹이 만들거나 보증한 앱이 아님" 한 줄

## D. 앱 안에서 지킬 것 (심사자가 설치해 볼 때)

- [ ] 시뮬레이션일 때 홈 출처 칩과 시뮬레이터 패널에 "시뮬레이션 — 실제 차 신호가 아닙니다"가 항상 보임
- [ ] 인터넷 권한 없음 — 확인: `aapt2 dump permissions apk/*.apk | grep -i internet` → 결과 없음
- [ ] `CONTROL_*` 권한 없음, `set*` 호출 없음 — 확인 명령은 [REVIEW.md](REVIEW.md) §3
- [ ] "현대차 안내" 퀘스트는 **예시**로 표시 ([REVIEW.md](REVIEW.md) §6)

## E. 영상 (1차 선택 제출물)

- [ ] [DEMO_SCRIPT.md](../DEMO_SCRIPT.md) 대로 60~90초 화면 녹화 — 누가: 팀 · 언제: APK 확정 뒤
- [ ] 녹화 개인정보 점검표 통과 (계정·알림·바탕화면 파일명 없음)
- [ ] 시뮬레이션이 나오는 장면엔 화면 라벨과 자막이 함께 보임

## F. 접수 메일·공개 전환 (팀이 할 일)

- [ ] **접수 메일에 저장소 주소 + 최종 커밋 해시**
  - 저장소: `https://github.com/13JVV/chagil-pleos`
  - 해시: `git rev-parse origin/main` (40자 전체를 적고, 7자 축약도 괄호로)
  - 메일을 보낸 뒤 `main`에 push하지 않는다. 고칠 게 생기면 새 해시로 메일을 다시 보낸다.
- [ ] 제출 페이지에 저장소 주소와 APK 경로(`apk/<파일명>`) 입력
- [ ] **11-16(월)까지 Public 전환** — `gh repo edit 13JVV/chagil-pleos --visibility public --accept-visibility-change-consequences` (전환 전에 A의 비밀 점검을 한 번 더)
- [ ] **11-23(월)까지 Public 유지** — 그 사이 저장소 이름 변경·삭제·비공개 전환 금지. 전환 뒤 로그아웃 창에서 주소·APK 경로가 열리는지 확인.
- [ ] 공개가 어렵다면: 소스+APK 압축 파일의 내려받기 주소를 **마감 전** 접수 메일로 (대안)
- [ ] Collaborator 초대는 필요 없음
- [ ] 커밋 작성자 이름·이메일이 공개되는 것에 대한 팀 결정 ([REVIEW.md](REVIEW.md) §9)
