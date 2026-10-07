package com.negative.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class BrokeEngineTest {
    private val t0 = LocalDate.of(2026, 10, 7)
    private fun data(bal: Double, days: Long, vararg ex: Expense) =
        AppData(true, bal, t0, t0.plusDays(days), "₱", 1, ex.toList())
    private fun ex(id: Long, amt: Double, date: LocalDate) = Expense(id, "x", amt, date)

    @Test fun tenDaysAway() {
        val s = BrokeEngine.compute(data(10000.0, 10), t0)
        assertEquals(1000.0, s.todayTarget, 0.001)
        assertEquals(1000.0, s.toWasteToday, 0.001)
        assertEquals(10, s.daysLeft)
        assertEquals(Mode.NORMAL, s.mode)
        assertEquals(Meter.TRACK, s.meter)
    }

    @Test fun tomorrowIsEmergency() {
        val s = BrokeEngine.compute(data(4200.0, 1), t0)
        assertEquals(Mode.EMERGENCY, s.mode)
        assertEquals(4200.0, s.toWasteToday, 0.001)
    }

    @Test fun brokeDateIsToday() {
        val s = BrokeEngine.compute(data(4200.0, 0), t0)
        assertEquals(Mode.EMERGENCY, s.mode)
        assertEquals(0, s.daysLeft)
        assertEquals(4200.0, s.toWasteToday, 0.001)
    }

    @Test fun brokeDatePassedWithMoneyLeft() {
        val s = BrokeEngine.compute(data(1000.0, 1), t0.plusDays(3))
        assertEquals(-2, s.rawDays)
        assertEquals(0, s.daysLeft)
        assertEquals(Mode.EMERGENCY, s.mode)
        assertEquals(1000.0, s.toWasteToday, 0.001)
        assertEquals(Meter.TOO_MUCH, s.meter)
    }

    @Test fun targetRecalculatesAfterDaysPass() {
        val s = BrokeEngine.compute(data(10000.0, 10, ex(1, 1800.0, t0)), t0.plusDays(2))
        assertEquals(8200.0, s.remaining, 0.001)
        assertEquals(8, s.rawDays)
        assertEquals(1025.0, s.todayTarget, 0.001)
    }

    @Test fun midnightRolloverResetsTodaySpend() {
        val d = data(10000.0, 10, ex(1, 300.0, t0))
        assertEquals(300.0, BrokeEngine.compute(d, t0).spentToday, 0.001)
        val next = BrokeEngine.compute(d, t0.plusDays(1))
        assertEquals(0.0, next.spentToday, 0.001)
        assertEquals(9700.0 / 9, next.todayTarget, 0.01)
    }

    @Test fun behindSchedule() {
        val s = BrokeEngine.compute(data(10000.0, 10, ex(1, 300.0, t0)), t0)
        assertEquals(300.0, s.spentToday, 0.001)
        assertEquals(700.0, s.toWasteToday, 0.001)
        assertEquals(700.0, s.behindToday, 0.001)
    }

    @Test fun hittingTodaysTarget() {
        val s = BrokeEngine.compute(data(10000.0, 10, ex(1, 1000.0, t0)), t0)
        assertEquals(0.0, s.toWasteToday, 0.001)
        assertEquals(0.0, s.behindToday, 0.001)
    }

    @Test fun changingBrokeDate() {
        val d = data(10000.0, 10).copy(brokeDate = t0.plusDays(5))
        assertEquals(2000.0, BrokeEngine.compute(d, t0).todayTarget, 0.001)
    }

    @Test fun zeroEarly() {
        val s = BrokeEngine.compute(data(1000.0, 5, ex(1, 1000.0, t0)), t0)
        assertEquals(Mode.EARLY, s.mode)
        assertEquals(Meter.MISSION, s.meter)
        assertEquals(0.0, s.remaining, 0.0)
    }

    @Test fun zeroExactlyOnBrokeDate() {
        val s = BrokeEngine.compute(data(1000.0, 5, ex(1, 1000.0, t0)), t0.plusDays(5))
        assertEquals(Mode.EXACT, s.mode)
    }

    @Test fun negativeBalance() {
        val s = BrokeEngine.compute(data(1000.0, 5, ex(1, 1500.0, t0)), t0)
        assertEquals(Mode.DEBT, s.mode)
        assertEquals(-500.0, s.remaining, 0.001)
        assertEquals(0.0, s.toWasteToday, 0.0)
    }

    @Test fun editAndDeleteRecalculate() {
        val d = data(10000.0, 10, ex(1, 300.0, t0), ex(2, 200.0, t0))
        val edited = d.copy(expenses = d.expenses.map { if (it.id == 1L) it.copy(amount = 500.0) else it })
        assertEquals(9300.0, BrokeEngine.compute(edited, t0).remaining, 0.001)
        val deleted = edited.copy(expenses = edited.expenses.filter { it.id != 2L })
        assertEquals(9500.0, BrokeEngine.compute(deleted, t0).remaining, 0.001)
        assertEquals(1, BrokeEngine.compute(deleted, t0).badDecisions)
    }

    @Test fun neverNanOrInfinity() {
        val cases = listOf(
            data(0.0, 0), data(0.0, 10), data(-5.0, 3), data(1.0, -4),
            data(1e11, 1, ex(1, 1e11, t0))
        )
        for (d in cases) for (offset in -3L..15L) {
            val s = BrokeEngine.compute(d, t0.plusDays(offset))
            listOf(s.remaining, s.todayTarget, s.toWasteToday, s.behindToday, s.spentToday, s.destroyed)
                .forEach { assertTrue(it.isFinite()) }
            assertTrue(s.progress.isFinite())
            assertTrue(s.daysLeft >= 0)
        }
    }

    @Test fun formatting() {
        assertEquals("₱1,250", BrokeEngine.fmt(1250.0, "₱"))
        assertEquals("-₱500.50", BrokeEngine.fmt(-500.5, "₱"))
        assertEquals("₱0", BrokeEngine.fmt(Double.NaN, "₱"))
        assertEquals("₱0", BrokeEngine.fmt(Double.POSITIVE_INFINITY, "₱"))
    }

    @Test fun amountParsing() {
        assertEquals(250.0, BrokeEngine.parseAmount("250")!!, 0.0)
        assertEquals(1200.5, BrokeEngine.parseAmount("1,200.50")!!, 0.0)
        assertNull(BrokeEngine.parseAmount(""))
        assertNull(BrokeEngine.parseAmount("abc"))
        assertNull(BrokeEngine.parseAmount("-5"))
        assertNull(BrokeEngine.parseAmount("0"))
        assertNull(BrokeEngine.parseAmount("NaN"))
        assertNull(BrokeEngine.parseAmount("Infinity"))
    }
}
