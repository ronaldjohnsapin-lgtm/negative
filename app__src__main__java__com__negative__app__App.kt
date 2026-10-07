package com.negative.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object Ui {
    val BG = Color(0xFF0B0C0F)
    val CARD = Color(0xFF17191F)
    val BORDER = Color(0xFF30333B)
    val MUTED = Color(0xFF9A9DA6)
    val WHITE = Color.White
    val RED = Color(0xFFFF5C5C)
    val GREEN = Color(0xFF3DDC97)
    val AMBER = Color(0xFFFFB347)
}

fun dateText(d: LocalDate, today: LocalDate): String =
    d.format(DateTimeFormatter.ofPattern(if (d.year == today.year) "MMMM d" else "MMMM d, yyyy", Locale.ENGLISH))

@Composable
fun NegativeApp(repo: Repo) {
    var data by remember { mutableStateOf(repo.load()) }
    val today by produceState(LocalDate.now()) {
        while (true) { value = LocalDate.now(); delay(15_000) } // handles midnight rollover
    }
    var tab by remember { mutableIntStateOf(0) }
    var roast by remember { mutableStateOf("Don't get excited. We both know how this ends.") }

    fun update(n: AppData) { data = n; repo.save(n) }

    if (!data.configured) {
        Setup(today) { bal, date ->
            update(AppData(true, bal, today, date, data.currency, data.humor, emptyList()))
        }
        return
    }

    val snap = BrokeEngine.compute(data, today)

    Scaffold(
        containerColor = Ui.BG,
        bottomBar = {
            NavigationBar(containerColor = Ui.CARD) {
                listOf("🏠" to "HOME", "🧾" to "EVIDENCE", "⚙️" to "SETTINGS").forEachIndexed { i, (ic, label) ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Text(ic) }, label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Ui.BORDER, selectedTextColor = Ui.WHITE, unselectedTextColor = Ui.MUTED
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> Home(snap, data, today, roast) { n, a ->
                    val nd = data.copy(expenses = data.expenses + Expense(System.currentTimeMillis(), n, a, today))
                    roast = Roasts.reaction(n, a, nd, BrokeEngine.compute(nd, today))
                    update(nd)
                }
                1 -> Evidence(snap, data,
                    onEdit = { e, n, a -> update(data.copy(expenses = data.expenses.map { if (it.id == e.id) it.copy(name = n, amount = a) else it })) },
                    onDelete = { e -> update(data.copy(expenses = data.expenses.filter { it.id != e.id })) })
                else -> SettingsScreen(snap, data, today) { update(it) }
            }
        }
    }
}

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth()
            .background(Ui.CARD, RoundedCornerShape(18.dp))
            .border(1.dp, Ui.BORDER, RoundedCornerShape(18.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
fun Label(t: String) = Text(t, color = Ui.MUTED, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)

@Composable
fun Home(s: Snapshot, d: AppData, today: LocalDate, roast: String, onAdd: (String, Double) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    val b = Roasts.banner(s, d)
    val tone = when (b.tone) { 1 -> Ui.RED; 2 -> Ui.GREEN; 3 -> Ui.AMBER; else -> Ui.WHITE }
    val meterTone = when (s.meter) {
        Meter.TRACK, Meter.MISSION -> Ui.GREEN
        Meter.BEHIND, Meter.TOO_MUCH -> Ui.AMBER
        else -> Ui.RED
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("NEGATIVE ${d.currency}", color = Ui.MUTED, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        CardBox {
            Label("BROKE DATE")
            Text(dateText(d.brokeDate, today), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardBox(Modifier.weight(1f)) {
                Label("MONEY REMAINING")
                Text(BrokeEngine.fmt(s.remaining, d.currency), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = if (s.remaining < 0) Ui.RED else Ui.WHITE)
            }
            CardBox(Modifier.weight(1f)) {
                Label("DAYS UNTIL BROKE")
                Text("${s.daysLeft}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        CardBox {
            Label(b.label)
            Text(b.big, fontSize = 46.sp, fontWeight = FontWeight.Black, color = tone)
            Spacer(Modifier.height(6.dp))
            Text(b.line1, fontWeight = FontWeight.Bold, color = tone)
            Text(b.line2, color = Ui.MUTED)
        }
        CardBox {
            Label("💀 BROKE-O-METER")
            Spacer(Modifier.height(6.dp))
            Text(s.meter.label, fontWeight = FontWeight.Bold, color = meterTone)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth().height(8.dp),
                color = meterTone, trackColor = Ui.BORDER)
        }
        CardBox {
            Label("FINANCIAL ADVISER")
            Spacer(Modifier.height(4.dp))
            Text(roast, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = { adding = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ui.WHITE, contentColor = Ui.BG)
        ) { Text("+ WASTE MONEY", fontWeight = FontWeight.ExtraBold) }
    }
    if (adding) ExpenseDialog(null, { n, a -> onAdd(n, a); adding = false }, { adding = false })
}

@Composable
fun ExpenseDialog(initial: Expense?, onSave: (String, Double) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amt by remember {
        mutableStateOf(initial?.amount?.let { if (it == Math.floor(it)) it.toLong().toString() else it.toString() } ?: "")
    }
    val parsed = BrokeEngine.parseAmount(amt)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Ui.CARD,
        title = { Text(if (initial == null) "What did you waste money on?" else "Edit the evidence") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Milk Tea") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amt, onValueChange = { amt = it }, label = { Text("Amount") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amt.isNotEmpty() && parsed == null
                )
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = {
                if (parsed != null) onSave(name.trim().ifEmpty { "Mysterious bad decision" }, parsed)
            }) { Text("MAKE IT DISAPPEAR") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
fun Evidence(s: Snapshot, d: AppData, onEdit: (Expense, String, Double) -> Unit, onDelete: (Expense) -> Unit) {
    var editing by remember { mutableStateOf<Expense?>(null) }
    var deleting by remember { mutableStateOf<Expense?>(null) }
    val sorted = d.expenses.sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.id })
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("EVIDENCE", fontSize = 26.sp, fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardBox(Modifier.weight(1f)) {
                Label("MONEY DESTROYED")
                Text(BrokeEngine.fmt(s.destroyed, d.currency), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            CardBox(Modifier.weight(1f)) {
                Label("BAD DECISIONS")
                Text("${s.badDecisions}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (sorted.isEmpty()) {
            Text("No evidence yet. Suspicious.", color = Ui.MUTED)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sorted, key = { it.id }) { e ->
                    Row(
                        Modifier.fillMaxWidth()
                            .background(Ui.CARD, RoundedCornerShape(14.dp))
                            .border(1.dp, Ui.BORDER, RoundedCornerShape(14.dp))
                            .clickable { editing = e }
                            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(e.name, fontWeight = FontWeight.SemiBold)
                            Text(e.date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)), color = Ui.MUTED, fontSize = 12.sp)
                        }
                        Text("-" + BrokeEngine.fmt(e.amount, d.currency), fontWeight = FontWeight.Bold)
                        TextButton(onClick = { deleting = e }) { Text("✕", color = Ui.MUTED) }
                    }
                }
            }
        }
    }
    editing?.let { e ->
        ExpenseDialog(e, { n, a -> onEdit(e, n, a); editing = null }, { editing = null })
    }
    deleting?.let { e ->
        AlertDialog(
            onDismissRequest = { deleting = null }, containerColor = Ui.CARD,
            title = { Text("Delete this evidence?") },
            text = { Text(Roasts.DELETE_LINE) },
            confirmButton = { TextButton(onClick = { onDelete(e); deleting = null }) { Text("DELETE ANYWAY", color = Ui.RED) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("KEEP IT") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val st = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                st.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    ) { DatePicker(state = st) }
}

@Composable
fun Setup(today: LocalDate, onDone: (Double, LocalDate) -> Unit) {
    var money by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today.plusDays(10)) }
    var picking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(Ui.BG).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text("NEGATIVE ₱", color = Ui.MUTED, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Tracking your money won't bring it back.", fontSize = 28.sp, fontWeight = FontWeight.Black)
        CardBox {
            Label("HOW MUCH MONEY DO YOU HAVE?")
            OutlinedTextField(value = money, onValueChange = { money = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), label = { Text("10000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Spacer(Modifier.height(14.dp))
            Label("WHEN SHOULD IT ALL BE GONE?")
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                Text(dateText(date, today), color = Ui.WHITE)
            }
            if (error.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(error, color = Ui.RED) }
        }
        Button(
            onClick = {
                val m = BrokeEngine.parseAmount(money)
                when {
                    m == null -> error = "Enter an amount above zero."
                    date.isBefore(today) -> error = "Broke Date can't be in the past."
                    else -> onDone(m, date)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ui.WHITE, contentColor = Ui.BG)
        ) { Text("PLAN MY FINANCIAL DESTRUCTION", fontWeight = FontWeight.ExtraBold) }
    }
    if (picking) DatePickDialog(date, { date = it; picking = false }, { picking = false })
}

@Composable
fun SettingsScreen(s: Snapshot, d: AppData, today: LocalDate, onChange: (AppData) -> Unit) {
    var bal by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    val parsed = BrokeEngine.parseAmount(bal)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SETTINGS", fontSize = 26.sp, fontWeight = FontWeight.Black)
        CardBox {
            Label("MONEY REMAINING (NOW ${BrokeEngine.fmt(s.remaining, d.currency)})")
            OutlinedTextField(value = bal, onValueChange = { bal = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), label = { Text("New balance") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = bal.isNotEmpty() && parsed == null)
            Spacer(Modifier.height(8.dp))
            Button(enabled = parsed != null, onClick = {
                if (parsed != null) {
                    onChange(d.copy(startBalance = BrokeEngine.round2(parsed + s.destroyed)))
                    bal = ""
                }
            }) { Text("UPDATE BALANCE") }
        }
        CardBox {
            Label("BROKE DATE")
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                Text(dateText(d.brokeDate, today), color = Ui.WHITE)
            }
            if (d.brokeDate.isBefore(today)) {
                Spacer(Modifier.height(6.dp))
                Text("That date has passed. NEGATIVE demands you spend everything immediately.", color = Ui.AMBER, fontSize = 12.sp)
            }
        }
        CardBox {
            Label("CURRENCY")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("₱", "$", "€", "£", "¥").forEach { c ->
                    FilterChip(selected = d.currency == c, onClick = { onChange(d.copy(currency = c)) }, label = { Text(c) })
                }
            }
        }
        CardBox {
            Label("HUMOR LEVEL")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("MILD", "SARCASTIC", "NO MERCY").forEachIndexed { i, t ->
                    FilterChip(selected = d.humor == i, onClick = { onChange(d.copy(humor = i)) }, label = { Text(t, fontSize = 12.sp) })
                }
            }
        }
        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text("RESET EVERYTHING", color = Ui.RED)
        }
    }
    if (picking) DatePickDialog(d.brokeDate, { onChange(d.copy(brokeDate = it)); picking = false }, { picking = false })
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false }, containerColor = Ui.CARD,
            title = { Text("Reset everything?") },
            text = { Text("All expenses and settings will be erased. The money stays gone.") },
            confirmButton = { TextButton(onClick = { confirmReset = false; onChange(AppData()) }) { Text("RESET", color = Ui.RED) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("CANCEL") } }
        )
    }
}
