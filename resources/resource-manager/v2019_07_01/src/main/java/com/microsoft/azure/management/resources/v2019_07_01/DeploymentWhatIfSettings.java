/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.resources.v2019_07_01;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Deployment What-If operation settings.
 */
public class DeploymentWhatIfSettings {
    /**
     * The format of the What-If results. Possible values include:
     * 'ResourceIdOnly', 'FullResourcePayloads'.
     */
    @JsonProperty(value = "resultFormat")
    private WhatIfResultFormat resultFormat;

    /**
     * Get the format of the What-If results. Possible values include: 'ResourceIdOnly', 'FullResourcePayloads'.
     *
     * @return the resultFormat value
     */
    public WhatIfResultFormat resultFormat() {
        return this.resultFormat;
    }

    /**
     * Set the format of the What-If results. Possible values include: 'ResourceIdOnly', 'FullResourcePayloads'.
     *
     * @param resultFormat the resultFormat value to set
     * @return the DeploymentWhatIfSettings object itself.
     */
    public DeploymentWhatIfSettings withResultFormat(WhatIfResultFormat resultFormat) {
        this.resultFormat = resultFormat;
        return this;
    }

}
