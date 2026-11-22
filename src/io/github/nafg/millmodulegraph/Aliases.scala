package io.github.nafg.millmodulegraph

/** Short identifiers for the items of a graph (`n0`, `n1`, …), safe to use in any diagram language */
private class Aliases(graph: ModuleGraph) {
  private val byId: Map[String, String] = {
    def ids(items: Seq[ModuleGraph.Item]): Seq[String] =
      items.flatMap {
        case n: ModuleGraph.Node  => Seq(n.id)
        case g: ModuleGraph.Group => g.id +: ids(g.items)
      }
    ids(graph.items).zipWithIndex.map((id, i) => id -> s"n$i").toMap
  }

  def apply(id: String): String = byId(id)
}
