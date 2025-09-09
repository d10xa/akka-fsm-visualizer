package analyzer

import scala.meta._

/**
 * AST traversal utilities for Scala code analysis  
 * Single Responsibility: AST navigation and tree operations
 */
object AstTraverser {
  
  /**
   * Collect all function definitions from AST
   */
  def collectFunctionDefinitions(t: Tree): Map[String, Tree] = {
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
          case defn @ Defn.Def.Initial(_, Term.Name(name), _, _, _, _) =>
            functions(name) = defn
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
      println(s"Warning: Maximum iterations exceeded in collectFunctionDefinitions. Partial results returned.")
    }
    
    functions.toMap
  }
  
  /**
   * Find all state objects (objects containing case objects extending some state trait)
   */
  def collectStateObjects(t: Tree): Set[String] = {
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
  
  /**
   * Check if tree contains state definitions (case objects extending state traits)
   */
  private def containsStateDefinitions(tree: Tree): Boolean = {
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
    // Use string contains for simpler and more reliable matching
    val hasStatePattern = 
      treeStr.contains("State") ||
      treeStr.contains("extends") && (
        treeStr.contains("State") ||
        treeStr.contains("Status") ||
        treeStr.contains("Phase") ||
        treeStr.contains("Step")
      )
    
    hasStatePattern
  }
  
  /**
   * Find initial state from startWith calls
   */
  def findInitialState(t: Tree, stateObjects: Set[String]): Option[String] = {
    // Look for startWith(State.SomeState, data) patterns
    val results = scala.collection.mutable.ListBuffer[String]()
    traverseForPattern(t, stateObjects) {
      case Term.Apply.Initial(Term.Name("startWith"), args) =>
        args.headOption.foreach { arg =>
          resolveStateExpression(arg, stateObjects).foreach(results += _)
        }
        
      case Term.Apply.Initial(Term.Select(_, Term.Name("startWith")), args) =>
        args.headOption.foreach { arg =>
          resolveStateExpression(arg, stateObjects).foreach(results += _)
        }
    }
    
    results.headOption
  }
  
  /**
   * Traverse tree looking for specific patterns
   */
  def traverseForPattern[T](tree: Tree, stateObjects: Set[String])(pattern: PartialFunction[Tree, T]): List[T] = {
    val results = scala.collection.mutable.ListBuffer[T]()
    val stack = scala.collection.mutable.Stack[Tree]()
    val visited = scala.collection.mutable.Set[Tree]()
    val maxIterations = 10000
    var iterations = 0
    
    stack.push(tree)
    
    while (stack.nonEmpty && iterations < maxIterations) {
      iterations += 1
      val current = stack.pop()
      
      if (!visited.contains(current)) {
        visited += current
        
        // Apply pattern if it matches
        if (pattern.isDefinedAt(current)) {
          pattern(current)
        }
        
        // Continue traversing children
        current.children.foreach { child =>
          if (!visited.contains(child)) {
            stack.push(child)
          }
        }
      }
    }
    
    results.toList
  }
  
  /**
   * Resolve state expressions like State.Idle to state names
   */
  def resolveStateExpression(expr: Term, stateObjects: Set[String]): Option[String] = {
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
}