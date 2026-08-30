plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

tasks.register<JavaExec>("runRuleEngineTests") {
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.contextreminder.core.RuleEngineTestKt")
}
