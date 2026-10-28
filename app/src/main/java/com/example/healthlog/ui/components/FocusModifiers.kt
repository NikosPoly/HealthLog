package com.example.healthlog.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

// Leaves the focused text field when this element is tapped: calls onTap, then clears the focus.
// "composed" reads the focus manager where the modifier is used, i.e. inside the dialog's
// own window; otherwise it would clear the focus of the screen behind the dialog.
internal fun Modifier.clearFocusOnTap(onTap: () -> Unit): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val currentOnTap by rememberUpdatedState(onTap)
    pointerInput(focusManager) {
        detectTapGestures {
            currentOnTap()
            focusManager.clearFocus()
        }
    }
}

// Leaves the focused text field when this element is pressed, even if a child
// (like a disabled button) would swallow the tap. Looks at the touch before the children do.
internal fun Modifier.clearFocusOnPress(onPress: () -> Unit): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val currentOnPress by rememberUpdatedState(onPress)
    pointerInput(focusManager) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            currentOnPress()
            focusManager.clearFocus()
        }
    }
}
