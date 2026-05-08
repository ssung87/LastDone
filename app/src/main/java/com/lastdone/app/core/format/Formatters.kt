package com.lastdone.app.core.format

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val IsoDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val LongDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
private val ShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일")
private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun formatIsoDate(date: LocalDate): String = date.format(IsoDateFormatter)
fun formatLongDate(date: LocalDate): String = date.format(LongDateFormatter)
fun formatShortDate(date: LocalDate): String = date.format(ShortDateFormatter)
fun formatTime(time: LocalTime): String = time.format(TimeFormatter)
