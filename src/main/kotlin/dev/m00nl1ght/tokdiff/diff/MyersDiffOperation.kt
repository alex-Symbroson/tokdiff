package dev.m00nl1ght.tokdiff.diff

internal sealed class MyersDiffOperation<out T> {

    data class Insert<T>(
        val value: T
    ) : MyersDiffOperation<T>()

    object Delete : MyersDiffOperation<Nothing>()

    object Skip : MyersDiffOperation<Nothing>()

}
