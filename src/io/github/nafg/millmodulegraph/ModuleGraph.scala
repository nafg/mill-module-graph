package io.github.nafg.millmodulegraph

import mill.api.{Cross, Module, Segments}
import mill.javalib.{JavaModule, TestModule}
import mill.scalalib.PlatformScalaModule

/** Modules and the `moduleDeps` between them, as boxes and arrows.
  *
  * @param items
  *   the top-level boxes and groups
  * @param edges
  *   one per dependency, from the depending module's box to the dependency's
  */
case class ModuleGraph(items: Seq[ModuleGraph.Item], edges: Seq[ModuleGraph.Edge])

object ModuleGraph {
  sealed trait Item {
    def id: String
    def label: String
  }

  /** A box drawn for one module.
    *
    * @param color
    *   its fill colour, a hex colour such as `#d6eaff` or a colour name such as `red`
    */
  case class Node(id: String, label: String, color: Option[String]) extends Item

  /** A labelled box drawn around several items */
  case class Group(id: String, label: String, items: Seq[Item]) extends Item

  case class Edge(from: String, to: String)

  /** The graph of `modules` and the modules under them.
    *
    *   - A [[mill.javalib.JavaModule]] is a node. If it has child modules that are drawn, it and they are drawn in a
    *     group.
    *   - A [[mill.scalalib.PlatformScalaModule]] (the JVM or JS half of a shared module) is drawn as its parent module:
    *     its dependencies and child modules belong to the parent, which is a node even if it is not a `JavaModule`.
    *   - The instances of a [[mill.api.Cross]] module, and the children of any other module, are drawn in a group,
    *     unless there is only one, which is drawn alone.
    *   - Dependencies on modules that are not drawn are left out.
    *
    * @param includeTests
    *   whether test modules are drawn
    * @param color
    *   a module's fill colour, a hex colour such as `#d6eaff` or a colour name such as `red`. A node drawn for a module
    *   with platform modules gets the module's colour if it has one, else its first platform module's
    */
  def of(modules: Seq[Module], includeTests: Boolean, color: Module => Option[String]): ModuleGraph = {
    def id(segments: Segments): String = segments.render

    def platformModules(m: Module): Seq[PlatformScalaModule] =
      m.moduleDirectChildren.collect { case p: PlatformScalaModule => p }

    /** The id of the node a module is drawn as */
    def nodeId(m: Module): String =
      m match {
        case _: PlatformScalaModule => id(Segments(m.moduleSegments.value.init))
        case _                      => id(m.moduleSegments)
      }

    val dependencies = Seq.newBuilder[Edge]

    def group(id: String, label: String, items: Seq[Item]): Seq[Item] =
      items match {
        case Seq()    => Nil
        case Seq(one) => Seq(one)
        case _        => Seq(Group(id, label, items))
      }

    def walk(m: Module): Seq[Item] =
      m match {
        case _: TestModule if !includeTests => Nil
        case c: Cross[?] => group(id(c.moduleSegments), c.moduleSegments.render, c.crossModules.flatMap(walk))
        case _           =>
          val platforms = platformModules(m)
          val children =
            (m +: platforms)
              .flatMap(_.moduleDirectChildren)
              .filterNot(_.isInstanceOf[PlatformScalaModule])
              .flatMap(walk)
          val label = m.moduleSegments.render
          val isNode = m.isInstanceOf[JavaModule] || platforms.nonEmpty
          if (!isNode)
            group(id(m.moduleSegments), label, children)
          else {
            (m +: platforms).collect { case j: JavaModule => j }.foreach { j =>
              j.moduleDeps.foreach(d => dependencies += Edge(nodeId(m), nodeId(d)))
            }
            val node = Node(nodeId(m), label, color(m).orElse(platforms.flatMap(color).headOption))
            if (children.isEmpty) Seq(node)
            else Seq(Group(node.id + "/group", label, node +: children))
          }
      }

    val items = modules.flatMap(walk)

    def nodeIds(items: Seq[Item]): Seq[String] =
      items.flatMap {
        case n: Node  => Seq(n.id)
        case g: Group => nodeIds(g.items)
      }
    val drawn = nodeIds(items).toSet

    ModuleGraph(
      items,
      dependencies.result().distinct.filter(e => e.from != e.to && drawn(e.from) && drawn(e.to))
    )
  }
}
