/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.billing.v2018_11_01_preview.implementation;

import retrofit2.Retrofit;
import com.google.common.reflect.TypeToken;
import com.microsoft.azure.management.billing.v2018_11_01_preview.ErrorResponseException;
import com.microsoft.rest.ServiceCallback;
import com.microsoft.rest.ServiceFuture;
import com.microsoft.rest.ServiceResponse;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.Response;
import rx.functions.Func1;
import rx.Observable;

/**
 * An instance of this class provides access to all the operations defined
 * in InvoicesByBillingProfiles.
 */
public class InvoicesByBillingProfilesInner {
    /** The Retrofit service to perform REST calls. */
    private InvoicesByBillingProfilesService service;
    /** The service client containing this operation class. */
    private BillingManagementClientImpl client;

    /**
     * Initializes an instance of InvoicesByBillingProfilesInner.
     *
     * @param retrofit the Retrofit instance built from a Retrofit Builder.
     * @param client the instance of the service client containing this operation class.
     */
    public InvoicesByBillingProfilesInner(Retrofit retrofit, BillingManagementClientImpl client) {
        this.service = retrofit.create(InvoicesByBillingProfilesService.class);
        this.client = client;
    }

    /**
     * The interface defining all the services for InvoicesByBillingProfiles to be
     * used by Retrofit to perform actually REST calls.
     */
    interface InvoicesByBillingProfilesService {
        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.management.billing.v2018_11_01_preview.InvoicesByBillingProfiles list" })
        @GET("providers/Microsoft.Billing/billingAccounts/{billingAccountName}/billingProfiles/{billingProfileName}/invoices")
        Observable<Response<ResponseBody>> list(@Path("billingAccountName") String billingAccountName, @Path("billingProfileName") String billingProfileName, @Query("api-version") String apiVersion, @Query("periodStartDate") String periodStartDate, @Query("periodEndDate") String periodEndDate, @Header("accept-language") String acceptLanguage, @Header("User-Agent") String userAgent);

    }

    /**
     * List of invoices for a billing profile.
     *
     * @param billingAccountName billing Account Id.
     * @param billingProfileName Billing Profile Id.
     * @param periodStartDate Invoice period start date.
     * @param periodEndDate Invoice period end date.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws ErrorResponseException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the InvoiceListResultInner object if successful.
     */
    public InvoiceListResultInner list(String billingAccountName, String billingProfileName, String periodStartDate, String periodEndDate) {
        return listWithServiceResponseAsync(billingAccountName, billingProfileName, periodStartDate, periodEndDate).toBlocking().single().body();
    }

    /**
     * List of invoices for a billing profile.
     *
     * @param billingAccountName billing Account Id.
     * @param billingProfileName Billing Profile Id.
     * @param periodStartDate Invoice period start date.
     * @param periodEndDate Invoice period end date.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<InvoiceListResultInner> listAsync(String billingAccountName, String billingProfileName, String periodStartDate, String periodEndDate, final ServiceCallback<InvoiceListResultInner> serviceCallback) {
        return ServiceFuture.fromResponse(listWithServiceResponseAsync(billingAccountName, billingProfileName, periodStartDate, periodEndDate), serviceCallback);
    }

    /**
     * List of invoices for a billing profile.
     *
     * @param billingAccountName billing Account Id.
     * @param billingProfileName Billing Profile Id.
     * @param periodStartDate Invoice period start date.
     * @param periodEndDate Invoice period end date.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the InvoiceListResultInner object
     */
    public Observable<InvoiceListResultInner> listAsync(String billingAccountName, String billingProfileName, String periodStartDate, String periodEndDate) {
        return listWithServiceResponseAsync(billingAccountName, billingProfileName, periodStartDate, periodEndDate).map(new Func1<ServiceResponse<InvoiceListResultInner>, InvoiceListResultInner>() {
            @Override
            public InvoiceListResultInner call(ServiceResponse<InvoiceListResultInner> response) {
                return response.body();
            }
        });
    }

    /**
     * List of invoices for a billing profile.
     *
     * @param billingAccountName billing Account Id.
     * @param billingProfileName Billing Profile Id.
     * @param periodStartDate Invoice period start date.
     * @param periodEndDate Invoice period end date.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the InvoiceListResultInner object
     */
    public Observable<ServiceResponse<InvoiceListResultInner>> listWithServiceResponseAsync(String billingAccountName, String billingProfileName, String periodStartDate, String periodEndDate) {
        if (billingAccountName == null) {
            throw new IllegalArgumentException("Parameter billingAccountName is required and cannot be null.");
        }
        if (billingProfileName == null) {
            throw new IllegalArgumentException("Parameter billingProfileName is required and cannot be null.");
        }
        if (this.client.apiVersion() == null) {
            throw new IllegalArgumentException("Parameter this.client.apiVersion() is required and cannot be null.");
        }
        if (periodStartDate == null) {
            throw new IllegalArgumentException("Parameter periodStartDate is required and cannot be null.");
        }
        if (periodEndDate == null) {
            throw new IllegalArgumentException("Parameter periodEndDate is required and cannot be null.");
        }
        return service.list(billingAccountName, billingProfileName, this.client.apiVersion(), periodStartDate, periodEndDate, this.client.acceptLanguage(), this.client.userAgent())
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponse<InvoiceListResultInner>>>() {
                @Override
                public Observable<ServiceResponse<InvoiceListResultInner>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponse<InvoiceListResultInner> clientResponse = listDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponse<InvoiceListResultInner> listDelegate(Response<ResponseBody> response) throws ErrorResponseException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<InvoiceListResultInner, ErrorResponseException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<InvoiceListResultInner>() { }.getType())
                .registerError(ErrorResponseException.class)
                .build(response);
    }

}
