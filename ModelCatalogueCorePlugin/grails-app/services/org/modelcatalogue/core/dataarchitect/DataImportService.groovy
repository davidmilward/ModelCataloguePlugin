package org.modelcatalogue.core.dataarchitect

import grails.transaction.Transactional
import org.modelcatalogue.core.Asset
import org.modelcatalogue.core.Classification
import org.modelcatalogue.core.ConceptualDomain
import org.modelcatalogue.core.DataElement
import org.modelcatalogue.core.DataType
import org.modelcatalogue.core.EnumeratedType
import org.modelcatalogue.core.ExtendibleElement
import org.modelcatalogue.core.MeasurementUnit
import org.modelcatalogue.core.Model
import org.modelcatalogue.core.PublishedElementStatus
import org.modelcatalogue.core.Relationship
import org.modelcatalogue.core.ValueDomain
import org.modelcatalogue.core.dataarchitect.xsd.*

class DataImportService {

    static transactional = false
    def publishedElementService, sessionFactory, relationshipService
    private static final QUOTED_CHARS = ["\\": "&#92;", ":" : "&#58;", "|" : "&#124;", "%" : "&#37;"]
    private static final REGEX = '(?i)MC_([A-Z0-9]{8}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{12})_\\d+'

    def importData(ArrayList headers, ArrayList rows, String name, Asset asset) {
        //get indexes of the appropriate sections
        DataImport newImporter = new DataImport(name: name, asset: asset)
        def metadataStartIndex = headers.indexOf("Metadata") + 1
        def metadataEndIndex = headers.size() - 1
        //iterate through the rows and import each line
        rows.eachWithIndex { def row, int i ->
            ImportRow importRow = populateImportRow(row, metadataStartIndex, metadataEndIndex, headers)
            newImporter.addRow(importRow)
        }
        newImporter.save(flush:true, failOnError:true)
    }

    protected void resolveRow(DataImport importer, ImportRow importRow) {
        importRow.resolveAll()
        if (!importRow.rowActions) {
            importer.removeFromPendingAction(importRow)
            importer.addToImportQueue(importRow)
        }
    }

    protected void resolveAll(DataImport importer){
        resolveAllPendingRows(importer)
        ingestImportQueue(importer)
    }

    protected void resolveAllPendingRows(DataImport importer){
        def resolveQueue = importer.pendingAction.iterator()
        while (resolveQueue.hasNext()) {
            ImportRow pendingRow = resolveQueue.next()
            pendingRow.resolveAll()
            if(pendingRow.rowActions.size()==0){
                resolveQueue.remove()
                importer.addToImportQueue(pendingRow)
            }
        }
    }

    def void ingestImportQueue(DataImport importer) {
        def queue = importer.importQueue.iterator()
        def it = 0
        while (queue.hasNext()) {
            ImportRow row = queue.next()
            if (!row.rowActions) {
                if(it<60) {
                    importer.ingestRow(row, true)
                    queue.remove()
                }else{
                    it=0
                    cleanUpGorm()
                }
            }
        }
        importer.actionPendingModels()
        importer.actionAsset()
    }

    /**
     * Clean up the session to speed up the import.
     * @see http://naleid.com/blog/2009/10/01/batch-import-performance-with-grails-and-mysql
     */
    def cleanUpGorm() {
        def propertyInstanceMap = org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin.PROPERTY_INSTANCE_MAP
        def session = sessionFactory.currentSession
        session.flush()
        session.clear()
        propertyInstanceMap.get().clear()
    }



    protected ImportRow populateImportRow(row, metadataStartIndex, metadataEndIndex, headers){
        ImportRow importRow = new ImportRow()
        importRow.classification = trim(row[0])
        importRow.parentModelCode = trim(row[1])
        importRow.parentModelName = trim(row[2])
        importRow.containingModelCode = trim(row[3])
        importRow.containingModelName = trim(row[4])
        importRow.dataElementCode = trim(row[5])
        importRow.dataElementName = trim(row[6])
        importRow.dataElementDescription =   trim(row[7])
        importRow.conceptualDomainName = trim(row[8])
        importRow.valueDomainCode = trim(row[9])
        importRow.valueDomainName = trim(row[10])
        importRow.measurementUnitName =   trim(row[11])
        importRow.measurementSymbol =   trim(row[12])
        importRow.dataType =   trim(row[13])
        importRow.rule =   trim(row[14])
        importRow.maxOccurs =   trim(row[15])
        importRow.minOccurs =   trim(row[16])
        def counter = metadataStartIndex
        def metadataColumns = [:]
        while (counter <= metadataEndIndex) {
            String key = headers[counter].toString()
            String value = (row[counter]!=null) ? row[counter].toString() : ""
            if (key!="" && key!="null") metadataColumns.put(key, value)
            counter++
        }
        importRow.metadata = (metadataColumns)?metadataColumns:null
        importRow.save(flush:true, failOnError: true)
        return importRow
    }

    def trim(rowValue){
        if(rowValue && rowValue instanceof String) rowValue = rowValue.trim()
        rowValue
    }

}
