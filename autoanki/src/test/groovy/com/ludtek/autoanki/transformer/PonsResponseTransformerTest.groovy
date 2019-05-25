package com.ludtek.autoanki.transformer

import spock.lang.Specification;

class PonsResponseTransformerTest extends Specification {
	
	def testParsePonsResponse1() {
		given:
			def transformer = new PonsResponseTransformer()
			def json = this.getClass().getResourceAsStream('/com/ludtek/autoanki/transformer/PonsResponse1.json')
		
		when:
			def response = transformer.parse(json)
		
		then:
			response != null
			response.translations.size() == 0
			response.roms.size() == 7
			response.roms[0].arabs.size() == 14			
			response.roms[0].description == 'Haus <-es, Häuser> [haus, (pl) ˈhɔyzɐ] (NOUN) (nt)'
			response.roms[0].wordclass == 'noun'
		
	}
	
	def testParsePonsResponse2() {
		given:
			def transformer = new PonsResponseTransformer()
			def json = this.getClass().getResourceAsStream('/com/ludtek/autoanki/transformer/PonsResponse2.json')
		
		when:
			def response = transformer.parse(json)
		
		then:
			response != null
			response.translations.size() == 1
			response.roms.size() == 0
			response.translations[0].source == 'they were brothers but quite different in kind'
			response.translations[0].target == 'sie waren Brüder, aber in ihrem Wesen ganz verschieden'
		
	}

}
