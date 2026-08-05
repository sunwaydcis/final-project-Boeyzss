ThisBuild / scalaVersion := "3.3.3"
ThisBuild / version      := "0.1.0"

// Detect OS classifier for JavaFX native libraries.
// ai-assisted: #1
// why: Asked AI for a cross-platform way to pick the correct JavaFX
// "classifier" (win/mac/linux) automatically instead of hard-coding one.
// Verified by running `sbt run` on Windows and confirming it launched.
lazy val osName = System.getProperty("os.name").toLowerCase match {
  case n if n.contains("mac")   => "mac"
  case n if n.contains("win")   => "win"
  case _                        => "linux"
}

lazy val javaFXModules = Seq("base", "controls", "fxml", "graphics")

lazy val root = (project in file("."))
  .settings(
    name := "MicrofinanceLoanTracker",

    libraryDependencies += "org.scalafx" %% "scalafx" % "21.0.0-R32",

    libraryDependencies ++= javaFXModules.map { m =>
      "org.openjfx" % s"javafx-$m" % "21.0.2" classifier osName
    },

    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,

    Compile / mainClass := Some("loantracker.ui.MainApp"),

    // Rubric requirement: -Wunused clean compile.
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Wunused:all"
    ),

    Test / fork := true,
    run / fork  := true
  )
