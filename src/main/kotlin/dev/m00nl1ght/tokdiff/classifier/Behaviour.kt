package dev.m00nl1ght.tokdiff.classifier

open class Behaviour(val name: String, val score: Float) {

    var occurencesByInput: MutableMap<String, Int> = HashMap()

}
