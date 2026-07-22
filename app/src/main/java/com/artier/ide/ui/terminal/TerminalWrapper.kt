package com.artier.ide.ui.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * TerminalView — Canvas-based terminal (Termux TerminalView pattern).
 * Renders TerminalEmulator buffer; I/O via TerminalManager → daemon node-pty.
 */
class TerminalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var sessionId: String = ""
    var onInput: (String) -> Unit = {}
    var onResize: (p1: Int, p2: Int) -> Unit = { _, _ -> }

    private val emulator = TerminalEmulator(80, 24)
    private var scrollOffset = 0

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC")
        typeface = Typeface.MONOSPACE
        textSize = 14f * resources.displayMetrics.density
    }

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#1E1E1E")
    }

    private val cursorPaint = Paint().apply {
        color = Color.parseColor("#CCCCCC")
        alpha = 180
    }

    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            textPaint.textSize = (textPaint.textSize * detector.scaleFactor).coerceIn(10f, 36f)
            recalculateDimensions()
            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(this@TerminalView, InputMethodManager.SHOW_IMPLICIT)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            val lineHeight = textPaint.textSize * 1.3f
            val maxScroll = emulator.getScrollbackSize()
            scrollOffset = (scrollOffset + (distanceY / lineHeight).toInt()).coerceIn(0, maxScroll)
            invalidate()
            return true
        }
    })

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    private fun recalculateDimensions() {
        if (width <= 0 || height <= 0) return
        val charWidth = textPaint.measureText("W").coerceAtLeast(1f)
        val lineHeight = textPaint.textSize * 1.3f
        val newCols = (width / charWidth).toInt().coerceAtLeast(20)
        val newRows = (height / lineHeight).toInt().coerceAtLeast(5)
        if (newCols != emulator.cols || newRows != emulator.rows) {
            emulator.resize(newCols, newRows)
            onResize(newCols, newRows)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalculateDimensions()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val lineHeight = textPaint.textSize * 1.3f
        val charWidth = textPaint.measureText("W")
        val allLines = emulator.getAllLines()
        val start = (allLines.size - emulator.rows - scrollOffset).coerceAtLeast(0)
        val visible = allLines.drop(start).take(emulator.rows)

        for (i in visible.indices) {
            val y = (i + 1) * lineHeight
            canvas.drawText(visible[i], 0f, y, textPaint)
        }

        // Cursor relative to screen (bottom of buffer when scrollOffset=0)
        val screenStart = (allLines.size - emulator.rows).coerceAtLeast(0)
        val cursorLineInAll = screenStart + emulator.cursorY
        val cursorRowOnView = cursorLineInAll - start
        if (cursorRowOnView in 0 until emulator.rows) {
            val cx = emulator.cursorX * charWidth
            val cy = (cursorRowOnView + 1) * lineHeight
            canvas.drawRect(cx, cy - textPaint.textSize, cx + charWidth, cy, cursorPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onCheckIsTextEditor(): Boolean = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN
        return object : BaseInputConnection(this, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                if (!text.isNullOrEmpty()) {
                    onInput(text.toString())
                }
                return true
            }

            override fun sendKeyEvent(event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_ENTER -> onInput("\r")
                        KeyEvent.KEYCODE_DEL -> onInput("\u007f")
                        KeyEvent.KEYCODE_TAB -> onInput("\t")
                        KeyEvent.KEYCODE_DPAD_UP -> onInput("\u001b[A")
                        KeyEvent.KEYCODE_DPAD_DOWN -> onInput("\u001b[B")
                        KeyEvent.KEYCODE_DPAD_RIGHT -> onInput("\u001b[C")
                        KeyEvent.KEYCODE_DPAD_LEFT -> onInput("\u001b[D")
                        else -> {
                            val ch = event.unicodeChar
                            if (ch != 0) onInput(ch.toChar().toString())
                        }
                    }
                }
                return true
            }
        }
    }

    fun feedOutput(data: String) {
        emulator.write(data)
        scrollOffset = 0
        invalidate()
    }

    fun clear() {
        emulator.reset()
        scrollOffset = 0
        invalidate()
    }

    fun getCols(): Int = emulator.cols
    fun getRows(): Int = emulator.rows
}

@Composable
fun TerminalWrapper(
    sessionId: String,
    workingDirectory: String = "/",
    modifier: Modifier = Modifier,
    terminalManager: TerminalManager? = null,
    onOutput: (String) -> Unit = {},
    onInput: (String) -> Unit = {},
    onExit: (Int) -> Unit = {}
) {
    val viewRef = remember { arrayOfNulls<TerminalView>(1) }

    DisposableEffect(sessionId, terminalManager) {
        terminalManager?.createSession(
            sessionId = sessionId,
            workingDirectory = workingDirectory,
            cols = 80,
            rows = 24,
            onOutput = { data ->
                viewRef[0]?.post { viewRef[0]?.feedOutput(data) }
                onOutput(data)
            },
            onExit = onExit
        )
        onDispose {
            terminalManager?.closeSession(sessionId)
        }
    }

    AndroidView(
        factory = { context ->
            TerminalView(context).apply {
                this.sessionId = sessionId
                this.onInput = { text ->
                    terminalManager?.sendInput(sessionId, text)
                    onInput(text)
                }
                this.onResize = { cols, rows ->
                    terminalManager?.resize(sessionId, cols, rows)
                }
                viewRef[0] = this
            }
        },
        modifier = modifier,
        update = { view ->
            view.sessionId = sessionId
            viewRef[0] = view
        }
    )
}
