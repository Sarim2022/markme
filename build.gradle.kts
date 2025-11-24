// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // These plugins are used by sub-modules and must be defined here with apply false.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // Google Services and Firebase App Distribution plugins with up-to-date versions
    // apply false means they are declared here, but not applied to the root project itself.
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.appdistribution") version "5.2.0" apply false
}


buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
    }
}