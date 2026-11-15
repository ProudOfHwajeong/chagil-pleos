# 1차 제출 체크리스트 (GitHub 저장소, APK, 영상)

마감은 2026-11-15(일) 23:59 KST다. 공개 전환은 11-16(월)까지 하고, 공개 상태는 11-23(월) 1차 발표일까지 유지한다.

누가 칸에서 통합은 `feat/screens`와 release 담당 에이전트, 팀은 사람(사용자)이 직접 하는 일이다.
각 줄의 확인 명령은 저장소 루트에서 실행한다.

## A. 저장소와 소스 코드

- [ ] 소스 코드 업로드. `main`에 머지된 코드가 제출 기준이다. 누가: 통합. 언제: 2단계 머지 후.
  - 확인: `git status`가 깨끗하고 `git log origin/main -1`이 제출할 커밋인지 본다.
- [ ] 깨끗한 클론에서 빌드된다. `local.properties`가 없어도 Android Studio가 SDK 경로를 채우면 빌드되어야 한다. 누가: 통합.
  - 확인: `git clone <repo> /tmp/x && cd /tmp/x && ./gradlew assembleRelease` (JAVA_HOME은 JDK 17)
- [ ] SDK AAR, 키스토어, 비밀번호, 개인정보가 없다. 누가: 통합. 언제: 최종 커밋 직전.
  - 확인: `git ls-files | grep -Ei '\.(aar|jks|keystore)$|keystore\.properties|local\.properties'`의 결과가 없어야 한다.
  - 확인: `git log --all -p | grep -nEi 'storePassword *=|keyPassword *=|BEGIN .*PRIVATE KEY'`의 결과가 없어야 한다. 변수 이름만 있는 build.gradle.kts 줄은 정상이다.
- [ ] LICENSE, NOTICE.md, PRIVACY.md가 루트에 있다. 누가: chore/compliance. 상태: 이 PR.

## B. APK

- [ ] `apk/` 폴더에 올린다. 누가: 통합(release).
- [x] 파일명은 공백 없이 `팀명_앱이름_release.apk` 형식. 팀명은 컬러팡 홍보팀이다.
  - 공백은 `_`로 바꿨다. 파일명: `컬러팡_홍보팀_차길들이기_release.apk`
- [ ] 실행 가능한 release 버전. 누가: 통합.
  - `./gradlew assembleRelease`의 결과물이어야 한다. 서명되지 않은 `*-release-unsigned.apk`는 설치가 안 되므로 제출할 수 없다.
  - 서명 키는 저장소 밖(`~/.chagil-signing/`)에 두고, 루트의 git-ignored `keystore.properties`가 가리키게 한다.
  - 확인: `~/Library/Android/sdk/build-tools/35.0.0/apksigner verify --print-certs apk/*_release.apk`의 결과가 `Verified`
  - 확인: `~/Library/Android/sdk/build-tools/35.0.0/aapt2 dump badging apk/*_release.apk | grep -E "package|debuggable"`의 결과에 `application-debuggable` 줄이 없어야 한다.
  - 확인: `adb -s emulator-5580 install -r apk/*_release.apk && adb -s emulator-5580 shell am start -n com.chagil.app/.MainActivity`를 실행하면 크래시 없이 시뮬레이션 모드로 뜬다.
  - 팀 확인: 사용자의 Pleos Connect 에뮬레이터에 같은 APK를 설치하고, Virtual Console의 와이퍼와 기어로 한 번 판정되는지 본다.
- [ ] 용량 확인. GitHub 일반 업로드 한도는 파일당 100MB(웹 업로드는 25MB)다. 이 앱은 몇 MB 정도로 예상돼 저장소에 직접 넣는다.
  - 확인: `ls -lh apk/`. 100MB를 넘으면 `gh release create v0.1.0 apk/<파일> --title "1차 제출"`로 올리고 Release 페이지 주소를 제출한다.
- [ ] APK와 제출 커밋이 같은 소스다. APK를 커밋한 그 커밋(또는 그 뒤로 문서만 바뀐 커밋)을 제출 해시로 쓴다.

## C. README.md (소유: `feat/screens`, 아래 5개가 모두 있어야 한다)

- [x] 앱 이름과 한 줄 소개: 차 길들이기. 차를 세워 두고 진짜 레버와 버튼을 움직이면, 차가 직접 정답을 판정하는 차 기능 실습 앱
- [x] 실행 방법: APK 설치 명령, 소스 빌드 명령(JDK 17, `./gradlew assembleRelease`), 앱을 실행한 뒤 첫 화면 설명
- [x] 테스트 환경과 필요한 설정: Android Automotive OS API 34, 가로 화면, 필요한 권한(아래 D), Vehicle Service가 없으면 자동으로 시뮬레이션 모드가 된다는 점, 수동 전환 방법, 주차(P) 상태에서만 드릴이 열린다는 점
- [x] Pleos Connect 에뮬레이터 버전과 화면 규격: `Pleos Connect v2.1.0 (AAOS API 34, arm64), 2560×1440 가로, density 160`, Vehicle SDK 2.3.2, Virtual Console에서 쓴 카드(Wipers, Gear Selection, Defroster, Exterior Lights, Steering Wheel Heat)
- [x] APK 파일 위치 또는 Release 링크: `apk/컬러팡_홍보팀_차길들이기_release.apk`
- [x] (권장) 프로토타입 범위 절: 무엇이 Vehicle SDK로 실제 판정되고 무엇이 시뮬레이션인지 표로 보여 준다. 공모전 약관 제9조 제2항의 오인 방지를 위한 것이다.
- [x] (권장) LICENSE, PRIVACY, NOTICE 링크와 42dot·현대자동차그룹이 만들거나 보증한 앱이 아니라는 한 줄

## D. 앱 안에서 지킬 것 (심사자가 설치해 볼 때)

- [ ] 시뮬레이션일 때 홈 출처 칩과 시뮬레이터 패널에 "시뮬레이션 중이에요. 실제 차 신호가 아니에요"가 항상 보인다.
- [ ] 인터넷 권한이 없다. 확인: `aapt2 dump permissions apk/*.apk | grep -i internet`의 결과가 없어야 한다.
- [ ] `CONTROL_*` 권한과 `set*` 호출이 없다. 확인 명령은 [REVIEW.md](REVIEW.md) 3장에 있다.
- [ ] 현대차 안내 퀘스트는 예시로 표시한다([REVIEW.md](REVIEW.md) 6장).

## E. 영상 (1차 선택 제출물)

- [ ] [DEMO_SCRIPT.md](../DEMO_SCRIPT.md)대로 60~90초 화면을 녹화한다. 누가: 팀. 언제: APK 확정 뒤.
- [ ] 녹화 개인정보 점검표를 통과한다(계정, 알림, 바탕화면 파일명이 보이지 않음).
- [ ] 시뮬레이션이 나오는 장면에는 화면 라벨과 자막이 함께 보인다.

## F. 접수 메일과 공개 전환 (팀이 할 일)

- [ ] 접수 메일에 저장소 주소와 최종 커밋 해시를 적는다.
  - 저장소: `https://github.com/ProudOfHwajeong/chagil-pleos`
  - 해시: `git rev-parse origin/main` (40자 전체를 적고, 7자 축약도 괄호 안에 함께 적는다)
  - 메일을 보낸 뒤에는 `main`에 push하지 않는다. 고칠 것이 생기면 새 해시로 메일을 다시 보낸다.
- [ ] 제출 페이지에 저장소 주소와 APK 경로(`apk/<파일명>`)를 입력한다.
- [ ] 11-16(월)까지 Public으로 전환한다. `gh repo edit ProudOfHwajeong/chagil-pleos --visibility public --accept-visibility-change-consequences` (전환 전에 A의 비밀 점검을 한 번 더 한다)
- [ ] 11-23(월)까지 Public을 유지한다. 그 사이 저장소 이름 변경, 삭제, 비공개 전환은 하지 않는다. 전환한 뒤 로그아웃한 창에서 주소와 APK 경로가 열리는지 확인한다.
- [ ] 공개가 어렵다면 대안으로, 소스와 APK를 묶은 압축 파일의 내려받기 주소를 마감 전에 접수 메일로 보낸다.
- [ ] Collaborator 초대는 필요 없다.
- [x] 커밋 작성자 이름과 이메일을 팀명으로 바꿨다([REVIEW.md](REVIEW.md) 9장).
