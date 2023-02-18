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
  def check(board: Seq[Cell]) =
    val lines = Seq(
      (0, 1, 2),
      (3, 4, 5),
      (6, 7, 8),
      (0, 3, 6),
      (1, 4, 7),
      (2, 5, 8),
      (0, 4, 8),
      (2, 4, 6)
    )
    lines.find { (a, b, c) =>
      (board(a), board(b), board(c)) match
        case (Cell.O, Cell.O, Cell.O) | (Cell.X, Cell.X, Cell.X) => true
        case _                                                   => false
    }.isDefined
  val turnVar = Var(0)
  val wonBy: Var[Option[Boolean]] = Var(None)
  val histories = Var(Seq(Seq.fill(9)(Cell.Empty)))
  val mat = Var(Seq.fill(9)(Cell.Empty))

  val cells = turnVar.signal
    .withCurrentValueOf(histories.signal)
    .withCurrentValueOf(wonBy)
    .map((turn, boards, winner) =>
      boards(turn).zipWithIndex
        .map((cell, loc) => Square(loc, cell, winner))
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
      val last = histories.now()(turn)
      last(loc) match
        case Cell.Empty =>
          val (leading, _ +: trailing) = last.splitAt(loc): @unchecked
          val present = (leading :+ (if turn % 2 == 0 then Cell.O
                                     else Cell.X)) ++ trailing
          histories.update(_.take(turn + 1) :+ present)
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
          if check(present) then wonBy.set(Some(turn % 2 == 0))
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
        child.text <-- turnVar.signal
          .withCurrentValueOf(wonBy)
          .map((t, w) =>
            w match
              case None =>
                s"Next Player: ${if t % 2 == 0 then "O" else "X"}"
              case Some(value) =>
                if value then "Winner: O" else "Winner X"
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
  def apply(loc: Int, cell: Cell, winner: Option[Boolean]) =
    div(
      button(
        cell.show,
        onClick
          .filter(_ => cell == Cell.Empty && winner.isEmpty)
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
