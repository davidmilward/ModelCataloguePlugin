<%@ page contentType="text/html;charset=UTF-8" defaultCodec="none" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Model Catalogue NHIC App</title>
    <asset:stylesheet href="metaDataCurator.css"/>
    <asset:stylesheet href="application.css"/>

</head>

<body ng-app="metadataCurator">
<div id="wrap">
    <div class="navbar navbar-default navbar-fixed-top" role="navigation">
        <div class="container">
            <div class="navbar-header">
                <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                    <span class="sr-only">Toggle navigation</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
                <a class="navbar-brand" href="#">Model Catalogue</a>
            </div>

            <div class="navbar-collapse collapse">
                <ul class="nav navbar-nav">
                    <li show-for-role="VIEWER" class="dropdown" ui-sref-active="active">
                        <a href="#" class="dropdown-toggle" data-toggle="dropdown">Catalogue Elements<b
                                class="caret"></b></a>
                        <ul class="dropdown-menu">
                            <li ui-sref-active="active"><a id="classificationsLink"
                                                           ui-sref="mc.resource.list({resource: 'classification'})"
                                                           ui-sref-opts="{inherit: false}">Data Sets / Classifications</a>
                            </li>
                            <li ui-sref-active="active"><a id="assetLink"
                                                           ui-sref="mc.resource.list({resource: 'asset'})"
                                                           ui-sref-opts="{inherit: false}">Assets</a>
                            </li>
                            <li ui-sref-active="active"><a id="modelLink"
                                                           ui-sref="mc.resource.list({resource: 'model'})"
                                                           ui-sref-opts="{inherit: false}">Models</a></li>
                            <li ui-sref-active="active"><a id="dataElementLink"
                                                           ui-sref="mc.resource.list({resource: 'dataElement'})"
                                                           ui-sref-opts="{inherit: false}">Data Elements</a>
                            </li>
                            <li ui-sref-active="active"><a id="valueDomainLink"
                                                           ui-sref="mc.resource.list({resource: 'valueDomain'})"
                                                           ui-sref-opts="{inherit: false}">Value Domains</a>
                            </li>
                            <li ui-sref-active="active"><a id="conceptualDomainLink"
                                                           ui-sref="mc.resource.list({resource: 'conceptualDomain'})"
                                                           ui-sref-opts="{inherit: false}">Conceptual Domains</a>
                            </li>
                            <li ui-sref-active="active"><a id="dataTypeLink"
                                                           ui-sref="mc.resource.list({resource: 'dataType'})"
                                                           ui-sref-opts="{inherit: false}">Data Types</a>
                            </li>
                            <li ui-sref-active="active"><a id="measurementUnitLink"
                                                           ui-sref="mc.resource.list({resource: 'measurementUnit'})"
                                                           ui-sref-opts="{inherit: false}">Measurement Unit</a>
                            </li>
                            <li show-for-role="ADMIN" ui-sref-active="active"><a id="relationshipTypeLink"
                                                                                 ui-sref="mc.resource.list({resource: 'relationshipType'})"
                                                                                 ui-sref-opts="{inherit: false}">Relationship Types</a>
                            </li>
                        </ul>
                    </li>

                    <li show-for-role="ADMIN" class="dropdown" ui-sref-active="active">
                        <a href="#" class="dropdown-toggle" data-toggle="dropdown">Data Architect<b
                                class="caret"></b></a>
                        <ul class="dropdown-menu">

                            <li show-for-role="CURATOR" ui-sref-active="active"><a id="batchLink"
                                                                                   ui-sref="mc.resource.list({resource: 'batch'})"
                                                                                   ui-sref-opts="{inherit: false}">Actions</a>
                            </li>

                            <li ui-sref-active="active"><a id="importsLink"
                                                           ui-sref="mc.dataArchitect.imports.list">Imports</a></li>
                            <li ui-sref-active="active"><a id="uninstantiatedElements"
                                                           ui-sref="mc.dataArchitect.uninstantiatedDataElements">Uninstantiated Data Elements</a>
                            </li>
                            <li ui-sref-active="active"><a id="findRelationsByMetadataKeys"
                                                           ui-sref="mc.dataArchitect.findRelationsByMetadataKeys">Create COSD Synonym Data Element Relationships</a>
                            </li>
                            <li ui-sref-active="active"><a id="metadataKeyCheck"
                                                           ui-sref="mc.dataArchitect.metadataKey">Data Elements without Metadata Key</a>
                            </li>
                            <li><a href="../model_catalogue/api/modelCatalogue/core/dataArchitect/uninstantiatedDataElements?format=xlsx&report=NHIC">Export Uninstantiated Elements</a>
                            </li>
                        </ul>
                    </li>

                    <li  class="dropdown" ui-sref-active="active" show-if-logged-in >
                        <a href="#" class="dropdown-toggle" data-toggle="dropdown">Administration<b
                                class="caret"></b></a>


                        <ul show-for-role="VIEWER" class="dropdown-menu">
                            <li ng-controller="metadataCurator.changePasswordCtrl">
                                <a ng-click="changePassword()">Change Password</a>
                            </li>
                        </ul>

                        <ul show-for-role="ADMIN" class="dropdown-menu">

                            <li class="dropdown-header">Roles</li>
                            <li><g:link controller="role" action='search'>Search roles</g:link></li>
                            <li><g:link controller="role" action='create'>Create role</g:link></li>
                            <li class="dropdown-header">Users</li>
                            <li><g:link controller="user" action='search'>Search users</g:link></li>
                            <li><g:link controller="user" action='create'>Create user</g:link></li>
                            <li><g:link controller="registrationCode" action='search'><g:message
                                    code="spring.security.ui.menu.registrationCode"/></g:link></li>

                        </ul>
                    </li>

                </ul>

                <form show-if-logged-in class="navbar-form navbar-right" ng-submit="logout()"
                      ng-controller="metadataCurator.logoutCtrl">
                    <button class="btn btn-danger" type="submit"><i class="glyphicon glyphicon-log-out"></i>
                    </button>
                </form>

                <form hide-if-logged-in class="navbar-form navbar-right" ng-submit="login()"
                      ng-controller="metadataCurator.loginCtrl">
                    <button class="btn btn-primary" type="submit"><i class="glyphicon glyphicon-log-in"></i>
                    </button>
                </form>

                <form class="navbar-form navbar-right navbar-input-group search-form" role="search"
                      autocomplete="off" show-for-role="VIEWER"
                      ng-submit="search()" ng-controller="metadataCurator.searchCtrl">
                    <a ng-click="clearSelection()" ng-class="{'invisible': !$stateParams.q}"
                       class="clear-selection btn btn-link"><span class="glyphicon glyphicon-remove"></span></a>

                    <div class="form-group">
                        <input
                                ng-model="searchSelect"
                                type="text"
                                name="search-term"
                                id="search-term"
                                placeholder="Search"
                                typeahead="result.term as result.label for result in getResults($viewValue)"
                                typeahead-on-select="search($item, $model, $label)"
                                typeahead-template-url="modelcatalogue/core/ui/omnisearchItem.html"
                                typeahead-wait-ms="300"
                                class="form-control"
                                ng-class="{'expanded': searchSelect}">
                    </div>
                    <button class="btn btn-default" ng-click="select(searchSelect)"><i
                            class="glyphicon glyphicon-search"></i></button>
                </form>
            </div><!--/.nav-collapse -->
        </div>
    </div>

    <div class="container">
        <div class="row">
            <messages-panel max="3" growl="true"></messages-panel>
        </div>

        <div class="row">
            <div class="col-md-12">
                <ui-view></ui-view>
            </div>
        </div>

        <div id="push"></div>
    </div>

</div>


<div id="footer">
    <div class="container">
        <p class="muted credit">

        <div class="row">
            <div class="col-sm-4 nav-left">
                <div show-if-logged-in>
                    <p class="feedback text-muted" id="feedback">Feedback</p>
                </div>
            </div>

            <div class="col-sm-4"><p
                    class="text-muted">Model Catalogue &copy; ${new Date()[Calendar.YEAR]} &nbsp&nbspv<g:meta
                        name="app.version"/></p></div>

            <div class="col-sm-4"></div>
        </div>
    </p>
    </div>
</div>



<asset:javascript src="metaDataCurator.js"/>
<asset:javascript src="jiraIssueTracker.js"/>

<script type="text/javascript">
    var demoConfig = angular.module('demo.config', ['mc.core.modelCatalogueApiRoot', 'mc.util.security'])
    demoConfig.config(['securityProvider', function (securityProvider) {
        securityProvider.springSecurity({
            contextPath: '${request.contextPath ?: ''}',
            roles: {
                VIEWER: ['ROLE_USER', 'ROLE_METADATA_CURATOR', 'ROLE_ADMIN'],
                CURATOR: ['ROLE_METADATA_CURATOR', 'ROLE_ADMIN'],
                ADMIN: ['ROLE_ADMIN']
            },
            <sec:ifLoggedIn>
            currentUser: {
                roles: ${grails.plugin.springsecurity.SpringSecurityUtils.getPrincipalAuthorities()*.authority.encodeAsJSON()},
                username: '${sec.username()}'
            }
            </sec:ifLoggedIn>
        })
    }])
    demoConfig.value('modelCatalogueApiRoot', '${request.contextPath ?: ''}/api/modelCatalogue/core')
</script>

</body>
</html>