package dev.m00nl1ght.tokdiff

fun parseTokens(input: String): List<String> {
    var begin = -1
    val list = ArrayList<String>()

    for (i in input.indices) {
        if (input[i] == '\'') {
            begin = if (begin < 0) i else {
                list.add(input.substring(begin + 1, i))
                -1
            }
        }
    }

    if (begin >= 0) throw RuntimeException("quote not closed from $begin")
    return list
}

fun applyBasicTokenizer(input: String): List<String> {
    return input.split(' ', '\n')
}
