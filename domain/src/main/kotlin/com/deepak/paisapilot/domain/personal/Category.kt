package com.deepak.paisapilot.domain.personal

enum class Category(val key: String) {
    Food("food"),
    Transport("transport"),
    Shopping("shopping"),
    Bills("bills"),
    Home("home"),
    Health("health"),
    Entertainment("entertainment"),
    Other("other"),
    ;

    companion object {
        fun fromKey(key: String): Category = entries.firstOrNull { it.key == key } ?: Other
    }
}
