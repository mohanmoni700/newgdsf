import play.Project._

name := "GDSService"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaCore,
  javaJdbc,
  javaEbean,
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
  "joda-time" % "joda-time" % "2.3",
  "mysql" % "mysql-connector-java" % "5.1.18",
  "org.apache.commons" % "commons-pool2" % "2.0"
)

play.Project.playJavaSettings
