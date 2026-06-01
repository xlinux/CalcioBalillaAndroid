package com.biliardino.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun formatDate(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "-"
        return try {
            val date = LocalDate.parse(isoDate.take(10), isoFormatter)
            date.format(displayFormatter)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun toIsoDate(displayDate: String): String {
        return try {
            val date = LocalDate.parse(displayDate, displayFormatter)
            date.format(isoFormatter)
        } catch (e: Exception) {
            displayDate
        }
    }

    fun now(): String = LocalDate.now().format(displayFormatter)
    fun monthsFromNow(months: Long): String = LocalDate.now().plusMonths(months).format(displayFormatter)
}
