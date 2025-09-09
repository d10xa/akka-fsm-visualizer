package analyzer

/**
 * Generates Mermaid diagram code from FSM links
 * Single Responsibility: Mermaid code generation
 */
object MermaidGenerator {
  
  def linksToMermaid(links: List[Link]): String = {
    if (links.isEmpty) {
      return """stateDiagram-v2
        |    [*] --> NoTransitions
        |    NoTransitions --> [*]
        |    note right of NoTransitions : No FSM transitions found
        |    note right of NoTransitions : Only state definitions detected""".stripMargin
    }

    val sb = new StringBuilder()
    sb.append("stateDiagram-v2\n")
    
    // Get all unique states
    val allStates = links.flatMap(link => List(link.from, link.to)).distinct
    
    // Add initial state - prefer startWith detection, fallback to Idle state
    val initialStateFromStartWith = findInitialStateFromLinks(links)
    val idleState = allStates.find(_.endsWith(".Idle"))
    val initialState = initialStateFromStartWith.orElse(idleState)
    
    initialState.foreach { initial =>
      val cleanInitial = cleanStateName(initial)
      sb.append(s"    [*] --> $cleanInitial\n")
    }
    
    // Add all transitions
    links.distinct.foreach { link =>
      val fromState = cleanStateName(link.from)
      val toState = cleanStateName(link.to)
      
      // Build transition label
      val label = buildTransitionLabel(link)
      
      link.arrow match {
        case Some("recovery") =>
          sb.append(s"    $fromState --> $toState : ${label.getOrElse("recovery")}\n")
        case Some("unparsed") =>
          sb.append(s"    $fromState --> $toState : [unparsed]\n")
        case Some("timeout") =>
          sb.append(s"    $fromState --> $toState : ${label.getOrElse("timeout")}\n")
        case _ =>
          label match {
            case Some(lbl) => sb.append(s"    $fromState --> $toState : $lbl\n")
            case None => sb.append(s"    $fromState --> $toState\n")
          }
      }
    }
    
    // Add final states 
    val stopStates = allStates.filter(_ == "stop")
    stopStates.foreach { stop =>
      val cleanStop = cleanStateName(stop)
      sb.append(s"    $cleanStop --> [*]\n")
    }
    
    sb.append("\n")
    
    // Add state styling
    allStates.foreach { state =>
      val cleanState = cleanStateName(state)
      
      if (state.contains("Recover")) {
        sb.append(s"    class $cleanState recovery\n")
      } else if (state.contains("Failed")) {
        sb.append(s"    class $cleanState failed\n")
      } else if (state == "stop") {
        sb.append(s"    class $cleanState stopState\n")
      } else if (state.contains("UnparsedTransition")) {
        sb.append(s"    class $cleanState unparsed\n")
      }
    }
    
    // Add CSS definitions
    sb.append("""
        |    classDef recovery fill:#17a2b8,stroke:#117a8b,stroke-width:2px,color:#fff
        |    classDef failed fill:#dc3545,stroke:#bd2130,stroke-width:2px,color:#fff  
        |    classDef stopState fill:#6c757d,stroke:#545b62,stroke-width:2px,color:#fff
        |    classDef unparsed fill:#ffa500,stroke:#ff6b00,stroke-width:2px,stroke-dasharray:5""".stripMargin)
    
    sb.toString()
  }
  
  private def findInitialStateFromLinks(links: List[Link]): Option[String] = {
    // Look for explicit initial state markers in event labels
    links.find(_.eventLabel.exists(_.contains("initial"))).map(_.to)
      .orElse(links.find(_.from.contains("startWith")).map(_.to))
  }
  
  private def buildTransitionLabel(link: Link): Option[String] = {
    val parts = List(
      link.eventLabel,
      if (link.isTimeout) Some("timeout") else None
    ).flatten
    
    if (parts.nonEmpty) Some(parts.mkString(" / ")) else None
  }
  
  private def cleanStateName(state: String): String = {
    // Remove any state object prefix for cleaner display (e.g., "State." or "OrderStates.")
    val cleaned = if (state.contains(".")) {
      val parts = state.split("\\.")
      if (parts.length >= 2) {
        parts.drop(1).mkString("_")
      } else {
        state.replace(".", "_")
      }
    } else {
      state
    }
    
    // Replace any remaining problematic characters for Mermaid
    cleaned.replace(" ", "_")
      .replace("-", "_")
      .replace("@", "_")
      .replace("#", "_")
      .replace("(", "_")
      .replace(")", "_")
  }
}