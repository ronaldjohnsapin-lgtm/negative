package com.negative.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class Repo(ctx: Context) {
    private val p = ctx.getSharedPreferences("negative", Context.MODE_PRIVATE)

    fun load(): AppData = try {
        val list = ArrayList<Expense>()
        val arr = JSONArray(p.getString("expenses", "[]") ?: "[]")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(Expense(o.getLong("id"), o.getString("name"), o.getDouble("amount"), LocalDate.parse(o.getString("date"))))
        }
        val now = LocalDate.now().toString()
        AppData(
            configured = p.getBoolean("configured", false),
            startBalance = p.getString("start", "0")?.toDoubleOrNull() ?: 0.0,
            planStart = LocalDate.parse(p.getString("planStart", now) ?: now),
            brokeDate = LocalDate.parse(p.getString("brokeDate", now) ?: now),
            currency = p.getString("currency", "₱") ?: "₱",
            humor = p.getInt("humor", 1),
            expenses = list
        )
    } catch (e: Exception) {
        AppData()
    }

    fun save(d: AppData) {
        val arr = JSONArray()
        d.expenses.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("amount", it.amount).put("date", it.date.toString()))
        }
        p.edit()
            .putBoolean("configured", d.configured)
            .putString("start", d.startBalance.toString())
            .putString("planStart", d.planStart.toString())
            .putString("brokeDate", d.brokeDate.toString())
            .putString("currency", d.currency)
            .putInt("humor", d.humor)
            .putString("expenses", arr.toString())
            .commit()
    }
}
