// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.security.keyvault.secrets;

import com.azure.core.http.HttpPipeline;
import com.azure.core.http.rest.Page;
import com.azure.core.http.rest.Response;
import com.azure.core.http.rest.PagedResponse;
import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.SimpleResponse;
import com.azure.core.implementation.RestProxy;
import com.azure.core.annotation.ReturnType;
import com.azure.core.annotation.ServiceClient;
import com.azure.core.annotation.ServiceMethod;
import com.azure.core.implementation.util.FluxUtil;
import com.azure.core.util.Context;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.Poller;
import com.azure.security.keyvault.secrets.models.DeletedSecret;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.exception.ResourceModifiedException;
import com.azure.core.exception.HttpRequestException;

import static com.azure.core.implementation.util.FluxUtil.monoError;
import static com.azure.core.implementation.util.FluxUtil.withContext;

/**
 * The SecretAsyncClient provides asynchronous methods to manage {@link KeyVaultSecret secrets} in the Azure Key Vault. The
 * client supports creating, retrieving, updating, deleting, purging, backing up, restoring and listing the {@link
 * KeyVaultSecret secrets}. The client also supports listing {@link DeletedSecret deleted secrets} for a soft-delete enabled
 * Azure Key Vault.
 *
 * <p><strong>Samples to construct the async client</strong></p>
 * {@codesnippet com.azure.security.keyvault.secrets.async.secretclient.construct}
 *
 * @see SecretClientBuilder
 * @see PagedFlux
 */
@ServiceClient(builder = SecretClientBuilder.class, isAsync = true, serviceInterfaces = SecretService.class)
public final class SecretAsyncClient {
    static final String API_VERSION = "7.0";
    static final String ACCEPT_LANGUAGE = "en-US";
    static final int DEFAULT_MAX_PAGE_RESULTS = 25;
    static final String CONTENT_TYPE_HEADER_VALUE = "application/json";
    static final String KEY_VAULT_SCOPE = "https://vault.azure.net/.default";

    private final String vaultUrl;
    private final SecretService service;
    private final ClientLogger logger = new ClientLogger(SecretAsyncClient.class);

    /**
     * Creates a SecretAsyncClient that uses {@code pipeline} to service requests
     *
     * @param vaultUrl URL for the Azure KeyVault service.
     * @param pipeline HttpPipeline that the HTTP requests and responses flow through.
     * @param version {@link SecretServiceVersion} of the service to be used when making requests.
     */
    SecretAsyncClient(URL vaultUrl, HttpPipeline pipeline, SecretServiceVersion version) {
        Objects.requireNonNull(vaultUrl,
            KeyVaultErrorCodeStrings.getErrorString(KeyVaultErrorCodeStrings.VAULT_END_POINT_REQUIRED));
        this.vaultUrl = vaultUrl.toString();
        this.service = RestProxy.create(SecretService.class, pipeline);
    }

    /**
     * Get the vault endpoint url to which service requests are sent to.
     * @return the vault endpoint url
     */
    public String getVaultUrl() {
        return vaultUrl;
    }

    /**
     * The set operation adds a secret to the key vault. If the named secret already exists, Azure Key Vault creates
     * a new version of that secret. This operation requires the {@code secrets/set} permission.
     *
     * <p>The {@link KeyVaultSecret} is required. The {@link SecretProperties#getExpiresOn() expires},
     * {@link SecretProperties#getContentType() contentType} and {@link SecretProperties#getNotBefore() notBefore}
     * values in {@code secret} are optional. The {@link SecretProperties#isEnabled() enabled} field is set to true
     * by key vault, if not specified.</p>
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Creates a new secret which activates in 1 day and expires in 1 year in the Azure Key Vault. Subscribes to the
     * call asynchronously and prints out the newly created secret details when a response is received.</p>
     * {@codesnippet com.azure.keyvault.secrets.secretclient.setSecret#secret}
     *
     * @param secret The Secret object containing information about the secret and its properties. The properties
     *     secret.name and secret.value must be non null.
     * @return A {@link Mono} containing the {@link KeyVaultSecret created secret}.
     * @throws NullPointerException if {@code secret} is {@code null}.
     * @throws ResourceModifiedException if {@code secret} is malformed.
     * @throws HttpRequestException if {@link KeyVaultSecret#getName()  name} or {@link KeyVaultSecret#getValue() value} is empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<KeyVaultSecret> setSecret(KeyVaultSecret secret) {
        try {
            return setSecretWithResponse(secret).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * The set operation adds a secret to the key vault. If the named secret already exists, Azure Key Vault creates
     * a new version of that secret. This operation requires the {@code secrets/set} permission.
     *
     * <p>The {@link KeyVaultSecret} is required. The {@link SecretProperties#getExpiresOn() expires},
     * {@link SecretProperties#getContentType() contentType} and {@link SecretProperties#getNotBefore() notBefore}
     * values in {@code secret} are optional. The {@link SecretProperties#isEnabled() enabled} field is set to true by
     * key vault, if not specified.</p>
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Creates a new secret which activates in 1 day and expires in 1 year in the Azure Key Vault. Subscribes to the
     * call asynchronously and prints out the newly created secret details when a response is received.</p>
     * {@codesnippet com.azure.keyvault.secrets.secretclient.setSecretWithResponse#secret}
     *
     * @param secret The Secret object containing information about the secret and its properties. The properties
     *     secret.name and secret.value must be non null.
     * @return A {@link Mono} containing a {@link Response} whose {@link Response#getValue() value} contains the {@link
     *     KeyVaultSecret created secret}.
     * @throws NullPointerException if {@code secret} is {@code null}.
     * @throws ResourceModifiedException if {@code secret} is malformed.
     * @throws HttpRequestException if {@link KeyVaultSecret#getName() name} or {@link KeyVaultSecret#getValue() value} is empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<Response<KeyVaultSecret>> setSecretWithResponse(KeyVaultSecret secret) {
        try {
            return withContext(context -> setSecretWithResponse(secret, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<KeyVaultSecret>> setSecretWithResponse(KeyVaultSecret secret, Context context) {
        Objects.requireNonNull(secret, "The Secret input parameter cannot be null.");
        SecretRequestParameters parameters = new SecretRequestParameters()
            .setValue(secret.getValue())
            .setTags(secret.getProperties().getTags())
            .setContentType(secret.getProperties().getContentType())
            .setSecretAttributes(new SecretRequestAttributes(secret.getProperties()));

        return service.setSecret(vaultUrl, secret.getName(), API_VERSION, ACCEPT_LANGUAGE, parameters,
            CONTENT_TYPE_HEADER_VALUE, context)
            .doOnRequest(ignored -> logger.info("Setting secret - {}", secret.getName()))
            .doOnSuccess(response -> logger.info("Set secret - {}", response.getValue().getName()))
            .doOnError(error -> logger.warning("Failed to set secret - {}", secret.getName(), error));
    }

    /**
     * The set operation adds a secret to the key vault. If the named secret already exists, Azure Key
     * Vault creates a new version of that secret. This operation requires the {@code secrets/set}
     * permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Creates a new secret in the key vault. Subscribes to the call asynchronously and prints out
     * the newly created secret details when a response is received.</p>
     * {@codesnippet com.azure.keyvault.secrets.secretclient.setSecret#string-string}
     *
     * @param name The name of the secret. It is required and cannot be null.
     * @param value The value of the secret. It is required and cannot be null.
     * @return A {@link Mono} containing the {@link KeyVaultSecret created secret}.
     * @throws ResourceModifiedException if invalid {@code name} or {@code value} are specified.
     * @throws HttpRequestException if {@code name} or {@code value} is empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<KeyVaultSecret> setSecret(String name, String value) {
        try {
            return withContext(context -> setSecretWithResponse(name, value, context)).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<KeyVaultSecret>> setSecretWithResponse(String name, String value, Context context) {
        SecretRequestParameters parameters = new SecretRequestParameters().setValue(value);
        return service.setSecret(vaultUrl, name, API_VERSION, ACCEPT_LANGUAGE, parameters, CONTENT_TYPE_HEADER_VALUE,
            context)
            .doOnRequest(ignored -> logger.info("Setting secret - {}", name))
            .doOnSuccess(response -> logger.info("Set secret - {}", response.getValue().getName()))
            .doOnError(error -> logger.warning("Failed to set secret - {}", name, error));
    }

    /**
     * Get the specified secret with specified version from the key vault. The get operation is applicable to any secret
     * stored in Azure Key Vault. This operation requires the {@code secrets/get} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Gets a specific version of the secret in the key vault. Subscribes to the call
     * asynchronously and prints out the returned secret details when a response is received.</p>
     * {@codesnippet com.azure.keyvault.secrets.secretclient.getSecret#string-string}
     *
     * @param name The name of the secret, cannot be null
     * @param version The version of the secret to retrieve. If this is an empty String or null, this
     *     call is equivalent to calling {@link #getSecret(String)}, with the latest version being
     *     retrieved.
     * @return A {@link Mono} containing a {@link Response} whose {@link Response#getValue() value}
     *     contains the requested {@link KeyVaultSecret secret}.
     * @throws ResourceNotFoundException when a secret with {@code name} and {@code version} doesn't
     *     exist in the key vault.
     * @throws HttpRequestException if {@code name}  name} or {@code version} is empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<KeyVaultSecret> getSecret(String name, String version) {
        try {
            return getSecretWithResponse(name, version).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Get the specified secret with specified version from the key vault. The get operation is
     * applicable to any secret stored in Azure Key Vault. This operation requires the {@code secrets/get} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Gets a specific version of the secret in the key vault. Subscribes to the call asynchronously and prints out
     * the returned secret details when a response is received.</p>
     * {@codesnippet com.azure.keyvault.secrets.secretclient.getSecretWithResponse#string-string}
     *
     * @param name The name of the secret, cannot be null
     * @param version The version of the secret to retrieve. If this is an empty String or null, this call is equivalent
     *     to calling {@link #getSecret(String)}, with the latest version being retrieved.
     * @return A {@link Mono} containing a {@link Response} whose {@link Response#getValue() value} contains the requested
     *     {@link KeyVaultSecret secret}.
     * @throws ResourceNotFoundException when a secret with {@code name} and {@code version} doesn't exist in the key
     *     vault.
     * @throws HttpRequestException if {@code name}  name} or {@code version} is empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<Response<KeyVaultSecret>> getSecretWithResponse(String name, String version) {
        try {
            return withContext(context -> getSecretWithResponse(name, version, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<KeyVaultSecret>> getSecretWithResponse(String name, String version, Context context) {
        return service.getSecret(vaultUrl, name, version == null ? "" : version, API_VERSION, ACCEPT_LANGUAGE,
            CONTENT_TYPE_HEADER_VALUE, context)
            .doOnRequest(ignoredValue -> logger.info("Retrieving secret - {}", name))
            .doOnSuccess(response -> logger.info("Retrieved secret - {}", response.getValue().getName()))
            .doOnError(error -> logger.warning("Failed to get secret - {}", name, error));
    }

    /**
     * Get the latest version of the specified secret from the key vault. The get operation is applicable to any secret
     * stored in Azure Key Vault.
     * This operation requires the {@code secrets/get} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Gets latest version of the secret in the key vault. Subscribes to the call asynchronously and prints out the
     * returned secret details when a response is received.</p>
     * {@codesnippet com.azure.keyvault.secrets.secretclient.getSecret#string}
     *
     * @param name The name of the secret.
     * @return A {@link Mono} containing the requested {@link KeyVaultSecret secret}.
     * @throws ResourceNotFoundException when a secret with {@code name} doesn't exist in the key vault.
     * @throws HttpRequestException if {@code name} is empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<KeyVaultSecret> getSecret(String name) {
        try {
            return getSecretWithResponse(name, "").flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Updates the attributes associated with the specified secret, but not the value of the specified secret in the key
     * vault. The update operation changes specified attributes of an existing stored secret and attributes that are not
     * specified in the request are left unchanged. The value of a secret itself cannot be changed. This operation
     * requires the {@code secrets/set} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Gets latest version of the secret, changes its notBefore time and then updates it in the Azure Key Vault.
     * Subscribes to the call asynchronously and prints out the returned secret details when a response is received.</p>
     * {@codesnippet com.azure.keyvault.secrets.secretclient.updateSecretProperties#secretProperties}
     *
     * <p>The {@code secret} is required and its fields {@link SecretProperties#getName() name} and {@link SecretProperties#getVersion()
     * version} cannot be null.</p>
     *
     * @param secretProperties The {@link SecretProperties secret properties} object with updated properties.
     * @return A {@link Mono} containing the {@link SecretProperties updated secret}.
     * @throws NullPointerException if {@code secret} is {@code null}.
     * @throws ResourceNotFoundException when a secret with {@link SecretProperties#getName() name} and {@link
     *     SecretProperties#getVersion() version} doesn't exist in the key vault.
     * @throws HttpRequestException if {@link SecretProperties#getName()}  name} or {@link SecretProperties#getVersion() version} is
     *     empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<SecretProperties> updateSecretProperties(SecretProperties secretProperties) {
        try {
            return updateSecretPropertiesWithResponse(secretProperties).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Updates the attributes associated with the specified secret, but not the value of the specified secret in the key
     * vault. The update operation changes specified attributes of an existing stored secret and attributes that are not
     * specified in the request are left unchanged. The value of a secret itself cannot be changed. This operation
     * requires the {@code secrets/set} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Gets latest version of the secret, changes its notBefore time and then updates it in the Azure Key Vault.
     * Subscribes to the call asynchronously and prints out the returned secret details when a response is received.</p>
     * {@codesnippet com.azure.keyvault.secrets.secretclient.updateSecretPropertiesWithResponse#secretProperties}
     *
     * <p>The {@code secret} is required and its fields {@link SecretProperties#getName() name} and {@link SecretProperties#getVersion()
     * version} cannot be null.</p>
     *
     * @param secretProperties The {@link SecretProperties secret properties} object with updated properties.
     * @return A {@link Mono} containing a {@link Response} whose {@link Response#getValue() value} contains the {@link
     *     SecretProperties updated secret}.
     * @throws NullPointerException if {@code secret} is {@code null}.
     * @throws ResourceNotFoundException when a secret with {@link SecretProperties#getName() name} and {@link
     *     SecretProperties#getVersion() version} doesn't exist in the key vault.
     * @throws HttpRequestException if {@link SecretProperties#getName()}  name} or {@link SecretProperties#getVersion() version} is
     *     empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<Response<SecretProperties>> updateSecretPropertiesWithResponse(SecretProperties secretProperties) {
        try {
            return withContext(context -> updateSecretPropertiesWithResponse(secretProperties, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<SecretProperties>> updateSecretPropertiesWithResponse(SecretProperties secretProperties, Context context) {
        Objects.requireNonNull(secretProperties, "The secret properties input parameter cannot be null.");
        SecretRequestParameters parameters = new SecretRequestParameters()
            .setTags(secretProperties.getTags())
            .setContentType(secretProperties.getContentType())
            .setSecretAttributes(new SecretRequestAttributes(secretProperties));

        return service.updateSecret(vaultUrl, secretProperties.getName(), secretProperties.getVersion(), API_VERSION, ACCEPT_LANGUAGE,
            parameters, CONTENT_TYPE_HEADER_VALUE, context)
            .doOnRequest(ignored -> logger.info("Updating secret - {}", secretProperties.getName()))
            .doOnSuccess(response -> logger.info("Updated secret - {}", response.getValue().getName()))
            .doOnError(error -> logger.warning("Failed to update secret - {}", secretProperties.getName(), error));
    }

    /**
     * Deletes a secret from the key vault. If soft-delete is enabled on the key vault then the secret is placed in the
     * deleted state and requires to be purged for permanent deletion else the secret is permanently deleted. The delete
     * operation applies to any secret stored in Azure Key Vault but it cannot be applied to an individual version of a
     * secret. This operation requires the {@code secrets/delete} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Deletes the secret in the Azure Key Vault. Subscribes to the call asynchronously and prints out the deleted
     * secret details when a response is received.</p>
     * {@codesnippet com.azure.keyvault.secrets.secretclient.deleteSecret#string}
     *
     * @param name The name of the secret to be deleted.
     * @return A {@link Poller} to poll on and retrieve {@link DeletedSecret deleted secret}.
     * @throws ResourceNotFoundException when a secret with {@code name} doesn't exist in the key vault.
     * @throws HttpRequestException when a secret with {@code name} is empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Poller<DeletedSecret, Void> beginDeleteSecret(String name) {
        return new Poller<>(Duration.ofSeconds(1), createPollOperation(name), () -> Mono.empty(), activationOperation(name), null);
    }

    private Supplier<Mono<DeletedSecret>> activationOperation(String name) {
        return () -> withContext(context -> deleteSecretWithResponse(name, context)
            .flatMap(deletedSecretResponse -> Mono.just(deletedSecretResponse.getValue())));
    }

    /*
    Polling operation to poll on create delete key operation status.
    */
    private Function<PollResponse<DeletedSecret>, Mono<PollResponse<DeletedSecret>>> createPollOperation(String keyName) {
        return prePollResponse ->
            withContext(context -> service.getDeletedSecretPoller(vaultUrl, keyName, API_VERSION, ACCEPT_LANGUAGE, CONTENT_TYPE_HEADER_VALUE, context)
                .flatMap(deletedSecretResponse -> {
                    if (deletedSecretResponse.getStatusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                        return Mono.defer(() -> Mono.just(new PollResponse<>(PollResponse.OperationStatus.IN_PROGRESS, prePollResponse.getValue())));
                    }
                    return Mono.defer(() -> Mono.just(new PollResponse<>(PollResponse.OperationStatus.SUCCESSFULLY_COMPLETED, deletedSecretResponse.getValue())));
                }))
                // This means either vault has soft-delete disabled or permission is not granted for the get deleted key operation.
                // In both cases deletion operation was successful when activation operation succeeded before reaching here.
                .onErrorReturn(new PollResponse<>(PollResponse.OperationStatus.SUCCESSFULLY_COMPLETED, prePollResponse.getValue()));
    }

    Mono<Response<DeletedSecret>> deleteSecretWithResponse(String name, Context context) {
        return service.deleteSecret(vaultUrl, name, API_VERSION, ACCEPT_LANGUAGE, CONTENT_TYPE_HEADER_VALUE, context)
            .doOnRequest(ignored -> logger.info("Deleting secret - {}", name))
            .doOnSuccess(response -> logger.info("Deleted secret - {}", response.getValue().getName()))
            .doOnError(error -> logger.warning("Failed to delete secret - {}", name, error));
    }

    /**
     * The get deleted secret operation returns the secrets that have been deleted for a vault enabled
     * for soft-delete. This operation requires the {@code secrets/list} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p> Gets the deleted secret from the key vault enabled for soft-delete. Subscribes to the call
     * asynchronously and prints out the deleted secret details when a response is received.</p>
     *
     * //Assuming secret is deleted on a soft-delete enabled vault.
     * {@codesnippet com.azure.keyvault.secrets.secretclient.getDeletedSecret#string}
     *
     * @param name The name of the deleted secret.
     * @return A {@link Mono} containing the {@link DeletedSecret deleted secret}.
     * @throws ResourceNotFoundException when a secret with {@code name} doesn't exist in the key vault.
     * @throws HttpRequestException when a secret with {@code name} is empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<DeletedSecret> getDeletedSecret(String name) {
        try {
            return getDeletedSecretWithResponse(name).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * The get deleted secret operation returns the secrets that have been deleted for a vault enabled
     * for soft-delete. This operation requires the {@code secrets/list} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p> Gets the deleted secret from the key vault enabled for soft-delete. Subscribes to the call
     * asynchronously and prints out the deleted secret details when a response is received.</p>
     *
     * //Assuming secret is deleted on a soft-delete enabled vault.
     * {@codesnippet com.azure.keyvault.secrets.secretclient.getDeletedSecretWithResponse#string}
     *
     * @param name The name of the deleted secret.
     * @return A {@link Mono} containing a {@link Response} whose {@link Response#getValue() value} contains the
     *     {@link DeletedSecret deleted secret}.
     * @throws ResourceNotFoundException when a secret with {@code name} doesn't exist in the key vault.
     * @throws HttpRequestException when a secret with {@code name} is empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<Response<DeletedSecret>> getDeletedSecretWithResponse(String name) {
        try {
            return withContext(context -> getDeletedSecretWithResponse(name, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<DeletedSecret>> getDeletedSecretWithResponse(String name, Context context) {
        return service.getDeletedSecret(vaultUrl, name, API_VERSION, ACCEPT_LANGUAGE, CONTENT_TYPE_HEADER_VALUE,
            context)
            .doOnRequest(ignored -> logger.info("Retrieving deleted secret - {}", name))
            .doOnSuccess(response -> logger.info("Retrieved deleted secret - {}", response.getValue().getName()))
            .doOnError(error -> logger.warning("Failed to retrieve deleted secret - {}", name, error));
    }

    /**
     * The purge deleted secret operation removes the secret permanently, without the possibility of
     * recovery. This operation can only be enabled on a soft-delete enabled vault. This operation
     * requires the {@code secrets/purge} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Purges the deleted secret from the key vault enabled for soft-delete. Subscribes to the call
     * asynchronously and prints out the status code from the server response when a response is received.</p>
     *
     * //Assuming secret is deleted on a soft-delete enabled vault.
     * {@codesnippet com.azure.keyvault.secrets.secretclient.purgeDeletedSecret#string}
     *
     * @param name The name of the secret.
     * @return An empty {@link Mono}.
     * @throws ResourceNotFoundException when a secret with {@code name} doesn't exist in the key vault.
     * @throws HttpRequestException when a secret with {@code name} is empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<Void> purgeDeletedSecret(String name) {
        try {
            return purgeDeletedSecretWithResponse(name).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * The purge deleted secret operation removes the secret permanently, without the possibility of
     * recovery. This operation can only be enabled on a soft-delete enabled vault. This operation
     * requires the {@code secrets/purge} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Purges the deleted secret from the key vault enabled for soft-delete. Subscribes to the call
     * asynchronously and prints out the status code from the server response when a response is received.</p>
     *
     * //Assuming secret is deleted on a soft-delete enabled vault.
     * {@codesnippet com.azure.keyvault.secrets.secretclient.purgeDeletedSecretWithResponse#string}
     *
     * @param name The name of the secret.
     * @return A {@link Mono} containing a Response containing status code and HTTP headers.
     * @throws ResourceNotFoundException when a secret with {@code name} doesn't exist in the key vault.
     * @throws HttpRequestException when a secret with {@code name} is empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<Response<Void>> purgeDeletedSecretWithResponse(String name) {
        try {
            return withContext(context -> purgeDeletedSecretWithResponse(name, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<Void>> purgeDeletedSecretWithResponse(String name, Context context) {
        return service.purgeDeletedSecret(vaultUrl, name, API_VERSION, ACCEPT_LANGUAGE, CONTENT_TYPE_HEADER_VALUE,
            context)
            .doOnRequest(ignored -> logger.info("Purging deleted secret - {}", name))
            .doOnSuccess(response -> logger.info("Purged deleted secret - {}", name))
            .doOnError(error -> logger.warning("Failed to purge deleted secret - {}", name, error));
    }

    /**
     * Recovers the deleted secret in the key vault to its latest version and can only be performed on a soft-delete
     * enabled vault.
     * This operation requires the {@code secrets/recover} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Recovers the deleted secret from the key vault enabled for soft-delete. Subscribes to the call asynchronously
     * and prints out the recovered secret details when a response is received.</p>
     *
     * //Assuming secret is deleted on a soft-delete enabled vault.
     * {@codesnippet com.azure.keyvault.secrets.secretclient.recoverDeletedSecret#string}
     *
     * @param name The name of the deleted secret to be recovered.
     * @return A {@link Poller} to poll on and retrieve the {@link KeyVaultSecret recovered secret}.
     * @throws ResourceNotFoundException when a secret with {@code name} doesn't exist in the key vault.
     * @throws HttpRequestException when a secret with {@code name} is empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Poller<KeyVaultSecret, Void> beginRecoverDeletedSecret(String name) {
        return new Poller<>(Duration.ofSeconds(1), createRecoverPollOperation(name), () -> Mono.empty(), recoverActivationOperation(name), null);
    }

    private Supplier<Mono<KeyVaultSecret>> recoverActivationOperation(String name) {
        return () -> withContext(context -> recoverDeletedSecretWithResponse(name, context)
            .flatMap(keyResponse -> Mono.just(keyResponse.getValue())));
    }

    /*
    Polling operation to poll on create delete key operation status.
    */
    private Function<PollResponse<KeyVaultSecret>, Mono<PollResponse<KeyVaultSecret>>> createRecoverPollOperation(String secretName) {
        return prePollResponse ->
            withContext(context -> service.getSecretPoller(vaultUrl, secretName, "", API_VERSION, ACCEPT_LANGUAGE, CONTENT_TYPE_HEADER_VALUE, context)
                .flatMap(secretResponse -> {
                    if (secretResponse.getStatusCode() == 404) {
                        return Mono.defer(() -> Mono.just(new PollResponse<>(PollResponse.OperationStatus.IN_PROGRESS, prePollResponse.getValue())));
                    }
                    return Mono.defer(() -> Mono.just(new PollResponse<>(PollResponse.OperationStatus.SUCCESSFULLY_COMPLETED, secretResponse.getValue())));
                }))
                // This means permission is not granted for the get deleted key operation.
                // In both cases deletion operation was successful when activation operation succeeded before reaching here.
                .onErrorReturn(new PollResponse<>(PollResponse.OperationStatus.SUCCESSFULLY_COMPLETED, prePollResponse.getValue()));
    }

    Mono<Response<KeyVaultSecret>> recoverDeletedSecretWithResponse(String name, Context context) {
        return service.recoverDeletedSecret(vaultUrl, name, API_VERSION, ACCEPT_LANGUAGE, CONTENT_TYPE_HEADER_VALUE,
            context)
            .doOnRequest(ignored -> logger.info("Recovering deleted secret - {}", name))
            .doOnSuccess(response -> logger.info("Recovered deleted secret - {}", response.getValue().getName()))
            .doOnError(error -> logger.warning("Failed to recover deleted secret - {}", name, error));
    }

    /**
     * Requests a backup of the specified secret be downloaded to the client. All versions of the
     * secret will be downloaded. This operation requires the {@code secrets/backup} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Backs up the secret from the key vault. Subscribes to the call asynchronously and prints out
     * the length of the secret's backup byte array returned in the response.</p>
     *
     * {@codesnippet com.azure.keyvault.secrets.secretclient.backupSecret#string}
     *
     * @param name The name of the secret.
     * @return A {@link Mono} containing the backed up secret blob.
     * @throws ResourceNotFoundException when a secret with {@code name} doesn't exist in the key vault.
     * @throws HttpRequestException when a secret with {@code name} is empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<byte[]> backupSecret(String name) {
        try {
            return backupSecretWithResponse(name).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Requests a backup of the specified secret be downloaded to the client. All versions of the
     * secret will be downloaded. This operation requires the {@code secrets/backup} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Backs up the secret from the key vault. Subscribes to the call asynchronously and prints out
     * the length of the secret's backup byte array returned in the response.</p>
     * {@codesnippet com.azure.keyvault.secrets.secretclient.backupSecretWithResponse#string}
     *
     * @param name The name of the secret.
     * @return A {@link Mono} containing a {@link Response} whose {@link Response#getValue() value}
     *     contains the backed up secret blob.
     * @throws ResourceNotFoundException when a secret with {@code name} doesn't exist in the key
     *     vault.
     * @throws HttpRequestException when a secret with {@code name} is empty string.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<Response<byte[]>> backupSecretWithResponse(String name) {
        try {
            return withContext(context -> backupSecretWithResponse(name, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<byte[]>> backupSecretWithResponse(String name, Context context) {
        return service.backupSecret(vaultUrl, name, API_VERSION, ACCEPT_LANGUAGE, CONTENT_TYPE_HEADER_VALUE, context)
            .doOnRequest(ignored -> logger.info("Backing up secret - {}", name))
            .doOnSuccess(response -> logger.info("Backed up secret - {}", name))
            .doOnError(error -> logger.warning("Failed to back up secret - {}", name, error))
            .flatMap(base64URLResponse -> Mono.just(new SimpleResponse<byte[]>(base64URLResponse.getRequest(),
                base64URLResponse.getStatusCode(), base64URLResponse.getHeaders(), base64URLResponse.getValue().getValue())));
    }

    /**
     * Restores a backed up secret, and all its versions, to a vault. This operation requires the
     * {@code secrets/restore} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Restores the secret in the key vault from its backup. Subscribes to the call asynchronously
     * and prints out the restored secret details when a response is received.</p>
     *
     * //Pass the Secret Backup Byte array to the restore operation.
     * {@codesnippet com.azure.keyvault.secrets.secretclient.restoreSecret#byte}
     *
     * @param backup The backup blob associated with the secret.
     * @return A {@link Mono} containing the {@link KeyVaultSecret restored secret}.
     * @throws ResourceModifiedException when {@code backup} blob is malformed.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<KeyVaultSecret> restoreSecretBackup(byte[] backup) {
        try {
            return restoreSecretBackupWithResponse(backup).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Restores a backed up secret, and all its versions, to a vault. This operation requires the
     * {@code secrets/restore} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Restores the secret in the key vault from its backup. Subscribes to the call asynchronously
     * and prints out the restored secret details when a response is received.</p>
     *
     * //Pass the Secret Backup Byte array to the restore operation.
     * {@codesnippet com.azure.keyvault.secrets.secretclient.restoreSecretWithResponse#byte}
     *
     * @param backup The backup blob associated with the secret.
     * @return A {@link Mono} containing a {@link Response} whose {@link Response#getValue() value}
     *     contains the {@link KeyVaultSecret restored secret}.
     * @throws ResourceModifiedException when {@code backup} blob is malformed.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Mono<Response<KeyVaultSecret>> restoreSecretBackupWithResponse(byte[] backup) {
        try {
            return withContext(context -> restoreSecretBackupWithResponse(backup, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<KeyVaultSecret>> restoreSecretBackupWithResponse(byte[] backup, Context context) {
        SecretRestoreRequestParameters parameters = new SecretRestoreRequestParameters().setSecretBackup(backup);
        return service.restoreSecret(vaultUrl, API_VERSION, ACCEPT_LANGUAGE, parameters, CONTENT_TYPE_HEADER_VALUE,
            context)
            .doOnRequest(ignored -> logger.info("Attempting to restore secret"))
            .doOnSuccess(response -> logger.info("Restored secret - {}", response.getValue().getName()))
            .doOnError(error -> logger.warning("Failed to restore secret", error));
    }

    /**
     * List secrets in the key vault. The list Secrets operation is applicable to the entire vault. The individual
     * secret response in the flux is represented by {@link SecretProperties} as only the secret identifier and its
     * attributes are provided in the response. The secret values and individual secret versions are not listed in the
     * response. This operation requires the {@code secrets/list} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>It is possible to get full Secrets with values from this information. Convert the {@link Flux} containing
     * {@link SecretProperties secret properties} to
     * {@link Flux} containing {@link KeyVaultSecret secret} using {@link SecretAsyncClient#getSecret(String, String)}
     * within {@link Flux#flatMap(Function)}.</p>
     * {@codesnippet com.azure.keyvault.secrets.secretclient.listSecrets}
     *
     * @return A {@link PagedFlux} containing {@link SecretProperties secret} of all the secrets in the vault.
     */
    @ServiceMethod(returns = ReturnType.COLLECTION)
    public PagedFlux<SecretProperties> listPropertiesOfSecrets() {
        try {
            return new PagedFlux<>(
                () -> withContext(context -> listSecretsFirstPage(context)),
                continuationToken -> withContext(context -> listSecretsNextPage(continuationToken, context)));
        } catch (RuntimeException ex) {
            return new PagedFlux<>(() -> monoError(logger, ex));
        }
    }

    PagedFlux<SecretProperties> listPropertiesOfSecrets(Context context) {
        return new PagedFlux<>(
            () -> listSecretsFirstPage(context),
            continuationToken -> listSecretsNextPage(continuationToken, context));
    }

    /*
     * Gets attributes of all the secrets given by the {@code nextPageLink} that was retrieved from a call to
     * {@link SecretAsyncClient#listSecrets()}.
     *
     * @param continuationToken The {@link PagedResponse#nextLink()} from a previous, successful call to one of the
     * list operations.
     * @return A {@link Mono} of {@link PagedResponse<SecretProperties>} from the next page of results.
     */
    private Mono<PagedResponse<SecretProperties>> listSecretsNextPage(String continuationToken, Context context) {
        try {
            return service.getSecrets(vaultUrl, continuationToken, ACCEPT_LANGUAGE, CONTENT_TYPE_HEADER_VALUE, context)
                .doOnRequest(ignoredValue -> logger.info("Retrieving the next secrets page - Page {}", continuationToken))
                .doOnSuccess(response -> logger.info("Retrieved the next secrets page - Page {}", continuationToken))
                .doOnError(error -> logger.warning("Failed to retrieve the next secrets page - Page {}",
                    continuationToken, error));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /*
     * Calls the service and retrieve first page result. It makes one call and retrieve {@code
     * DEFAULT_MAX_PAGE_RESULTS} values.
     */
    private Mono<PagedResponse<SecretProperties>> listSecretsFirstPage(Context context) {
        try {
            return service.getSecrets(vaultUrl, DEFAULT_MAX_PAGE_RESULTS, API_VERSION, ACCEPT_LANGUAGE,
                CONTENT_TYPE_HEADER_VALUE, context)
                .doOnRequest(ignored -> logger.info("Listing secrets"))
                .doOnSuccess(response -> logger.info("Listed secrets"))
                .doOnError(error -> logger.warning("Failed to list secrets", error));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Lists {@link DeletedSecret deleted secrets} of the key vault. The get deleted secrets operation returns the
     * secrets that have been deleted for a vault enabled for soft-delete. This operation requires the
     * {@code secrets/list} permission.
     *
     * <p><strong>Code Samples</strong></p>
     * <p>Lists the deleted secrets in the key vault. Subscribes to the call asynchronously and prints out the
     * recovery id of each deleted secret when a response is received.</p>
     * {@codesnippet com.azure.keyvault.secrets.secretclient.listDeletedSecrets}
     *
     * @return A {@link Flux} containing all of the {@link DeletedSecret deleted secrets} in the vault.
     */
    @ServiceMethod(returns = ReturnType.COLLECTION)
    public PagedFlux<DeletedSecret> listDeletedSecrets() {
        try {
            return new PagedFlux<>(
                () -> withContext(context -> listDeletedSecretsFirstPage(context)),
                continuationToken -> withContext(context -> listDeletedSecretsNextPage(continuationToken, context)));
        } catch (RuntimeException ex) {
            return new PagedFlux<>(() -> monoError(logger, ex));
        }
    }

    PagedFlux<DeletedSecret> listDeletedSecrets(Context context) {
        return new PagedFlux<>(
            () -> listDeletedSecretsFirstPage(context),
            continuationToken -> listDeletedSecretsNextPage(continuationToken, context));
    }


    /**
     * Gets attributes of all the secrets given by the {@code nextPageLink} that was retrieved from a call to
     * {@link SecretAsyncClient#listDeletedSecrets()}.
     *
     * @param continuationToken The {@link Page#getContinuationToken()} from a previous, successful call to one of the
     *     list operations.
     * @return A {@link Mono} of {@link PagedResponse} that contains {@link DeletedSecret} from the next page of
     * results.
     */
    private Mono<PagedResponse<DeletedSecret>> listDeletedSecretsNextPage(String continuationToken, Context context) {
        try {
            return service.getDeletedSecrets(vaultUrl, continuationToken, ACCEPT_LANGUAGE, CONTENT_TYPE_HEADER_VALUE,
                context)
                .doOnRequest(ignoredValue -> logger.info("Retrieving the next deleted secrets page - Page {}",
                    continuationToken))
                .doOnSuccess(response -> logger.info("Retrieved the next deleted secrets page - Page {}",
                    continuationToken))
                .doOnError(error -> logger.warning("Failed to retrieve the next deleted secrets page - Page {}",
                    continuationToken, error));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /*
     * Calls the service and retrieve first page result. It makes one call and retrieve {@code
     * DEFAULT_MAX_PAGE_RESULTS} values.
     */
    private Mono<PagedResponse<DeletedSecret>> listDeletedSecretsFirstPage(Context context) {
        try {
            return service.getDeletedSecrets(vaultUrl, DEFAULT_MAX_PAGE_RESULTS, API_VERSION, ACCEPT_LANGUAGE,
                CONTENT_TYPE_HEADER_VALUE, context)
                .doOnRequest(ignored -> logger.info("Listing deleted secrets"))
                .doOnSuccess(response -> logger.info("Listed deleted secrets"))
                .doOnError(error -> logger.warning("Failed to list deleted secrets", error));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * List all versions of the specified secret. The individual secret response in the flux is represented by {@link
     * SecretProperties} as only the secret identifier and its attributes are provided in the response. The secret values
     * are not provided in the response. This operation requires the {@code secrets/list} permission.
     *
     * <p>It is possible to get the Secret with value of all the versions from this information. Convert the {@link
     * Flux} containing {@link SecretProperties secret} to {@link Flux} containing {@link KeyVaultSecret secret} using
     * {@link SecretAsyncClient#getSecret(String, String)} within {@link Flux#flatMap(Function)}.</p>
     *
     * {@codesnippet com.azure.keyvault.secrets.secretclient.listSecretVersions#string}
     *
     * @param name The name of the secret.
     * @return A {@link PagedFlux} containing {@link SecretProperties secret} of all the versions of the specified secret in
     *     the vault. Flux is empty if secret with {@code name} does not exist in key vault
     * @throws ResourceNotFoundException when a secret with {@code name} doesn't exist in the key vault.
     * @throws HttpRequestException when a secret with {@code name} is empty string.
     */
    @ServiceMethod(returns = ReturnType.COLLECTION)
    public PagedFlux<SecretProperties> listPropertiesOfSecretVersions(String name) {
        try {
            return new PagedFlux<>(
                () -> withContext(context -> listSecretVersionsFirstPage(name, context)),
                continuationToken -> withContext(context -> listSecretVersionsNextPage(continuationToken, context)));
        } catch (RuntimeException ex) {
            return new PagedFlux<>(() -> monoError(logger, ex));
        }
    }

    PagedFlux<SecretProperties> listPropertiesOfSecretVersions(String name, Context context) {
        return new PagedFlux<>(
            () -> listSecretVersionsFirstPage(name, context),
            continuationToken -> listSecretVersionsNextPage(continuationToken, context));
    }

    /*
     * Gets attributes of all the secrets versions given by the {@code nextPageLink} that was retrieved from a call to
     * {@link SecretAsyncClient#listSecretVersions()}.
     *
     * @param continuationToken The {@link PagedResponse#nextLink()} from a previous, successful call to one of the
     * list operations.
     *
     * @return A {@link Mono} of {@link PagedResponse<SecretProperties>} from the next page of results.
     */
    private Mono<PagedResponse<SecretProperties>> listSecretVersionsNextPage(String continuationToken, Context context) {
        try {
            return service.getSecrets(vaultUrl, continuationToken, ACCEPT_LANGUAGE, CONTENT_TYPE_HEADER_VALUE, context)
                .doOnRequest(ignoredValue -> logger.info("Retrieving the next secrets versions page - Page {}",
                    continuationToken))
                .doOnSuccess(response -> logger.info("Retrieved the next secrets versions page - Page {}",
                    continuationToken))
                .doOnError(error -> logger.warning("Failed to retrieve the next secrets versions page - Page {}",
                    continuationToken, error));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /*
     * Calls the service and retrieve first page result. It makes one call and retrieve {@code
     * DEFAULT_MAX_PAGE_RESULTS} values.
     */
    private Mono<PagedResponse<SecretProperties>> listSecretVersionsFirstPage(String name, Context context) {
        try {
            return service.getSecretVersions(vaultUrl, name, DEFAULT_MAX_PAGE_RESULTS, API_VERSION, ACCEPT_LANGUAGE,
                CONTENT_TYPE_HEADER_VALUE, context)
                .doOnRequest(ignored -> logger.info("Listing secret versions - {}", name))
                .doOnSuccess(response -> logger.info("Listed secret versions - {}", name))
                .doOnError(error -> logger.warning(String.format("Failed to list secret versions - {}", name), error));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }
}
