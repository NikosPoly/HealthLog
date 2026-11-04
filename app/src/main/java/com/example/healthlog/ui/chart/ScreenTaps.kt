package com.example.healthlog.ui.chart

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

// Taps anywhere on the screen, used to close the box of a tapped dot on the chart when
// the tap is outside the chart. The history screen watches the taps (watchTaps) and
// hands the latest one to the chart pages (ScreenTap).

// A tap anywhere on the screen, at a position relative to the whole screen.
// A new object for every tap, even at the same spot, so each one is noticed.
class ScreenTap(val position: Offset)

// Reports every tap on this element or anything inside it, with its position relative to
// the whole screen. It only watches: the buttons, tabs and chart underneath still get
// their taps as usual. A swipe is not a tap.
// composed: so the element's position is remembered across recompositions, like
// clearFocusOnTap in ui/input.
internal fun Modifier.watchTaps(onTap: (Offset) -> Unit): Modifier = composed {
    val position = remember { ElementPosition() }
    val currentOnTap by rememberUpdatedState(onTap)

    onGloballyPositioned { position.coordinates = it }
        .pointerInput(Unit) {
            awaitEachGesture {
                // Initial pass: sees the touch before the elements underneath, without
                // taking it from them.
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                val moved = up != null &&
                    (up.position - down.position).getDistance() > viewConfiguration.touchSlop
                val element = position.coordinates

                if (up != null && !moved && element != null && element.isAttached) {
                    currentOnTap(element.localToRoot(up.position))
                }
            }
        }
}

// Where an element is on the screen: used by watchTaps to give taps a position on the
// whole screen, and by the chart to tell whether a tap was on it (contains).
// A plain field, not state: it changes on every layout and nothing is redrawn from it.
internal class ElementPosition {
    var coordinates: LayoutCoordinates? = null

    // Whether a position on the screen is inside the element.
    fun contains(position: Offset): Boolean {
        val element = coordinates?.takeIf { it.isAttached } ?: return false
        return element.boundsInRoot().contains(position)
    }
}
