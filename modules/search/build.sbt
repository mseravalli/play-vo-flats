import play.sbt.PlayImport.guice

name := """search"""

libraryDependencies ++= Seq("org.mongodb" % "mongo-java-driver" % "3.4.1",ws,guice)