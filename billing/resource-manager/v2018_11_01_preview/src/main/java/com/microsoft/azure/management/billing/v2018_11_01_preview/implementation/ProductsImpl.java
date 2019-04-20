/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 *
 */

package com.microsoft.azure.management.billing.v2018_11_01_preview.implementation;

import com.microsoft.azure.arm.model.implementation.WrapperImpl;
import com.microsoft.azure.management.billing.v2018_11_01_preview.Products;
import rx.Observable;
import rx.functions.Func1;
import com.microsoft.azure.management.billing.v2018_11_01_preview.BillingAccountProductSummary;
import com.microsoft.azure.management.billing.v2018_11_01_preview.InvoiceSectionBillingAccountProductSummary;

class ProductsImpl extends WrapperImpl<ProductsInner> implements Products {
    private final BillingManager manager;

    ProductsImpl(BillingManager manager) {
        super(manager.inner().products());
        this.manager = manager;
    }

    public BillingManager manager() {
        return this.manager;
    }

    private InvoiceSectionBillingAccountProductSummaryImpl wrapModel(ProductSummaryInner inner) {
        return  new InvoiceSectionBillingAccountProductSummaryImpl(inner, manager());
    }

    @Override
    public Observable<BillingAccountProductSummary> transferAsync(String billingAccountName, String invoiceSectionName, String productName) {
        ProductsInner client = this.inner();
        return client.transferAsync(billingAccountName, invoiceSectionName, productName)
        .map(new Func1<ProductSummaryInner, BillingAccountProductSummary>() {
            @Override
            public BillingAccountProductSummary call(ProductSummaryInner inner) {
                return new BillingAccountProductSummaryImpl(inner, manager());
            }
        });
    }

    @Override
    public Observable<InvoiceSectionBillingAccountProductSummary> getAsync(String billingAccountName, String invoiceSectionName, String productName) {
        ProductsInner client = this.inner();
        return client.getAsync(billingAccountName, invoiceSectionName, productName)
        .map(new Func1<ProductSummaryInner, InvoiceSectionBillingAccountProductSummary>() {
            @Override
            public InvoiceSectionBillingAccountProductSummary call(ProductSummaryInner inner) {
                return wrapModel(inner);
            }
       });
    }

}
