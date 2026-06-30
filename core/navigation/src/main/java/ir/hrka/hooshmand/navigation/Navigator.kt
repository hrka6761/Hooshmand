package ir.hrka.hooshmand.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 *
 * @param state - The navigation state that will be updated in response to navigation events.
 */
class Navigator(val state: NavigationState) {

    /**
     * Navigate to a navigation destination
     *
     * @param destination - the navigation destination to navigate to.
     */
    fun navigate(destination: NavKey) {
        when (destination) {
            state.currentTopLevelDestination -> clearSubStack()
            in state.topLevelKeys -> goToTopLevelDestination(destination)
            else -> goToSubDestination(destination)
        }
    }

    /**
     * Go back to the previous navigation destination.
     */
    fun goBack() {
        when (state.currentDestination) {
            state.startDestination -> error("You cannot go back from the start route")
            state.currentTopLevelDestination -> {
                // We're at the base of the current sub stack, go back to the previous top level
                // stack.
                state.topLevelStack.removeLastOrNull()
            }
            else -> state.currentSubStack.removeLastOrNull()
        }
    }

    /**
     * Go to a non top level destination.
     */
    private fun goToSubDestination(destination: NavKey) {
        state.currentSubStack.apply {
            // Remove it if it's already in the stack so it's added at the end.
            remove(destination)
            add(destination)
        }
    }

    /**
     * Go to a top level destination.
     */
    private fun goToTopLevelDestination(destination: NavKey) {
        state.topLevelStack.apply {
            if (destination == state.startDestination) {
                // This is the start destination. Clear the stack so it's added as the only key.
                clear()
            } else {
                // Remove it if it's already in the stack so it's added at the end.
                remove(destination)
            }
            add(destination)
        }
    }

    /**
     * Clearing all but the root destination in the current sub stack.
     */
    private fun clearSubStack() {
        state.currentSubStack.run {
            if (size > 1) subList(1, size).clear()
        }
    }
}
