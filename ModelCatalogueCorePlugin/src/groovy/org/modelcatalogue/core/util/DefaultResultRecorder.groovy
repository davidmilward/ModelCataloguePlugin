package org.modelcatalogue.core.util

import grails.converters.JSON
import grails.converters.XML
import groovy.util.slurpersupport.GPathResult
import groovy.xml.XmlUtil
import org.codehaus.groovy.grails.web.json.JSONElement

class DefaultResultRecorder implements ResultRecorder {

    public static final ResultRecorder NULL_RECOREDER = new ResultRecorder() {
        @Override
        File recordResult(String fixtureName, JSONElement json) {
            return null
        }

        @Override
        File recordResult(String fixtureName, GPathResult xml) {
            return null
        }

        @Override
        File recordInputJSON(String fixtureName, Map json) {
            return null
        }

        @Override
        File recordInputJSON(String fixtureName, String json) {
            return null
        }

        @Override
        File recordInputXML(String fixtureName, String xml) {
            return null
        }

        @Override
        File recordInputXML(String fixtureName, Map xml) {
            return null
        }

        @Override
        File recordInputXML(String fixtureName, XML xml) {
            return null
        }
    }

    private final String xmlFixturesRoot        // = "../ModelCatalogueCorePlugin/target/xml-samples/modelcatalogue/core"
    private final String jsonFixturesRoot       // = "../ModelCatalogueCorePlugin/test/js/modelcatalogue/core"
    private final String domainClass
    private String jsonFixturesSuffix   = "gen.fixture.js"
    private String xmlFixturesSuffix    = "gen.xml"

    private DefaultResultRecorder(String xmlFixturesRoot, String jsonFixturesRoot, String domainClass) {
        this.xmlFixturesRoot = xmlFixturesRoot
        this.jsonFixturesRoot = jsonFixturesRoot
        this.domainClass = domainClass
    }

    static ResultRecorder create(String xmlRoot, String jsonRoot, String domainClass) {
        String domainClassFilter = System.getProperty('record.domain')
        if (domainClassFilter && !(domainClass.matches(domainClassFilter))) {
            return NULL_RECOREDER
        }
        return new DefaultResultRecorder(xmlRoot, jsonRoot, domainClass)

    }

    File recordResult(String fixtureName, JSONElement json) {
        File fixtureFile = newFixture(jsonFixturesRoot, "${domainClass}/fixtures", fixtureName, jsonFixturesSuffix)
        fixtureFile.text = getFixtureText(domainClass, fixtureName, new JSON(json).toString(true))
        fixtureFile
    }

    File recordInputJSON(String fixtureName, Map json) {
        File fixtureFile = newFixture(jsonFixturesRoot, "${domainClass}/fixtures", fixtureName, jsonFixturesSuffix)
        fixtureFile.text = getFixtureText(domainClass, fixtureName, new JSON(json).toString(true))
        fixtureFile
    }

    File recordInputJSON(String fixtureName, String json) {
        File fixtureFile = newFixture(jsonFixturesRoot, "${domainClass}/fixtures", fixtureName, jsonFixturesSuffix)
        fixtureFile.text = getFixtureText(domainClass, fixtureName, json)
        fixtureFile

    }

    /**
     * Records the given xml text as fixture returning the file created or updated.
     *
     * @param fixtureName name of the fixture variable and the file holding it as well
     * @param xml xml to be saved to the fixture
     */
    File recordResult(String fixtureName, GPathResult xml) {
        File fixtureFile = newFixture(xmlFixturesRoot, domainClass, fixtureName, xmlFixturesSuffix)
        fixtureFile.text = getXmlFixtureText(xml)
        fixtureFile
    }

    File recordInputXML(String fixtureName, String xml) {
        File fixtureFile = newFixture(xmlFixturesRoot, domainClass, fixtureName, xmlFixturesSuffix)
        fixtureFile.text = getXmlFixtureText(xml)
        fixtureFile
    }

    File recordInputXML(String fixtureName, Map xml) {
        File fixtureFile = newFixture(xmlFixturesRoot, domainClass, fixtureName, xmlFixturesSuffix)
        fixtureFile.text = getXmlFixtureText(new XML(xml).toString())
        fixtureFile
    }

    File recordInputXML(String fixtureName, XML xml) {
        File fixtureFile = newFixture(xmlFixturesRoot, domainClass, fixtureName, xmlFixturesSuffix)
        fixtureFile.text = getXmlFixtureText(xml.toString())
        fixtureFile
    }



    private static File newFixture(String filePath, prefix, fixtureName, suffix){

        def fixtureFile =  new File("${filePath}/${prefix}/${fixtureName}.${suffix}")
        fixtureFile.parentFile.mkdirs()
        return fixtureFile
    }

    private static String getXmlFixtureText(String xml){

        return """${
            xml.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        }"""

    }

    private static String getXmlFixtureText(GPathResult xml){

        return """${
            XmlUtil.serialize(xml).replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        }"""

    }

    private static String getFixtureText(String domainClass, String fixtureName, String json){
        return """/** Generated automatically from $domainClass. Do not edit this file manually! */
    (function (window) {
        window['fixtures'] = window['fixtures'] || {};
        var fixtures = window['fixtures']
        fixtures['$domainClass'] = fixtures['$domainClass'] || {};
        var $domainClass = fixtures['$domainClass']

        window.fixtures.$domainClass.$fixtureName = $json

    })(window)"""
        }

}
