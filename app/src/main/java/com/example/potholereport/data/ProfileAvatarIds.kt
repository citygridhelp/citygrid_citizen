package com.example.potholereport.data

object ProfileAvatarIds {
    const val NEUTRAL = "neutral"
    const val MAN = "man"
    const val WOMAN = "woman"
    const val YOUTH = "youth"
    const val ELDER = "elder"

    const val STUDENT = "student"
    const val PILOT = "pilot"
    const val CHEF = "chef"
    const val ARTIST = "artist"
    const val SPORT = "sport"
    const val GLASSES = "glasses"
    const val CURLY = "curly"
    const val PONYTAIL = "ponytail"
    const val BEANIE = "beanie"
    const val BRAIDS = "braids"
    const val WORKER = "worker"
    const val COOL = "cool"
    const val BLOSSOM = "blossom"
    const val WAVE = "wave"
    const val SUNSET = "sunset"
    const val VIOLET = "violet"
    const val CORAL = "coral"
    const val FOREST = "forest"
    const val AMBER = "amber"
    const val SLATE = "slate"

    const val NEON = "neon"
    const val MOHAWK = "mohawk"
    const val HEADPHONES = "headphones"
    const val STAR = "star"
    const val CYBER = "cyber"
    const val DISCO = "disco"
    const val GRAFFITI = "graffiti"
    const val FLAME = "flame"
    const val GALAXY = "galaxy"
    const val CHROME = "chrome"

    val ALL = setOf(
        NEUTRAL, MAN, WOMAN, YOUTH, ELDER,
        STUDENT, PILOT, CHEF, ARTIST, SPORT,
        GLASSES, CURLY, PONYTAIL, BEANIE, BRAIDS,
        WORKER, COOL, BLOSSOM, WAVE, SUNSET,
        VIOLET, CORAL, FOREST, AMBER, SLATE,
        NEON, MOHAWK, HEADPHONES, STAR, CYBER,
        DISCO, GRAFFITI, FLAME, GALAXY, CHROME,
    )

    val ORDERED = listOf(
        NEON, CYBER, GALAXY, MOHAWK, DISCO,
        HEADPHONES, STAR, GRAFFITI, FLAME, CHROME,
        COOL, ARTIST, WOMAN, MAN, YOUTH,
        GLASSES, CURLY, PONYTAIL, BEANIE, BRAIDS,
        SUNSET, WAVE, BLOSSOM, VIOLET, CORAL,
        STUDENT, SPORT, CHEF, PILOT, WORKER,
        NEUTRAL, ELDER, FOREST, AMBER, SLATE,
    )
}
