plugins {
  java
}

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  compile("io.vertx:vertx-core:3.6.2")
  compile("io.vertx:vertx-codegen:3.6.2")
  compile("io.vertx:vertx-service-proxy:3.6.2")
  annotationProcessor("io.vertx:vertx-core:3.6.2")
  annotationProcessor("io.vertx:vertx-codegen:3.6.2:processor")
  annotationProcessor("io.vertx:vertx-service-proxy:3.6.2")
}
val compileJava = tasks.named<JavaCompile>("compileJava")
compileJava {
  options.annotationProcessorGeneratedSourcesDirectory = file("build/generated")
  options.compilerArgs = listOf(
    "-Acodegen.output=src/main"
  )
}
val jar = tasks.named<Jar>("jar")
jar {
  exclude("**/*.java")
}
sourceSets {
  main {
    java {
      srcDirs += srcDir("build/generated")
    }
  }
}
