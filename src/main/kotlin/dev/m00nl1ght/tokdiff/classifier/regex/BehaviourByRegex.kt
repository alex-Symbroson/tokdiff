package dev.m00nl1ght.tokdiff.classifier.regex

import dev.m00nl1ght.tokdiff.classifier.Behaviour
import java.util.regex.Pattern

class BehaviourByRegex(
    name: String,
    score: Float,
    basePattern: String? = null,
    repeatingPattern: String? = null,
    endPattern: String? = null,
    val initiator: Boolean = true,
    val tokenLimit: (segmentCount: Int) -> Int = { _ -> -1 }
) : Behaviour(name, score) {
    val basePattern: Pattern? = if (basePattern == null) null else Pattern.compile(basePattern)
    val repeatingPattern: Pattern? = if (repeatingPattern == null) null else Pattern.compile(repeatingPattern)
    val endPattern: Pattern? = if (endPattern == null) null else Pattern.compile(endPattern)
}
