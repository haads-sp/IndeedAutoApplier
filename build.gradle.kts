plugins {
    id("java")
    id("application")

}

group = "com.haadlit_sp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("com.haadlit_sp.Main")
}

dependencies {
    implementation("com.microsoft.playwright:playwright:1.52.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}


tasks.test {
    useJUnitPlatform()
}
