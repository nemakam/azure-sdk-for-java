/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.servicefabricmesh;

import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.microsoft.rest.ExpandableStringEnum;

/**
 * Defines values for ResourceStatus.
 */
public final class ResourceStatus extends ExpandableStringEnum<ResourceStatus> {
    /** Static value Unknown for ResourceStatus. */
    public static final ResourceStatus UNKNOWN = fromString("Unknown");

    /** Static value Ready for ResourceStatus. */
    public static final ResourceStatus READY = fromString("Ready");

    /** Static value Upgrading for ResourceStatus. */
    public static final ResourceStatus UPGRADING = fromString("Upgrading");

    /** Static value Creating for ResourceStatus. */
    public static final ResourceStatus CREATING = fromString("Creating");

    /** Static value Deleting for ResourceStatus. */
    public static final ResourceStatus DELETING = fromString("Deleting");

    /** Static value Failed for ResourceStatus. */
    public static final ResourceStatus FAILED = fromString("Failed");

    /**
     * Creates or finds a ResourceStatus from its string representation.
     * @param name a name to look for
     * @return the corresponding ResourceStatus
     */
    @JsonCreator
    public static ResourceStatus fromString(String name) {
        return fromString(name, ResourceStatus.class);
    }

    /**
     * @return known ResourceStatus values
     */
    public static Collection<ResourceStatus> values() {
        return values(ResourceStatus.class);
    }
}
