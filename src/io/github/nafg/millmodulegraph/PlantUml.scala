package io.github.nafg.millmodulegraph

import java.io.ByteArrayOutputStream

import scala.jdk.CollectionConverters.*

import net.sourceforge.plantuml.error.PSystemError
import net.sourceforge.plantuml.{FileFormat, FileFormatOption, SourceStringReader}

/** Renders a [[ModuleGraph]] as a PlantUML diagram laid out by Eclipse ELK, and that as SVG */
object PlantUml {
  def render(graph: ModuleGraph): String = {
    val alias = new Aliases(graph)
    def label(text: String) = "\"" + text.replace("\"", "'") + "\""

    def draw(item: ModuleGraph.Item, indent: String): Seq[String] =
      item match {
        case n: ModuleGraph.Node =>
          // PlantUML writes a colour name, like a hex colour, after a #
          val color = n.color.fold("")(c => " " + (if (c.startsWith("#")) c else "#" + c))
          Seq(indent + "rectangle " + label(n.label) + " as " + alias(n.id) + color)
        case g: ModuleGraph.Group =>
          (indent + "package " + label(g.label) + " as " + alias(g.id) + " {") +:
            g.items.flatMap(draw(_, indent + "  ")) :+
            (indent + "}")
      }

    val lines =
      Seq("@startuml", "!pragma layout elk", "skinparam packageStyle rectangle", "skinparam shadowing false") ++
        graph.items.flatMap(draw(_, "")) ++
        graph.edges.map(e => s"${alias(e.from)} --> ${alias(e.to)}") :+
        "@enduml"
    lines.mkString("", "\n", "\n")
  }

  /** Draws a PlantUML diagram as SVG. Text is measured with PlantUML's built-in width table rather than the system's
    * fonts, so this works on a JVM without fonts installed.
    */
  def toSvg(plantUml: String): String = {
    val reader = new SourceStringReader(plantUml)
    reader.getBlocks.asScala.map(_.getDiagram).collectFirst { case e: PSystemError => e }.foreach { error =>
      throw new IllegalArgumentException(
        "PlantUML rejected the diagram:\n" + error.getPureAsciiFormatted.asScala.mkString("\n")
      )
    }
    val out = new ByteArrayOutputStream
    reader.outputImage(out, new FileFormatOption(FileFormat.SVG_DETERMINISTIC))
    out.toString("UTF-8")
  }
}
