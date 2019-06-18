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
 * Defines values for NetworkKind.
 */
public final class NetworkKind extends ExpandableStringEnum<NetworkKind> {
    /** Static value Local for NetworkKind. */
    public static final NetworkKind LOCAL = fromString("Local");

    /**
     * Creates or finds a NetworkKind from its string representation.
     * @param name a name to look for
     * @return the corresponding NetworkKind
     */
    @JsonCreator
    public static NetworkKind fromString(String name) {
        return fromString(name, NetworkKind.class);
    }

    /**
     * @return known NetworkKind values
     */
    public static Collection<NetworkKind> values() {
        return values(NetworkKind.class);
    }
}
