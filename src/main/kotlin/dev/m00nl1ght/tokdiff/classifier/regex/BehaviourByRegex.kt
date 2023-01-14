package dev.m00nl1ght.tokdiff.classifier.regex

import dev.m00nl1ght.tokdiff.classifier.Behaviour

class BehaviourByRegex(name: String, score: Float, val pattern: Regex) : Behaviour(name, score)
