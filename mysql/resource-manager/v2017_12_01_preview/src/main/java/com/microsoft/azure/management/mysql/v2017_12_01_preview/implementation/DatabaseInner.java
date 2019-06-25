/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.mysql.v2017_12_01_preview.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;
import com.microsoft.azure.ProxyResource;

/**
 * Represents a Database.
 */
@JsonFlatten
public class DatabaseInner extends ProxyResource {
    /**
     * The charset of the database.
     */
    @JsonProperty(value = "properties.charset")
    private String charset;

    /**
     * The collation of the database.
     */
    @JsonProperty(value = "properties.collation")
    private String collation;

    /**
     * Get the charset of the database.
     *
     * @return the charset value
     */
    public String charset() {
        return this.charset;
    }

    /**
     * Set the charset of the database.
     *
     * @param charset the charset value to set
     * @return the DatabaseInner object itself.
     */
    public DatabaseInner withCharset(String charset) {
        this.charset = charset;
        return this;
    }

    /**
     * Get the collation of the database.
     *
     * @return the collation value
     */
    public String collation() {
        return this.collation;
    }

    /**
     * Set the collation of the database.
     *
     * @param collation the collation value to set
     * @return the DatabaseInner object itself.
     */
    public DatabaseInner withCollation(String collation) {
        this.collation = collation;
        return this;
    }

}
