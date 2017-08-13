lazy val commonSettings = Seq(
  organization := "com.example",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.3",
  cancelable in Global := true
)

lazy val root = (project in file(".")).
  settings(
    commonSettings,
    name := "Eliminator",
    libraryDependencies ++= Seq(
      "com.github.gilbertw1" %% "slack-scala-client" % "0.2.1",
      "com.github.nscala-time" %% "nscala-time" % "2.16.0"
    )
  )

