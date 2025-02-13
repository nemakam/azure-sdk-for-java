/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.network.v2019_09_01;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Rule condition of type application.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "ruleConditionType")
@JsonTypeName("ApplicationRuleCondition")
public class ApplicationRuleCondition extends FirewallPolicyRuleCondition {
    /**
     * List of source IP addresses for this rule.
     */
    @JsonProperty(value = "sourceAddresses")
    private List<String> sourceAddresses;

    /**
     * List of destination IP addresses or Service Tags.
     */
    @JsonProperty(value = "destinationAddresses")
    private List<String> destinationAddresses;

    /**
     * Array of Application Protocols.
     */
    @JsonProperty(value = "protocols")
    private List<FirewallPolicyRuleConditionApplicationProtocol> protocols;

    /**
     * List of FQDNs for this rule condition.
     */
    @JsonProperty(value = "targetFqdns")
    private List<String> targetFqdns;

    /**
     * List of FQDN Tags for this rule condition.
     */
    @JsonProperty(value = "fqdnTags")
    private List<String> fqdnTags;

    /**
     * Get list of source IP addresses for this rule.
     *
     * @return the sourceAddresses value
     */
    public List<String> sourceAddresses() {
        return this.sourceAddresses;
    }

    /**
     * Set list of source IP addresses for this rule.
     *
     * @param sourceAddresses the sourceAddresses value to set
     * @return the ApplicationRuleCondition object itself.
     */
    public ApplicationRuleCondition withSourceAddresses(List<String> sourceAddresses) {
        this.sourceAddresses = sourceAddresses;
        return this;
    }

    /**
     * Get list of destination IP addresses or Service Tags.
     *
     * @return the destinationAddresses value
     */
    public List<String> destinationAddresses() {
        return this.destinationAddresses;
    }

    /**
     * Set list of destination IP addresses or Service Tags.
     *
     * @param destinationAddresses the destinationAddresses value to set
     * @return the ApplicationRuleCondition object itself.
     */
    public ApplicationRuleCondition withDestinationAddresses(List<String> destinationAddresses) {
        this.destinationAddresses = destinationAddresses;
        return this;
    }

    /**
     * Get array of Application Protocols.
     *
     * @return the protocols value
     */
    public List<FirewallPolicyRuleConditionApplicationProtocol> protocols() {
        return this.protocols;
    }

    /**
     * Set array of Application Protocols.
     *
     * @param protocols the protocols value to set
     * @return the ApplicationRuleCondition object itself.
     */
    public ApplicationRuleCondition withProtocols(List<FirewallPolicyRuleConditionApplicationProtocol> protocols) {
        this.protocols = protocols;
        return this;
    }

    /**
     * Get list of FQDNs for this rule condition.
     *
     * @return the targetFqdns value
     */
    public List<String> targetFqdns() {
        return this.targetFqdns;
    }

    /**
     * Set list of FQDNs for this rule condition.
     *
     * @param targetFqdns the targetFqdns value to set
     * @return the ApplicationRuleCondition object itself.
     */
    public ApplicationRuleCondition withTargetFqdns(List<String> targetFqdns) {
        this.targetFqdns = targetFqdns;
        return this;
    }

    /**
     * Get list of FQDN Tags for this rule condition.
     *
     * @return the fqdnTags value
     */
    public List<String> fqdnTags() {
        return this.fqdnTags;
    }

    /**
     * Set list of FQDN Tags for this rule condition.
     *
     * @param fqdnTags the fqdnTags value to set
     * @return the ApplicationRuleCondition object itself.
     */
    public ApplicationRuleCondition withFqdnTags(List<String> fqdnTags) {
        this.fqdnTags = fqdnTags;
        return this;
    }

}
