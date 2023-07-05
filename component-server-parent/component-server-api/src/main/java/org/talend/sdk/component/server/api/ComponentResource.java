/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.server.api;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.PATH;
import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.QUERY;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.OBJECT;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.STRING;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentIndices;
import org.talend.sdk.component.server.front.model.Dependencies;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;

@Path("component")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Component", description = "Endpoints related to component metadata access.")
public interface ComponentResource {

    @GET
    @Path("dependencies")
    @Operation(description = "Returns a list of dependencies for the given components. "
            + "IMPORTANT: don't forget to add the component itself since it will not be part of the dependencies."
            + "Then you can use /dependency/{id} to download the binary.")
    @APIResponse(responseCode = "200", description = "The list of dependencies per component",
            content = @Content(mediaType = APPLICATION_JSON))
    Dependencies getDependencies(@QueryParam("identifier") @Parameter(name = "identifier",
            description = "The identifier id to request. Repeat this parameter to request more than one element.",
            in = QUERY) String[] ids);

    @GET
    @Path("dependency/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(description = "Return a binary of the dependency represented by `id`. "
            + "It can be maven coordinates for dependencies or a component id.")
    @APIResponse(responseCode = "200", description = "The dependency binary (jar).",
            content = @Content(mediaType = APPLICATION_OCTET_STREAM))
    @APIResponse(responseCode = "404",
            description = "If the plugin is missing, payload will be an ErrorPayload with the code PLUGIN_MISSING.",
            content = @Content(mediaType = APPLICATION_JSON,
                    schema = @Schema(type = OBJECT, implementation = ErrorPayload.class)))
    StreamingOutput getDependency(@PathParam("id") @Parameter(name = "id", description = "the dependency binary (jar).",
            in = PATH) String id);

    @GET
    @Path("index")
    @Operation(operationId = "getComponentIndex", description = "Returns the list of available components.")
    @APIResponse(responseCode = "200", description = "The index of available components.",
            content = @Content(mediaType = APPLICATION_OCTET_STREAM))
    ComponentIndices getIndex(
            @QueryParam("language") @DefaultValue("en") @Parameter(name = "language",
                    description = "the language for display names.", in = QUERY,
                    schema = @Schema(type = STRING, defaultValue = "en")) String language,
            @QueryParam("includeIconContent") @DefaultValue("false") @Parameter(name = "includeIconContent",
                    description = "should the icon binary format be included in the payload.", in = QUERY,
                    schema = @Schema(type = STRING, defaultValue = "en")) boolean includeIconContent,
            @QueryParam("q") @Parameter(name = "q",
                    description = "Query in simple query language to filter components. "
                            + "It provides access to the component `plugin`, `name`, `id` and `metadata` of the first configuration property. "
                            + "Ex: `(id = AYETAE658349453) AND (metadata[configurationtype::type] = dataset) AND (plugin = jdbc-component) AND "
                            + "(name = input)`",
                    in = QUERY, schema = @Schema(type = STRING)) String query);

    @GET
    @Path("icon/family/{id}")
    @Produces({ APPLICATION_JSON, APPLICATION_OCTET_STREAM })
    @Operation(description = "Returns the icon for a family.")
    @APIResponse(responseCode = "200", description = "Returns a particular family icon in raw bytes.",
            content = @Content(mediaType = APPLICATION_OCTET_STREAM))
    @APIResponse(responseCode = "404", description = "The family or icon is not found",
            content = @Content(mediaType = APPLICATION_JSON,
                    schema = @Schema(type = OBJECT, implementation = ErrorPayload.class)))
    Response familyIcon(
            @PathParam("id") @Parameter(name = "id", description = "the family identifier", in = PATH) String id);

    @GET
    @Path("icon/{id}")
    @Produces({ APPLICATION_JSON, APPLICATION_OCTET_STREAM })
    @Operation(description = "Returns a particular component icon in raw bytes.")
    @APIResponse(responseCode = "200", description = "The component icon in binary form.",
            content = @Content(mediaType = APPLICATION_OCTET_STREAM))
    @APIResponse(responseCode = "404", description = "The family or icon is not found",
            content = @Content(mediaType = APPLICATION_JSON))
    Response icon(@PathParam("id") @Parameter(name = "id", description = "the component icon identifier",
            in = PATH) String id);

    @POST
    @Path("migrate/{id}/{configurationVersion}")
    @Operation(operationId = "migrateComponent",
            description = "Allows to migrate a component configuration without calling any component execution.")
    @APIResponse(responseCode = "200",
            description = "the new configuration for that component (or the same if no migration was needed).",
            content = @Content(mediaType = APPLICATION_JSON))
    @APIResponse(responseCode = "404", description = "The component is not found",
            content = @Content(mediaType = APPLICATION_JSON,
                    schema = @Schema(type = OBJECT, implementation = ErrorPayload.class)))
    Map<String, String> migrate(
            @PathParam("id") @Parameter(name = "id", description = "the component identifier", in = PATH) String id,
            @PathParam("configurationVersion") @Parameter(name = "configurationVersion",
                    description = "the configuration version you send", in = PATH) int version,
            @RequestBody(description = "the actual configuration in key/value form.", required = true,
                    content = @Content(mediaType = APPLICATION_JSON,
                            schema = @Schema(type = OBJECT))) Map<String, String> config);

    @GET
    @Path("details") // bulk mode to avoid to fetch components one by one when reloading a pipeline/job
    @Operation(operationId = "getComponentDetail",
            description = "Returns the set of metadata about a few components identified by their 'id'.")
    @APIResponse(responseCode = "200", description = "the list of details for the requested components.",
            content = @Content(mediaType = APPLICATION_JSON))
    @APIResponse(responseCode = "400", description = "Some identifiers were not valid.",
            content = @Content(mediaType = APPLICATION_JSON,
                    schema = @Schema(type = OBJECT, implementation = SampleErrorForBulk.class)))
    ComponentDetailList getDetail(
            @QueryParam("language") @DefaultValue("en") @Parameter(name = "language",
                    description = "the language for display names.",
                    in = QUERY,
                    schema = @Schema(type = STRING, defaultValue = "en")) String language,
            @QueryParam("identifiers") @Parameter(name = "identifiers",
                    description = "the identifiers id to request." +
                            "Use this parameter multiple times to request more than one element",
                    in = QUERY) String[] ids);

    // @Unused, only for sample
    class SampleErrorForBulk {

        private ErrorPayload error1;

        private ErrorPayload error2;
    }
}
