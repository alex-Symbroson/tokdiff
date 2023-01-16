package dev.m00nl1ght.tokdiff.classifier.regex

import dev.m00nl1ght.tokdiff.classifier.Behaviour
import java.util.regex.Pattern

class BehaviourByRegex(
    name: String,
    score: Float,
    basePattern: String? = null,
    repeatingPattern: String? = null,
    val initiator: Boolean = true
) : Behaviour(name, score) {
    val basePattern: Pattern? = if (basePattern == null) null else Pattern.compile(basePattern)
    val baseTokenCount = 1 + (basePattern?.count { c -> c == '~' } ?: -1)
    val repeatingPattern: Pattern? = if (repeatingPattern == null) null else Pattern.compile(repeatingPattern)
    val repeatingTokenCount = repeatingPattern?.count { c -> c == '~' } ?: 0
}
