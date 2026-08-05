name := "FoodPantryInventory"
version := "0.1"
scalaVersion := "3.3.3"

libraryDependencies ++= Seq(
  "org.scalafx" %% "scalafx" % "21.0.0-R32",
  "org.scalatest" %% "scalatest" % "3.2.18" % Test,
  "org.apache.pdfbox" % "pdfbox" % "2.0.30",
  "org.xerial" % "sqlite-jdbc" % "3.45.2.0"
)

// Add OS-specific JavaFX libraries for ScalaFX
val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => 
    val arch = System.getProperty("os.arch").toLowerCase
    if (arch.contains("aarch64") || arch.contains("arm64")) "mac-aarch64" else "mac"
  case n if n.startsWith("Windows") => "win"
  case _                            => throw new Exception("Unknown platform!")
}

libraryDependencies ++= Seq("base", "controls", "fxml", "graphics", "media", "swing", "web").map(m =>
  "org.openjfx" % s"javafx-$m" % "21" classifier osName
)

scalacOptions ++= Seq(
  "-Wunused:all"
)
