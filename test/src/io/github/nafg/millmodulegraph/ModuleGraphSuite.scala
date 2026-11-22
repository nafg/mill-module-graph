package io.github.nafg.millmodulegraph

import io.github.nafg.millmodulegraph.ModuleGraph.{Edge, Group, Node}
import mill.*
import mill.api.{Discover, Module}
import mill.scalajslib.ScalaJSModule
import mill.scalalib.*
import mill.testkit.{TestRootModule, UnitTester}
import mill.util.TokenReaders.*

class ModuleGraphSuite extends munit.FunSuite {
  trait Scala extends ScalaModule {
    override def scalaVersion = "3.8.2"
  }
  trait ScalaJS extends Scala with ScalaJSModule {
    override def scalaJSVersion = "1.20.1"
  }

  object build extends TestRootModule with ModuleGraphModule {
    // A shared module whose JVM and JS halves are its only children
    object shared extends Module {
      object jvm extends Scala with PlatformScalaModule
      object js extends ScalaJS with PlatformScalaModule
    }

    object app extends Scala {
      override def moduleDeps = Seq(shared.jvm)
      object test extends ScalaTests with TestModule.Munit
    }

    // A cross module whose instances are themselves modules with platform halves
    object lib extends Cross[LibModule]("a", "b")
    trait LibModule extends Cross.Module[String] with Scala {
      object jvm extends Scala with PlatformScalaModule {
        override def moduleDeps = Seq(shared.jvm)
      }
      object js extends ScalaJS with PlatformScalaModule {
        override def moduleDeps = Seq(shared.js)
      }
    }

    // A cross module with one instance
    object single extends Cross[SingleModule]("only")
    trait SingleModule extends Cross.Module[String] with Scala {
      override def moduleDeps = Seq(app)
    }

    override def moduleGraphColor(module: Module) =
      if (module == shared) Some("#d6eaff")
      else if (module == app) Some("lightgreen")
      else None

    lazy val millDiscover = Discover[this.type]
  }

  test("graph of a build") {
    assertEquals(
      ModuleGraph.of(build.moduleDirectChildren, includeTests = false, build.moduleGraphColor),
      ModuleGraph(
        Seq(
          Node("app", "app", Some("lightgreen")),
          Group("lib", "lib", Seq(Node("lib.a", "lib.a", None), Node("lib.b", "lib.b", None))),
          Node("shared", "shared", Some("#d6eaff")),
          Node("single.only", "single.only", None)
        ),
        Seq(Edge("app", "shared"), Edge("lib.a", "shared"), Edge("lib.b", "shared"), Edge("single.only", "app"))
      )
    )
  }

  test("graph of a build, with tests") {
    val graph = ModuleGraph.of(build.moduleDirectChildren, includeTests = true, build.moduleGraphColor)
    assertEquals(
      graph.items.head,
      Group("app/group", "app", Seq(Node("app", "app", Some("lightgreen")), Node("app.test", "app.test", None)))
    )
    assert(graph.edges.contains(Edge("app.test", "app")), graph.edges)
  }

  test("moduleGraph writes Mermaid, PlantUML and SVG") {
    UnitTester(build, os.temp.dir()).scoped { tester =>
      val files = tester(build.moduleGraph()) match {
        case Left(failing)                         => failing.throwException
        case Right(UnitTester.Result(pathRefs, _)) => pathRefs
      }
      assertEquals(files.map(_.path.last), Seq("module-graph.mmd", "module-graph.puml", "module-graph.svg"))
      val svg = os.read(files.last.path)
      assert(svg.startsWith("<svg"), svg.take(200))
      assert(svg.contains(">single.only<"), svg)
    }
  }
}
