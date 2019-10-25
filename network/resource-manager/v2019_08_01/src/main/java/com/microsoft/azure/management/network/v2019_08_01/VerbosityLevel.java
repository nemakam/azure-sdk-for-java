/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.network.v2019_08_01;

import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.microsoft.rest.ExpandableStringEnum;

/**
 * Defines values for VerbosityLevel.
 */
public final class VerbosityLevel extends ExpandableStringEnum<VerbosityLevel> {
    /** Static value Normal for VerbosityLevel. */
    public static final VerbosityLevel NORMAL = fromString("Normal");

    /** Static value Minimum for VerbosityLevel. */
    public static final VerbosityLevel MINIMUM = fromString("Minimum");

    /** Static value Full for VerbosityLevel. */
    public static final VerbosityLevel FULL = fromString("Full");

    /**
     * Creates or finds a VerbosityLevel from its string representation.
     * @param name a name to look for
     * @return the corresponding VerbosityLevel
     */
    @JsonCreator
    public static VerbosityLevel fromString(String name) {
        return fromString(name, VerbosityLevel.class);
    }

    /**
     * @return known VerbosityLevel values
     */
    public static Collection<VerbosityLevel> values() {
        return values(VerbosityLevel.class);
    }
}
