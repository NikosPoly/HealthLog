package com.example.healthlog.ui.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationParityTest {

    @Test
    fun verifyStringParityBetweenEnglishAndGreek() {
        val enFileCandidates = listOf(
            File("app/src/main/res/values/strings.xml"),
            File("src/main/res/values/strings.xml")
        )
        val enFile = enFileCandidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Could not find values/strings.xml in ${enFileCandidates.map { it.absolutePath }}")

        val elFileCandidates = listOf(
            File("app/src/main/res/values-el/strings.xml"),
            File("src/main/res/values-el/strings.xml")
        )
        val elFile = elFileCandidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Could not find values-el/strings.xml in ${elFileCandidates.map { it.absolutePath }}")

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()

        val enDoc = dBuilder.parse(enFile)
        val elDoc = dBuilder.parse(elFile)

        // Verify <string> tags
        val enStrings = mutableMapOf<String, Boolean>() // key to translatable flag
        val enStringNodes = enDoc.getElementsByTagName("string")
        for (i in 0 until enStringNodes.length) {
            val node = enStringNodes.item(i) as Element
            val name = node.getAttribute("name")
            val translatable = if (node.hasAttribute("translatable")) {
                node.getAttribute("translatable").toBoolean()
            } else {
                true
            }
            enStrings[name] = translatable
        }

        val elStrings = mutableSetOf<String>()
        val elStringNodes = elDoc.getElementsByTagName("string")
        for (i in 0 until elStringNodes.length) {
            val node = elStringNodes.item(i) as Element
            val name = node.getAttribute("name")
            elStrings.add(name)
        }

        val missingInEl = enStrings.filter { (name, translatable) ->
            translatable && !elStrings.contains(name)
        }.keys

        val missingInEn = elStrings.filter { !enStrings.containsKey(it) }

        assertTrue(
            "The following translatable strings in values/strings.xml are missing in values-el/strings.xml: $missingInEl",
            missingInEl.isEmpty()
        )

        assertTrue(
            "The following strings in values-el/strings.xml do not exist in values/strings.xml: $missingInEn",
            missingInEn.isEmpty()
        )

        // Verify <plurals> tags
        val enPlurals = mutableSetOf<String>()
        val enPluralNodes = enDoc.getElementsByTagName("plurals")
        for (i in 0 until enPluralNodes.length) {
            val node = enPluralNodes.item(i) as Element
            enPlurals.add(node.getAttribute("name"))
        }

        val elPlurals = mutableSetOf<String>()
        val elPluralNodes = elDoc.getElementsByTagName("plurals")
        for (i in 0 until elPluralNodes.length) {
            val node = elPluralNodes.item(i) as Element
            elPlurals.add(node.getAttribute("name"))
        }

        val missingPluralsInEl = enPlurals.filter { !elPlurals.contains(it) }
        val missingPluralsInEn = elPlurals.filter { !enPlurals.contains(it) }

        assertTrue(
            "Plurals in values/strings.xml missing in values-el/strings.xml: $missingPluralsInEl",
            missingPluralsInEl.isEmpty()
        )
        assertTrue(
            "Plurals in values-el/strings.xml missing in values/strings.xml: $missingPluralsInEn",
            missingPluralsInEn.isEmpty()
        )
    }
}
