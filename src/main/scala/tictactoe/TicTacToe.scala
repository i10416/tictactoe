package tictactoe
import com.raquo.laminar.api.L.{*, given}
import scala.collection.immutable.SortedMap
import com.raquo.laminar.CollectionCommand
import com.raquo.laminar.modifiers.ChildrenCommandInserter
import com.raquo.laminar.modifiers.ChildrenCommandInserter.ChildrenCommand

enum Cell:
  case Empty
  case X
  case O

object Cell:
  extension (cell: Cell)
    def show: String = cell match
      case Empty => " "
      case X     => "X"
      case O     => "O"

object Game:
  val turnVar = Var(0)
  val histories = Var(Seq(Seq.fill(9)(Cell.Empty)))
  val mat = Var(Seq.fill(9)(Cell.Empty))

  val cells = turnVar.signal
    .withCurrentValueOf(histories.signal)
    .map((turn, boards) =>
      boards(turn).zipWithIndex
        .map((cell, loc) => Square(loc, cell))
        .grouped(3) // group 3 items into row
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
      val turn = turnVar.now()
      histories.now()(turn)(loc) match
        case Cell.Empty =>
          histories.update(histories =>
            val history = histories(turn)
            val (leading, _ +: trailing) = history.splitAt(loc): @unchecked
            // this is safe because each history has exact 9 cells and loc is [0,9)
            val present = (leading :+ (if turn % 2 == 0 then Cell.O
                                       else Cell.X)) ++ trailing

            histories.take(turn + 1) :+ present
          )
          cmdBus.emit(
            CollectionCommand.ReplaceAll(
              List.tabulate(turn + 1)(i =>
                button(
                  s"jump to $i",
                  cls := "btn jump-to",
                  onClick.mapTo(Cmd.JumpTo(i)) --> obs
                )
              )
            )
          )
          turnVar.update(_ + 1)
        case _ => ()
    case Cmd.JumpTo(t) => turnVar.set(t)
  }

  val cmdBus = EventBus[ChildrenCommand]()
  val cmdStream = cmdBus.events
  val count = cmdStream.scanLeft(0)((acc, _) => acc + 1)
  val board = div(
    children <-- cells,
    div(
      children.command <-- cmdStream
    )
  )

  val layout = div(
    h1(
      "Tic Tac Toe"
    ),
    div(
      em(
        p(
          child.text <-- turnVar.signal.map(t => s"Turn $t")
        )
      ),
      p(
        child.text <-- turnVar.signal.map(t =>
          s"Next Player: ${if t % 2 == 0 then "O" else "X"}"
        )
      )
    ),
    board
    // div(
    //  boardNaive
    // )
  )
enum Cmd:
  case Choose(loc: Int)
  case JumpTo(t: Int)

object Square:
  def apply(loc: Int, cell: Cell) =
    div(
      button(
        cell.show,
        onClick
          .filter(_ => cell == Cell.Empty)
          .mapTo(Cmd.Choose(loc)) --> Game.obs,
        disabled := cell != Cell.Empty
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
