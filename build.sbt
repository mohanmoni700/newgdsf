name := """GDSService"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs,
  "com.h2database" % "h2" % "1.3.168",
  "org.springframework" % "spring-core" % "3.2.1.RELEASE",
  "org.springframework" % "spring-context" % "3.2.1.RELEASE",
  "org.springframework" % "spring-orm" % "3.2.1.RELEASE",
  "org.springframework" % "spring-jdbc" % "3.2.1.RELEASE",
  "org.springframework" % "spring-tx" % "3.2.1.RELEASE",
  "org.springframework" % "spring-expression" % "3.2.1.RELEASE",
  "org.springframework" % "spring-aop" % "3.2.1.RELEASE",
  "org.springframework" % "spring-aspects" % "3.2.1.RELEASE",
  "org.springframework.data" % "spring-data-redis" % "1.3.2.RELEASE",
  "org.aspectj" % "aspectjrt" % "1.6.11",
  "org.aspectj" % "aspectjweaver" % "1.6.10",
  "org.springframework" % "spring-test" % "3.2.1.RELEASE" % "test",
  "org.hibernate" % "hibernate-entitymanager" % "4.1.9.Final",
  "cglib" % "cglib" % "2.2.2",
  "redis.clients" % "jedis" % "2.4.2",
  "org.pojomatic" % "pojomatic" % "1.0",
  "joda-time" % "joda-time" % "2.9.4",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.3.0",
  "mysql" % "mysql-connector-java" % "5.1.18",
  "org.apache.commons" % "commons-pool2" % "2.0",
  "org.apache.axis2" % "axis2" % "1.6.2",
  "org.apache.axis2" % "axis2-kernel" % "1.6.2",
  "org.apache.axis2" % "axis2-transport-http" % "1.6.2",
  "org.apache.axis2" % "axis2-transport-local" % "1.6.2",
  "org.apache.axis2" % "axis2-xmlbeans" % "1.6.2",
  "com.thoughtworks.xstream" % "xstream" % "1.4.7",
  "com.sun.xml.ws" % "jaxws-rt" % "2.3.3"
  "com.squareup.okhttp3" % "logging-interceptor" % "4.12.0"
)

TaskKey[Unit]("stop") := {
  val pidFile = target.value / "universal" / "stage" / "RUNNING_PID"
  if (!pidFile.exists) throw new Exception("App not started!")
  val pid = IO.read(pidFile)
  s"kill $pid".!
  println(s"Stopped application with process ID $pid")
}
