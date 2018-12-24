plugins {
    java
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compile("io.vertx:vertx-core:3.6.0")
    compile("io.vertx:vertx-codegen:3.6.0")
    compile("io.vertx:vertx-service-proxy:3.6.0")
    annotationProcessor("io.vertx:vertx-core:3.6.0")
    annotationProcessor("io.vertx:vertx-codegen:3.6.0:processor")
    annotationProcessor("io.vertx:vertx-service-proxy:3.6.0")
}

tasks.getByName("compileJava") {
    this as JavaCompile
    options.compilerArgs = listOf(
            "-Acodegen.output=src/main"
    )
}
tasks.getByName("jar") {
    this as Jar
    exclude("**/*.java")
}
sourceSets {
    main {
        java {
            srcDirs += srcDir("build/classes/java/main/")
        }
    }
}