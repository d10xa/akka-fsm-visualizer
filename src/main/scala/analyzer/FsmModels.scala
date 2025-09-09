package analyzer

/**
 * Data models for FSM analysis
 */
case class Link(
  from: String, 
  to: String, 
  arrow: Option[String], 
  eventLabel: Option[String] = None, 
  isTimeout: Boolean = false
)