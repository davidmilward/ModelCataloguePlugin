package org.modelcatalogue.core.dataarchitect

import org.modelcatalogue.core.DataElement
import org.modelcatalogue.core.Model

/**
 * Created by adammilward on 17/04/2014.
 */
class ImportRow {

    private static final QUOTED_CHARS = ["\\": "&#92;", ":" : "&#58;", "|" : "&#124;", "%" : "&#37;"]
    private static final REGEX = '(?i)MC_([A-Z0-9]{8}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{12})_\\d+'

    String dataElementCode
    String dataElementName
    String dataElementDescription
    String conceptualDomainName
    String conceptualDomainDescription
    String dataType
    String parentModelName
    String parentModelCode
    String containingModelName
    String containingModelCode
    String measurementUnitName
    String measurementSymbol
    String valueDomainName
    String valueDomainCode
    String classification
    String minOccurs
    String maxOccurs
    String rule
    Map metadata
    Boolean imported = false
    Set<RowAction> rowActions = []

    static hasMany = [rowActions: RowAction]

    static constraints = {
        dataElementCode nullable: true, maxSize: 255
        dataElementName nullable: true, maxSize: 255
        dataElementDescription nullable: true, maxSize: 2000
        conceptualDomainName nullable: true, maxSize: 255
        conceptualDomainDescription  nullable: true, maxSize: 2000
        dataType nullable: true, maxSize: 10000
        parentModelName nullable: true, maxSize: 255
        parentModelCode nullable: true, maxSize: 255
        containingModelName nullable: true, maxSize: 255
        containingModelCode nullable: true, maxSize: 255
        measurementUnitName nullable: true, maxSize: 255
        measurementSymbol nullable: true, maxSize: 255
        classification nullable: true, maxSize: 255
        valueDomainName nullable: true, maxSize: 255
        valueDomainCode nullable: true, maxSize: 255
        minOccurs nullable: true, maxSize: 255
        maxOccurs nullable: true, maxSize: 255
        metadata nullable: true
        rowActions  nullable: true
        imported nulable: true
        rule nullable: true
    }

    static mapping = {
        metadata type: "text"
    }

    def resolveAction(String field, ActionType actionType){
        def actionsToResolve = rowActions.findAll{it.field == field && it.actionType==actionType}
        actionsToResolve.each { RowAction actionToResolve ->
            if (actionToResolve) {
                rowActions.remove(actionToResolve)
            }
        }
    }

    def resolveAll(){
        def queue = rowActions.iterator()
        while (queue.hasNext()) {
            RowAction rowAction = queue.next()
            if (rowAction.actionType!=ActionType.RESOLVE_ERROR) {
                queue.remove()
            }
        }
    }

    protected void validateAndActionRow(){
        if (!conceptualDomainName) {
            RowAction action = new RowAction(field: "conceptualDomainName", action: "please enter conceptual domain name to import row", actionType: ActionType.RESOLVE_ERROR).save()
            addToRowActions(action)
        }

        if (!containingModelName) {
            RowAction action = new RowAction(field: "containingModelName", action: "please complete the containing model name to import row", actionType: ActionType.RESOLVE_ERROR).save()
            addToRowActions(action)
        }

        if (!containingModelCode) {
            RowAction action = new RowAction(field: "containingModelCode", action: "Containing model does not have model catalogue code. New model will be created.", actionType: ActionType.CREATE_CONTAINING_MODEL).save()
            addToRowActions(action)
        } else {
            def md = Model.findByModelCatalogueId(containingModelCode)
            if(!md){
                RowAction action = new RowAction(field: "containingModelCode", action: "Containing Model Id does not match an existing element. New model will be created.", actionType: ActionType.CREATE_CONTAINING_MODEL).save()
                addToRowActions(action)
            }

            if (!containingModelCode.matches(REGEX)) {
                RowAction action = new RowAction(field: "containingModelCode", action: "the model catalogue code for the containing model is invalid, please action to import row", actionType: ActionType.RESOLVE_ERROR).save()
                addToRowActions(action)
            }
        }

        if (!parentModelCode) {
            if (parentModelName) {
                RowAction action = new RowAction(field: "parentModelCode", action: "Parent model does not have model catalogue code. New model will be created.", actionType: ActionType.CREATE_PARENT_MODEL).save()
                addToRowActions(action)
            }
        } else {
            def md = Model.findByModelCatalogueId(parentModelCode)
            if(!md){
                RowAction action = new RowAction(field: "parentModelCode", action: "Parent Model Id does not match an existing element. New model will be created.", actionType: ActionType.CREATE_PARENT_MODEL).save()
                addToRowActions(action)
            }
            if (!parentModelCode.matches(REGEX)) {
                RowAction action = new RowAction(field: "parentModelCode", action: "the model catalogue code for the parent model is invalid, please action to import row", actionType: ActionType.RESOLVE_ERROR).save()
                addToRowActions(action)
            }
        }

        if (!dataElementName) {
            RowAction action = new RowAction(field: "dataElementName", action: "No data element in row. Only Model information imported", actionType: ActionType.MODEL_ONLY_ROW).save()
            addToRowActions(action)
        } else {
            //we are importing a data element so we need to do these additional checks
            if (!dataElementCode) {
                RowAction action = new RowAction(field: "dataElementCode", action: "Data element does not have model catalogue code. New data element will be created.", actionType: ActionType.CREATE_DATA_ELEMENT).save()
                addToRowActions(action)
            } else {
                def de = DataElement.findByModelCatalogueId(dataElementCode)
                if (!dataElementCode.matches(REGEX)) {
                    RowAction action = new RowAction(field: "dataElementCode", action: "the model catalogue code for the data element is invalid, please action to import row", actionType: ActionType.RESOLVE_ERROR).save()
                    addToRowActions(action)
                }
                if (!de) {
                    RowAction action = new RowAction(field: "dataElementCode", action: "Data Element Id does not match an existing element. New data element will be created.", actionType: ActionType.CREATE_DATA_ELEMENT).save()
                    addToRowActions(action)
                }
            }
            if (!dataType) {
                RowAction action = new RowAction(field: "dataType", action: "the row does not contain a data type therefore will not be associated with a value domain, is this the expected outcome?", actionType: ActionType.DECISION).save()
                addToRowActions(action)
            }
            if (dataType.contains("|")) {
                try {
                    dataType = sortEnumAsString(dataType)
                } catch (Exception e) {
                    RowAction action = new RowAction(field: "dataType", action: "the row has an invalid enumerated data type. Please action to import row", actionType: ActionType.RESOLVE_ERROR).save()
                    addToRowActions(action)
                }
            }
        }
    }

    protected String sortEnumAsString(String inputString) {
        if (inputString == null) return null
        String sortedString
        Map<String, String> ret = [:]
        inputString.split(/\|/).each { String part ->
            if (!part) return
            String[] pair = part.split("(?<!\\\\):")
            if (pair.length != 2) throw new IllegalArgumentException("Wrong enumerated value '$part' in encoded enumeration '$s'")
            ret[unquote(pair[0])] = unquote(pair[1])
        }
        sortedString = ret.sort() collect { key, val ->
            "${quote(key)}:${quote(val)}"
        }.join('|').trim()
        sortedString
    }

    protected String quote(String s) {
        if (s == null) return null
        String ret = s
        QUOTED_CHARS.each { original, replacement ->
            ret = ret.replace(original, replacement)
        }
        ret
    }

    protected String unquote(String s) {
        if (s == null) return null
        String ret = s
        QUOTED_CHARS.reverseEach { original, pattern ->
            ret = ret.replace(pattern, original)
        }
        ret
    }

}
