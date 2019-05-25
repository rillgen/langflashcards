package com.ludtek.autoanki.model.pons

import groovy.transform.Canonical

@Canonical
class Response {
    List<Rom> roms
    List<Translation> translations
}
