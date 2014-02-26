package uk.co.mc.core.util

import grails.converters.JSON
import groovy.util.logging.Log
import groovy.util.slurpersupport.GPathResult
import groovy.xml.XmlUtil
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Created by adammilward on 24/02/2014.
 */
@Log
class ResultRecorder {

    /**
    /**
     * Records the given json as fixture returning the file created or updated.
     *
     * The json will be available as <code>fixtures.resourceName.fixtureName</code> variable.
     *
     * @param fixtureName name of the fixture variable and the file holding it as well
     * @param json json to be saved to the fixture
     */

    private static final String XMLFILEPATH = "../ModelCatalogueCorePlugin/target/xml-samples/modelcatalogue/core"
    private static final String JSONFILEPATH = "../ModelCatalogueCorePlugin/test/js/modelcatalogue/core"


    protected static File recordResult(String fixtureName, JSONElement json, String domainClass) {
        File fixtureFile = newFixture(JSONFILEPATH, "${domainClass}/fixtures", fixtureName, "gen.fixture.js")
        fixtureFile.parentFile.mkdirs()
        fixtureFile.text = getFixtureText(domainClass, fixtureName, new JSON(json).toString(true))
        ////log.info "New fixture file created at $fixtureFile.canonicalPath"
        fixtureFile
    }

    protected static File recordInputJSON(String fixtureName, Map json, String domainClass) {
        File fixtureFile = newFixture(JSONFILEPATH, "${domainClass}/fixtures", fixtureName, "gen.fixture.js")
        fixtureFile.parentFile.mkdirs()
        fixtureFile.text = getFixtureText(domainClass, fixtureName, new JSON(json).toString(true))
        //log.info "New fixture file created at $fixtureFile.canonicalPath"
        fixtureFile
    }

    protected static File recordInputJSON(String fixtureName, String json, String domainClass) {
        File fixtureFile = newFixture(JSONFILEPATH, "${domainClass}/fixtures", fixtureName, "gen.fixture.js")
        fixtureFile.text = getFixtureText(domainClass, fixtureName, json)
        //log.info "New fixture file created at $fixtureFile.canonicalPath"
        fixtureFile

    }

    /**
     * Records the given xml text as fixture returning the file created or updated.
     *
     * @param fixtureName name of the fixture variable and the file holding it as well
     * @param xml xml to be saved to the fixture
     */
    protected static File recordResult(String fixtureName, GPathResult xml, String controller) {
        File fixtureFile = newFixture(XMLFILEPATH, controller, fixtureName, "gen.xml")
        fixtureFile.text = getXmlFixtureText(xml)
        //log.info "New xml file created at $fixtureFile.canonicalPath"
        fixtureFile
    }

    protected static File recordInputXML(String fixtureName, String xml, String controller) {
        File fixtureFile = newFixture(XMLFILEPATH, controller, fixtureName, "gen.xml")
        fixtureFile.text = getXmlFixtureText(xml)
        //log.info "New xml file created at $fixtureFile.canonicalPath"
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
