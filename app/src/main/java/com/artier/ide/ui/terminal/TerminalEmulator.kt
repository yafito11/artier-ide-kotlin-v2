package com.artier.ide.ui.terminal

/**
 * TerminalEmulator — Termux-style escape sequence interpreter + scroll buffer.
 * Inspired by Termux TerminalEmulator (open-source): screen buffer, cursor, ANSI CSI.
 */
class TerminalEmulator(
    initialCols: Int = 80,
    initialRows: Int = 24,
    private val scrollback: Int = 5000
) {
    var cols: Int = initialCols
        private set
    var rows: Int = initialRows
        private set

    private var screen = Array(initialRows) { CharArray(initialCols) { ' ' } }
    private val scrollBuffer = ArrayDeque<String>(scrollback)

    var cursorX = 0
        private set
    var cursorY = 0
        private set

    private var savedCursorX = 0
    private var savedCursorY = 0

    private var escapeState = EscapeState.NORMAL
    private val csiParams = StringBuilder()
    private var csiPrivate = false

    private var insertMode = false
    private var autoWrap = true
    private var scrollTop = 0
    private var scrollBottom = initialRows - 1

    private enum class EscapeState {
        NORMAL, ESCAPE, CSI, OSC
    }

    fun write(data: String) {
        var i = 0
        while (i < data.length) {
            val c = data[i]
            when (escapeState) {
                EscapeState.NORMAL -> {
                    when {
                        c == '\u001b' -> escapeState = EscapeState.ESCAPE
                        c == '\n' -> lineFeed()
                        c == '\r' -> cursorX = 0
                        c == '\b' -> if (cursorX > 0) cursorX--
                        c == '\t' -> tab()
                        c.code < 32 -> { }
                        else -> putChar(c)
                    }
                }
                EscapeState.ESCAPE -> {
                    when (c) {
                        '[' -> {
                            escapeState = EscapeState.CSI
                            csiParams.clear()
                            csiPrivate = false
                        }
                        ']' -> escapeState = EscapeState.OSC
                        '7' -> { savedCursorX = cursorX; savedCursorY = cursorY; escapeState = EscapeState.NORMAL }
                        '8' -> { cursorX = savedCursorX; cursorY = savedCursorY; escapeState = EscapeState.NORMAL }
                        'c' -> { reset(); escapeState = EscapeState.NORMAL }
                        'D' -> { lineFeed(); escapeState = EscapeState.NORMAL }
                        'M' -> { reverseLineFeed(); escapeState = EscapeState.NORMAL }
                        'E' -> { cursorX = 0; lineFeed(); escapeState = EscapeState.NORMAL }
                        else -> escapeState = EscapeState.NORMAL
                    }
                }
                EscapeState.CSI -> {
                    when {
                        c == '?' && csiParams.isEmpty() -> csiPrivate = true
                        c in '0'..'9' || c == ';' -> csiParams.append(c)
                        else -> {
                            handleCsi(c, parseParams(csiParams.toString()), csiPrivate)
                            escapeState = EscapeState.NORMAL
                        }
                    }
                }
                EscapeState.OSC -> {
                    if (c == '\u0007') {
                        escapeState = EscapeState.NORMAL
                    }
                }
            }
            i++
        }
    }

    private fun putChar(c: Char) {
        if (cursorX >= cols) {
            if (autoWrap) {
                cursorX = 0
                lineFeed()
            } else {
                cursorX = cols - 1
            }
        }
        if (insertMode) {
            for (x in cols - 1 downTo cursorX + 1) {
                screen[cursorY][x] = screen[cursorY][x - 1]
            }
        }
        screen[cursorY][cursorX] = c
        cursorX++
    }

    private fun lineFeed() {
        if (cursorY < scrollBottom) {
            cursorY++
        } else {
            val line = String(screen[scrollTop])
            if (scrollBuffer.size >= scrollback) scrollBuffer.removeFirst()
            scrollBuffer.addLast(line)
            for (y in scrollTop until scrollBottom) {
                System.arraycopy(screen[y + 1], 0, screen[y], 0, cols)
            }
            screen[scrollBottom].fill(' ')
        }
    }

    private fun reverseLineFeed() {
        if (cursorY > scrollTop) {
            cursorY--
        } else {
            for (y in scrollBottom downTo scrollTop + 1) {
                System.arraycopy(screen[y - 1], 0, screen[y], 0, cols)
            }
            screen[scrollTop].fill(' ')
        }
    }

    private fun tab() {
        val next = ((cursorX / 8) + 1) * 8
        cursorX = next.coerceAtMost(cols - 1)
    }

    private fun parseParams(s: String): IntArray {
        if (s.isEmpty()) return intArrayOf()
        return s.split(';').map { it.toIntOrNull() ?: 0 }.toIntArray()
    }

    private fun param(params: IntArray, index: Int, default: Int = 1): Int {
        return if (index < params.size && params[index] > 0) params[index] else default
    }

    private fun handleCsi(cmd: Char, params: IntArray, private: Boolean) {
        when (cmd) {
            'A' -> cursorY = (cursorY - param(params, 0)).coerceAtLeast(0)
            'B' -> cursorY = (cursorY + param(params, 0)).coerceAtMost(rows - 1)
            'C' -> cursorX = (cursorX + param(params, 0)).coerceAtMost(cols - 1)
            'D' -> cursorX = (cursorX - param(params, 0)).coerceAtLeast(0)
            'E' -> { cursorY = (cursorY + param(params, 0)).coerceAtMost(rows - 1); cursorX = 0 }
            'F' -> { cursorY = (cursorY - param(params, 0)).coerceAtLeast(0); cursorX = 0 }
            'G' -> cursorX = (param(params, 0) - 1).coerceIn(0, cols - 1)
            'H', 'f' -> {
                val row = param(params, 0) - 1
                val col = if (params.size > 1) params[1] - 1 else 0
                cursorY = row.coerceIn(0, rows - 1)
                cursorX = col.coerceIn(0, cols - 1)
            }
            'J' -> {
                when (if (params.isEmpty()) 0 else params[0]) {
                    0 -> {
                        for (x in cursorX until cols) screen[cursorY][x] = ' '
                        for (y in cursorY + 1 until rows) screen[y].fill(' ')
                    }
                    1 -> {
                        for (y in 0 until cursorY) screen[y].fill(' ')
                        for (x in 0..cursorX) screen[cursorY][x] = ' '
                    }
                    2, 3 -> clearScreen()
                }
            }
            'K' -> {
                when (if (params.isEmpty()) 0 else params[0]) {
                    0 -> for (x in cursorX until cols) screen[cursorY][x] = ' '
                    1 -> for (x in 0..cursorX) screen[cursorY][x] = ' '
                    2 -> screen[cursorY].fill(' ')
                }
            }
            'P' -> {
                val n = param(params, 0)
                for (i in 0 until n) {
                    for (x in cursorX until cols - 1) {
                        screen[cursorY][x] = screen[cursorY][x + 1]
                    }
                    screen[cursorY][cols - 1] = ' '
                }
            }
            '@' -> {
                val n = param(params, 0)
                for (i in 0 until n) {
                    for (x in cols - 1 downTo cursorX + 1) {
                        screen[cursorY][x] = screen[cursorY][x - 1]
                    }
                    screen[cursorY][cursorX] = ' '
                }
            }
            'r' -> {
                if (!private) {
                    scrollTop = if (params.isNotEmpty()) (params[0] - 1).coerceIn(0, rows - 1) else 0
                    scrollBottom = if (params.size > 1) (params[1] - 1).coerceIn(0, rows - 1) else rows - 1
                    if (scrollTop > scrollBottom) {
                        val t = scrollTop
                        scrollTop = scrollBottom
                        scrollBottom = t
                    }
                }
            }
            'h' -> if (private) setPrivateMode(params, true)
            'l' -> if (private) setPrivateMode(params, false)
            's' -> { savedCursorX = cursorX; savedCursorY = cursorY }
            'u' -> { cursorX = savedCursorX; cursorY = savedCursorY }
            else -> { }
        }
    }

    private fun setPrivateMode(params: IntArray, enable: Boolean) {
        for (p in params) {
            when (p) {
                7 -> autoWrap = enable
                4 -> insertMode = enable
            }
        }
    }

    fun clearScreen() {
        for (y in 0 until rows) screen[y].fill(' ')
        cursorX = 0
        cursorY = 0
    }

    fun reset() {
        clearScreen()
        scrollBuffer.clear()
        insertMode = false
        autoWrap = true
        scrollTop = 0
        scrollBottom = rows - 1
        escapeState = EscapeState.NORMAL
    }

    fun resize(newCols: Int, newRows: Int) {
        val newScreen = Array(newRows) { CharArray(newCols) { ' ' } }
        val copyRows = minOf(rows, newRows)
        val copyCols = minOf(cols, newCols)
        for (y in 0 until copyRows) {
            System.arraycopy(screen[y], 0, newScreen[y], 0, copyCols)
        }
        screen = newScreen
        cols = newCols
        rows = newRows
        scrollTop = 0
        scrollBottom = rows - 1
        cursorX = cursorX.coerceIn(0, cols - 1)
        cursorY = cursorY.coerceIn(0, rows - 1)
    }

    fun getVisibleLines(): List<String> {
        return (0 until rows).map { y -> String(screen[y]).trimEnd() }
    }

    fun getAllLines(): List<String> {
        return scrollBuffer.toList() + getVisibleLines()
    }

    fun getScrollbackSize(): Int = scrollBuffer.size
}
