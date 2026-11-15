# NOTICE — 제3자 구성 요소

차 길들이기 프로토타입이 쓰는 제3자 구성 요소와 라이선스입니다.
버전은 `gradle/libs.versions.toml` 기준이며, 의존성을 추가·변경하면 이 표도 같이 고칩니다.

## APK에 포함되는 구성 요소

| 구성 요소 | 좌표(대표) | 버전 | 라이선스 | 출처 |
|---|---|---|---|---|
| Kotlin 표준 라이브러리 | `org.jetbrains.kotlin:kotlin-stdlib` | 2.1.20 | Apache-2.0 | https://github.com/JetBrains/kotlin |
| kotlinx.serialization (JSON) | `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.7.3 | Apache-2.0 | https://github.com/Kotlin/kotlinx.serialization |
| kotlinx.coroutines | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.9.0 | Apache-2.0 | https://github.com/Kotlin/kotlinx.coroutines |
| AndroidX Core KTX | `androidx.core:core-ktx` | 1.15.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx |
| AndroidX Lifecycle (runtime-compose, viewmodel-compose) | `androidx.lifecycle:*` | 2.8.7 | Apache-2.0 | 〃 |
| AndroidX Activity Compose | `androidx.activity:activity-compose` | 1.9.3 | Apache-2.0 | 〃 |
| Jetpack Compose (UI, Graphics, Material 3, Tooling Preview) | `androidx.compose.*` (BOM) | BOM 2024.12.01 | Apache-2.0 | https://developer.android.com/jetpack/compose |
| Pleos Vehicle SDK | `ai.pleos.playground:Vehicle` | 2.3.2 | 42dot Software Development Kit License Agreement (독점 라이선스) | https://nexus-playground.pleos.ai |

## APK에 포함되지 않는 구성 요소

| 구성 요소 | 쓰임 | 라이선스 |
|---|---|---|
| Android Car 라이브러리 (`android.car`) | `useLibrary("android.car")`로 기기 플랫폼의 것을 씀. APK에 넣지 않음 | Apache-2.0 (AOSP) |
| Gradle Wrapper (`gradle/wrapper/gradle-wrapper.jar`) | 빌드 도구. 저장소에 포함됨 | Apache-2.0 |
| Android Gradle Plugin, Kotlin Gradle Plugin | 빌드 도구 | Apache-2.0 |
| Compose UI Tooling | debug 빌드에만 포함 | Apache-2.0 |
| JUnit 4 | 단위 테스트에만 사용 | EPL-1.0 |

## Pleos Vehicle SDK에 대하여

- SDK는 42dot Inc.의 소유이며 **이 저장소에 SDK 파일(AAR 등)을 넣지 않습니다.**
  빌드할 때 `settings.gradle.kts`에 적힌 Pleos Playground Maven 저장소에서 받습니다.
  (42dot SDK License 3.3·3.4: 소유권, 복제·재배포 금지)
- APK에는 앱이 동작하는 데 필요한 SDK 코드가 빌드 결과로 함께 들어갑니다.
  이 APK는 공모전 심사용으로만 제출하며, SDK를 따로 떼어 배포하지 않습니다.
- SDK 안의 저작권·상표 표시는 지우거나 바꾸지 않습니다. (3.7)
- 앱은 SDK의 읽기 API만 쓰며 차량을 제어하는 `set*` 함수는 부르지 않습니다.

## 상표

"Pleos", "Pleos Connect", "42dot"은 42dot Inc.의, "현대자동차", "현대차", "Hyundai"는
현대자동차(주)의 상표 또는 상호입니다. 이 프로젝트는 대상 플랫폼과 공모전을 설명하기 위해서만
이 이름을 쓰며, 해당 회사의 로고를 쓰지 않습니다. 이 프로젝트는 42dot 또는 현대자동차그룹이
만들거나 보증한 것이 아닙니다.

## Apache-2.0 고지

Apache-2.0 구성 요소는 다음 조건에 따라 씁니다:
Licensed under the Apache License, Version 2.0. 전문: https://www.apache.org/licenses/LICENSE-2.0
각 라이브러리의 NOTICE 파일 내용은 해당 아티팩트에 포함된 원문을 따릅니다.
