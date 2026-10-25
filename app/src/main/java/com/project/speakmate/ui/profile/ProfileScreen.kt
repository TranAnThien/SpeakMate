package com.project.speakmate.ui.profile

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProfileScreen(
    initialShowSettings: Boolean = false,
    onLogoutClick: () -> Unit = {}
) {
    var showSettings by remember { mutableStateOf(initialShowSettings) }

    Crossfade(targetState = showSettings, label = "ProfileSettingsTransition") { isSettingsOpen ->
        if (isSettingsOpen) {
            SettingsContent(
                onBackClick = { showSettings = false },
                onLogoutClick = onLogoutClick
            )
        } else {
            ProfileMainContent(
                onOpenSettingsClick = { showSettings = true }
            )
        }
    }
}

@Composable
fun ProfileMainContent(
    onOpenSettingsClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFF7F9FC))
            .verticalScroll(scrollState)
    ) {
        StatusBarMock()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.size(48.dp))

            Text(
                text = "Cá nhân",
                color = Color(0xFF23457B),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onOpenSettingsClick() }
            ) {
                SettingsGearIcon(
                    color = Color(0xFF18263D),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(80.dp)
                        .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0x1A000000))
                        .clip(CircleShape)
                        .background(Color(0xFFDBEAFE))
                        .border(4.dp, Color.White, CircleShape)
                ) {
                    AvatarPlaceholderIllustration()

                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80",
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = "Đặng Trần Anh Quân",
                    color = Color(0xFF23457B),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "dangquan@gmail.com",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp
                )
            }

            Column(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp), spotColor = Color(0x0D000000))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEFF6FF))
                    ) {
                        LevelBadgeIllustration(color = Color(0xFF23457B))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = Color(0xFF18263D), fontWeight = FontWeight.Medium)) {
                                append("Trình độ hiện tại: ")
                            }
                            withStyle(style = SpanStyle(color = Color(0xFF23457B), fontWeight = FontWeight.Bold)) {
                                append("A2")
                            }
                        },
                        fontSize = 16.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFF6FF))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Đây là kết quả ước lượng phục vụ học tập. Tiếp tục luyện tập để đạt kết quả chính xác nhất.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 18.sp
                    )
                }

                Text(
                    text = "Đánh giá gần nhất: 07/10/2026",
                    color = Color(0xFF18263D),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                text = "Hồ sơ kỹ năng nói",
                color = Color(0xFF23457B),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SkillCardItem(
                title = "Range (Từ vựng)",
                description = "Sử dụng từ vựng đa dạng, linh hoạt",
                isPass = true
            )
            SkillCardItem(
                title = "Accuracy (Chính xác)",
                description = "Ngữ pháp và phát âm đúng chuẩn",
                isPass = false
            )
            SkillCardItem(
                title = "Fluency (Trôi chảy)",
                description = "Khả năng duy trì lời nói tương đối trôi chảy",
                isPass = true
            )
            SkillCardItem(
                title = "Interaction (Tương tác)",
                description = "Khả năng phản hồi và dẫn dắt",
                isPass = true
            )
            SkillCardItem(
                title = "Coherence (Gắn kết)",
                description = "Tính liên kết giữa các câu",
                isPass = false
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        ProfileBottomNavBar()
    }
}

@Composable
fun SkillCardItem(
    title: String,
    description: String,
    isPass: Boolean
) {
    val statusColor = if (isPass) Color(0xFF22C55E) else Color(0xFFF59E0B)
    val statusText = if (isPass) "ĐẠT" else "CẦN CẢI THIỆN"
    val progressFraction = if (isPass) 1f else 0.6f

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x0A000000))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFF18263D),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = description,
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(90.dp)
        ) {
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(statusColor)
                )
            }
        }
    }
}

@Composable
fun SettingsContent(
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFF7F9FC))
    ) {
        StatusBarMock()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onBackClick() }
            ) {
                ArrowBackIcon(
                    color = Color(0xFF18263D),
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "Cài đặt",
                color = Color(0xFF23457B),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.size(48.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "ỨNG DỤNG",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x0A000000))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                SettingMenuItem(
                    label = "Ngôn ngữ / Language",
                    valueText = "Tiếng Việt",
                    showDivider = true,
                    iconContent = { GlobeLanguageIcon(color = Color(0xFF23457B), modifier = Modifier.size(20.dp)) }
                )
                SettingMenuItem(
                    label = "Chính sách quyền riêng tư",
                    showDivider = true,
                    iconContent = { ShieldPrivacyIcon(color = Color(0xFF23457B), modifier = Modifier.size(20.dp)) }
                )
                SettingMenuItem(
                    label = "Điều khoản sử dụng",
                    showDivider = false,
                    iconContent = { DocumentTermsIcon(color = Color(0xFF23457B), modifier = Modifier.size(20.dp)) }
                )
            }

            Text(
                text = "TÀI KHOẢN",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x0A000000))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLogoutClick() }
                        .padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF2F2))
                    ) {
                        LogoutIcon(color = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "Đăng xuất",
                        color = Color(0xFFEF4444),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    ChevronRightIcon(
                        color = Color(0xFFEF4444).copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    }
}

@Composable
fun SettingMenuItem(
    label: String,
    valueText: String? = null,
    showDivider: Boolean = true,
    onClick: () -> Unit = {},
    iconContent: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEFF6FF))
            ) {
                iconContent()
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = label,
                color = Color(0xFF18263D),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            if (valueText != null) {
                Text(
                    text = valueText,
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            ChevronRightIcon(
                color = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = Color(0xFFE2E8F0),
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun StatusBarMock() {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
        }
    }
}

@Composable
fun ProfileBottomNavBar() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, spotColor = Color(0x14000000))
            .background(Color.White)
            .padding(top = 12.dp, bottom = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { }
            ) {
                ProfileMapNavIcon(color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lộ trình",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { }
            ) {
                ProfileHeadsetNavIcon(color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nhập vai",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { }
            ) {
                ProfileHistoryNavIcon(color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lịch sử",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { }
            ) {
                ProfilePersonNavIcon(color = Color(0xFF23457B))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cá nhân",
                    color = Color(0xFF23457B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .width(120.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFCBD5E1))
        )
    }
}

@Composable
fun AvatarPlaceholderIllustration() {
    Canvas(modifier = Modifier.size(44.dp)) {
        val color = Color(0xFF23457B)
        drawCircle(
            color = color,
            radius = size.width * 0.22f,
            center = Offset(size.width * 0.5f, size.height * 0.32f)
        )
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.15f, size.height * 0.60f),
            size = Size(size.width * 0.70f, size.height * 0.65f)
        )
    }
}

@Composable
fun LevelBadgeIllustration(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        drawRoundRect(
            color = color.copy(alpha = 0.4f),
            topLeft = Offset(size.width * 0.15f, size.height * 0.55f),
            size = Size(size.width * 0.18f, size.height * 0.35f),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        drawRoundRect(
            color = color.copy(alpha = 0.7f),
            topLeft = Offset(size.width * 0.41f, size.height * 0.35f),
            size = Size(size.width * 0.18f, size.height * 0.55f),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.67f, size.height * 0.15f),
            size = Size(size.width * 0.18f, size.height * 0.75f),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}

@Composable
fun SettingsGearIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.width * 0.42f
        val innerRadius = size.width * 0.28f

        for (i in 0 until 8) {
            val angleRad = Math.toRadians((i * 45).toDouble())
            val startX = center.x + (innerRadius * cos(angleRad)).toFloat()
            val startY = center.y + (innerRadius * sin(angleRad)).toFloat()
            val endX = center.x + (outerRadius * cos(angleRad)).toFloat()
            val endY = center.y + (outerRadius * sin(angleRad)).toFloat()
            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawCircle(
            color = color,
            radius = size.width * 0.28f,
            center = center,
            style = Stroke(width = 2.2.dp.toPx())
        )
    }
}

@Composable
fun ArrowBackIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(
            color = color,
            start = Offset(size.width * 0.80f, size.height * 0.50f),
            end = Offset(size.width * 0.20f, size.height * 0.50f),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
        val path = Path().apply {
            moveTo(size.width * 0.48f, size.height * 0.22f)
            lineTo(size.width * 0.20f, size.height * 0.50f)
            lineTo(size.width * 0.48f, size.height * 0.78f)
        }
        drawPath(path, color = color, style = stroke)
    }
}

@Composable
fun ChevronRightIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.35f, size.height * 0.20f)
            lineTo(size.width * 0.68f, size.height * 0.50f)
            lineTo(size.width * 0.35f, size.height * 0.80f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun GlobeLanguageIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.8.dp.toPx())
        drawCircle(color = color, radius = size.width * 0.42f, style = stroke)
        drawOval(
            color = color,
            topLeft = Offset(size.width * 0.28f, size.height * 0.08f),
            size = Size(size.width * 0.44f, size.height * 0.84f),
            style = stroke
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.08f, size.height * 0.50f),
            end = Offset(size.width * 0.92f, size.height * 0.50f),
            strokeWidth = 1.8.dp.toPx()
        )
    }
}

@Composable
fun ShieldPrivacyIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.50f, size.height * 0.10f)
            lineTo(size.width * 0.85f, size.height * 0.24f)
            cubicTo(
                size.width * 0.85f, size.height * 0.58f,
                size.width * 0.70f, size.height * 0.78f,
                size.width * 0.50f, size.height * 0.90f
            )
            cubicTo(
                size.width * 0.30f, size.height * 0.78f,
                size.width * 0.15f, size.height * 0.58f,
                size.width * 0.15f, size.height * 0.24f
            )
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
fun DocumentTermsIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.18f, size.height * 0.10f),
            size = Size(size.width * 0.64f, size.height * 0.80f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx())
        )
        drawLine(color = color, start = Offset(size.width * 0.32f, size.height * 0.35f), end = Offset(size.width * 0.68f, size.height * 0.35f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(size.width * 0.32f, size.height * 0.52f), end = Offset(size.width * 0.68f, size.height * 0.52f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(size.width * 0.32f, size.height * 0.69f), end = Offset(size.width * 0.52f, size.height * 0.69f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
fun LogoutIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val doorPath = Path().apply {
            moveTo(size.width * 0.48f, size.height * 0.15f)
            lineTo(size.width * 0.18f, size.height * 0.15f)
            lineTo(size.width * 0.18f, size.height * 0.85f)
            lineTo(size.width * 0.48f, size.height * 0.85f)
        }
        drawPath(doorPath, color = color, style = stroke)
        drawLine(
            color = color,
            start = Offset(size.width * 0.36f, size.height * 0.50f),
            end = Offset(size.width * 0.85f, size.height * 0.50f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        val arrowPath = Path().apply {
            moveTo(size.width * 0.65f, size.height * 0.30f)
            lineTo(size.width * 0.85f, size.height * 0.50f)
            lineTo(size.width * 0.65f, size.height * 0.70f)
        }
        drawPath(arrowPath, color = color, style = stroke)
    }
}

@Composable
fun ProfileMapNavIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.20f)
            lineTo(size.width * 0.36f, size.height * 0.10f)
            lineTo(size.width * 0.64f, size.height * 0.20f)
            lineTo(size.width * 0.92f, size.height * 0.10f)
            lineTo(size.width * 0.92f, size.height * 0.80f)
            lineTo(size.width * 0.64f, size.height * 0.90f)
            lineTo(size.width * 0.36f, size.height * 0.80f)
            lineTo(size.width * 0.08f, size.height * 0.90f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round))
    }
}

@Composable
fun ProfileHeadsetNavIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.15f, size.height * 0.12f),
            size = Size(size.width * 0.70f, size.height * 0.65f),
            style = stroke
        )
        drawRoundRect(color = color, topLeft = Offset(size.width * 0.12f, size.height * 0.45f), size = Size(size.width * 0.18f, size.height * 0.32f), cornerRadius = CornerRadius(3.dp.toPx()))
        drawRoundRect(color = color, topLeft = Offset(size.width * 0.70f, size.height * 0.45f), size = Size(size.width * 0.18f, size.height * 0.32f), cornerRadius = CornerRadius(3.dp.toPx()))
    }
}

@Composable
fun ProfileHistoryNavIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = color,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(size.width * 0.12f, size.height * 0.12f),
            size = Size(size.width * 0.76f, size.height * 0.76f),
            style = stroke
        )
        drawLine(color = color, start = Offset(size.width * 0.5f, size.height * 0.5f), end = Offset(size.width * 0.5f, size.height * 0.28f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(size.width * 0.5f, size.height * 0.5f), end = Offset(size.width * 0.66f, size.height * 0.58f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
fun ProfilePersonNavIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        drawCircle(
            color = color,
            radius = size.width * 0.20f,
            center = Offset(size.width * 0.50f, size.height * 0.30f)
        )
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(size.width * 0.16f, size.height * 0.56f),
            size = Size(size.width * 0.68f, size.height * 0.60f)
        )
    }
}

@Preview(
    name = "Profile & Settings Interactive Preview",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(initialShowSettings = false)
}