import scala.meta._
import scala.scalajs.js.annotation.JSExportTopLevel

case class Link(from: String, to: String, arrow: Option[String])

@JSExportTopLevel("AkkaFsmAnalyzer")
object AkkaFsmAnalyzer {

  def parseScalaCode(code: String): Either[String, String] = {
    try {
      val input = Input.VirtualFile("fsm.scala", code)
      val tree = input.parse[meta.Source].get
      val links = eval(tree)
      Right(linksToMermaid(links))
    } catch {
      case ex: StackOverflowError =>
        Left(s"Stack overflow error: The FSM is too complex or has circular dependencies")
      case ex: OutOfMemoryError =>
        Left(s"Out of memory error: The FSM is too large to process")
      case ex: Exception => 
        Left(s"Parse error: ${ex.getMessage}")
    }
  }

  private def eval(t: Tree): List[Link] = {
    // Collect all function definitions first
    val functions = collectFunctionDefinitions(t)
    // Find all state objects (objects containing case objects extending some state trait)
    val stateObjects = collectStateObjects(t)
    
    evalWithFunctions(t, functions, stateObjects)
  }
  
  
  private def collectFunctionDefinitions(t: Tree): Map[String, Tree] = {
    val functions = scala.collection.mutable.Map[String, Tree]()
    val stack = scala.collection.mutable.Stack[Tree]()
    val visited = scala.collection.mutable.Set[Tree]()
    val maxIterations = 10000
    var iterations = 0
    
    stack.push(t)
    
    while (stack.nonEmpty && iterations < maxIterations) {
      iterations += 1
      val current = stack.pop()
      
      if (!visited.contains(current)) {
        visited += current
      
        current match {
          case Defn.Def.Initial(_, Term.Name(name), _, _, _, body) =>
            functions += (name -> body)
            // Still process children for nested functions
            current.children.foreach { child =>
              if (!visited.contains(child)) {
                stack.push(child)
              }
            }
            
          case tree: Tree =>
            tree.children.foreach { child =>
              if (!visited.contains(child)) {
                stack.push(child)
              }
            }
        }
      }
    }
    
    // Don't throw exception, just return what we found
    if (iterations >= maxIterations) {
      // Log warning but continue
      println(s"Warning: Maximum iterations exceeded in collectFunctionDefinitions. Partial results returned.")
    }
    
    functions.toMap
  }
  
  private def collectStateObjects(t: Tree): Set[String] = {
    val stateObjects = scala.collection.mutable.Set[String]()
    val stack = scala.collection.mutable.Stack[Tree]()
    val visited = scala.collection.mutable.Set[Tree]()
    val maxIterations = 5000
    var iterations = 0
    
    stack.push(t)
    
    while (stack.nonEmpty && iterations < maxIterations) {
      iterations += 1
      val current = stack.pop()
      
      if (!visited.contains(current)) {
        visited += current
      
        current match {
          case Defn.Object.Initial(_, Term.Name(name), _) if containsStateDefinitions(current) =>
            stateObjects += name
            current.children.foreach { child =>
              if (!visited.contains(child)) {
                stack.push(child)
              }
            }
            
          case tree: Tree =>
            tree.children.foreach { child =>
              if (!visited.contains(child)) {
                stack.push(child)
              }
            }
        }
      }
    }
    
    if (iterations >= maxIterations) {
      println(s"Warning: Maximum iterations exceeded in collectStateObjects. Partial results returned.")
    }
    
    stateObjects.toSet
  }
  
  private def isRecoveryFunction(functionName: String, body: Tree): Boolean = {
    // Check if function is a recovery function based on name patterns or content
    val nameBasedRecovery = 
      functionName.toLowerCase.contains("recover") ||
      functionName.toLowerCase.contains("decision") ||
      functionName == "recoverStateDecision" // Support legacy naming for backward compatibility
    
    // Check if body contains recovery-related patterns like Target.enter calls
    val bodyStr = body.toString()
    val contentBasedRecovery = bodyStr.contains("Target.enter")
    
    nameBasedRecovery || contentBasedRecovery
  }
  
  private def containsStateDefinitions(tree: Tree): Boolean = {
    // Check if the tree contains case object definitions that extend traits
    // This should look for typical FSM state patterns rather than hardcoded names
    val treeStr = tree.toString()
    
    // Must have case objects and extends
    if (!(treeStr.contains("case object") && treeStr.contains("extends"))) {
      return false
    }
    
    // Should not contain typical event/message patterns
    val hasEventOrMessagePattern = 
      treeStr.contains("Event") ||
      treeStr.contains("Message") ||
      treeStr.contains("Command")
    
    // If it contains event/message patterns, it's likely not a state container
    if (hasEventOrMessagePattern) {
      return false
    }
    
    // Look for typical state-related patterns in the trait names
    val hasStatePattern = 
      treeStr.contains("State") ||
      treeStr.matches(".*extends\\s+\\w*[Ss]tate\\w*.*") ||
      treeStr.matches(".*extends\\s+\\w*Status.*") ||
      treeStr.matches(".*extends\\s+\\w*Phase.*") ||
      treeStr.matches(".*extends\\s+\\w*Step.*")
    
    hasStatePattern
  }
  
  private def evalWithFunctions(t: Tree, functions: Map[String, Tree], stateObjects: Set[String]): List[Link] = {
    val links = scala.collection.mutable.ListBuffer[Link]()
    val stack = scala.collection.mutable.Stack[Tree]()
    val visited = scala.collection.mutable.Set[Tree]()
    val maxIterations = 50000 // Safety limit
    var iterations = 0
    var foundWhenBlocks = false
    
    stack.push(t)
    
    while (stack.nonEmpty && iterations < maxIterations) {
      iterations += 1
      val current = stack.pop()
      
      // Skip if we've already processed this exact tree node
      if (!visited.contains(current)) {
        visited += current
      
        current match {
          case Term.Apply.Initial(q"when(..$exprs)", terms) =>
            foundWhenBlocks = true
            try {
              val currentState = exprs.head.toString()
              
              // Show all transitions including from functions
              val states = terms.flatMap(term => evalWhen(term, functions, stateObjects, currentState))
              if (states.nonEmpty) {
                states.foreach(to => links += Link(currentState, to, None))
              } else {
                // No transitions found, mark as unparsed
                links += Link(currentState, "UnparsedTransition", Some("unparsed"))
              }
            } catch {
              case _: Exception =>
                // Failed to parse this when block, add as unparsed
                links += Link(exprs.head.toString(), "UnparsedTransition", Some("unparsed"))
            }
            
          case Defn.Def.Initial(_, Term.Name(functionName), _, _, _, body) if isRecoveryFunction(functionName, body) =>
            val recoveryState = s"${stateObjects.headOption.getOrElse("State")}.RecoverSelf"
            val states = evalWhen(body, functions, stateObjects, recoveryState)
            states.foreach(to => 
              links += Link(recoveryState, to, Some("recovery"))
            )
            
          case tree: Tree =>
            // Add children to stack for iterative processing, but avoid cycles
            tree.children.foreach { child =>
              if (!visited.contains(child)) {
                stack.push(child)
              }
            }
        }
      }
    }
    
    // If no when blocks were found but we have recovery functions, process them
    if (!foundWhenBlocks) {
      functions.foreach { case (functionName, body) =>
        if (isRecoveryFunction(functionName, body)) {
          val recoveryState = s"${stateObjects.headOption.getOrElse("State")}.RecoverSelf"
          val states = evalWhen(body, functions, stateObjects, recoveryState)
          states.foreach(to => 
            links += Link(recoveryState, to, Some("recovery"))
          )
        }
      }
    }
    
    if (iterations >= maxIterations) {
      println(s"Warning: Maximum iterations exceeded in evalWithFunctions. Partial results returned.")
    }
    
    links.toList
  }

  private def evalWhen(t: Tree, functions: Map[String, Tree], stateObjects: Set[String], currentState: String = ""): List[String] = {
    val results = scala.collection.mutable.ListBuffer[String]()
    val stack = scala.collection.mutable.Stack[Tree]()
    val visited = scala.collection.mutable.Set[Tree]()
    val maxIterations = 10000 // Smaller limit for evalWhen
    var iterations = 0
    
    stack.push(t)
    
    while (stack.nonEmpty && iterations < maxIterations) {
      iterations += 1
      val current = stack.pop()
      
      // Skip if we've already processed this exact tree node
      if (!visited.contains(current)) {
        visited += current
      
        current match {
          // Match any state object, not just "State"
          case s @ Term.Select(Term.Name(objName), _: Term.Name) if stateObjects.contains(objName) =>
            results += s.toString()
            
          case q"goto(..$args)" =>
            args.headOption.foreach { arg =>
              results += arg.toString()
            }
            
          case q"stopSuccess()" =>
            results += "stop"
            
          case Term.Apply.Initial(
              Term.Select(Term.Name("Target"), Term.Name("enter")),
              args
            ) =>
            // Extract the first argument which should be the state
            args.headOption.foreach {
              case select @ Term.Select(Term.Name(objName), _: Term.Name) if stateObjects.contains(objName) =>
                results += select.toString()
              case _ => // Ignore other argument patterns
            }
          
          // Handle function calls - add function body to stack, but avoid cycles
          case Term.Apply.Initial(Term.Name(funcName), _) if functions.contains(funcName) =>
            val funcBody = functions(funcName)
            if (!visited.contains(funcBody)) {
              stack.push(funcBody)
            }
            
          case Term.Apply.Initial(Term.Select(_, Term.Name(funcName)), _) if functions.contains(funcName) =>
            val funcBody = functions(funcName)
            if (!visited.contains(funcBody)) {
              stack.push(funcBody)
            }
            
          case tree: Tree =>
            // Add children to stack for iterative processing, but avoid cycles
            tree.children.foreach { child =>
              if (!visited.contains(child)) {
                stack.push(child)
              }
            }
        }
      }
    }
    
    if (iterations >= maxIterations) {
      println(s"Warning: Maximum iterations exceeded in evalWhen. Partial results returned.")
    }
    
    results.toList
  }

  private def linksToMermaid(links: List[Link]): String = {
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
    
    // Add initial state if we have an Idle state (from any state object)
    val idleState = allStates.find(_.endsWith(".Idle"))
    idleState.foreach { idle =>
      val cleanIdle = cleanStateName(idle)
      sb.append(s"    [*] --> $cleanIdle\n")
    }
    
    // Add all transitions
    links.distinct.foreach { link =>
      val fromState = cleanStateName(link.from)
      val toState = cleanStateName(link.to)
      
      link.arrow match {
        case Some("recovery") =>
          sb.append(s"    $fromState --> $toState : recovery\n")
        case Some("unparsed") =>
          sb.append(s"    $fromState --> $toState : [unparsed]\n")
        case _ =>
          sb.append(s"    $fromState --> $toState\n")
      }
    }
    
    // Add final states
    if (allStates.contains("stop")) {
      sb.append("    stop --> [*]\n")
    }
    
    // Add styling for different types of states
    sb.append("\n")
    
    // Define CSS classes once
    val hasRecoveryStates = allStates.exists(_.contains("Recover"))
    val hasFailedStates = allStates.exists(_.contains("Failed"))
    val hasStopState = allStates.contains("stop")
    val hasUnparsedStates = allStates.exists(_.contains("UnparsedTransition"))
    
    if (hasRecoveryStates) {
      sb.append("    classDef recovery fill:#4ecdc4\n")
    }
    if (hasFailedStates) {
      sb.append("    classDef failed fill:#ff6b6b\n")
    }
    if (hasStopState) {
      sb.append("    classDef stopState fill:#ff6b6b\n")
    }
    if (hasUnparsedStates) {
      sb.append("    classDef unparsed fill:#ffa500,stroke:#ff6b00,stroke-width:2px,stroke-dasharray:5\n")
    }
    
    // Apply classes to states
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
    
    sb.toString()
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
    
    // Make sure the name is valid for Mermaid
    // Replace any characters that might cause issues
    cleaned
      .replaceAll("[^a-zA-Z0-9_]", "_")  // Replace non-alphanumeric chars with underscore
      .replaceAll("_{2,}", "_")          // Replace multiple underscores with single
      .replaceAll("^_+|_+$", "")        // Remove leading/trailing underscores
      .take(50)                          // Limit length
      match {
        case "" => "UnknownState"
        case name => name
      }
  }
}