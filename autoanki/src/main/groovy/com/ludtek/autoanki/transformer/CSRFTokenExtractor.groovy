package com.ludtek.autoanki.transformer

import org.ccil.cowan.tagsoup.Parser

class CSRFTokenExtractor {

    static String extractCSRFTokenFromLoginPage(InputStream body) {

        def parser = new XmlSlurper(new Parser())

        def root = parser.parse(body)

        def input = root.body.'**'.find { node ->
            node.name() == 'input' && node.@name == 'csrf_token'
        }

        input.@value.text()
    }

    static String extractCSRFTokenFromEditPage(InputStream body) {
        def parser = new XmlSlurper(new Parser())
        def root = parser.parse(body)
        def scripts = root.body.text()

        def tokenRegex = /editor.csrf_token2 = '(.+)'/
        def tokenMatcher = (scripts =~ /$tokenRegex/)

        tokenMatcher[0][1]
    }
}
