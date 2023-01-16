package dev.m00nl1ght.tokdiff

fun parseTokens(input: String): List<String> {
    val list = ArrayList<String>()

    var cBegin = -1
    var cSep = '\''

    for (i in input.indices) {
        if (cBegin < 0) {
            if (input[i] == '\'') {
                cBegin = i
                cSep = '\''
            } else if (input[i] == '"') {
                cBegin = i
                cSep = '"'
            }
        } else if (input[i] == cSep) {
            list.add(input.substring(cBegin + 1, i))
            cBegin = -1
        }
    }

    if (cBegin >= 0) throw RuntimeException("quote not closed from $cBegin")
    return list
}

fun applyBasicTokenizer(input: String): List<String> {
    return input.split(' ', '\n')
}
