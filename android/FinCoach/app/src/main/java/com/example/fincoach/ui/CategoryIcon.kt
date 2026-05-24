package com.example.fincoach.ui

// Эмодзи по названию категории
fun categoryEmoji(categoryTitle: String, isIncome: Boolean): String {
    val key = categoryTitle.lowercase()
    return when {
        "жил"        in key || "аренд" in key            -> "🏠"   // 🏠
        "красот"     in key || "космет" in key           -> "💄"   // 💄
        "здоров"     in key || "аптек" in key            -> "💊"   // 💊
        "такси"      in key                              -> "🚕"   // 🚕
        "транспорт"  in key                              -> "🚌"   // 🚌
        "связ"       in key || "интернет" in key         -> "📱"   // 📱
        "развлеч"    in key                              -> "🎮"   // 🎮
        "продукт"    in key || "магазин" in key          -> "🛒"   // 🛒
        "кафе"       in key || "ресторан" in key         -> "🍕"   // 🍕
        "еда"        in key || "фастфуд" in key          -> "🍔"   // 🍔
        "одежд"      in key || "обувь" in key            -> "👕"   // 👕
        "образован"  in key || "курс" in key             -> "🎓"   // 🎓
        "спорт"      in key || "фитнес" in key           -> "⚽"         // ⚽
        "путешеств"  in key || "отпуск" in key           -> "✈️"   // ✈️
        "животн"     in key || "питомец" in key          -> "🐾"   // 🐾
        "зарплат"    in key                              -> "💵"   // 💵
        "фриланс"    in key                              -> "💼"   // 💼
        "подработ"   in key                              -> "💰"   // 💰
        "преми"      in key                              -> "🎉"   // 🎉
        "инвест"     in key || "акци" in key             -> "📈"   // 📈
        "долг"       in key || "займ" in key             -> "🔁"   // 🔁
        "подар"      in key                              -> "🎁"   // 🎁
        else -> if (isIncome) "💰" else "💸"             // 💰 / 💸
    }
}
