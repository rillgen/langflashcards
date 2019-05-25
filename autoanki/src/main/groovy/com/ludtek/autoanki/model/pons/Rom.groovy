package com.ludtek.autoanki.model.pons

import groovy.transform.Canonical

@Canonical
class Rom {
    String headword
    String description
    String wordclass
    List<Arab> arabs
}
