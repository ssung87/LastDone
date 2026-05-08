package com.lastdone.app.ui.templates

data class BuiltInTemplate(
    val name: String,
    val intervalDays: Int,
    val icon: String,
    val categoryName: String
)

val builtInTemplates: List<BuiltInTemplate> = listOf(
    BuiltInTemplate("침구 세탁", 14, "🛏️", "집안관리"),
    BuiltInTemplate("에어컨 필터 청소", 30, "❄️", "집안관리"),
    BuiltInTemplate("냉장고 청소", 60, "🧊", "집안관리"),
    BuiltInTemplate("세탁조 청소", 60, "🫧", "집안관리"),
    BuiltInTemplate("가스레인지 후드 청소", 30, "🔥", "집안관리"),
    BuiltInTemplate("화장실 대청소", 14, "🚽", "집안관리"),
    BuiltInTemplate("정수기 필터 교체", 180, "💧", "집안관리"),

    BuiltInTemplate("칫솔 교체", 90, "🪥", "개인위생"),
    BuiltInTemplate("수건 교체", 180, "🧖", "개인위생"),
    BuiltInTemplate("면도날 교체", 14, "🪒", "개인위생"),

    BuiltInTemplate("자동차 세차", 14, "🚗", "차량"),
    BuiltInTemplate("엔진오일 교체", 180, "🛢️", "차량"),
    BuiltInTemplate("타이어 공기압 점검", 30, "🛞", "차량"),
    BuiltInTemplate("와이퍼 교체", 365, "🌧️", "차량"),

    BuiltInTemplate("강아지 목욕", 14, "🐶", "반려동물"),
    BuiltInTemplate("강아지 발톱 깎기", 21, "✂️", "반려동물"),
    BuiltInTemplate("사료 그릇 살균", 14, "🥣", "반려동물"),

    BuiltInTemplate("건강검진", 365, "🩺", "건강"),
    BuiltInTemplate("스케일링", 180, "🦷", "건강"),
    BuiltInTemplate("안과 검진", 365, "👁️", "건강")
)
