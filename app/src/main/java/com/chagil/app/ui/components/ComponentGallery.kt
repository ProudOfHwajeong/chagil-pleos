package com.chagil.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chagil.app.ui.theme.ChagilShapes
import com.chagil.app.ui.theme.ChagilSpacing
import com.chagil.app.ui.theme.ChagilTheme

/**
 * Design review gallery. Not a production screen: it composes the components the way
 * docs/design/SCREENS.md describes so the screens team can compare against it.
 * page 0 = drill running, 1 = home, 2 = hint/time-out/complete states sheet.
 */
@Composable
fun ComponentGallery(page: Int = 0, night: Boolean = true, animate: Boolean = true) {
    ChagilTheme(night = night) {
        Box(Modifier.fillMaxSize().background(ChagilTheme.colors.background)) {
            when (page) {
                1 -> GalleryHome(animate)
                2 -> GalleryStates(animate)
                else -> GalleryDrill(animate)
            }
        }
    }
}

@Composable
private fun gutter(maxWidth: androidx.compose.ui.unit.Dp) =
    if (maxWidth < 1400.dp) ChagilSpacing.pageGutterCompact else ChagilSpacing.pageGutter

@Composable
private fun GalleryDrill(animate: Boolean) = BoxWithConstraints(Modifier.fillMaxSize()) {
    val g = gutter(maxWidth)
    val c = ChagilTheme.colors
    Column(Modifier.fillMaxSize().padding(horizontal = g).padding(bottom = g)) {
        DrillTopBar("폭우 드릴", progress = "2/4단계") {
            SourceChip(simulation = true)
            TopBarButton("일시정지", {}, icon = ChagilIcon.PAUSE)
            TopBarButton("그만하기", {}, icon = ChagilIcon.CLOSE)
        }
        SimBanner(Modifier.fillMaxWidth())
        Spacer(Modifier.height(ChagilSpacing.xl))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(ChagilSpacing.xl)) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                InstructionPanel(
                    headline = "와이퍼를 한 단계 올려 보세요",
                    voice = "오른쪽 레버예요. 비 오는 날 쓰는 단계로 옮겨 보세요.",
                    checking = true,
                )
                Spacer(Modifier.height(ChagilSpacing.l))
                ControlLocator(ControlPoint.WIPER_STALK, Modifier.fillMaxWidth().weight(1f), animate = animate)
            }
            StepColumn(
                listOf(
                    StepItem("전조등 켜기", StepState.DONE),
                    StepItem("와이퍼 한 단계", StepState.ACTIVE),
                    StepItem("앞유리 김서림 제거", StepState.PENDING),
                    StepItem("와이퍼 끄기", StepState.PENDING),
                ),
                remaining = 31,
            )
        }
    }
}

@Composable
private fun StepColumn(steps: List<StepItem>, remaining: Int?) {
    val c = ChagilTheme.colors
    Column(
        Modifier.width(460.dp).fillMaxHeight().clip(ChagilShapes.large).background(c.surface).padding(ChagilSpacing.xl),
    ) {
        Text("할 일", style = ChagilTheme.type.label, color = c.textSecondary)
        Spacer(Modifier.height(ChagilSpacing.s))
        StepList(steps)
        Spacer(Modifier.weight(1f))
        if (remaining != null) CountdownRing(remaining, 45, Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun GalleryHome(animate: Boolean) = BoxWithConstraints(Modifier.fillMaxSize()) {
    val g = gutter(maxWidth)
    val c = ChagilTheme.colors
    Column(Modifier.fillMaxSize().padding(horizontal = g).padding(bottom = g)) {
        Row(Modifier.fillMaxWidth().height(ChagilSpacing.topBarHeight), verticalAlignment = Alignment.CenterVertically) {
            Text("차 길들이기", style = ChagilTheme.type.headline, color = c.text)
            Spacer(Modifier.width(ChagilSpacing.l))
            Text("차를 세워 두고, 진짜 레버로 연습해요", style = ChagilTheme.type.bodyLarge, color = c.textSecondary, modifier = Modifier.weight(1f), maxLines = 1)
            SafetyChip(SafetyChipLevel.L2_PARKED)
            Spacer(Modifier.width(ChagilSpacing.s))
            SourceChip(simulation = true)
        }
        SimBanner(Modifier.fillMaxWidth())
        Spacer(Modifier.height(ChagilSpacing.l))
        Row(Modifier.fillMaxWidth().height(330.dp), horizontalArrangement = Arrangement.spacedBy(ChagilSpacing.l)) {
            DrillCard("폭우 드릴", "전조등, 와이퍼, 앞유리 김서림", "45초 · 4단계", DrillCardState.OPEN, DrillGlyph.RAIN, Modifier.weight(1f).fillMaxHeight(), best = "최고 11.2초", onClick = {})
            DrillCard("세차 대비", "세차 전에 와이퍼 AUTO 끄기", "20초 · 2단계", DrillCardState.OPEN, DrillGlyph.CAR_WASH, Modifier.weight(1f).fillMaxHeight(), onClick = {})
            DrillCard("성에 낀 아침", "앞유리, 뒷유리 열선, 핸들 열선", "60초 · 4단계", DrillCardState.OPEN, DrillGlyph.FROST, Modifier.weight(1f).fillMaxHeight(), onClick = {})
            DrillCard("출발 준비", "탄 사람 모두 벨트 확인", "곧 열려요", DrillCardState.COMING_SOON, DrillGlyph.BELT, Modifier.weight(1f).fillMaxHeight())
        }
        Spacer(Modifier.height(ChagilSpacing.l))
        Row(
            Modifier.fillMaxWidth().weight(1f).clip(ChagilShapes.large).background(c.surface).padding(ChagilSpacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(420.dp), verticalArrangement = Arrangement.spacedBy(ChagilSpacing.s)) {
                Text("내 차 도면", style = ChagilTheme.type.headline, color = c.text)
                Text("3 / 7칸에 불이 들어왔어요", style = ChagilTheme.type.bodyLarge, color = c.textSecondary)
                Spacer(Modifier.height(ChagilSpacing.s))
                TopBarButton("도면 보기", {}, icon = ChagilIcon.MAP)
            }
            CockpitMap(
                mapOf(
                    ControlPoint.LIGHT_STALK to ZoneState.LIT,
                    ControlPoint.WIPER_STALK to ZoneState.LIT,
                    ControlPoint.HVAC_FRONT_DEFROST to ZoneState.LIT,
                    ControlPoint.HVAC_REAR_DEFROST to ZoneState.UNLIT,
                    ControlPoint.STEERING_HEAT_BUTTON to ZoneState.UNLIT,
                    ControlPoint.GEAR_SELECTOR to ZoneState.UNLIT,
                    ControlPoint.SEAT_BELT to ZoneState.QUIZ,
                ),
                Modifier.weight(1f).fillMaxHeight(),
                animate = animate,
            )
        }
    }
}

@Composable
private fun GalleryStates(animate: Boolean) = BoxWithConstraints(Modifier.fillMaxSize()) {
    val g = gutter(maxWidth)
    val c = ChagilTheme.colors
    Row(Modifier.fillMaxSize().padding(g), horizontalArrangement = Arrangement.spacedBy(ChagilSpacing.xl)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(ChagilSpacing.l)) {
            Box(Modifier.fillMaxWidth().clip(ChagilShapes.large).background(c.surface).padding(ChagilSpacing.xl)) {
                InstructionPanel(
                    eyebrow = "지금은",
                    headline = "“한 번 닦기”예요",
                    voice = "반대쪽으로 한 칸 옮겨 보세요.",
                    readValue = "와이퍼 MIST",
                    tone = InstructionTone.HINT,
                )
            }
            Row(Modifier.fillMaxWidth().weight(1f).clip(ChagilShapes.large).background(c.surface).padding(ChagilSpacing.xl)) {
                Column(Modifier.weight(1f)) {
                    InstructionPanel(
                        headline = "괜찮아요. 여기 있어요",
                        voice = "공조 패널, 앞유리 그림이 있는 버튼이에요.",
                        tone = InstructionTone.CALM,
                    )
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(ChagilSpacing.targetGap)) {
                        PrimaryButton("다시", {}, icon = ChagilIcon.RETRY)
                        SecondaryButton("나중에", {})
                    }
                }
                ControlLocator(ControlPoint.HVAC_FRONT_DEFROST, Modifier.weight(1f).fillMaxHeight(), tone = LocatorTone.ATTENTION, animate = animate)
            }
        }
        Column(Modifier.width(620.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(ChagilSpacing.l)) {
            Column(Modifier.fillMaxWidth().clip(ChagilShapes.large).background(c.surface).padding(ChagilSpacing.xl)) {
                ResultHero("11.2초", deltaText = "지난번보다 3.1초 빨라요", improved = true)
            }
            LogPanel(
                listOf(
                    LogLine("00:02.1", "LowBeam 스위치 AUTOMATIC → ON · 통과", LogKind.PASS),
                    LogLine("00:04.0", "Wiper 스위치 OFF → MIST · 오조작", LogKind.HINT),
                    LogLine("00:11.2", "드릴 완료 · 11.2초", LogKind.DONE),
                ),
                simulation = true,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

@Preview(widthDp = 1707, heightDp = 960)
@Composable
private fun GalleryDrillPreview() = ComponentGallery(0, night = true, animate = false)

@Preview(widthDp = 1707, heightDp = 960)
@Composable
private fun GalleryHomeDayPreview() = ComponentGallery(1, night = false, animate = false)

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun GalleryStatesCompactPreview() = ComponentGallery(2, night = true, animate = false)
