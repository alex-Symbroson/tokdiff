package dev.m00nl1ght.tokdiff.classifier.regex

import dev.m00nl1ght.tokdiff.classifier.Category

class CategoryByRegex(name: String, val behaviours: List<BehaviourByRegex>) : Category(name)
