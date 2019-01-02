plugins {
  java
}

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  compile("io.vertx:vertx-core:3.6.2")
  compile("io.vertx:vertx-codegen:3.6.2:processor")
  compile("io.vertx:vertx-service-proxy:3.6.2")
}
task<JavaCompile>("annotationProcessing") {
  source = sourceSets["main"].java
  classpath = configurations.compile.get() + configurations.compileOnly.get()
  destinationDir = project.file("build/generated")
  options.annotationProcessorPath = classpath
  options.compilerArgs = listOf(
    "-proc:only",
    "-processor", "io.vertx.codegen.CodeGenProcessor",
    "-Acodegen.output=src/main"
  )
}

tasks.getByName("compileJava").dependsOn("annotationProcessing")

sourceSets {
  main {
    java {
      srcDir("build/generated")
    }
  }
}