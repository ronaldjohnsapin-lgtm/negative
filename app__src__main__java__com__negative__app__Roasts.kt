package com.negative.app

import kotlin.math.abs

data class Banner(val label: String, val big: String, val line1: String, val line2: String, val tone: Int)

object Roasts {
    const val DELETE_LINE = "Deleting it here doesn't mean you didn't buy it."
    private fun pick(h: Int, a: String, b: String, c: String): String =
        when (h) { 0 -> a; 2 -> c; else -> b }

    fun banner(s: Snapshot, d: AppData): Banner {
        val c = d.currency
        val h = d.humor
        val f = { x: Double -> BrokeEngine.fmt(x, c) }
        return when (s.mode) {
            Mode.DEBT -> Banner("📉 DEBT UNLOCKED", f(s.remaining), "Congratulations. You unlocked debt.",
                pick(h, "You're ${f(abs(s.remaining))} below zero.", "You're ${f(abs(s.remaining))} below zero. Bold.",
                    "You owe ${f(abs(s.remaining))} to a universe that warned you."), 1)
            Mode.EXACT -> Banner("🎉 WE DID IT!", f(0.0), "Not sure what we did, but we did it.", "Balance: exactly ${f(0.0)}.", 2)
            Mode.EARLY -> Banner("🏆 BROKE AHEAD OF SCHEDULE", f(0.0),
                "BROKE ${s.rawDays} DAY${if (s.rawDays == 1) "" else "S"} AHEAD OF SCHEDULE.",
                "Impressive efficiency. Terrible planning.", 2)
            Mode.EMERGENCY -> Banner("🚨 FINANCIAL EMERGENCY", f(s.remaining),
                when {
                    s.rawDays == 1 -> "THIS IS NOT A DRILL."
                    s.rawDays == 0 -> "BROKE DATE IS TODAY."
                    else -> "BROKE DATE WAS ${-s.rawDays} DAY${if (s.rawDays == -1) "" else "S"} AGO."
                },
                "YOU HAVE TOO MUCH MONEY. YOU MUST SPEND ${f(s.remaining)}.", 1)
            Mode.NORMAL -> when {
                s.behindToday > 0.0 -> Banner("STILL TO WASTE TODAY", f(s.toWasteToday), "STOP SAVING MONEY.",
                    "You're ${f(s.behindToday)} behind schedule.", 3)
                s.toWasteToday <= 0.0 && s.todayTarget > 0.0 -> Banner("TODAY'S QUOTA", f(0.0), "Quota destroyed.",
                    pick(h, "Rest up. Tomorrow we do it again.", "Disappointingly disciplined. Tomorrow, more.",
                        "Done already? Weak. Come back tomorrow."), 2)
                else -> Banner("YOU SHOULD SPEND TODAY", f(s.toWasteToday),
                    pick(h, "Stay focused. ${c}0 is the destination.", "Saving is a lifestyle we don't support.",
                        "Your wallet isn't a personality. Empty it."),
                    "Daily target: ${f(s.todayTarget)}", 0)
            }
        }
    }

    fun reaction(name: String, amount: Double, d: AppData, after: Snapshot): String {
        val h = d.humor
        val n = name.lowercase()
        val ratio = if (d.startBalance > 0.0) amount / d.startBalance else 0.0
        return when {
            after.remaining < 0.0 -> "Congratulations. You unlocked debt."
            after.remaining == 0.0 -> "🎉 WE DID IT! Not sure what we did, but we did it."
            ratio >= 0.3 -> pick(h, "That's a bold move.", "Have you considered simply being rich?",
                "Have you considered simply being rich? Because that's clearly the plan.")
            ratio >= 0.1 -> pick(h, "Interesting strategy.", "Interesting strategy.",
                "Interesting strategy. Is it working? It's working.")
            Regex("coffee|kape|milk tea|boba|starbucks|latte").containsMatchIn(n) ->
                pick(h, "A refreshing choice.", "Water was available.", "Water was available. Free. From a tap. You chose this.")
            Regex("shopee|lazada|temu|amazon").containsMatchIn(n) ->
                pick(h, "Package on the way.", "Package incoming. Financial stability outgoing.",
                    "Package incoming. Financial stability outgoing. Tracking number: regret.")
            Regex("grab|foodpanda|delivery|jollibee|mcdo").containsMatchIn(n) ->
                pick(h, "Delicious progress.", "Cooking was an option.", "Cooking was an option. So was the fridge.")
            else -> pick(h, "Progress.", "And so it begins.", "Another one gone. Beautiful.")
        }
    }
}
