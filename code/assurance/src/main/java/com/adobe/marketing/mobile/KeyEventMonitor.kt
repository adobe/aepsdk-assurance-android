package com.adobe.marketing.mobile

import android.view.KeyEvent

enum class KeyType {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    companion object {
        fun fromKeyCode(code: Int): KeyType? {
            return when (code) {
                KeyEvent.KEYCODE_DPAD_UP -> UP
                KeyEvent.KEYCODE_DPAD_DOWN -> DOWN
                KeyEvent.KEYCODE_DPAD_LEFT -> LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT -> RIGHT
                else -> null
            }
        }
    }
}

class KeyEventMonitor internal constructor(private val keyCombination: Array<KeyType>) {
    private val cachedKeys = mutableListOf<KeyType>()
    private var isConnected = false

    fun onKeyEventDetected(event: KeyEvent) {
        if (event.action == KeyEvent.ACTION_DOWN) return
        KeyType.fromKeyCode(event.keyCode)?.let { keyType ->
            handleKeyType(keyType)
        } ?: cachedKeys.clear()
    }

    private fun handleKeyType(keyType: KeyType) {
        cachedKeys.apply {
            if (!isValidSequence() || size >= keyCombination.size) {
                clear()
            }
            add(keyType)

            if (size == keyCombination.size && isValidSequence()) {
                clear()
                keyCombinationDetected()
            }
        }
    }

    private fun isValidSequence(): Boolean =
        cachedKeys.zip(keyCombination)
            .take(cachedKeys.size)
            .all { (cached, expected) -> cached == expected }

    private fun keyCombinationDetected() {
        if (isConnected) {
            Assurance.endSession()
            isConnected = false
        } else {
            Assurance.startSession()
            isConnected = true
        }
    }

}