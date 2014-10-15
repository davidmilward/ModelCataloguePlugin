package org.modelcatalogue.core.dataarchitect

import org.modelcatalogue.core.Asset
import org.modelcatalogue.core.Classification
import org.modelcatalogue.core.ConceptualDomain
import org.modelcatalogue.core.DataType
import org.modelcatalogue.core.EnumeratedType
import org.modelcatalogue.core.ExtendibleElement
import org.modelcatalogue.core.MeasurementUnit
import org.modelcatalogue.core.Model
import org.modelcatalogue.core.DataElement
import org.modelcatalogue.core.PublishedElement
import org.modelcatalogue.core.PublishedElementStatus
import org.modelcatalogue.core.Relationship
import org.modelcatalogue.core.RelationshipService
import org.modelcatalogue.core.ValueDomain

class DataImport {

    def publishedElementService, relationshipService
    String name
    Collection<Model> models = []
    Collection<DataElement> updatedDataElements = []
    Collection<String> messages = []
    Asset asset
    Set pendingAction = []
    Set importQueue = []
    Set imported = []

    // -- options
//    Boolean createModelsForElements
    // --
    Collection<Classification> classifications = []
    Collection<ConceptualDomain> conceptualDomains = []

//    List<Model> circularModels = []
//    List<Model> modelsCreated = []
    List<DataElement> elementsCreated = []

    static hasMany = [pendingAction: ImportRow, importQueue: ImportRow, imported: ImportRow]

    static constraints = {
        name nullable: false
        imported nullable: true
        pendingAction nullable: true
        importQueue nullable: true
    }


    protected addRow(ImportRow row){
        if(!pendingAction.contains(row) && !importQueue.contains(row)) row.validateAndActionRow()
        addToPendingAction(row)
    }


    def void ingestRow(ImportRow row, Boolean bulkIngest = false) {
        if(row.rowActions.isEmpty()) {
            ConceptualDomain conceptualDomain = importConceptualDomain(row.conceptualDomainName, row.conceptualDomainDescription)
            Classification classification = importClassification(row.classification)
            if(!row.dataElementName){
                //only need to import the model information
                importModels(row)
            }else {
                DataType dataType = (row.dataType) ? importDataType(row.dataElementName, row.dataType) : null
                Model model = importModels(row, conceptualDomain, classification)
                MeasurementUnit measurementUnit = importMeasurementUnit(row)
                if (dataType || measurementUnit) {
                    //need to import value domain stuff as well
                    importDataElementAndValueDomain(row, model, dataType, measurementUnit, conceptualDomain, classification)
                } else {
                    //doesn't have a value domain so easy
                    importDataElement(row, model, conceptualDomain, classification)
                }
            }
            if(!bulkIngest) removeFromImportQueue(row)
            row.imported = true
            addToImported(row)
        }
    }


    protected ConceptualDomain importConceptualDomain(String name, String description) {
        ConceptualDomain conceptualDomain = conceptualDomains.find{it.name==name}
        if(!conceptualDomain){
            conceptualDomain = ConceptualDomain.findByName(name)
            if (!conceptualDomain) { conceptualDomain = new ConceptualDomain(name: name, description: description).save() }
            addConceptualDomainToImport(conceptualDomain)
        }
        return conceptualDomain
    }

    protected Classification importClassification(String name) {
        Classification classification = classifications.find{it.name==name}
        if(!classification) {
            classification = Classification.findByName(name)
            if (!classification) classification = new Classification(name: name).save()
            addClassificationToImport(classification)
        }
        return classification
    }

    protected void addConceptualDomainToImport(ConceptualDomain conceptualDomain){
        if(!conceptualDomains.contains(conceptualDomain)) conceptualDomains.add(conceptualDomain)
    }

    protected void addClassificationToImport(Classification classification){
        if(!classifications.contains(classification)) classifications.add(classification)
    }

    def importModels(ImportRow row, ConceptualDomain conceptualDomain, Classification classification) {

        String parentCode = row.parentModelCode
        String parentName = row.parentModelName
        String modelCode =  row.containingModelCode
        String modelName =  row.containingModelName
        Model model = Model.findByModelCatalogueId(modelCode)
        Model parentModel = Model.findByModelCatalogueId(parentCode)

        //if there are no models or an id hasn't been specified then try to match the model
        if (!model) { model = matchOrCreateModel([name: modelName, modelCatalogueId: (modelCode) ? modelCode : null], conceptualDomain, classification)
        }else{ updateModel(model, modelName) }

        if(!parentModel){ parentModel = matchOrCreateModel([name: parentName, modelCatalogueId: (parentCode) ? parentCode : null], conceptualDomain, classification)
        }else{updateModel( parentModel, parentName) }

        if(model){ model = addModelToImport(model) }
        if(parentModel){ parentModel = addModelToImport(parentModel) }
        if(model && parentModel) {model.addToChildOf(parentModel) }
        return model
    }

    protected Model matchOrCreateModel(Map modelParams, ConceptualDomain conceptualDomain, Classification classification) {
        //check cache of models to see if it has already been created
        Model model = models.find{it.name == modelParams.name}
        if(!model && modelParams.name){
            model = createModel(modelParams, classification, conceptualDomain)
        }else{
            model.addToClassifications(classification)
        }
        return model
    }

    def updateModel(Model model, String name){
        if(name && name!=model.name){
            model.name = name
            model.status = PublishedElementStatus.UPDATED
            model.save()
        }
    }

    protected createModel(Map modelParams, Classification classification, ConceptualDomain conceptualDomain){
        Model model = new Model(modelParams)
        model.modelCatalogueId = modelParams.modelCatalogueId
        model.save()
        model.addToClassifications(classification)
        model.addToHasContextOf(conceptualDomain)
        return model
    }

    protected Model addModelToImport(Model model) {
        if (!models.find{it.id == model.id}) {
            models.add(model)
        }
        return model
    }

    protected MeasurementUnit importMeasurementUnit(ImportRow row) {
        MeasurementUnit mu
        if(row.measurementUnitName && row.measurementSymbol) { mu = MeasurementUnit.findByNameAndSymbol(row.measurementUnitName, row.measurementSymbol)
        }else {
            if (!mu) mu = MeasurementUnit.findBySymbol(row.measurementSymbol)
            if (!mu) mu = MeasurementUnit.findByNameIlike(row.measurementUnitName)

        }
        if(!mu && row.measurementUnitName) mu = new MeasurementUnit(name: row.measurementUnitName, symbol: row.measurementSymbol).save()
        return mu
    }

    protected DataType importDataType(String name, String data) {
        DataType dataTypeReturn = createOrMatchEnumeratedType(data, name)
        if(!dataTypeReturn) dataTypeReturn = DataType.findByName(data)
        if(!dataTypeReturn) dataTypeReturn = DataType.findByNameIlike(data)
        return dataTypeReturn
    }


    protected DataElement importDataElementAndValueDomain(ImportRow row, Model model, DataType dataType, MeasurementUnit measurementUnit, ConceptualDomain conceptualDomain, Classification classification) {

        //find out if data element exists using unique code
        DataElement dataElement = DataElement.findByModelCatalogueId(row.dataElementCode)
        if (dataElement) dataElement = updateDataElementAndValueDomain(row, dataType, measurementUnit, model, dataElement, conceptualDomain, classification)

        if (!dataElement) {
            dataElement = new DataElement(name: row.dataElementName, description: row.dataElementDescription, status: PublishedElementStatus.FINALIZED)
            dataElement.modelCatalogueId = row.dataElementCode
            ValueDomain valueDomain = (measurementUnit)?importValueDomain(row, conceptualDomain, dataType, measurementUnit) : importValueDomain(row, conceptualDomain, dataType)
            dataElement.valueDomain = valueDomain
            dataElement.save()
            dataElement = updateMetadata(row.metadata, dataElement)
            dataElement.addToClassifications(classification)
            addModelToImport(model)
            addUpdatedDataElements(dataElement, model, conceptualDomain, getRelationMetadata(row))

        }

        return dataElement
    }


    protected Map getRelationMetadata(ImportRow row){
        Map relationMetadata  = [:]
        if(row.minOccurs) relationMetadata.put("MinOccurs", row.minOccurs)
        if(row.maxOccurs) relationMetadata.put("MaxOccurs", row.maxOccurs)
        return relationMetadata
    }

    protected DataElement updateDataElementAndValueDomain(ImportRow row, DataType dataType, MeasurementUnit measurementUnit, DataElement dataElement,  Model model, ConceptualDomain conceptualDomain, Classification classification) {
        Boolean dataElementChanged = checkDataElementForChanges(row, dataElement, classification)
        Boolean valueDomainChanged = checkValueDomainForChanges(row, dataType, measurementUnit, dataElement.valueDomain, conceptualDomain)

        if (dataElementChanged || valueDomainChanged) {

            if(model.status!=PublishedElementStatus.UPDATED && model.status!=PublishedElementStatus.DRAFT){
                model.status = PublishedElementStatus.UPDATED
                model.save()
            }

            addModelToImport(model)
            publishedElementService.archiveAndIncreaseVersion(dataElement)
            dataElement.refresh()

            if(dataElementChanged) {
                dataElement.name = row.dataElementName
                dataElement.description = row.dataElementDescription
                dataElement.status = PublishedElementStatus.UPDATED
                dataElement.save()
                dataElement.addToClassifications(classification)
                dataElement = updateMetadata(row.metadata, dataElement)
            }

            if (valueDomainChanged) {
                if(dataElement.status != PublishedElementStatus.UPDATED) {
                    dataElement.status = PublishedElementStatus.UPDATED
                    dataElement.save()
                }
                importValueDomain(row, dataType, measurementUnit, dataElement, conceptualDomain)

            }

            addUpdatedDataElements(dataElement, model, conceptualDomain, getRelationMetadata(row))

        }

        return dataElement
    }


    protected Boolean checkDataElementForChanges(ImportRow row, DataElement dataElement, Classification classification) {
        Boolean hasDataElementChanged = false
        if (dataElement.name != row.dataElementName || dataElement.description != row.dataElementDescription|| !dataElement.classifications.contains(classification)) { return true }

        row.metadata.each { key, value ->
            if (key) { key = key.toString().take(255) }
            value = (value && value != "") ? value.toString().take(255) : null
            if (dataElement.ext.get(key) != value) { hasDataElementChanged = true}
        }
        return hasDataElementChanged
    }


    protected ExtendibleElement updateMetadata(Map metadata, ExtendibleElement instance) {
        metadata.each { key, value ->
            if (key) { key = key.toString().take(255) }
            if (value) { value = value.toString().take(255) }
            instance.ext.put(key, value)
        }
        instance.save()
        instance
    }

    protected Boolean checkValueDomainForChanges(ImportRow row, DataType dataType, MeasurementUnit measurementUnit, ValueDomain valueDomain, ConceptualDomain conceptualDomain){
        if(row.valueDomainCode && (valueDomain.modelCatalogueId!=row.valueDomainCode)) return true
        if(valueDomain) {
            if (!valueDomain.conceptualDomains.contains(conceptualDomain)) return true
            if (valueDomain.unitOfMeasure != measurementUnit) return true
            if (valueDomain.rule!=row.rule) return true
            if (dataType instanceof EnumeratedType && valueDomain.dataType instanceof EnumeratedType) {
                if (valueDomain.dataType.enumAsString != dataType.enumAsString)  return true
            } else if (valueDomain.dataType != dataType) return true
        }else{
            if(row.dataType||row.measurementUnitName) return true
        }
        return false
    }

    protected DataElement importDataElement(ImportRow row, Model model, ConceptualDomain cd, Classification classification) {

        //find out if data element exists using unique code
        DataElement de = DataElement.findByModelCatalogueId(row.dataElementCode)
        if (de) { de = updateDataElement(row, de, model, cd, classification) }

        if (!de && row.dataElementName) {
            de = new DataElement(name: row.dataElementName, description: row.dataElementDescription, status: PublishedElementStatus.FINALIZED)
            if(row.dataElementCode) de.modelCatalogueId = row.dataElementCode
            de.save()
            de.addToClassifications(classification)
            de = updateMetadata(row.metadata, de)
            addModelToImport(model)
            addUpdatedDataElements(de, model, cd, getRelationMetadata(row))
        }
        return de
    }

    protected DataElement updateDataElement(Map params, DataElement dataElement, Map metadata, Model model, ConceptualDomain conceptualDomain, Classification classification) {
        if (checkDataElementForChanges(params, metadata, dataElement, classification)) {
            if(model.status!=PublishedElementStatus.UPDATED && model.status!=PublishedElementStatus.DRAFT){
                model.status = PublishedElementStatus.UPDATED
                model.save()
            }
            addModelToImport(importer, model)
            publishedElementService.archiveAndIncreaseVersion(dataElement)
            dataElement.refresh()
            dataElement.name = params.name
            dataElement.description = params.description
            dataElement.status = PublishedElementStatus.UPDATED
            dataElement.save()
            dataElement.addToClassifications(classification)
            dataElement = updateMetadata(metadata, dataElement)
            addUpdatedDataElements(dataElement, model, conceptualDomain, getRelationMetadata(row))
        }

        return dataElement
    }


    protected  ValueDomain importValueDomain(ImportRow row, ConceptualDomain conceptualDomain, DataType dataType, MeasurementUnit measurementUnit = null,  Boolean multiple=Boolean.FALSE) {
        ValueDomain vd = ValueDomain.findByModelCatalogueId(row.valueDomainCode)
        if(!vd) vd = findValueDomain(row, dataType, measurementUnit, row.rule)
        if (!vd) {
            String name = (row.valueDomainName)? row.valueDomainName : row.dataElementName
            vd = new ValueDomain(name: name, dataType: dataType, measurementUnit: measurementUnit, multiple: multiple).save()
            if (row.rule!=""){
                vd.rule = row.rule
                vd.save()
            }
        }
        if (vd) {
            vd.addToConceptualDomains(conceptualDomain)
        } else {
            println("Value Domain: " + name + " not imported.")
        }
    }


    protected <E extends PublishedElement> E  addClassifications(E element) {
        element.addToClassifications(classifications.first())
        element
    }



    List<Model> findModels(String name) {
        Model.executeQuery("""
            select m from Model m left join m.classifications c
            where m.name = :name and c in :classifications
            group by m
        """, [name: name, classifications: classifications])
    }

    Model findModel(String name) {
        List<Model> models = findModels(name)
        if (models) {
            return models[0]
        }
    }

    protected ValueDomain findValueDomain(ImportRow row, DataType dataType = null, MeasurementUnit measurementUnit = null, String rule = null) {
        List<ValueDomain> valueDomains
        ValueDomain valueDomain = ValueDomain.findByModelCatalogueId(row.valueDomainCode)
        if(valueDomain) return valueDomain
        else valueDomains = ValueDomain.findAllByNameOrNameIlike(name, "$name (in %)")
            for (ValueDomain domain in valueDomains) {
                if (dataType && domain.dataType == dataType) {
                    if (conceptualDomains.intersect(domain.conceptualDomains) && domain.rule == rule) {
                        return domain
                    }
                } else if (!dataType) {
                    if (conceptualDomains.intersect(domain.conceptualDomains) && domain.rule == rule) {
                        return domain
                    }
                }

            }

        return null
    }




    protected Model copyRelations(Model newModel, Model oldModel) {

        for (Relationship r in oldModel.incomingRelationships) {
            if (r.archived || r.relationshipType.name == 'supersession' || r.relationshipType.name == 'base' || r.relationshipType.name == 'hierarchy') continue
            def newR = relationshipService.link(r.source, newModel, r.relationshipType)
            if (newR.hasErrors()) {
                log.error("ERROR copying relationships: $newR.errors")
            } else {
                r.ext.each { key, value ->
                    newR.ext.put(key, value)
                }
            }
        }

        for (Relationship r in oldModel.outgoingRelationships) {
            if (r.archived || r.relationshipType.name == 'supersession' || r.relationshipType.name == 'base') continue
            def newR = relationshipService.link(newModel, r.destination, r.relationshipType)
            if (newR.hasErrors()) {
                log.error("ERROR copying relationships: $newR.errors")
            } else {
                r.ext.each { key, value ->
                    newR.ext.put(key, value)
                }
            }
        }

        return newModel
    }



    protected matchOrCreateDataElement(String name, ValueDomain domain, String description = null) {
        DataElement dataElement = findDataElement(name, domain)

        if (!dataElement) {
            dataElement = new DataElement(name: name, description: description, valueDomain: domain, status: PublishedElementStatus.PENDING)
            dataElement = addClassifications(dataElement)
            elementsCreated << dataElement.save(failOnError: true)
        }

        dataElement
    }

    protected DataElement findDataElement(String name, ValueDomain domain) {
        if (!name) {
            return null
        }

        def elements

        if (domain) {
            elements = DataElement.executeQuery("""
                select de from DataElement de left join de.classifications c
                where de.name = :name and de.valueDomain = :domain and c in :classifications
                group by de
            """, [name: name, domain: domain, classifications: classifications])
        } else {
            elements = DataElement.executeQuery("""
                select de from DataElement de left join de.classifications c
                where de.name = :name and de.valueDomain is null and c in :classifications
                group by de
            """, [name: name, classifications: classifications])
        }

        if (elements) {
            return elements[0]
        }
    }


    protected static EnumeratedType createOrMatchEnumeratedType(String name, String data) {
        if (data.contains("\n") || data.contains("\r")) {
            String[] lines = data.split("\\r?\\n")
            if (lines != null && lines.size() > 0) {
                Map enumerations = parseLines(lines)
                if (!enumerations.isEmpty()) {
                    EnumeratedType type = EnumeratedType.findByEnumAsString(sortEnumAsString(enumerations))
                    if (type) {
                        return type
                    }
                    return new EnumeratedType(name: name, enumerations: enumerations).save(failOnError: true)
                }
            }
        }
        return null
    }

    protected static sortEnumAsString(Map<String, String> enumerations) {
        return enumerations.sort().collect { String key, String val ->
            "${quote(key)}:${quote(val)}"
        }.join('|')

    }

    protected static Map<String, String> parseLines(String[] lines) {
        Map<String, String> enumerations = [:]
        lines.each { String enumeratedValues ->
            String[] EV = enumeratedValues.split(":")
            if (EV != null && EV.size() > 1 && EV[0] != null && EV[1] != null) {
                String key = EV[0]
                String value = EV[1]
                key = key.trim()
                if (value.isEmpty()) value = "_" else {
                    if (value.size() > 244) value = value[0..244]
                    value.trim()
                }
                enumerations.put(key, value)
            }
        }
        return enumerations
    }

    protected void addUpdatedDataElements(DataElement dataElement, Model model, ConceptualDomain conceptualDomain, Map metadata = [:]){
        if(model.status==PublishedElementStatus.FINALIZED){
            model.status = PublishedElementStatus.UPDATED
            model.save()
        }
        updatedDataElements.add([dataElement, model, conceptualDomain])
    }


    protected void actionPendingModels() {
        models.each { model ->
            def pendingDataElements = updatedDataElements.findAll { it[1] == model }

            if(model.status == PublishedElementStatus.UPDATED) {
                def archivedModel = publishedElementService.archiveAndIncreaseVersion(model)
                model.refresh()
            }

            if (pendingDataElements) {
                pendingDataElements.each { it ->
                    def dataElement = it[0]
                    def relationship = model.addToContains(dataElement)
                    if(it[3]?.MinOccurs) relationship.ext.put("MinOccurs" , it[3].MinOccurs)
                    if(it[3]?.MaxOccurs) relationship.ext.put("MaxOccurs" , it[3].MaxOccurs)
                    dataElement.status = PublishedElementStatus.FINALIZED
                    dataElement.save(flush:true, failOnError:true)
                }
            }
            model.refresh()
            model.status = PublishedElementStatus.PENDING
            model.save(flush:true, failOnError:true)
        }

        models.each{ Model model->
            publishedElementService.finalizeTree(model)
        }
    }

    def actionAsset(){

        Asset asset = asset

        classifications.each{ Classification classification ->
            classification.addToRelatedTo(asset)
        }

        conceptualDomains.each{ ConceptualDomain conceptualDomain ->
            conceptualDomain.addToRelatedTo(asset)
        }

        asset.status = PublishedElementStatus.FINALIZED
        asset.save()
    }

}
