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
    def collect(tree: Tree): Map[String, Tree] = tree match {
      case Defn.Def.Initial(_, Term.Name(name), _, _, _, body) =>
        Map(name -> body) ++ tree.children.flatMap(collect).toMap
      case _ =>
        tree.children.flatMap(collect).toMap
    }
    collect(t)
  }
  
  private def collectStateObjects(t: Tree): Set[String] = {
    def collect(tree: Tree): Set[String] = tree match {
      case Defn.Object.Initial(_, Term.Name(name), _) if containsStateDefinitions(tree) =>
        Set(name) ++ tree.children.flatMap(collect)
      case _ =>
        tree.children.flatMap(collect).toSet
    }
    collect(t)
  }
  
  private def containsStateDefinitions(tree: Tree): Boolean = {
    def hasStatePattern(t: Tree): Boolean = t match {
      case Defn.Object.Initial(_, _, _) => 
        t.children.exists(hasStatePattern)
      case _ => 
        t.toString().contains("extends") && 
        (t.toString().contains("State") || t.toString().contains("Event"))
    }
    hasStatePattern(tree)
  }
  
  private def evalWithFunctions(t: Tree, functions: Map[String, Tree], stateObjects: Set[String]): List[Link] = t match {
    case Term.Apply.Initial(q"when(..$exprs)", terms) =>
      terms.flatMap(term => evalWhen(term, functions, stateObjects)).map(to => Link(exprs.head.toString(), to, None))
      
    case Defn.Def.Initial(_, Term.Name("recoverStateDecision"), _, _, _, body) =>
      evalWhen(body, functions, stateObjects).map(to => 
        Link(s"${stateObjects.headOption.getOrElse("State")}.RecoverSelf", to, Some("recovery"))
      )
      
    case t: Tree =>
      t.children.flatMap(tree => evalWithFunctions(tree, functions, stateObjects))
  }

  private def evalWhen(t: Tree, functions: Map[String, Tree], stateObjects: Set[String]): List[String] = t match {
    // Match any state object, not just "State"
    case s @ Term.Select(Term.Name(objName), _: Term.Name) if stateObjects.contains(objName) =>
      List(s.toString())
      
    case q"goto(..$args)" =>
      args.headOption.map(_.toString()).toList
      
    case q"stopSuccess()" =>
      List("stop")
      
    case Term.Apply.Initial(
        Term.Select(Term.Name("Target"), Term.Name("enter")),
        args
      ) if args.headOption.exists(_.isInstanceOf[Term.Select]) =>
      args.collectFirst {
        case select @ Term.Select(Term.Name(objName), _: Term.Name) if stateObjects.contains(objName) => select.toString()
      }.toList
    
    // Handle function calls - recursively analyze the function body
    case Term.Apply.Initial(Term.Name(funcName), _) if functions.contains(funcName) =>
      evalWhen(functions(funcName), functions, stateObjects)
      
    case Term.Apply.Initial(Term.Select(_, Term.Name(funcName)), _) if functions.contains(funcName) =>
      evalWhen(functions(funcName), functions, stateObjects)
      
    case t: Tree =>
      t.children.flatMap(child => evalWhen(child, functions, stateObjects))
  }

  private def linksToMermaid(links: List[Link]): String = {
    if (links.isEmpty) {
      return """stateDiagram-v2
        |    [*] --> EmptyFSM
        |    EmptyFSM --> [*]
        |    note right of EmptyFSM : No FSM transitions found""".stripMargin
    }

    val sb = new StringBuilder()
    sb.append("stateDiagram-v2\n")
    
    // Get all unique states
    val allStates = links.flatMap(link => List(link.from, link.to)).distinct
    
    // Add initial state if we have an Idle state (from any state object)
    val idleState = allStates.find(_.endsWith(".Idle"))
    idleState.foreach { idle =>
      sb.append(s"    [*] --> $idle\n")
    }
    
    // Add all transitions
    links.distinct.foreach { link =>
      val fromState = cleanStateName(link.from)
      val toState = cleanStateName(link.to)
      
      link.arrow match {
        case Some("recovery") =>
          sb.append(s"    $fromState --> $toState : recovery\n")
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
    
    // Color recovery states
    allStates.filter(_.contains("Recover")).foreach { state =>
      val cleanState = cleanStateName(state)
      sb.append(s"    classDef recovery fill:#4ecdc4\n")
      sb.append(s"    class $cleanState recovery\n")
    }
    
    // Color failed states
    allStates.filter(_.contains("Failed")).foreach { state =>
      val cleanState = cleanStateName(state)
      sb.append(s"    classDef failed fill:#ff6b6b\n")
      sb.append(s"    class $cleanState failed\n")
    }
    
    // Color stop states
    if (allStates.contains("stop")) {
      sb.append("    classDef stopState fill:#ff6b6b\n")
      sb.append("    class stop stopState\n")
    }
    
    sb.toString()
  }
  
  private def cleanStateName(state: String): String = {
    // Remove any state object prefix for cleaner display (e.g., "State." or "OrderStates.")
    if (state.contains(".")) {
      val parts = state.split("\\.")
      if (parts.length >= 2) {
        parts.drop(1).mkString("_")
      } else {
        state.replace(".", "_")
      }
    } else {
      state
    }
  }
}