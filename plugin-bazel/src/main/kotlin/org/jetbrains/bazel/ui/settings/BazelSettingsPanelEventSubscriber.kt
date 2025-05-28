package org.jetbrains.bazel.ui.settings

object BazelSettingsPanelEventSubscriber {
  class Ticket internal constructor()

  enum class BazelSettingsPanelEventType {
    RESET_TOOL_WINDOW_BUTTON_PRESSED
  }

  private val actionsLists = Array<MutableList<Pair<Ticket, () -> Any>>>(
    BazelSettingsPanelEventType.entries.size) { mutableListOf() }

  fun subscribe(eventType: BazelSettingsPanelEventType, action: () -> Any): Ticket {
    val newTicket = Ticket()
    actionsLists[eventType.ordinal].add(Pair(newTicket, action))
    return newTicket
  }

  fun unsubscribe(eventType: BazelSettingsPanelEventType, ticket: Ticket): Boolean {
    val eventListIterator = actionsLists[eventType.ordinal].iterator()
    while (eventListIterator.hasNext()) {
      val ticketActionPair = eventListIterator.next()
      if (ticketActionPair.first === ticket) {
        eventListIterator.remove()
        return true
      }
    }
    return false
  }

  internal fun runActions(eventType: BazelSettingsPanelEventType) {
    actionsLists[eventType.ordinal].forEach { it.second() }
  }
}
