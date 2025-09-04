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

  private def eval(t: Tree): List[Link] = t match {
    case Term.Apply.Initial(q"when(..$exprs)", terms) =>
      terms.flatMap(evalWhen).map(to => Link(exprs.head.toString(), to, None))
      
    case Defn.Def.Initial(_, Term.Name("recoverStateDecision"), _, _, _, body) =>
      evalWhen(body).map(to => 
        Link("State.RecoverSelf", to, Some("recovery"))
      )
      
    case t: Tree =>
      t.children.flatMap(eval)
  }

  private def evalWhen(t: Tree): List[String] = t match {
    case s @ Term.Select(Term.Name("State"), _: Term.Name) =>
      List(s.toString())
      
    case q"stopSuccess()" =>
      List("stop")
      
    case Term.Apply.Initial(
        Term.Select(Term.Name("Target"), Term.Name("enter")),
        Term.ArgClause(
          List(select @ Term.Select(Term.Name("State"), _: Term.Name), _),
          None
        )
      ) =>
      List(select.toString())
      
    case t: Tree =>
      t.children.flatMap(evalWhen)
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
    
    // Add initial state if we have an Idle state
    if (allStates.contains("State.Idle")) {
      sb.append("    [*] --> State.Idle\n")
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
    // Remove "State." prefix for cleaner display
    state.replace("State.", "").replace(".", "_")
  }
}