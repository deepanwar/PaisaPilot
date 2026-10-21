package com.deepak.paisapilot.domain.money

fun formatInr(paise: Paise): String {
    val rupees = paise.value / 100
    val fraction = (paise.value % 100).toInt()
    return "₹${groupRupees(rupees)}.${fraction.toString().padStart(2, '0')}"
}

private fun groupRupees(rupees: Long): String {
    val digits = rupees.toString()
    if (digits.length <= 3) return digits
    val head = digits.dropLast(3)
    val groupedHead = head.reversed().chunked(2).joinToString(",").reversed()
    return "$groupedHead,${digits.takeLast(3)}"
}
