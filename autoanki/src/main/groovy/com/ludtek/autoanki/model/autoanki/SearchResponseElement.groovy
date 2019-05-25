package com.ludtek.autoanki.model.autoanki

import com.ludtek.autoanki.model.pons.Translation
import groovy.transform.Canonical

@Canonical
class SearchResponseElement {
    UUID id
    ElementType type
    String desc
    Map<Integer, Translation> translations
}
