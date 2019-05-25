package com.ludtek.autoanki.transformer

import com.ludtek.autoanki.model.pons.Arab
import com.ludtek.autoanki.model.pons.Response
import com.ludtek.autoanki.model.pons.Rom
import com.ludtek.autoanki.model.pons.Translation

class PonsResponseTransformer {

    Response parse(InputStream body) {

        def slurper = new groovy.json.JsonSlurper()

        def parsed = slurper.parse(body)

        def response = new Response()

        if (parsed) {

            response.translations = parsed[0].hits.findAll { hit -> hit.type == 'translation' }.collect { translation ->
                def mtrans = new Translation()

                mtrans.source = sanitize(translation.source)
                mtrans.target = sanitize(translation.target)
                mtrans
            }

            response.roms = parsed[0].hits.findAll { hit -> hit.type == 'entry' }.roms.flatten().collect { rom ->
                def mrom = new Rom()

                mrom.wordclass = rom.wordclass
                mrom.headword = rom.headword
                mrom.description = sanitize(rom.headword_full)

                mrom.arabs = rom.arabs.collect { arab ->
                    def marab = new Arab()

                    marab.header = sanitize(arab.header)

                    marab.translations = arab.translations.collect { translation ->
                        def mtrans = new Translation()
                        mtrans.source = sanitize(translation.source)
                        mtrans.target = sanitize(translation.target)
                        mtrans
                    }
                    marab
                }
                mrom
            }
        }

        response
    }

    private static String sanitize(String text) {
        text.replaceAll(/<span class=".+?">|<\/span>?/, '').replaceAll("&#39;", "'").replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll(/<acronym title=".+?">/, "(").replaceAll(/<\/acronym>?/, ")").replaceAll(/<strong class=".+?">/, "").replaceAll(/<\/strong>/, "").replaceAll(/[\u0000-\u001f]/, "").trim()
    }
}
