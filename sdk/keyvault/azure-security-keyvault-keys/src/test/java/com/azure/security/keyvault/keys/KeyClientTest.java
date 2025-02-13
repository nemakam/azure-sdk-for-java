// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.security.keyvault.keys;

import com.azure.core.exception.ResourceModifiedException;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.Poller;
import com.azure.security.keyvault.keys.models.CreateKeyOptions;
import com.azure.security.keyvault.keys.models.DeletedKey;
import com.azure.security.keyvault.keys.models.KeyVaultKey;
import com.azure.security.keyvault.keys.models.KeyProperties;
import com.azure.security.keyvault.keys.models.KeyType;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class KeyClientTest extends KeyClientTestBase {

    private KeyClient client;

    @Override
    protected void beforeTest() {
        beforeTestSetup();

        if (interceptorManager.isPlaybackMode()) {
            client = clientSetup(pipeline -> new KeyClientBuilder()
                .vaultUrl(getEndpoint())
                .pipeline(pipeline)
                .buildClient());
        } else {
            client = clientSetup(pipeline -> new KeyClientBuilder()
                .vaultUrl(getEndpoint())
                .pipeline(pipeline)
                .buildClient());
        }
    }

    /**
     * Tests that a key can be created in the key vault.
     */
    public void setKey() {
        setKeyRunner((expected) -> assertKeyEquals(expected, client.createKey(expected)));
    }

    /**
     * Tests that an attempt to create a key with empty string name throws an error.
     */
    public void setKeyEmptyName() {
        assertRestException(() -> client.createKey("", KeyType.RSA), ResourceModifiedException.class, HttpURLConnection.HTTP_BAD_REQUEST);
    }

    /**
     * Tests that we cannot create keys when key type is null.
     */
    public void setKeyNullType() {
        setKeyEmptyValueRunner((key) -> {
            assertRestException(() -> client.createKey(key.getName(), key.getKeyType()), ResourceModifiedException.class, HttpURLConnection.HTTP_BAD_REQUEST);
        });
    }

    /**
     * Verifies that an exception is thrown when null key object is passed for creation.
     */
    public void setKeyNull() {
        assertRunnableThrowsException(() -> client.createKey(null), NullPointerException.class);
        assertRunnableThrowsException(() -> client.createKey(null), NullPointerException.class);
    }

    /**
     * Tests that a key is able to be updated when it exists.
     */
    public void updateKey() {
        updateKeyRunner((original, updated) -> {
            assertKeyEquals(original, client.createKey(original));
            KeyVaultKey keyToUpdate = client.getKey(original.getName());
            client.updateKeyProperties(keyToUpdate.getProperties().setExpiresOn(updated.getExpiresOn()));
            assertKeyEquals(updated, client.getKey(original.getName()));
        });
    }

    /**
     * Tests that a key is able to be updated when it is disabled.
     */
    public void updateDisabledKey() {
        updateDisabledKeyRunner((original, updated) -> {
            assertKeyEquals(original, client.createKey(original));
            KeyVaultKey keyToUpdate = client.getKey(original.getName());
            client.updateKeyProperties(keyToUpdate.getProperties().setExpiresOn(updated.getExpiresOn()));
            assertKeyEquals(updated, client.getKey(original.getName()));
        });
    }

    /**
     * Tests that an existing key can be retrieved.
     */
    public void getKey() {
        getKeyRunner((original) -> {
            client.createKey(original);
            assertKeyEquals(original, client.getKey(original.getName()));
        });
    }

    /**
     * Tests that a specific version of the key can be retrieved.
     */
    public void getKeySpecificVersion() {
        getKeySpecificVersionRunner((key, keyWithNewVal) -> {
            KeyVaultKey keyVersionOne = client.createKey(key);
            KeyVaultKey keyVersionTwo = client.createKey(keyWithNewVal);
            assertKeyEquals(key, client.getKey(keyVersionOne.getName(), keyVersionOne.getProperties().getVersion()));
            assertKeyEquals(keyWithNewVal, client.getKey(keyVersionTwo.getName(), keyVersionTwo.getProperties().getVersion()));
        });
    }

    /**
     * Tests that an attempt to get a non-existing key throws an error.
     */
    public void getKeyNotFound() {
        assertRestException(() -> client.getKey("non-existing"),  ResourceNotFoundException.class, HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * Tests that an existing key can be deleted.
     */
    public void deleteKey() {
        deleteKeyRunner((keyToDelete) -> {
            assertKeyEquals(keyToDelete,  client.createKey(keyToDelete));

            Poller<DeletedKey, Void> deletedKeyPoller = client.beginDeleteKey(keyToDelete.getName());

            while (deletedKeyPoller.getStatus() != PollResponse.OperationStatus.IN_PROGRESS
                && !deletedKeyPoller.isComplete()) {
                sleepInRecordMode(2000);
            }

            DeletedKey deletedKey = deletedKeyPoller.getLastPollResponse().getValue();

            // Key is being deleted on server.
            while (!deletedKeyPoller.isComplete()) {
                sleepInRecordMode(2000);
            }

            assertNotNull(deletedKey.getDeletedOn());
            assertNotNull(deletedKey.getRecoveryId());
            assertNotNull(deletedKey.getScheduledPurgeDate());
            assertEquals(keyToDelete.getName(), deletedKey.getName());
            client.purgeDeletedKey(keyToDelete.getName());
            pollOnKeyPurge(keyToDelete.getName());
        });
    }

    public void deleteKeyNotFound() {
        Poller<DeletedKey, Void> deletedKeyPoller = client.beginDeleteKey("non-existing");
        while (!deletedKeyPoller.isComplete()) { sleepInRecordMode(1000); }
        assertEquals(deletedKeyPoller.getLastPollResponse().getStatus(), PollResponse.OperationStatus.FAILED);
    }

    /**
     * Tests that an attempt to retrieve a non existing deleted key throws an error on a soft-delete enabled vault.
     */
    public void getDeletedKeyNotFound() {
        assertRestException(() -> client.getDeletedKey("non-existing"),  ResourceNotFoundException.class, HttpURLConnection.HTTP_NOT_FOUND);
    }


    /**
     * Tests that a deleted key can be recovered on a soft-delete enabled vault.
     */
    public void recoverDeletedKey() {
        recoverDeletedKeyRunner((keyToDeleteAndRecover) -> {
            assertKeyEquals(keyToDeleteAndRecover, client.createKey(keyToDeleteAndRecover));
            Poller<DeletedKey, Void> poller = client.beginDeleteKey(keyToDeleteAndRecover.getName());
            while (!poller.isComplete()) { sleepInRecordMode(1000); }
            assertNotNull(poller.getLastPollResponse().getValue());
            Poller<KeyVaultKey, Void> recoverPoller = client.beginRecoverDeletedKey(keyToDeleteAndRecover.getName());
            while (recoverPoller.getStatus() != PollResponse.OperationStatus.IN_PROGRESS
                && !recoverPoller.isComplete()) { sleepInRecordMode(1000); }
            KeyVaultKey recoveredKey = recoverPoller.getLastPollResponse().getValue();
            while (!recoverPoller.isComplete()) { sleepInRecordMode(1000); }
            assertEquals(keyToDeleteAndRecover.getName(), recoveredKey.getName());
            assertEquals(keyToDeleteAndRecover.getNotBefore(), recoveredKey.getProperties().getNotBefore());
            assertEquals(keyToDeleteAndRecover.getExpiresOn(), recoveredKey.getProperties().getExpiresOn());
        });
    }

    /**
     * Tests that an attempt to recover a non existing deleted key throws an error on a soft-delete enabled vault.
     */
    public void recoverDeletedKeyNotFound() {
        Poller<KeyVaultKey, Void> poller = client.beginRecoverDeletedKey("non-existing");
        while (!poller.isComplete()) { sleepInRecordMode(1000); }
        assertEquals(poller.getStatus(), PollResponse.OperationStatus.FAILED);
    }

    /**
     * Tests that a key can be backed up in the key vault.
     */
    public void backupKey() {
        backupKeyRunner((keyToBackup) -> {
            assertKeyEquals(keyToBackup, client.createKey(keyToBackup));
            byte[] backupBytes = (client.backupKey(keyToBackup.getName()));
            assertNotNull(backupBytes);
            assertTrue(backupBytes.length > 0);
        });
    }

    /**
     * Tests that an attempt to backup a non existing key throws an error.
     */
    public void backupKeyNotFound() {
        assertRestException(() -> client.backupKey("non-existing"),  ResourceNotFoundException.class, HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * Tests that a key can be backed up in the key vault.
     */
    public void restoreKey() {
        restoreKeyRunner((keyToBackupAndRestore) -> {
            assertKeyEquals(keyToBackupAndRestore, client.createKey(keyToBackupAndRestore));
            byte[] backupBytes = (client.backupKey(keyToBackupAndRestore.getName()));
            assertNotNull(backupBytes);
            assertTrue(backupBytes.length > 0);
            Poller<DeletedKey, Void> poller = client.beginDeleteKey(keyToBackupAndRestore.getName());
            while (!poller.isComplete()) { sleepInRecordMode(1000); }
            client.purgeDeletedKey(keyToBackupAndRestore.getName());
            pollOnKeyPurge(keyToBackupAndRestore.getName());
            sleepInRecordMode(60000);
            KeyVaultKey restoredKey = client.restoreKeyBackup(backupBytes);
            assertEquals(keyToBackupAndRestore.getName(), restoredKey.getName());
            assertEquals(keyToBackupAndRestore.getExpiresOn(), restoredKey.getProperties().getExpiresOn());
        });
    }

    /**
     * Tests that an attempt to restore a key from malformed backup bytes throws an error.
     */
    public void restoreKeyFromMalformedBackup() {
        byte[] keyBackupBytes = "non-existing".getBytes();
        assertRestException(() -> client.restoreKeyBackup(keyBackupBytes), ResourceModifiedException.class, HttpURLConnection.HTTP_BAD_REQUEST);
    }

    /**
     * Tests that keys can be listed in the key vault.
     */
    public void listKeys() {
        listKeysRunner((keys) -> {
            HashMap<String, CreateKeyOptions> keysToList = keys;
            for (CreateKeyOptions key :  keysToList.values()) {
                assertKeyEquals(key, client.createKey(key));
                sleepInRecordMode(5000);
            }

            for (KeyProperties actualKey : client.listPropertiesOfKeys()) {
                if (keys.containsKey(actualKey.getName())) {
                    CreateKeyOptions expectedKey = keys.get(actualKey.getName());
                    assertEquals(expectedKey.getExpiresOn(), actualKey.getExpiresOn());
                    assertEquals(expectedKey.getNotBefore(), actualKey.getNotBefore());
                    keys.remove(actualKey.getName());
                }
            }
            assertEquals(0, keys.size());
        });
    }

    /**
     * Tests that a deleted key can be retrieved on a soft-delete enabled vault.
     */
    public void getDeletedKey() {
        getDeletedKeyRunner((keyToDeleteAndGet) -> {
            assertKeyEquals(keyToDeleteAndGet, client.createKey(keyToDeleteAndGet));
            Poller<DeletedKey, Void> poller = client.beginDeleteKey(keyToDeleteAndGet.getName());
            while (!poller.isComplete()) { sleepInRecordMode(1000); }
            sleepInRecordMode(30000);
            DeletedKey deletedKey = client.getDeletedKey(keyToDeleteAndGet.getName());
            assertNotNull(deletedKey.getDeletedOn());
            assertNotNull(deletedKey.getRecoveryId());
            assertNotNull(deletedKey.getScheduledPurgeDate());
            assertEquals(keyToDeleteAndGet.getName(), deletedKey.getName());
            client.purgeDeletedKey(keyToDeleteAndGet.getName());
            pollOnKeyPurge(keyToDeleteAndGet.getName());
            sleepInRecordMode(10000);
        });
    }


    /**
     * Tests that deleted keys can be listed in the key vault.
     */
    @Override
    public void listDeletedKeys() {
        listDeletedKeysRunner((keys) -> {
            HashMap<String, CreateKeyOptions> keysToDelete = keys;
            for (CreateKeyOptions key : keysToDelete.values()) {
                assertKeyEquals(key, client.createKey(key));
            }

            for (CreateKeyOptions key : keysToDelete.values()) {
                Poller<DeletedKey, Void> poller = client.beginDeleteKey(key.getName());
                while (!poller.isComplete()) { sleepInRecordMode(1000); }
            }
            sleepInRecordMode(60000);
            Iterable<DeletedKey> deletedKeys =  client.listDeletedKeys();
            for (DeletedKey actualKey : deletedKeys) {
                if (keysToDelete.containsKey(actualKey.getName())) {
                    assertNotNull(actualKey.getDeletedOn());
                    assertNotNull(actualKey.getRecoveryId());
                    keysToDelete.remove(actualKey.getName());
                }
            }

            assertEquals(0, keysToDelete.size());

            for (DeletedKey deletedKey : deletedKeys) {
                client.purgeDeletedKey(deletedKey.getName());
                pollOnKeyPurge(deletedKey.getName());
            }
            sleepInRecordMode(10000);
        });
    }

    /**
     * Tests that key versions can be listed in the key vault.
     */
    @Override
    public void listKeyVersions() {
        listKeyVersionsRunner((keys) -> {
            List<CreateKeyOptions> keyVersions = keys;
            String keyName = null;
            for (CreateKeyOptions key : keyVersions) {
                keyName = key.getName();
                assertKeyEquals(key, client.createKey(key));
            }

            Iterable<KeyProperties> keyVersionsOutput =  client.listPropertiesOfKeyVersions(keyName);
            List<KeyProperties> keyVersionsList = new ArrayList<>();
            keyVersionsOutput.forEach(keyVersionsList::add);
            assertEquals(keyVersions.size(), keyVersionsList.size());

            Poller<DeletedKey, Void> poller = client.beginDeleteKey(keyName);
            while (!poller.isComplete()) { sleepInRecordMode(1000); }

            client.purgeDeletedKey(keyName);
            pollOnKeyPurge(keyName);
        });
    }

    private DeletedKey pollOnKeyDeletion(String keyName) {
        int pendingPollCount = 0;
        while (pendingPollCount < 30) {
            DeletedKey deletedKey = null;
            try {
                deletedKey = client.getDeletedKey(keyName);
            } catch (ResourceNotFoundException e) {
            }
            if (deletedKey == null) {
                sleepInRecordMode(2000);
                pendingPollCount += 1;
                continue;
            } else {
                return deletedKey;
            }
        }
        System.err.printf("Deleted Key %s not found \n", keyName);
        return null;
    }

    private DeletedKey pollOnKeyPurge(String keyName) {
        int pendingPollCount = 0;
        while (pendingPollCount < 10) {
            DeletedKey deletedKey = null;
            try {
                deletedKey = client.getDeletedKey(keyName);
            } catch (ResourceNotFoundException e) {
            }
            if (deletedKey != null) {
                sleepInRecordMode(2000);
                pendingPollCount += 1;
                continue;
            } else {
                return deletedKey;
            }
        }
        System.err.printf("Deleted Key %s was not purged \n", keyName);
        return null;
    }

}
