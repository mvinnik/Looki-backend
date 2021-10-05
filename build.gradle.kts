import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm") version "1.5.31"
    id("com.google.protobuf") version "0.8.17"
}

group = "one.looki.backend.grpc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main")
        }
    }
}

val grpcVersion = "1.41.0"
val grpcKotlinVersion = "1.1.0"
val protobufVersion = "3.18.0"
val coroutinesVersion = "1.5.2"

dependencies {
    // Core
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")

    // Grpc
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")

    // SLF4J
    implementation("org.slf4j:slf4j-simple:1.7.32")

    // MongoDb
    implementation("org.mongodb:mongodb-driver-reactivestreams:4.3.2")

    // JWT
    implementation("com.auth0:java-jwt:3.18.2")
    implementation("com.google.api-client:google-api-client:1.31.5")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk7@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}