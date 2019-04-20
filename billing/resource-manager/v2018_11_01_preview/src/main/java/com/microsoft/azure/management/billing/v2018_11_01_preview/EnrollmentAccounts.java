/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.billing.v2018_11_01_preview;

import rx.Observable;
import com.microsoft.azure.management.billing.v2018_11_01_preview.implementation.EnrollmentAccountsInner;
import com.microsoft.azure.arm.model.HasInner;

/**
 * Type representing EnrollmentAccounts.
 */
public interface EnrollmentAccounts extends HasInner<EnrollmentAccountsInner> {
    /**
     * Get the enrollment account by id.
     *
     * @param billingAccountName billing Account Id.
     * @param enrollmentAccountName Enrollment Account Id.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable for the request
     */
    Observable<EnrollmentAccount> getByEnrollmentAccountAccountIdAsync(String billingAccountName, String enrollmentAccountName);

}
