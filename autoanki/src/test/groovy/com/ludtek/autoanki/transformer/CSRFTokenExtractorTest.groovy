package com.ludtek.autoanki.transformer

import spock.lang.Specification;

class CSRFTokenExtractorTest extends Specification {
	
	def testExtractCSRFTFromLoginPage() {
		given:
			def transformer = new CSRFTokenExtractor()
			def html = this.getClass().getResourceAsStream('/com/ludtek/autoanki/transformer/LoginPage.html')
			
		when:
			def output = transformer.extractCSRFTokenFromLoginPage(html)
		
		then:
			output != null
			output == 'eyJpcCI6ICIyMDAuMC4yMzAuMjM1IiwgImlhdCI6IDE0ODQ2NTk5NzIsICJvcCI6ICJsb2dpbiJ9.1xNHAD9KK9JH1fV8zBz8Yzc4skToFgduKEoo9bWFrhE'			
		
	}
	
	def testExtractCSRFTFromEditPage() {
		given:
			def transformer = new CSRFTokenExtractor()
			def html = this.getClass().getResourceAsStream('/com/ludtek/autoanki/transformer/EditCard.html')
			
		when:
			def output = transformer.extractCSRFTokenFromEditPage(html)
		
		then:
			output != null
			output == 'eyJpYXQiOiAxNDg3NTMzMzU3LCAidWlkIjogIjgzMTVmMjJlIiwgIm9wIjogImVkaXQifQ.DXV2wlAgxowm408dBWLmMZNZc8hxcykqbTucJ-Zg_xQ'
		
	}
	
	def testExtractCSRFTFromEdit2Page() {
		given:
			def transformer = new CSRFTokenExtractor()
			def html = this.getClass().getResourceAsStream('/com/ludtek/autoanki/transformer/EditCard2.html')
			
		when:
			def output = transformer.extractCSRFTokenFromEditPage(html)
		
		then:
			output != null
			output == 'eyJpYXQiOiAxNDg3NzA5NjMwLCAidWlkIjogIjgzMTVmMjJlIiwgIm9wIjogImVkaXQifQ.Z97TIcp0tqT6zTZLLXHOEmucjaXTrJm74GKRtAgaexE'
		
	}

}
