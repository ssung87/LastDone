package com.lastdone.app.ui.home

data class SuggestedItem(
    val name: String,
    val intervalDays: Int,
    val icon: String
)

val defaultSuggestions: List<SuggestedItem> = listOf(
    SuggestedItem(name = "침구 세탁", intervalDays = 14, icon = "🛏️"),
    SuggestedItem(name = "칫솔 교체", intervalDays = 90, icon = "🪥"),
    SuggestedItem(name = "에어컨 필터 청소", intervalDays = 30, icon = "❄️"),
    SuggestedItem(name = "정수기 필터 교체", intervalDays = 180, icon = "💧"),
    SuggestedItem(name = "강아지 목욕", intervalDays = 14, icon = "🐶")
)
