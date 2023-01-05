package dev.m00nl1ght.tokdiff.diff

internal data class Region(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top

    val size: Int
        get() = width + height

    val delta: Int
        get() = width - height
}
