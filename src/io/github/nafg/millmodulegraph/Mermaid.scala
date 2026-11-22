package io.github.nafg.millmodulegraph

/** Renders a [[ModuleGraph]] as a Mermaid flowchart */
object Mermaid {
  def render(graph: ModuleGraph): String = {
    val alias = new Aliases(graph)
    val colors = {
      def all(items: Seq[ModuleGraph.Item]): Seq[String] =
        items.flatMap {
          case n: ModuleGraph.Node  => n.color.toSeq
          case g: ModuleGraph.Group => all(g.items)
        }
      all(graph.items).distinct.zipWithIndex.map((color, i) => color -> s"color$i").toMap
    }
    def label(text: String) = "\"" + text.replace("\"", "#quot;") + "\""

    def draw(item: ModuleGraph.Item, indent: String): Seq[String] =
      item match {
        case n: ModuleGraph.Node =>
          Seq(indent + alias(n.id) + "[" + label(n.label) + "]" + n.color.fold("")(c => ":::" + colors(c)))
        case g: ModuleGraph.Group =>
          (indent + "subgraph " + alias(g.id) + "[" + label(g.label) + "]") +:
            g.items.flatMap(draw(_, indent + "  ")) :+
            (indent + "end")
      }

    val lines =
      Seq("flowchart TB") ++
        colors.toSeq.sortBy(_._2).map((color, cls) => s"  classDef $cls fill:$color") ++
        graph.items.flatMap(draw(_, "  ")) ++
        graph.edges.map(e => s"  ${alias(e.from)} --> ${alias(e.to)}")
    lines.mkString("", "\n", "\n")
  }
}
