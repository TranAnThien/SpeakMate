package com.example.myapp

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
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*

fun Modifier.dashedBorder(
	width: Dp,
	color: Color,
	cornerRadius: Dp,
	dashLength: Dp = 6.dp,
	gapLength: Dp = 5.dp
) = this.drawWithContent {
	drawContent()
	drawRoundRect(
		color = color,
		style = Stroke(
			width = width.toPx(),
			pathEffect = PathEffect.dashPathEffect(
				floatArrayOf(dashLength.toPx(), gapLength.toPx()), 0f
			)
		),
		cornerRadius = CornerRadius(cornerRadius.toPx())
	)
}

@Composable
fun TrainingHomeScreen() {
	val scrollState = rememberScrollState()

	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(color = Color(0xFFF7F9FC))
			.verticalScroll(scrollState)
	) {
		Row(
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier
				.padding(bottom = 9.dp)
				.fillMaxWidth()
				.padding(vertical = 12.dp, horizontal = 24.dp)
		) {

		}

		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 20.dp)
		) {
			Text(
				"Lộ trình",
				color = Color(0xFF23457B),
				fontSize = 24.sp,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.padding(bottom = 17.dp)
			)

			Row(
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.padding(bottom = 16.dp)
					.shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1418263D))
					.clip(shape = RoundedCornerShape(16.dp))
					.border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(16.dp))
					.fillMaxWidth()
					.background(color = Color(0xFFFFFFFF))
					.padding(vertical = 13.dp)
			) {
				Box(
					contentAlignment = Alignment.Center,
					modifier = Modifier
						.padding(start = 16.dp, end = 14.dp)
						.size(42.dp)
						.clip(CircleShape)
						.background(color = Color(0xFFDBEAFE))
						.clickable { println("Pressed!") }
				) {
					Box(
						contentAlignment = Alignment.Center,
						modifier = Modifier
							.size(28.dp)
							.clip(CircleShape)
							.background(color = Color(0xFF3B82F6))
					) {
						Text(
							"A2",
							color = Color(0xFFFFFFFF),
							fontSize = 11.sp,
							fontWeight = FontWeight.Bold,
						)
					}
				}
				Column {
					Text(
						"Trình độ nói ước lượng",
						color = Color(0xFF64748B),
						fontSize = 12.sp,
					)
					Text(
						"A2",
						color = Color(0xFF23457B),
						fontSize = 19.sp,
						fontWeight = FontWeight.Bold,
					)
				}
			}

			Column(
				modifier = Modifier
					.padding(bottom = 28.dp)
					.shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1418263D))
					.clip(shape = RoundedCornerShape(16.dp))
					.border(width = 1.dp, color = Color(0xFFE2E8F0), shape = RoundedCornerShape(16.dp))
					.fillMaxWidth()
					.background(color = Color(0xFFFFFFFF))
					.padding(vertical = 15.dp, horizontal = 16.dp)
			) {
				Text(
					"MỤC TIÊU HIỆN TẠI",
					color = Color(0xFF64748B),
					fontSize = 11.sp,
					fontWeight = FontWeight.Bold,
					modifier = Modifier.padding(bottom = 4.dp)
				)
				Text(
					"Gọi món và trao đổi tại quán cà phê",
					color = Color(0xFF18263D),
					fontSize = 17.sp,
					fontWeight = FontWeight.Bold,
				)
			}

			Box(
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 28.dp)
			) {
				Canvas(modifier = Modifier.matchParentSize()) {
					val stroke = Stroke(
						width = 2.dp.toPx(),
						pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f)
					)
					val path = Path().apply {
						moveTo(size.width * 0.35f, 90.dp.toPx())
						cubicTo(
							size.width * 0.45f, 180.dp.toPx(),
							size.width * 0.55f, 200.dp.toPx(),
							size.width * 0.65f, 260.dp.toPx()
						)
						cubicTo(
							size.width * 0.55f, 400.dp.toPx(),
							size.width * 0.40f, 460.dp.toPx(),
							size.width * 0.35f, 540.dp.toPx()
						)
						cubicTo(
							size.width * 0.45f, 660.dp.toPx(),
							size.width * 0.55f, 700.dp.toPx(),
							size.width * 0.65f, 770.dp.toPx()
						)
					}
					drawPath(path, color = Color(0xFFCBD5E1), style = stroke)
				}

				Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 8.dp)
				) {
					Column(
						horizontalAlignment = Alignment.CenterHorizontally,
						modifier = Modifier.padding(bottom = 32.dp, start = 8.dp)
					) {
						Box(
							modifier = Modifier
								.padding(bottom = 10.dp)
								.size(104.dp)
						) {
							Box(
								contentAlignment = Alignment.Center,
								modifier = Modifier
									.size(96.dp)
									.align(Alignment.Center)
									.clip(RoundedCornerShape(24.dp))
									.background(Color.White)
									.border(1.5.dp, Color(0xFF237A57), RoundedCornerShape(24.dp))
							) {
								CoffeeCupIllustration(color = Color(0xFF23457B))
							}
							Box(
								contentAlignment = Alignment.Center,
								modifier = Modifier
									.size(30.dp)
									.align(Alignment.TopEnd)
									.shadow(
										elevation = 3.dp,
										shape = RoundedCornerShape(10.dp),
										spotColor = Color(0x1418263D)
									)
									.clip(RoundedCornerShape(10.dp))
									.background(Color.White)
									.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
							) {
								RefreshIcon(color = Color(0xFF23457B), modifier = Modifier.size(16.dp))
							}
							Box(
								contentAlignment = Alignment.Center,
								modifier = Modifier
									.size(24.dp)
									.align(Alignment.BottomEnd)
									.clip(CircleShape)
									.background(Color(0xFF237A57))
									.border(2.dp, Color.White, CircleShape)
							) {
								CheckIcon(color = Color.White, modifier = Modifier.size(12.dp))
							}
						}

						Column(
							horizontalAlignment = Alignment.CenterHorizontally,
							modifier = Modifier
								.shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1418263D))
								.clip(shape = RoundedCornerShape(16.dp))
								.border(width = 1.dp, color = Color(0x47237A57), shape = RoundedCornerShape(16.dp))
								.background(color = Color(0xFFFFFFFF))
								.padding(vertical = 11.dp, horizontal = 42.dp)
						) {
							Row(
								verticalAlignment = Alignment.CenterVertically,
								modifier = Modifier.padding(bottom = 4.dp)
							) {
								Text("Bài luyện", color = Color(0xFF64748B), fontSize = 11.sp)
								Text("  •  ", color = Color(0xFFCBD5E1), fontSize = 10.sp)
								Text(
									"Đã hoàn thành",
									color = Color(0xFF237A57),
									fontSize = 11.sp,
									fontWeight = FontWeight.Bold,
								)
							}
							Text(
								"Gọi đồ uống",
								color = Color(0xFF18263D),
								fontSize = 15.sp,
								fontWeight = FontWeight.Bold,
							)
						}
					}

					Column(
						horizontalAlignment = Alignment.End,
						modifier = Modifier
							.padding(bottom = 32.dp)
							.fillMaxWidth()
					) {
						Column(
							horizontalAlignment = Alignment.CenterHorizontally,
							modifier = Modifier.padding(end = 4.dp)
						) {
							Box(
								contentAlignment = Alignment.Center,
								modifier = Modifier
									.padding(bottom = 10.dp)
									.size(96.dp)
									.shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color(0x2223457B))
									.clip(RoundedCornerShape(24.dp))
									.background(Color(0xFFEAF0F9))
									.border(1.5.dp, Color(0xFF23457B), RoundedCornerShape(24.dp))
							) {
								DrinkSizeIllustration(color = Color(0xFF23457B))
							}

							Column(
								horizontalAlignment = Alignment.CenterHorizontally,
								modifier = Modifier
									.shadow(elevation = 10.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1F23457B))
									.clip(shape = RoundedCornerShape(16.dp))
									.border(width = 1.dp, color = Color(0xFF23457B), shape = RoundedCornerShape(16.dp))
									.background(color = Color(0xFFEAF0F9))
									.padding(vertical = 13.dp, horizontal = 16.dp)
							) {
								Box(
									modifier = Modifier
										.padding(bottom = 7.dp)
										.clip(shape = RoundedCornerShape(12.dp))
										.background(color = Color(0xFF23457B))
										.padding(vertical = 3.dp, horizontal = 10.dp)
								) {
									Text(
										"Đang học",
										color = Color(0xFFFFFFFF),
										fontSize = 11.sp,
										fontWeight = FontWeight.Bold,
									)
								}
								Text(
									"Điều chỉnh đơn gọi món",
									color = Color(0xFF23457B),
									fontSize = 15.sp,
									fontWeight = FontWeight.Bold,
									modifier = Modifier.padding(bottom = 6.dp)
								)
								Text(
									"Luyện yêu cầu đổi kích cỡ đồ uống và xác nhận lại đơn.",
									color = Color(0xFF64748B),
									fontSize = 13.sp,
									textAlign = TextAlign.Center,
									modifier = Modifier.width(216.dp)
								)
							}
						}
					}

					Column(
						horizontalAlignment = Alignment.CenterHorizontally,
						modifier = Modifier.padding(bottom = 32.dp, start = 4.dp)
					) {
						Box(
							modifier = Modifier
								.padding(bottom = 10.dp)
								.size(102.dp)
						) {
							Box(
								contentAlignment = Alignment.Center,
								modifier = Modifier
									.size(96.dp)
									.align(Alignment.Center)
									.clip(RoundedCornerShape(24.dp))
									.background(Color(0xFFF1F5F9))
									.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
							) {
								ReceiptIllustration(color = Color(0xFF475569))
							}
							Box(
								contentAlignment = Alignment.Center,
								modifier = Modifier
									.size(24.dp)
									.align(Alignment.BottomEnd)
									.clip(CircleShape)
									.background(Color(0xFF64748B))
									.border(2.dp, Color.White, CircleShape)
							) {
								LockIcon(color = Color.White, modifier = Modifier.size(12.dp))
							}
						}

						Column(
							horizontalAlignment = Alignment.CenterHorizontally,
							modifier = Modifier
								.clip(shape = RoundedCornerShape(16.dp))
								.background(color = Color(0xFFF8FAFC))
								.dashedBorder(width = 1.dp, color = Color(0xFFCBD5E1), cornerRadius = 16.dp)
								.padding(vertical = 11.dp, horizontal = 24.dp)
						) {
							Row(
								verticalAlignment = Alignment.CenterVertically,
								modifier = Modifier.padding(bottom = 4.dp)
							) {
								Text("Bài luyện", color = Color(0xFF64748B), fontSize = 11.sp)
								Text("  •  ", color = Color(0xFFCBD5E1), fontSize = 10.sp)
								Text("Chưa mở khóa", color = Color(0xFF64748B), fontSize = 11.sp)
							}
							Text(
								"Hỏi thông tin và thanh toán",
								color = Color(0xFF334155),
								fontSize = 15.sp,
								fontWeight = FontWeight.Bold,
							)
						}
					}

					Column(
						horizontalAlignment = Alignment.End,
						modifier = Modifier.fillMaxWidth()
					) {
						Column(
							horizontalAlignment = Alignment.CenterHorizontally,
							modifier = Modifier.padding(end = 4.dp)
						) {
							Box(
								modifier = Modifier
									.padding(bottom = 10.dp)
									.size(102.dp)
							) {
								Box(
									contentAlignment = Alignment.Center,
									modifier = Modifier
										.size(96.dp)
										.align(Alignment.Center)
										.clip(RoundedCornerShape(24.dp))
										.background(Color(0xFFEFF4F9))
										.dashedBorder(1.2.dp, Color(0xFF94A3B8), 24.dp)
								) {
									FlagIllustration(color = Color(0xFF23457B))
								}
								Box(
									contentAlignment = Alignment.Center,
									modifier = Modifier
										.size(24.dp)
										.align(Alignment.BottomEnd)
										.clip(CircleShape)
										.background(Color(0xFF64748B))
										.border(2.dp, Color.White, CircleShape)
								) {
									LockIcon(color = Color.White, modifier = Modifier.size(12.dp))
								}
							}

							Column(
								horizontalAlignment = Alignment.CenterHorizontally,
								modifier = Modifier
									.clip(shape = RoundedCornerShape(16.dp))
									.background(color = Color(0xFFF8FAFC))
									.dashedBorder(width = 1.dp, color = Color(0xFFCBD5E1), cornerRadius = 16.dp)
									.padding(vertical = 10.dp, horizontal = 24.dp)
							) {
								Row(
									verticalAlignment = Alignment.CenterVertically,
									modifier = Modifier.padding(bottom = 5.dp)
								) {
									Text(
										"Đánh giá mục tiêu",
										color = Color(0xFF23457B),
										fontSize = 11.sp,
										fontWeight = FontWeight.Bold
									)
									Text("  •  ", color = Color(0xFFCBD5E1), fontSize = 10.sp)
									Text("Chưa mở khóa", color = Color(0xFF64748B), fontSize = 11.sp)
								}
								Text(
									"Đánh giá mục tiêu",
									color = Color(0xFF334155),
									fontSize = 15.sp,
									fontWeight = FontWeight.Bold,
								)
							}
						}
					}
				}
			}

			Row(
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.padding(bottom = 16.dp)
					.fillMaxWidth()
					.clip(shape = RoundedCornerShape(16.dp))
					.background(color = Color(0xFFF8FAFC))
					.dashedBorder(
						width = 1.2.dp,
						color = Color(0xFFCBD5E1),
						cornerRadius = 16.dp
					)
					.padding(horizontal = 16.dp, vertical = 16.dp)
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(
						"MỤC TIÊU TIẾP THEO",
						color = Color(0xFF64748B),
						fontSize = 11.sp,
						fontWeight = FontWeight.Bold,
						modifier = Modifier.padding(bottom = 4.dp)
					)
					Text(
						"Hỏi đường và sử dụng phương tiện",
						color = Color(0xFF334155),
						fontSize = 15.sp,
						fontWeight = FontWeight.Bold,
					)
				}
				Box(
					contentAlignment = Alignment.Center,
					modifier = Modifier
						.size(42.dp)
						.clip(RoundedCornerShape(12.dp))
						.background(Color(0xFFEFF4F9))
				) {
					LockIcon(color = Color(0xFF64748B), modifier = Modifier.size(18.dp))
				}
			}
		}

		Column(
			modifier = Modifier
				.fillMaxWidth()
				.background(Color(0xFFF7F9FC))
		) {
			HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

			Row(
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					.padding(horizontal = 16.dp, vertical = 12.dp)
					.shadow(
						elevation = 10.dp,
						shape = RoundedCornerShape(20.dp),
						spotColor = Color(0x1A18263D)
					)
					.clip(RoundedCornerShape(20.dp))
					.background(Color.White)
					.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
					.fillMaxWidth()
					.padding(horizontal = 16.dp, vertical = 12.dp)
			) {
				Column(
					modifier = Modifier
						.weight(1f)
						.padding(end = 8.dp)
				) {
					Text(
						text = "BÀI LUYỆN HIỆN TẠI",
						color = Color(0xFF64748B),
						fontSize = 11.sp,
						fontWeight = FontWeight.Bold,
						modifier = Modifier.padding(bottom = 4.dp)
					)
					Text(
						text = "Điều chỉnh đơn gọi món",
						color = Color(0xFF18263D),
						fontSize = 15.sp,
						fontWeight = FontWeight.Bold
					)
				}

				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.Center,
					modifier = Modifier
						.clip(RoundedCornerShape(14.dp))
						.background(Color(0xFF23457B))
						.clickable { println("Tiếp tục lộ trình!") }
						.padding(horizontal = 16.dp, vertical = 12.dp)
				) {
					Text(
						text = "Tiếp tục lộ trình",
						color = Color.White,
						fontSize = 14.sp,
						fontWeight = FontWeight.Bold
					)
					Spacer(modifier = Modifier.width(8.dp))
					PlayArrowIcon(color = Color.White, modifier = Modifier.size(11.dp))
				}
			}

			Column(
				horizontalAlignment = Alignment.CenterHorizontally,
				modifier = Modifier
					.fillMaxWidth()
					.background(Color.White)
					.padding(top = 10.dp, bottom = 8.dp)
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
						MapNavIcon(color = Color(0xFF23457B))
						Spacer(modifier = Modifier.height(4.dp))
						Text(
							text = "Lộ trình",
							color = Color(0xFF23457B),
							fontSize = 12.sp,
							fontWeight = FontWeight.Bold
						)
					}

					Column(
						horizontalAlignment = Alignment.CenterHorizontally,
						modifier = Modifier.clickable { }
					) {
						HeadsetNavIcon(color = Color(0xFF64748B))
						Spacer(modifier = Modifier.height(4.dp))
						Text(
							text = "Nhập vai",
							color = Color(0xFF64748B),
							fontSize = 12.sp,
							fontWeight = FontWeight.Medium
						)
					}

					Column(
						horizontalAlignment = Alignment.CenterHorizontally,
						modifier = Modifier.clickable { }
					) {
						HistoryNavIcon(color = Color(0xFF64748B))
						Spacer(modifier = Modifier.height(4.dp))
						Text(
							text = "Lịch sử",
							color = Color(0xFF64748B),
							fontSize = 12.sp,
							fontWeight = FontWeight.Medium
						)
					}

					Column(
						horizontalAlignment = Alignment.CenterHorizontally,
						modifier = Modifier.clickable { }
					) {
						PersonNavIcon(color = Color(0xFF64748B))
						Spacer(modifier = Modifier.height(4.dp))
						Text(
							text = "Cá nhân",
							color = Color(0xFF64748B),
							fontSize = 12.sp,
							fontWeight = FontWeight.Medium
						)
					}
				}

				Box(
					modifier = Modifier
						.width(134.dp)
						.height(5.dp)
						.clip(RoundedCornerShape(50))
						.background(Color(0xFFCBD5E1))
				)
			}
		}
	}
}

@Composable
fun CheckIcon(color: Color, modifier: Modifier = Modifier) {
	Canvas(modifier = modifier) {
		val path = Path().apply {
			moveTo(size.width * 0.18f, size.height * 0.52f)
			lineTo(size.width * 0.42f, size.height * 0.76f)
			lineTo(size.width * 0.84f, size.height * 0.26f)
		}
		drawPath(
			path = path,
			color = color,
			style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
		)
	}
}

@Composable
fun RefreshIcon(color: Color, modifier: Modifier = Modifier) {
	Canvas(modifier = modifier) {
		val strokeWidth = 2.dp.toPx()
		val arcPadding = size.width * 0.14f

		drawArc(
			color = color,
			startAngle = 270f,
			sweepAngle = 255f,
			useCenter = false,
			topLeft = Offset(arcPadding, arcPadding),
			size = Size(size.width - arcPadding * 2, size.height - arcPadding * 2),
			style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
		)

		val tipX = size.width * 0.44f
		val tipY = arcPadding
		val wingSize = size.width * 0.17f

		val arrowPath = Path().apply {
			moveTo(tipX + wingSize, tipY - wingSize * 0.75f)
			lineTo(tipX, tipY)
			lineTo(tipX + wingSize, tipY + wingSize * 0.75f)
		}
		drawPath(
			path = arrowPath,
			color = color,
			style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
		)
	}
}

@Composable
fun LockIcon(color: Color, modifier: Modifier = Modifier) {
	Canvas(modifier = modifier) {
		drawArc(
			color = color,
			startAngle = 180f,
			sweepAngle = 180f,
			useCenter = false,
			topLeft = Offset(size.width * 0.26f, size.height * 0.10f),
			size = Size(size.width * 0.48f, size.height * 0.50f),
			style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
		)
		drawRoundRect(
			color = color,
			topLeft = Offset(size.width * 0.16f, size.height * 0.42f),
			size = Size(size.width * 0.68f, size.height * 0.48f),
			cornerRadius = CornerRadius(3.dp.toPx())
		)
	}
}

@Composable
fun PlayArrowIcon(color: Color, modifier: Modifier = Modifier) {
	Canvas(modifier = modifier) {
		val path = Path().apply {
			moveTo(size.width * 0.15f, size.height * 0.10f)
			lineTo(size.width * 0.90f, size.height * 0.50f)
			lineTo(size.width * 0.15f, size.height * 0.90f)
			close()
		}
		drawPath(path = path, color = color)
	}
}

@Composable
fun PersonNavIcon(color: Color) {
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

@Composable
fun CoffeeCupIllustration(color: Color) {
	Canvas(modifier = Modifier.size(44.dp)) {
		val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
		drawLine(color = Color(0xFF237A57), start = Offset(size.width * 0.35f, size.height * 0.25f), end = Offset(size.width * 0.38f, size.height * 0.15f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
		drawLine(color = Color(0xFF237A57), start = Offset(size.width * 0.48f, size.height * 0.25f), end = Offset(size.width * 0.51f, size.height * 0.15f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
		drawLine(color = Color(0xFF237A57), start = Offset(size.width * 0.61f, size.height * 0.25f), end = Offset(size.width * 0.64f, size.height * 0.15f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
		drawRoundRect(
			color = Color(0xFFDBEAFE),
			topLeft = Offset(size.width * 0.22f, size.height * 0.35f),
			size = Size(size.width * 0.48f, size.height * 0.42f),
			cornerRadius = CornerRadius(6.dp.toPx())
		)
		drawRoundRect(
			color = color,
			topLeft = Offset(size.width * 0.22f, size.height * 0.35f),
			size = Size(size.width * 0.48f, size.height * 0.42f),
			cornerRadius = CornerRadius(6.dp.toPx()),
			style = stroke
		)
		drawArc(
			color = color,
			startAngle = -70f,
			sweepAngle = 140f,
			useCenter = false,
			topLeft = Offset(size.width * 0.60f, size.height * 0.42f),
			size = Size(size.width * 0.22f, size.height * 0.25f),
			style = stroke
		)
		drawLine(color = color, start = Offset(size.width * 0.15f, size.height * 0.78f), end = Offset(size.width * 0.78f, size.height * 0.78f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
	}
}

@Composable
fun DrinkSizeIllustration(color: Color) {
	Canvas(modifier = Modifier.size(44.dp)) {
		val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
		val cupPath = Path().apply {
			moveTo(size.width * 0.22f, size.height * 0.32f)
			lineTo(size.width * 0.62f, size.height * 0.32f)
			lineTo(size.width * 0.55f, size.height * 0.82f)
			lineTo(size.width * 0.29f, size.height * 0.82f)
			close()
		}
		drawPath(cupPath, color = Color(0xFFDBEAFE))
		drawPath(cupPath, color = color, style = stroke)
		drawLine(color = color, start = Offset(size.width * 0.18f, size.height * 0.32f), end = Offset(size.width * 0.66f, size.height * 0.32f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
		val strawPath = Path().apply {
			moveTo(size.width * 0.42f, size.height * 0.32f)
			lineTo(size.width * 0.46f, size.height * 0.16f)
			lineTo(size.width * 0.56f, size.height * 0.13f)
		}
		drawPath(strawPath, color = color, style = stroke)
		val arrowX = size.width * 0.82f
		drawLine(color = color, start = Offset(arrowX, size.height * 0.32f), end = Offset(arrowX, size.height * 0.78f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
		drawLine(color = color, start = Offset(arrowX - 4.dp.toPx(), size.height * 0.40f), end = Offset(arrowX, size.height * 0.32f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
		drawLine(color = color, start = Offset(arrowX + 4.dp.toPx(), size.height * 0.40f), end = Offset(arrowX, size.height * 0.32f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
		drawLine(color = color, start = Offset(arrowX - 4.dp.toPx(), size.height * 0.70f), end = Offset(arrowX, size.height * 0.78f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
		drawLine(color = color, start = Offset(arrowX + 4.dp.toPx(), size.height * 0.70f), end = Offset(arrowX, size.height * 0.78f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
	}
}

@Composable
fun ReceiptIllustration(color: Color) {
	Canvas(modifier = Modifier.size(40.dp)) {
		val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
		val path = Path().apply {
			moveTo(size.width * 0.25f, size.height * 0.15f)
			lineTo(size.width * 0.75f, size.height * 0.15f)
			lineTo(size.width * 0.75f, size.height * 0.85f)
			lineTo(size.width * 0.63f, size.height * 0.78f)
			lineTo(size.width * 0.50f, size.height * 0.85f)
			lineTo(size.width * 0.37f, size.height * 0.78f)
			lineTo(size.width * 0.25f, size.height * 0.85f)
			close()
		}
		drawPath(path, color = Color(0xFFE2E8F0))
		drawPath(path, color = color, style = stroke)
		drawLine(color = color, start = Offset(size.width * 0.38f, size.height * 0.35f), end = Offset(size.width * 0.62f, size.height * 0.35f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
		drawLine(color = color, start = Offset(size.width * 0.38f, size.height * 0.50f), end = Offset(size.width * 0.62f, size.height * 0.50f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
		drawLine(color = color, start = Offset(size.width * 0.38f, size.height * 0.65f), end = Offset(size.width * 0.50f, size.height * 0.65f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
	}
}

@Composable
fun FlagIllustration(color: Color) {
	Canvas(modifier = Modifier.size(40.dp)) {
		val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
		drawLine(color = color, start = Offset(size.width * 0.30f, size.height * 0.20f), end = Offset(size.width * 0.30f, size.height * 0.82f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
		drawLine(color = color, start = Offset(size.width * 0.20f, size.height * 0.82f), end = Offset(size.width * 0.40f, size.height * 0.82f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
		val flagPath = Path().apply {
			moveTo(size.width * 0.30f, size.height * 0.24f)
			lineTo(size.width * 0.78f, size.height * 0.24f)
			lineTo(size.width * 0.68f, size.height * 0.40f)
			lineTo(size.width * 0.78f, size.height * 0.56f)
			lineTo(size.width * 0.30f, size.height * 0.56f)
			close()
		}
		drawPath(flagPath, color = Color(0xFFDBEAFE))
		drawPath(flagPath, color = color, style = stroke)
	}
}

@Composable
fun MapNavIcon(color: Color) {
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
		drawPath(path, color = color)
		drawLine(color = Color.White, start = Offset(size.width * 0.36f, size.height * 0.10f), end = Offset(size.width * 0.36f, size.height * 0.80f), strokeWidth = 1.5.dp.toPx())
		drawLine(color = Color.White, start = Offset(size.width * 0.64f, size.height * 0.20f), end = Offset(size.width * 0.64f, size.height * 0.90f), strokeWidth = 1.5.dp.toPx())
	}
}

@Composable
fun HeadsetNavIcon(color: Color) {
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
		drawArc(
			color = color,
			startAngle = 0f,
			sweepAngle = 110f,
			useCenter = false,
			topLeft = Offset(size.width * 0.30f, size.height * 0.45f),
			size = Size(size.width * 0.55f, size.height * 0.45f),
			style = stroke
		)
	}
}

@Composable
fun HistoryNavIcon(color: Color) {
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

@Preview(
	name = "Training Home Preview",
	showBackground = true,
	showSystemUi = true
)
@Composable
fun TrainingHomeScreenPreview() {
	TrainingHomeScreen()
}