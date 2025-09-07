import scala.meta._
import scala.scalajs.js.annotation.JSExportTopLevel

case class Link(from: String, to: String, arrow: Option[String], eventLabel: Option[String] = None, isTimeout: Boolean = false)

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
    // Find startWith initial state
    val initialState = findInitialState(t, stateObjects)
    // Find onTransition blocks
    val onTransitionLinks = findOnTransitionBlocks(t, stateObjects)
    
    val mainLinks = evalWithFunctions(t, functions, stateObjects)
    (mainLinks ++ onTransitionLinks).distinct
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
  
  private def findInitialState(t: Tree, stateObjects: Set[String]): Option[String] = {
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
          // Match startWith(state, data)
          case Term.Apply.Initial(Term.Name("startWith"), args) =>
            args.headOption.foreach {
              case select @ Term.Select(Term.Name(objName), _: Term.Name) if stateObjects.contains(objName) =>
                return Some(select.toString())
              case _ => // Continue searching
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
    
    None
  }
  
  private def findOnTransitionBlocks(t: Tree, stateObjects: Set[String]): List[Link] = {
    val links = scala.collection.mutable.ListBuffer[Link]()
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
          // Match onTransition { case fromState -> toState => ... }
          case Term.Apply.Initial(Term.Name("onTransition"), List(Term.Block(cases))) =>
            cases.foreach {
              case Case(Pat.Extract(Term.Name("->"), List(fromPat, toPat)), _, _) =>
                val fromState = extractStateFromPattern(fromPat, stateObjects)
                val toState = extractStateFromPattern(toPat, stateObjects)
                
                (fromState, toState) match {
                  case (Some(from), Some(to)) =>
                    links += Link(from, to, Some("transition"), Some("onTransition"))
                  case _ => // Skip invalid patterns
                }
                
              case Case(Pat.ExtractInfix(fromPat, Term.Name("->"), List(toPat)), _, _) =>
                val fromState = extractStateFromPattern(fromPat, stateObjects)
                val toState = extractStateFromPattern(toPat, stateObjects)
                
                (fromState, toState) match {
                  case (Some(from), Some(to)) =>
                    links += Link(from, to, Some("transition"), Some("onTransition"))
                  case _ => // Skip invalid patterns
                }
                
              case _ => // Skip other case patterns
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
    
    links.toList
  }
  
  private def extractStateFromPattern(pattern: Pat, stateObjects: Set[String]): Option[String] = {
    pattern match {
      case Pat.Var(Term.Name(objName)) if stateObjects.contains(objName) =>
        Some(pattern.toString())
      case Pat.Wildcard() => Some("_") // Wildcard pattern
      case _ => 
        // Try to extract from string representation for complex patterns
        val patStr = pattern.toString()
        if (stateObjects.exists(obj => patStr.startsWith(s"$obj."))) {
          Some(patStr)
        } else {
          None
        }
    }
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
              
              // Check if this when block has a timeout
              val hasTimeout = exprs.length > 1 && exprs(1).toString().contains("stateTimeout")
              
              // Show all transitions including from functions
              val transitions = terms.flatMap(term => evalWhenWithEvents(term, functions, stateObjects, currentState))
              if (transitions.nonEmpty) {
                transitions.foreach { case (to, event, isTimeout) =>
                  links += Link(currentState, to, None, event, isTimeout)
                }
              } else {
                // No transitions found, mark as unparsed
                links += Link(currentState, "UnparsedTransition", Some("unparsed"))
              }
              
              // Add timeout transition if stateTimeout is specified
              if (hasTimeout) {
                // Look for StateTimeout handling in the cases
                val timeoutTransitions = terms.flatMap(term => findTimeoutTransitions(term, functions, stateObjects, currentState))
                timeoutTransitions.foreach { case (to, event) =>
                  links += Link(currentState, to, Some("timeout"), event, isTimeout = true)
                }
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
              links += Link(recoveryState, to, Some("recovery"), Some("recovery"))
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
            links += Link(recoveryState, to, Some("recovery"), Some("recovery"))
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
              val stateName = resolveStateExpression(arg, stateObjects)
              stateName.foreach { state =>
                results += state
              }
            }
            
          // Handle replying transitions: goto(state) replying msg
          case Term.Apply.Initial(q"goto(..$args)", _) =>
            args.headOption.foreach { arg =>
              val stateName = resolveStateExpression(arg, stateObjects)
              stateName.foreach { state =>
                results += state
              }
            }
            
          // Handle complex replying patterns: goto(state) replying message
          case Term.ApplyInfix(Term.Apply.Initial(q"goto(..$args)", _), _, _, _) =>
            args.headOption.foreach { arg =>
              val stateName = resolveStateExpression(arg, stateObjects)
              stateName.foreach { state =>
                results += state
              }
            }
            
          // Handle stay() transitions
          case q"stay()" =>
            if (currentState.nonEmpty) {
              results += currentState
            }
            
          // Handle stay replying
          case Term.Apply.Initial(q"stay()", _) =>
            if (currentState.nonEmpty) {
              results += currentState
            }
            
          case Term.ApplyInfix(q"stay()", _, _, _) =>
            if (currentState.nonEmpty) {
              results += currentState
            }
            
          // Handle if-else constructions
          case Term.If(condition, thenBranch, elseBranch) =>
            // Process both branches to find all possible transitions
            val thenResults = evalWhen(thenBranch, functions, stateObjects, currentState)
            val elseResults = evalWhen(elseBranch, functions, stateObjects, currentState)
            results ++= thenResults
            results ++= elseResults
            
          // Handle match-case constructions
          case Term.Match(expr, cases) =>
            // Process all case branches to find all possible transitions
            cases.foreach { case Case(_, _, body) =>
              val caseResults = evalWhen(body, functions, stateObjects, currentState)
              results ++= caseResults
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

  private def evalWhenWithEvents(t: Tree, functions: Map[String, Tree], stateObjects: Set[String], currentState: String = ""): List[(String, Option[String], Boolean)] = {
    val results = scala.collection.mutable.ListBuffer[(String, Option[String], Boolean)]()
    val stack = scala.collection.mutable.Stack[Tree]()
    val visited = scala.collection.mutable.Set[Tree]()
    val maxIterations = 10000
    var iterations = 0
    var currentEvent: Option[String] = None
    
    stack.push(t)
    
    while (stack.nonEmpty && iterations < maxIterations) {
      iterations += 1
      val current = stack.pop()
      
      if (!visited.contains(current)) {
        visited += current
        
        current match {
          // Match Event patterns: case Event(EventName, data) =>
          case Case(Pat.Extract(Term.Name("Event"), List(eventPat, _)), _, body) =>
            currentEvent = extractEventName(eventPat)
            val transitions = evalWhen(body, functions, stateObjects, currentState)
            transitions.foreach { to =>
              results += ((to, currentEvent, false))
            }
            
          // Handle StateTimeout event
          case Case(Pat.Extract(Term.Name("Event"), List(Term.Name("StateTimeout"), _)), _, body) =>
            val transitions = evalWhen(body, functions, stateObjects, currentState)
            transitions.foreach { to =>
              results += ((to, Some("StateTimeout"), true))
            }
            
          // Regular state transitions without event info
          case s @ Term.Select(Term.Name(objName), _: Term.Name) if stateObjects.contains(objName) =>
            results += ((s.toString(), currentEvent, false))
            
          case q"goto(..$args)" =>
            args.headOption.foreach { arg =>
              val stateName = resolveStateExpression(arg, stateObjects)
              stateName.foreach { state =>
                results += ((state, currentEvent, false))
              }
            }
            
          case Term.Apply.Initial(q"goto(..$args)", _) =>
            args.headOption.foreach { arg =>
              val stateName = resolveStateExpression(arg, stateObjects)
              stateName.foreach { state =>
                results += ((state, currentEvent, false))
              }
            }
            
          case Term.ApplyInfix(Term.Apply.Initial(q"goto(..$args)", _), _, _, _) =>
            args.headOption.foreach { arg =>
              val stateName = resolveStateExpression(arg, stateObjects)
              stateName.foreach { state =>
                results += ((state, currentEvent, false))
              }
            }
            
          case q"stay()" =>
            if (currentState.nonEmpty) {
              results += ((currentState, currentEvent, false))
            }
            
          case Term.Apply.Initial(q"stay()", _) =>
            if (currentState.nonEmpty) {
              results += ((currentState, currentEvent, false))
            }
            
          case Term.ApplyInfix(q"stay()", _, _, _) =>
            if (currentState.nonEmpty) {
              results += ((currentState, currentEvent, false))
            }
            
          case q"stopSuccess()" =>
            results += (("stop", currentEvent, false))
            
          case tree: Tree =>
            tree.children.foreach { child =>
              if (!visited.contains(child)) {
                stack.push(child)
              }
            }
        }
      }
    }
    
    results.toList
  }
  
  private def findTimeoutTransitions(t: Tree, functions: Map[String, Tree], stateObjects: Set[String], currentState: String): List[(String, Option[String])] = {
    val results = scala.collection.mutable.ListBuffer[(String, Option[String])]()
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
          // Match StateTimeout event: case Event(StateTimeout, _) =>
          case Case(Pat.Extract(Term.Name("Event"), List(Term.Name("StateTimeout"), _)), _, body) =>
            val transitions = evalWhen(body, functions, stateObjects, currentState)
            transitions.foreach { to =>
              results += ((to, Some("StateTimeout")))
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
    
    results.toList
  }
  
  private def extractEventName(eventPat: Pat): Option[String] = {
    eventPat match {
      case Pat.Var(Term.Name(name)) => Some(name)
      case Term.Name(name) => Some(name)
      case _ => None
    }
  }
  
  private def resolveStateExpression(expr: Term, stateObjects: Set[String]): Option[String] = {
    expr match {
      // Direct state reference: State.Idle
      case select @ Term.Select(Term.Name(objName), _: Term.Name) if stateObjects.contains(objName) =>
        Some(select.toString())
        
      // Simple variable name like 'to' or 'targetState' - we can't resolve these without context
      // So we filter them out if they're not valid state references
      case Term.Name(name) =>
        // Check if it looks like a valid state name (contains dots or matches state objects)
        if (name.contains(".") || stateObjects.exists(obj => name.startsWith(obj))) {
          Some(name)
        } else {
          // This is likely a variable name, not a direct state - skip it
          None
        }
        
      // If expression - we need to evaluate both branches
      case Term.If(_, thenBranch, elseBranch) =>
        // For if expressions, we should collect all possible states from both branches
        // But since this is complex, let's mark it as conditional for now
        val thenState = resolveStateExpression(thenBranch, stateObjects)
        val elseState = resolveStateExpression(elseBranch, stateObjects)
        
        // For now, just return the first valid state we find
        // TODO: In the future, we could return multiple states for conditional transitions
        thenState.orElse(elseState)
        
      // Complex expressions - try string representation as fallback
      case _ =>
        val exprStr = expr.toString()
        if (stateObjects.exists(obj => exprStr.contains(s"$obj."))) {
          Some(exprStr)
        } else {
          None
        }
    }
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
        case Some("transition") =>
          sb.append(s"    $fromState --> $toState : ${label.getOrElse("onTransition")}\n")
        case _ =>
          label match {
            case Some(lbl) => sb.append(s"    $fromState --> $toState : $lbl\n")
            case None => sb.append(s"    $fromState --> $toState\n")
          }
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
  
  private def findInitialStateFromLinks(links: List[Link]): Option[String] = {
    // This would need to be populated during parsing when we find startWith
    // For now, return None and rely on Idle state detection
    None
  }
  
  private def buildTransitionLabel(link: Link): Option[String] = {
    val eventLabel = link.eventLabel
    val isTimeout = link.isTimeout
    
    (eventLabel, isTimeout) match {
      case (Some(event), true) => Some(s"$event [timeout]")
      case (Some(event), false) => Some(event)
      case (None, true) => Some("[timeout]")
      case (None, false) => None
    }
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