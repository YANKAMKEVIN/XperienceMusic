package com.xperiencemusic.shared.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccordsAppPageSourceTest {
    @Test
    fun `lit le ChordPro passé en argument de la fonction Nuxt`() {
        val html = """<script>window.__NUXT__=(function(a,b,c,d,e){return {data:[{title:"T (Do)",body:d,url:"/9-x"}],""" +
            """state:{partition:{partition:{title:a,chordpro:d,numero:"JEM9"}}}}}("Titre",false,9,"{t: Titre}\n[C]Gloire \"à\" toi/","x"));</script>"""
        assertEquals("{t: Titre}\n[C]Gloire \"à\" toi/", extractNuxtChordPro(html))
    }

    @Test
    fun `lit aussi un ChordPro écrit directement dans les données`() {
        val html = """window.__NUXT__=(function(a){return {state:{chordpro:"{t: Seul}\n[G]Amen"}}}(1));"""
        assertEquals("{t: Seul}\n[G]Amen", extractNuxtChordPro(html))
    }

    @Test
    fun `une page sans données Nuxt ne donne rien`() {
        assertNull(extractNuxtChordPro("<html><body>Liste des chants</body></html>"))
    }
}
