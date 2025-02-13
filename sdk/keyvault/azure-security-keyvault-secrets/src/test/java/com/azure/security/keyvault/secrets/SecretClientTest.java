// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.security.keyvault.secrets;

import com.azure.core.exception.ResourceModifiedException;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.Poller;
import com.azure.security.keyvault.secrets.models.DeletedSecret;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SecretClientTest extends SecretClientTestBase {

    private SecretClient client;

    @Override
    protected void beforeTest() {
        beforeTestSetup();

        if (interceptorManager.isPlaybackMode()) {
            client = clientSetup(pipeline -> new SecretClientBuilder()
                .pipeline(pipeline)
                .vaultUrl(getEndpoint())
                .buildClient());
        } else {
            client = clientSetup(pipeline -> new SecretClientBuilder()
                .pipeline(pipeline)
                .vaultUrl(getEndpoint())
                .buildClient());
        }
    }

    /**
     * Tests that a secret can be created in the key vault.
     */
    public void setSecret() {
        setSecretRunner((expected) -> assertSecretEquals(expected, client.setSecret(expected)));
    }

    /**
     * Tests that we cannot create a secret when the secret is an empty string.
     */
    public void setSecretEmptyName() {
        assertRestException(() -> client.setSecret("", "A value"), HttpURLConnection.HTTP_BAD_METHOD);
    }

    /**
     * Tests that we can create secrets when value is not null or an empty string.
     */
    public void setSecretEmptyValue() {
        setSecretEmptyValueRunner((secret) -> {
            assertSecretEquals(secret, client.setSecret(secret.getName(), secret.getValue()));
            assertSecretEquals(secret, client.getSecret(secret.getName()));
        });
    }

    /**
     * Verifies that an exception is thrown when null secret object is passed for creation.
     */
    public void setSecretNull() {
        assertRunnableThrowsException(() -> client.setSecret(null), NullPointerException.class);
    }

    /**
     * Tests that a secret is able to be updated when it exists.
     */
    public void updateSecret() {
        updateSecretRunner((original, updated) -> {
            assertSecretEquals(original, client.setSecret(original));
            KeyVaultSecret secretToUpdate = client.getSecret(original.getName());
            client.updateSecretProperties(secretToUpdate.getProperties().setExpiresOn(updated.getProperties().getExpiresOn()));
            assertSecretEquals(updated, client.getSecret(original.getName()));
        });
    }

    /**
     * Tests that a secret is not able to be updated when it is disabled. 403 error is expected.
     */
    public void updateDisabledSecret() {
        updateDisabledSecretRunner((original, updated) -> {
            assertSecretEquals(original, client.setSecret(original));
            assertRestException(() -> client.getSecret(original.getName()), ResourceModifiedException.class, HttpURLConnection.HTTP_FORBIDDEN);
        });
    }

    /**
     * Tests that an existing secret can be retrieved.
     */
    public void getSecret() {
        getSecretRunner((original) -> {
            client.setSecret(original);
            assertSecretEquals(original, client.getSecret(original.getName()));
        });
    }

    /**
     * Tests that a specific version of the secret can be retrieved.
     */
    public void getSecretSpecificVersion() {
        getSecretSpecificVersionRunner((secret, secretWithNewVal) -> {
            KeyVaultSecret secretVersionOne = client.setSecret(secret);
            KeyVaultSecret secretVersionTwo = client.setSecret(secretWithNewVal);
            assertSecretEquals(secret, client.getSecret(secretVersionOne.getName(), secretVersionOne.getProperties().getVersion()));
            assertSecretEquals(secretWithNewVal, client.getSecret(secretVersionTwo.getName(), secretVersionTwo.getProperties().getVersion()));
        });
    }

    /**
     * Tests that an attempt to get a non-existing secret throws an error.
     */
    public void getSecretNotFound() {
        assertRestException(() -> client.getSecret("non-existing"),  ResourceNotFoundException.class, HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * Tests that an existing secret can be deleted.
     */
    public void deleteSecret() {
        deleteSecretRunner((secretToDelete) -> {
            assertSecretEquals(secretToDelete,  client.setSecret(secretToDelete));
            Poller<DeletedSecret, Void> poller = client.beginDeleteSecret(secretToDelete.getName());
            while (!poller.isComplete()) { sleepInRecordMode(1000); }
            DeletedSecret deletedSecret = poller.getLastPollResponse().getValue();
            assertNotNull(deletedSecret.getDeletedOn());
            assertNotNull(deletedSecret.getRecoveryId());
            assertNotNull(deletedSecret.getScheduledPurgeDate());
            assertEquals(secretToDelete.getName(), deletedSecret.getName());
            client.purgeDeletedSecret(secretToDelete.getName());
            pollOnSecretPurge(secretToDelete.getName());
        });
    }

    public void deleteSecretNotFound() {
        Poller<DeletedSecret, Void> poller = client.beginDeleteSecret("non-existing");
        while (!poller.isComplete()) { sleepInRecordMode(1000); }
        assertEquals(poller.getStatus(), PollResponse.OperationStatus.FAILED);
    }

    /**
     * Tests that a deleted secret can be retrieved on a soft-delete enabled vault.
     */
    public void getDeletedSecret() {
        getDeletedSecretRunner((secretToDeleteAndGet) -> {
            assertSecretEquals(secretToDeleteAndGet, client.setSecret(secretToDeleteAndGet));
            Poller<DeletedSecret, Void> poller = client.beginDeleteSecret(secretToDeleteAndGet.getName());
            while (!poller.isComplete()) { sleepInRecordMode(1000); }
            sleepInRecordMode(30000);
            DeletedSecret deletedSecret = client.getDeletedSecret(secretToDeleteAndGet.getName());
            assertNotNull(deletedSecret.getDeletedOn());
            assertNotNull(deletedSecret.getRecoveryId());
            assertNotNull(deletedSecret.getScheduledPurgeDate());
            assertEquals(secretToDeleteAndGet.getName(), deletedSecret.getName());
            client.purgeDeletedSecret(secretToDeleteAndGet.getName());
            pollOnSecretPurge(secretToDeleteAndGet.getName());
            sleepInRecordMode(10000);
        });
    }

    /**
     * Tests that an attempt to retrieve a non existing deleted secret throws an error on a soft-delete enabled vault.
     */
    public void getDeletedSecretNotFound() {
        assertRestException(() -> client.getDeletedSecret("non-existing"),  ResourceNotFoundException.class, HttpURLConnection.HTTP_NOT_FOUND);
    }


    /**
     * Tests that a deleted secret can be recovered on a soft-delete enabled vault.
     */
    public void recoverDeletedSecret() {
        recoverDeletedSecretRunner((secretToDeleteAndRecover) -> {
            assertSecretEquals(secretToDeleteAndRecover, client.setSecret(secretToDeleteAndRecover));
            Poller<DeletedSecret, Void> delPoller = client.beginDeleteSecret(secretToDeleteAndRecover.getName());
            while (!delPoller.isComplete()) { sleepInRecordMode(1000); }
            Poller<KeyVaultSecret, Void> poller = client.beginRecoverDeletedSecret(secretToDeleteAndRecover.getName());
            while (!poller.isComplete()) { sleepInRecordMode(1000); }
            KeyVaultSecret recoveredSecret = poller.getLastPollResponse().getValue();
            assertEquals(secretToDeleteAndRecover.getName(), recoveredSecret.getName());
            assertEquals(secretToDeleteAndRecover.getProperties().getNotBefore(), recoveredSecret.getProperties().getNotBefore());
            assertEquals(secretToDeleteAndRecover.getProperties().getExpiresOn(), recoveredSecret.getProperties().getExpiresOn());
        });
    }

    /**
     * Tests that an attempt to recover a non existing deleted secret throws an error on a soft-delete enabled vault.
     */
    public void recoverDeletedSecretNotFound() {
        Poller<KeyVaultSecret, Void> poller = client.beginRecoverDeletedSecret("non-existing");
        while (!poller.isComplete()) { sleepInRecordMode(1000); }
        assertEquals(poller.getStatus(), PollResponse.OperationStatus.FAILED);
    }

    /**
     * Tests that a secret can be backed up in the key vault.
     */
    public void backupSecret() {
        backupSecretRunner((secretToBackup) -> {
            assertSecretEquals(secretToBackup, client.setSecret(secretToBackup));
            byte[] backupBytes = (client.backupSecret(secretToBackup.getName()));
            assertNotNull(backupBytes);
            assertTrue(backupBytes.length > 0);
        });
    }

    /**
     * Tests that an attempt to backup a non existing secret throws an error.
     */
    public void backupSecretNotFound() {
        assertRestException(() -> client.backupSecret("non-existing"),  ResourceNotFoundException.class, HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * Tests that a secret can be backed up in the key vault.
     */
    public synchronized void restoreSecret() {
        restoreSecretRunner((secretToBackupAndRestore) -> {
            assertSecretEquals(secretToBackupAndRestore, client.setSecret(secretToBackupAndRestore));
            byte[] backupBytes = (client.backupSecret(secretToBackupAndRestore.getName()));
            assertNotNull(backupBytes);
            assertTrue(backupBytes.length > 0);
            Poller<DeletedSecret, Void> poller = client.beginDeleteSecret(secretToBackupAndRestore.getName());
            while (!poller.isComplete()) { sleepInRecordMode(1000); }
            client.purgeDeletedSecret(secretToBackupAndRestore.getName());
            pollOnSecretPurge(secretToBackupAndRestore.getName());
            sleepInRecordMode(60000);
            KeyVaultSecret restoredSecret = client.restoreSecretBackup(backupBytes);
            assertEquals(secretToBackupAndRestore.getName(), restoredSecret.getName());
            assertEquals(secretToBackupAndRestore.getProperties().getExpiresOn(), restoredSecret.getProperties().getExpiresOn());
        });
    }

    /**
     * Tests that an attempt to restore a secret from malformed backup bytes throws an error.
     */
    public void restoreSecretFromMalformedBackup() {
        byte[] secretBackupBytes = "non-existing".getBytes();
        assertRestException(() -> client.restoreSecretBackup(secretBackupBytes), ResourceModifiedException.class, HttpURLConnection.HTTP_BAD_REQUEST);
    }

    /**
     * Tests that secrets can be listed in the key vault.
     */
    public void listSecrets() {
        listSecretsRunner((secrets) -> {
            for (KeyVaultSecret secret :  secrets.values()) {
                assertSecretEquals(secret, client.setSecret(secret));
            }

            for (SecretProperties actualSecret : client.listPropertiesOfSecrets()) {
                if (secrets.containsKey(actualSecret.getName())) {
                    KeyVaultSecret expectedSecret = secrets.get(actualSecret.getName());
                    assertEquals(expectedSecret.getProperties().getExpiresOn(), actualSecret.getExpiresOn());
                    assertEquals(expectedSecret.getProperties().getNotBefore(), actualSecret.getNotBefore());
                    secrets.remove(actualSecret.getName());
                }
            }
            assertEquals(0, secrets.size());
        });
    }

    /**
     * Tests that deleted secrets can be listed in the key vault.
     */
    @Override
    public void listDeletedSecrets() {
        listDeletedSecretsRunner((secrets) -> {

            for (KeyVaultSecret secret : secrets.values()) {
                assertSecretEquals(secret, client.setSecret(secret));
            }

            for (KeyVaultSecret secret : secrets.values()) {
                Poller<DeletedSecret, Void> poller = client.beginDeleteSecret(secret.getName());
                while (!poller.isComplete()) { sleepInRecordMode(1000); }
            }

            sleepInRecordMode(60000);
            Iterable<DeletedSecret> deletedSecrets =  client.listDeletedSecrets();
            for (DeletedSecret actualSecret : deletedSecrets) {
                if (secrets.containsKey(actualSecret.getName())) {
                    assertNotNull(actualSecret.getDeletedOn());
                    assertNotNull(actualSecret.getRecoveryId());
                    secrets.remove(actualSecret.getName());
                }
            }

            assertEquals(0, secrets.size());

            for (DeletedSecret deletedSecret : deletedSecrets) {
                client.purgeDeletedSecret(deletedSecret.getName());
                pollOnSecretPurge(deletedSecret.getName());
            }
            sleepInRecordMode(10000);
        });
    }

    /**
     * Tests that secret versions can be listed in the key vault.
     */
    @Override
    public void listSecretVersions() {
        listSecretVersionsRunner((secrets) -> {
            List<KeyVaultSecret> secretVersions = secrets;
            String secretName = null;
            for (KeyVaultSecret secret : secretVersions) {
                secretName = secret.getName();
                assertSecretEquals(secret, client.setSecret(secret));
            }

            Iterable<SecretProperties> secretVersionsOutput =  client.listPropertiesOfSecretVersions(secretName);
            List<SecretProperties> secretVersionsList = new ArrayList<>();
            secretVersionsOutput.forEach(secretVersionsList::add);
            assertEquals(secretVersions.size(), secretVersionsList.size());

            Poller<DeletedSecret, Void> poller = client.beginDeleteSecret(secretName);
            while (!poller.isComplete()) { sleepInRecordMode(1000); }

            client.purgeDeletedSecret(secretName);
            pollOnSecretPurge(secretName);
        });

    }

    private void pollOnSecretDeletion(String secretName) {
        int pendingPollCount = 0;
        while (pendingPollCount < 30) {
            DeletedSecret deletedSecret = null;
            try {
                deletedSecret = client.getDeletedSecret(secretName);
            } catch (ResourceNotFoundException e) {
            }
            if (deletedSecret == null) {
                sleepInRecordMode(2000);
                pendingPollCount += 1;
            } else {
                return;
            }
        }
        System.err.printf("Deleted Secret %s not found \n", secretName);
    }

    private void pollOnSecretPurge(String secretName) {
        int pendingPollCount = 0;
        while (pendingPollCount < 10) {
            DeletedSecret deletedSecret = null;
            try {
                deletedSecret = client.getDeletedSecret(secretName);
            } catch (ResourceNotFoundException e) {
            }
            if (deletedSecret != null) {
                sleepInRecordMode(2000);
                pendingPollCount += 1;
            } else {
                return;
            }
        }
        System.err.printf("Deleted Secret %s was not purged \n", secretName);
    }
}
