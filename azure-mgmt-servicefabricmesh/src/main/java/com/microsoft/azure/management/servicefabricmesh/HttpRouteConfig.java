/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.servicefabricmesh;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes the hostname properties for http routing.
 */
public class HttpRouteConfig {
    /**
     * http route name.
     */
    @JsonProperty(value = "name", required = true)
    private String name;

    /**
     * Describes a rule for http route matching.
     */
    @JsonProperty(value = "match", required = true)
    private HttpRouteMatchRule match;

    /**
     * Describes destination endpoint for routing traffic.
     */
    @JsonProperty(value = "destination", required = true)
    private GatewayDestination destination;

    /**
     * Get http route name.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Set http route name.
     *
     * @param name the name value to set
     * @return the HttpRouteConfig object itself.
     */
    public HttpRouteConfig withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get describes a rule for http route matching.
     *
     * @return the match value
     */
    public HttpRouteMatchRule match() {
        return this.match;
    }

    /**
     * Set describes a rule for http route matching.
     *
     * @param match the match value to set
     * @return the HttpRouteConfig object itself.
     */
    public HttpRouteConfig withMatch(HttpRouteMatchRule match) {
        this.match = match;
        return this;
    }

    /**
     * Get describes destination endpoint for routing traffic.
     *
     * @return the destination value
     */
    public GatewayDestination destination() {
        return this.destination;
    }

    /**
     * Set describes destination endpoint for routing traffic.
     *
     * @param destination the destination value to set
     * @return the HttpRouteConfig object itself.
     */
    public HttpRouteConfig withDestination(GatewayDestination destination) {
        this.destination = destination;
        return this;
    }

}
