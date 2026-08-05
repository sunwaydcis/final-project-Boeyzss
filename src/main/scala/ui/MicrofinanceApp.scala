package ui

import java.nio.file.Paths
import java.time.Clock

import persistence.JsonStateStore
import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.scene.Scene

// ai-assisted: #3
// why: Codex helped create a reusable ScalaFX navigation shell while keeping each assessed screen separate.
object MicrofinanceApp extends JFXApp3:
  override def start(): Unit =
    val dataPath = Paths.get(System.getProperty("user.dir"), "data", "microfinance-state.json")
    val controller = ApplicationController(JsonStateStore(dataPath), Clock.systemDefaultZone())
    val shell = AppShell(controller)

    stage = new JFXApp3.PrimaryStage {
      title = "Microfinance Loan Tracker"
      width = 1240
      height = 780
      minWidth = 1040
      minHeight = 680
      scene = new Scene(shell) {
        fill = scalafx.scene.paint.Color.web("#F3F6FA")
        Option(getClass.getResource("/styles.css")).foreach(resource =>
          stylesheets += resource.toExternalForm
        )
      }
    }
