package com.spellbee.ai

object PhonemeAI {

    fun generateSuggestions(word: String, language: String = "English"): List<String> {
        val suggestions = mutableListOf<String>()
        val lowerWord = word.lowercase()

        when (language.lowercase()) {
            "spanish" -> {
                if (lowerWord.contains("b")) suggestions.add(lowerWord.replace("b", "v"))
                if (lowerWord.contains("v")) suggestions.add(lowerWord.replace("v", "b"))
                if (lowerWord.startsWith("h")) suggestions.add(lowerWord.replaceFirst("h", ""))
                else suggestions.add("h$lowerWord")
                if (lowerWord.contains("c")) suggestions.add(lowerWord.replace("c", "s"))
                if (lowerWord.contains("z")) suggestions.add(lowerWord.replace("z", "s"))
            }
            "french" -> {
                if (lowerWord.endsWith("er")) suggestions.add(lowerWord.replace(Regex("er$"), "ez"))
                if (lowerWord.contains("ph")) suggestions.add(lowerWord.replace("ph", "f"))
                if (lowerWord.contains("c")) suggestions.add(lowerWord.replace("c", "ss"))
            }
            else -> {
                // English Phoneme Rules
                if (lowerWord.contains("ph")) suggestions.add(lowerWord.replace("ph", "f"))
                if (lowerWord.contains("f")) suggestions.add(lowerWord.replace("f", "ph"))
                if (lowerWord.startsWith("k")) suggestions.add(lowerWord.replaceFirst("k", "c"))
                if (lowerWord.startsWith("c")) suggestions.add(lowerWord.replaceFirst("c", "k"))
                if (lowerWord.endsWith("ck")) suggestions.add(lowerWord.replace("ck", "k"))
                if (lowerWord.contains("tion")) suggestions.add(lowerWord.replace("tion", "sion"))
                if (lowerWord.contains("sion")) suggestions.add(lowerWord.replace("sion", "tion"))
                if (lowerWord.contains("ie")) suggestions.add(lowerWord.replace("ie", "ei"))
                if (lowerWord.contains("ei")) suggestions.add(lowerWord.replace("ei", "ie"))
                if (lowerWord.contains("ee")) suggestions.add(lowerWord.replace("ee", "ea"))
                if (lowerWord.contains("ea")) suggestions.add(lowerWord.replace("ea", "ee"))
            }
        }

        return suggestions.distinct().take(5)
    }

    fun detectCommands(text: String): String? {
        val lower = text.lowercase()
        return when {
            lower.contains("repeat") -> "Repeat requested"
            lower.contains("use in sentence") || lower.contains("sentence") -> "Sentence requested"
            lower.contains("definition") || lower.contains("define") -> "Definition requested"
            lower.contains("origin") || lower.contains("etymology") -> "Origin requested"
            else -> null
        }
    }
}
