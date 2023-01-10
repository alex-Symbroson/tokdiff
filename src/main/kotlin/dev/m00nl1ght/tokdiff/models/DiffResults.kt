package dev.m00nl1ght.tokdiff.models

class DiffResults(
    val name: String,
    val inputs: List<TokenChain>,
    val diffs: List<DiffChunk>
)
