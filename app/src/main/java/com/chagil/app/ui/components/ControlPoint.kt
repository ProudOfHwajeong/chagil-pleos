package com.chagil.app.ui.components

/**
 * Controls the cockpit illustration can point at. Enum names equal the quest JSON "control"
 * strings (docs/SPEC.md §5). [label] is the on-screen name, [where] a short spoken-style hint.
 */
enum class ControlPoint(val label: String, val where: String) {
    LIGHT_STALK("라이트 레버", "핸들 왼쪽 레버"),
    WIPER_STALK("와이퍼 레버", "핸들 오른쪽 레버"),
    HVAC_FRONT_DEFROST("앞유리 김서림 버튼", "공조 패널, 앞유리 그림"),
    HVAC_REAR_DEFROST("뒷유리 열선 버튼", "공조 패널, 네모 창 그림"),
    STEERING_HEAT_BUTTON("핸들 열선 버튼", "공조 패널, 핸들 그림"),
    GEAR_SELECTOR("기어", "가운데 기어 손잡이"),
    SEAT_BELT("안전벨트", "왼쪽 어깨 위"),
    ;

    companion object {
        /** Quest JSON key → ControlPoint, or null for an unknown key (draw no highlight). */
        fun fromKey(key: String?): ControlPoint? = entries.firstOrNull { it.name == key?.trim() }
    }
}
