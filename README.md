# Tic-Tac-Toe Tutorial in Scala

This is a port of [Tic-Tac-Toe React tutorial](https://beta.reactjs.org/learn/tutorial-tic-tac-toe) with following Scala stack:

* [Scala.js](https://www.scala-js.org/)
* [sbt](https://www.scala-sbt.org/)
* [Vite](https://vitejs.dev/)
* [Laminar](https://laminar.dev/)


You will build a small tic-tac-toe game during this tutorial.
You can see what it will look like when you’re finished here:

![tictactoe](./tictactoe.png)


## Install

You need to explicitly install the following software:

* sbt, as part of [getting started with Scala](https://docs.scala-lang.org/getting-started/index.html) (or if you prefer, through [its standalone download](https://www.scala-sbt.org/download.html))
* [Node.js](https://nodejs.org/en/)

Other software will be downloaded automatically by the commands below.

## Prepare

Before doing anything, including before importing in an IDE, run

```
$ npm install
```

## Development

Open two terminals.
In the first one, start `sbt` and, within, continuously build the Scala.js project:

```
$ sbt
...
> ~fastLinkJS
...
```

In the second one, start the Vite development server with

```
$ npm run dev
...
```

Follow the URL presented to you by Vite to open the application.

You can now continuously edit the `Main.scala` file, and Vite will automatically reload the page on save.

## Production build

Make a production build with

```
$ npm run build
```

You can then find the built files in the `dist/` directory.
You will need an HTTP server, such as `python3 -m http.server`, to open the files, as Vite rewrites `<script>` tags to prevent cross-origin requests.


## Tic Tac Toe Tutorial

First, we create entrypoint at `src/main/scala/tictactoe/Index.scala`.

`renderOnDomContentLoaded` function binds Laminar HTML element to real DOM element.

```scala
package tictactoe
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

@main def program =
  renderOnDomContentLoaded(
    dom.document.getElementById("app"),
    p("Hello, Laminar")
  )
```

Run `sbt ~fastLinkJS` and `npm run dev`, then open http://localhost:3000

You will find `Hello, Laminar` message like this.

![text "Hello, Laminar" is displayed at the center of screen](./HelloLaminar.png)


Next, We create a cell component of Tic Tac Toe board.

```scala

object Square:
  def apply() = button("Btn")
```

`button("Btn")` is equivalent to `<button>Btn</button>` HTML.  Laminar supports other HTML tags such as `h1`~`h6`, `div` or `p` too.

Laminar's HTML elements take `Modifier[ReactiveHtmlElement[HTMLButtonElement]]` as  varargs. `Modifier` may be other HTML element, `child` or `children` element(s), style or other HTML attributes or event handler such as `onClick` or `onBlur`.


```scala
@main def program =
  renderOnDomContentLoaded(
    dom.document.getElementById("app"),
    button(
      "Btn",
      fontSize.larger,
      width("56px"),
      height("56px"),
      display.block,
      color.black,
      fontWeight.bold
    )
  )
```

For example, the code above is rendered as following.

![There is a button on the left side in the image. That button has a black border and the word "Btn" in bold type.](./btn.png)

The code bellow displays buttons in 3 x 3 grid. Each button should show one of " ", "O" or "X" according to state, but we put "Btn" as a placeholder for now.

```scala
@main def program =
  renderOnDomContentLoaded(
    dom.document.getElementById("app"),
    div(
      List
        .tabulate(9)(identity)
        .grouped(3)
        .map(
          _.map(_ =>
            button(
              "Btn",
              fontSize.larger,
              width("56px"),
              height("56px"),
              display.block,
              color.black,
              fontWeight.bold
            )
          )
        )
        .map(div(_).amend(display.flex))
        .toList
    )
  )

```

You can easily create 3 x 3 grid with Scala's collection combinators(`map`, `grouped`, etc.)

![](btns.png)

You may notice that there is fancy syntax; `.amend(display.flex)`. `amend` and `amendThis` are helper methods that allow us to provide additional properties to HTML elements. `amend` takes `Modifier` as does `HTMLEmement#apply`.

For now, you have not use any stateful component at all. However, we cannot do without state management in most of modern frontend application development.

In this tutorial, you use mainly two state management API from Laminar. One is `Var[T]` and the other is `EventBus[T]`.

`Var` is a _reactive_ value that notifies the value changes to its listeners. This feature is useful to track state and propagate it to UI. 

In Tic Rac Toe, each cell in game board starts with `Empty` and Two player(`O` and `X`) choose a cell in turn and fill it with his/her mark(`O` or `X`), so we also need to track player turn.


```scala
enum Cell:
  case Empty
  case X
  case O

object Game:
  val turn = Var(0)
  val board = Var(Seq.fill(9)(Cell.Empty))
```

Let's implement interactive UI.

Tic-Tac-Toe UI must satisfy the following specs.

1. when player `O` clicks the empty cell, cell is turned into `O`, and when `X` does, cell is turned into `X`.
2. once  `X` or `O` is set to a cell, player cannot modify the cell.
3. when `board` state changes, board UI immediately reflects the change.


First, we get signal from `Var` and transform it into HTML elements. `children` property in HTML element can subscribe reactive signal using `<--`. 

In genral, `<--` binds reactive value to an element whereas `:=` assigns value to an element. For example, `button` with `disabled <-- intSignal.map(_ % 2 == 0)` listens intSignal changes and is re-rendered every time `intSignal` changes from odd to even or from even to odd, but `button` with `disabled := <value from constructor>` won't change until its ascendant element is rendered again.

Let's map state value to UI. It is not so difficult. Pay attention aroung `div(children <-- Game.cells)` and `board.signal..map(...)`.

```scala
@main def program =
  renderOnDomContentLoaded(
    dom.document.getElementById("app"),
    div(children <-- Game.cells)
  )

object Game:
  val turn = Var(0)
  val board = Var(Seq.fill(9)(Cell.Empty))
  object Square:
    def apply(loc: Int, cell: Cell) =
       div(
         button(
           cell.toString(),
         ).amend(
           fontSize.larger,
           width("56px"),
           height("56px"),
           display.block,
           color.black,
           fontWeight.bold
         ),
         padding("2px")
       )

  val cells = board.signal
    .map(state =>
      state.zipWithIndex
        .map((cell, loc) => Square(loc, cell))
        .grouped(3)
        .map(
          div(_).amend(
            display.flex,
            justifyContent.center,
            flexWrap.nowrap
          )
        )
        .toSeq
    )
```

![9 "empty" words are arranged in 3 x 3 grid.](./empties.png)

At this point, you cannot change cell state yet :(

To change cell state, you have to set `onClick` event handler and define how to mutate board state.


First, we define `Cmd` as enum so that compiler can exhaustive match clauses.

```scala
enum Cmd:
  case Choose(loc: Int)
```

Then, we create `Observer`, which recieves `Cmd` and mutate states.
```scala
  val obs: Observer[Cmd] = Observer[Cmd] {
    case Cmd.Choose(loc) =>
      val t = turn.now()
      board.now()(loc) match
        case Cell.Empty =>
          // replace element at loc
          board.update(state =>
            val (leading, _ +: trailing) = state.splitAt(loc): @unchecked
            (leading :+ (if turn % 2 == 0 then Cell.O
                                      else Cell.X)) ++ trailing
          )
          turn.update(_ + 1)
        case _ => ()
    case _ => ()
  }
```

Finally, we dispatch `Choose` event from `Square` to `Observer` using `-->` operator.

```scala
  object Square:
    def apply(loc: Int, cell: Cell) =
       div(
         button(
           cell.toString(),
           onClick
             .filter(_ => cell == Cell.Empty)
             .mapTo(Cmd.Choose(loc)) --> obs
         ).amend(
           fontSize.larger,
           width("56px"),
           height("56px"),
           display.block,
           color.black,
           fontWeight.bold
         ),
         padding("2px")
       )
```

When you click an empty cell, it dispatches `Cmd.Choose` event to `Observer`, then `Observer` mutates board and turn state. On state changes, `Var` emits signal that invokes re-rendering of listeners(in this example `div(children <-- Game.cells)` is refreshed).

```scala
@main def program =
  renderOnDomContentLoaded(
    dom.document.getElementById("app"),
    div(children <-- Game.cells)
  )

object Game:
  val turn = Var(0)
  val board = Var(Seq.fill(9)(Cell.Empty))
  object Square:
    def apply(loc: Int, cell: Cell) =
       div(
         button(
           cell.toString(),
           onClick
             // prevent onClick event when cell is not empty.
             .filter(_ => cell == Cell.Empty)
             .mapTo(Cmd.Choose(loc)) --> obs
         ).amend(
           fontSize.larger,
           width("56px"),
           height("56px"),
           display.block,
           color.black,
           fontWeight.bold
         ),
         padding("2px")
       )

  val cells = board.signal
    .map(state =>
      state.zipWithIndex
        .map((cell, loc) => Square(loc, cell))
        .grouped(3)
        .map(
          div(_).amend(
            display.flex,
            justifyContent.center,
            flexWrap.nowrap
          )
        )
        .toSeq
    )
  val obs: Observer[Cmd] = Observer[Cmd] {
    case Cmd.Choose(loc) =>
      val t = turn.now()
      board.now()(loc) match
        case Cell.Empty =>
          // replace element at loc
          board.update(state =>
            val (leading, _ +: trailing) = state.splitAt(loc): @unchecked
            (leading :+ (if t % 2 == 0 then Cell.O
                                      else Cell.X)) ++ trailing
          )
          turn.update(_ + 1)
        case _ => ()
    case _ => ()
  }
```

!["The image demonstrating synchronization between  board UI and board state. When user clicks "Empty" cell, it turns into "O" or "X".](./tictactoebase.png)

`Empty` cell does not look nice, so we define a helper method that pretty print `Cell`.

```scala
object Cell:
  extension (cell: Cell)
    def show: String = cell match
      case Empty => ""
      case X     => "X"
      case O     => "O"
```

And replace `cell.toString()` with `cell.show`.

```scala
  object Square:
    def apply(loc: Int, cell: Cell) =
       div(
         button(
           cell.show,
```

![Empty cells do not display "Empty" word.](./tictactoe-no-empty.png)