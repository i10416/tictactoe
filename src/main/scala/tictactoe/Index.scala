package tictactoe
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

@main def program =
  renderOnDomContentLoaded(
    dom.document.getElementById("app"),
    Game.layout
  )
