# mill-module-graph

A [Mill](https://mill-build.org) plugin that draws a build's module dependency graph: one box per module, one arrow per
`moduleDeps` entry. `./mill visualize` draws tasks, not modules.

It writes the graph as:

- **Mermaid** (`module-graph.mmd`), for GitHub, GitLab and other Markdown renderers
- **SVG** (`module-graph.svg`), laid out by [Eclipse ELK](https://eclipse.dev/elk/) and drawn by
  [PlantUML](https://plantuml.com), entirely on the JVM: no Graphviz, browser or installed fonts needed
- **PlantUML** (`module-graph.puml`), the source of the SVG

How modules are drawn:

- The JVM and JS halves of a shared module (`PlatformScalaModule`s) are one box
- The instances of a cross module, and a module together with its child modules, are drawn inside a labelled box
- Test modules are left out unless you ask for them

Requires Mill 1.3 or later: the plugin is compiled with Scala 3.9, which earlier versions of Mill can't read in a
build.

## Usage

Add the plugin to the header of `build.mill`:

```scala
//| mvnDeps:
//| - "io.github.nafg.millmodulegraph::millmodulegraph_mill1:<version>"
```

Mix `ModuleGraphModule` into the root module, or into a module of its own that draws the whole build:

```scala
import io.github.nafg.millmodulegraph.ModuleGraphModule

object moduleGraph extends ModuleGraphModule {
  override def moduleGraphModules = build.moduleDirectChildren
}
```

Then run

```sh
./mill moduleGraph
```

and the files are written to `out/moduleGraph/moduleGraph.dest/`.

### Settings

```scala
import io.github.nafg.millmodulegraph.ModuleGraphModule
import mill.api.Module
import mill.scalajslib.ScalaJSModule
import mill.scalalib.JavaModule

object moduleGraph extends ModuleGraphModule {
  override def moduleGraphModules = build.moduleDirectChildren

  // Draw test modules too (default: false)
  override def moduleGraphIncludeTests = true

  // Colour boxes by kind of module
  override def moduleGraphColor(module: Module) =
    module match {
      case _: ScalaJSModule => Some("#fff2cc")
      case _: JavaModule    => Some("#e2f0d9")
      case _                => None
    }
}
```
