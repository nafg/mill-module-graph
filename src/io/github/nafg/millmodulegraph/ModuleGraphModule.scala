package io.github.nafg.millmodulegraph

import mill.*
import mill.api.{DefaultTaskModule, Module}

/** Draws the graph of a build's modules and their `moduleDeps`. Its default task is [[moduleGraph]]. */
trait ModuleGraphModule extends DefaultTaskModule {

  override def defaultTask(): String = "moduleGraph"

  /** The modules drawn, with the modules under them. To draw the whole build from a module other than the root,
    * override this with `build.moduleDirectChildren`.
    */
  def moduleGraphModules: Seq[Module] = moduleDirectChildren

  /** Whether test modules are drawn */
  def moduleGraphIncludeTests: Boolean = false

  /** A module's fill colour, a hex colour such as `#d6eaff` or a colour name such as `red` */
  def moduleGraphColor(module: Module): Option[String] = None

  /** Writes the module graph as Mermaid (`module-graph.mmd`), PlantUML (`module-graph.puml`) and SVG
    * (`module-graph.svg`)
    */
  def moduleGraph(): Command[Seq[PathRef]] = Task.Command {
    val graph =
      ModuleGraph.of(moduleGraphModules, moduleGraphIncludeTests, moduleGraphColor)
    val plantUml = PlantUml.render(graph)
    val files = Seq(
      "module-graph.mmd" -> Mermaid.render(graph),
      "module-graph.puml" -> plantUml,
      "module-graph.svg" -> PlantUml.toSvg(plantUml)
    )
    for ((name, content) <- files) yield {
      val path = Task.dest / name
      os.write(path, content)
      Task.log.info(s"Wrote $path")
      PathRef(path)
    }
  }
}
