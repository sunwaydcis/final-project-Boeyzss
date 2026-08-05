ThisBuild / scalaVersion := "3.3.6"
ThisBuild / version := "1.0.0"
ThisBuild / organization := "edu.sunway.prg2104"

lazy val javaFxVersion = "21.0.7"

lazy val javaFxClassifier = {
  val operatingSystem = System.getProperty("os.name").toLowerCase
  val architecture = System.getProperty("os.arch").toLowerCase
  val armSuffix = if (architecture == "aarch64" || architecture == "arm64") "-aarch64" else ""

  if (operatingSystem.contains("mac")) s"mac$armSuffix"
  else if (operatingSystem.contains("win")) "win"
  else if (operatingSystem.contains("linux")) s"linux$armSuffix"
  else sys.error(s"Unsupported operating system: $operatingSystem ($architecture)")
}

lazy val javaFxModules = Seq("base", "graphics", "controls")

lazy val root = (project in file("."))
  .settings(
    name := "microfinance-loan-tracker",
    Compile / mainClass := Some("ui.MicrofinanceApp"),
    Compile / run / fork := true,
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Wunused:all",
      "-release:21"
    ),
    javacOptions ++= Seq("--release", "21"),
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "21.0.0-R32",
      "com.lihaoyi" %% "upickle" % "4.4.3",
      "org.scalameta" %% "munit" % "1.0.2" % Test
    ),
    libraryDependencies ++= javaFxModules.map(moduleName =>
      "org.openjfx" % s"javafx-$moduleName" % javaFxVersion classifier javaFxClassifier
    ),
    Test / parallelExecution := false
  )
