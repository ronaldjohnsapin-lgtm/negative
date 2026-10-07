package com.negative.app

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class Expense(val id: Long, val name: String, val amount: Double, val date: LocalDate)

data class AppData(
    val configured: Boolean = false,
    val startBalance: Double = 0.0,
    val planStart: LocalDate = LocalDate.now(),
    val brokeDate: LocalDate = LocalDate.now().plusDays(10),
    val currency: String = "₱",
    val humor: Int = 1, // 0 MILD, 1 SARCASTIC, 2 NO MERCY
    val expenses: List<Expense> = emptyList()
)

enum class Mode { NORMAL, EMERGENCY, EARLY, EXACT, DEBT }

enum class Meter(val label: String) {
    TOO_MUCH("TOO MUCH MONEY"),
    TRACK("ON TRACK"),
    BEHIND("FALLING BEHIND"),
    FAST("SPENDING TOO FAST"),
    SOLVENT("DANGEROUSLY SOLVENT"),
    ALMOST("ALMOST BROKE"),
    MISSION("MISSION ACCOMPLISHED")
}

data class Snapshot(
    val remaining: Double,
    val rawDays: Int,      // may be negative once the Broke Date has passed
    val daysLeft: Int,     // never negative
    val spentToday: Double,
    val todayTarget: Double,
    val toWasteToday: Double,
    val behindToday: Double,
    val mode: Mode,
    val meter: Meter,
    val destroyed: Double,
    val badDecisions: Int,
    val progress: Float
)

object BrokeEngine {
    fun safe(x: Double): Double = if (x.isNaN() || x.isInfinite()) 0.0 else x
    fun round2(x: Double): Double = Math.round(safe(x) * 100.0) / 100.0

    fun compute(d: AppData, today: LocalDate): Snapshot {
        val destroyed = round2(d.expenses.sumOf { it.amount })
        val remaining = round2(d.startBalance - destroyed)
        val raw = ChronoUnit.DAYS.between(today, d.brokeDate).toInt()
        val days = max(raw, 0)
        val spentToday = round2(d.expenses.filter { it.date == today }.sumOf { it.amount })
        val startOfDay = remaining + spentToday
        val eff = max(raw, 1) // Broke Date today/past: spend everything now
        val target = if (startOfDay > 0.0) round2(startOfDay / eff) else 0.0
        val toWaste = if (remaining > 0.0) round2(min(max(target - spentToday, 0.0), remaining)) else 0.0
        val behind = if (spentToday > 0.0 && spentToday < target && remaining > 0.0) round2(target - spentToday) else 0.0

        val mode = when {
            remaining < 0.0 -> Mode.DEBT
            remaining == 0.0 -> if (raw > 0) Mode.EARLY else Mode.EXACT
            raw <= 1 -> Mode.EMERGENCY
            else -> Mode.NORMAL
        }

        val total = max(ChronoUnit.DAYS.between(d.planStart, d.brokeDate).toInt(), 1)
        val ideal = d.startBalance * min(days.toDouble() / total, 1.0)
        val rr = if (ideal > 0.0) remaining / ideal else Double.POSITIVE_INFINITY
        val meter = when {
            remaining <= 0.0 -> Meter.MISSION
            remaining <= d.startBalance * 0.05 -> Meter.ALMOST
            rr > 1.5 -> Meter.TOO_MUCH
            rr > 1.1 -> Meter.BEHIND
            rr >= 0.9 -> Meter.TRACK
            remaining <= d.startBalance * 0.2 -> Meter.SOLVENT
            else -> Meter.FAST
        }
        val progress = if (d.startBalance > 0.0) (1.0 - remaining / d.startBalance).coerceIn(0.0, 1.0).toFloat() else 1f
        return Snapshot(remaining, raw, days, spentToday, target, toWaste, behind, mode, meter,
            destroyed, d.expenses.size, progress)
    }

    fun fmt(amount: Double, cur: String): String {
        val v = round2(amount)
        val a = abs(v)
        val body = if (a == Math.floor(a)) String.format(Locale.US, "%,d", a.toLong())
        else String.format(Locale.US, "%,.2f", a)
        return (if (v < 0) "-" else "") + cur + body
    }

    fun parseAmount(s: String): Double? {
        val v = s.trim().replace(",", "").toDoubleOrNull() ?: return null
        return if (v.isFinite() && v > 0.0 && v < 1e12) round2(v) else null
    }
}
