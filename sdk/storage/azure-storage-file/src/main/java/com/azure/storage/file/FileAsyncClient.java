// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.storage.file;

import com.azure.core.annotation.ServiceClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.PagedResponse;
import com.azure.core.http.rest.Response;
import com.azure.core.http.rest.SimpleResponse;
import com.azure.core.implementation.http.PagedResponseBase;
import com.azure.core.implementation.util.FluxUtil;
import com.azure.core.implementation.util.ImplUtils;
import com.azure.core.util.Context;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.PollResponse.OperationStatus;
import com.azure.core.util.polling.Poller;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.implementation.Constants;
import com.azure.storage.common.implementation.StorageImplUtils;
import com.azure.storage.file.implementation.AzureFileStorageImpl;
import com.azure.storage.file.implementation.models.FileGetPropertiesHeaders;
import com.azure.storage.file.implementation.models.FileRangeWriteType;
import com.azure.storage.file.implementation.models.FileStartCopyHeaders;
import com.azure.storage.file.implementation.models.FileUploadRangeFromURLHeaders;
import com.azure.storage.file.implementation.models.FileUploadRangeHeaders;
import com.azure.storage.file.implementation.models.FilesCreateResponse;
import com.azure.storage.file.implementation.models.FilesGetPropertiesResponse;
import com.azure.storage.file.implementation.models.FilesSetHTTPHeadersResponse;
import com.azure.storage.file.implementation.models.FilesSetMetadataResponse;
import com.azure.storage.file.implementation.models.FilesUploadRangeFromURLResponse;
import com.azure.storage.file.implementation.models.FilesUploadRangeResponse;
import com.azure.storage.file.models.CopyStatusType;
import com.azure.storage.file.models.FileCopyInfo;
import com.azure.storage.file.models.FileDownloadAsyncResponse;
import com.azure.storage.file.models.FileHttpHeaders;
import com.azure.storage.file.models.FileInfo;
import com.azure.storage.file.models.FileMetadataInfo;
import com.azure.storage.file.models.FileProperties;
import com.azure.storage.file.models.FileRange;
import com.azure.storage.file.models.FileStorageException;
import com.azure.storage.file.models.FileUploadInfo;
import com.azure.storage.file.models.FileUploadRangeFromUrlInfo;
import com.azure.storage.file.models.HandleItem;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.azure.core.implementation.util.FluxUtil.fluxError;
import static com.azure.core.implementation.util.FluxUtil.monoError;
import static com.azure.core.implementation.util.FluxUtil.pagedFluxError;
import static com.azure.core.implementation.util.FluxUtil.withContext;

/**
 * This class provides a client that contains all the operations for interacting with file in Azure Storage File
 * Service. Operations allowed by the client are creating, copying, uploading, downloading, deleting and listing on a
 * file, retrieving properties, setting metadata and list or force close handles of the file.
 *
 * <p><strong>Instantiating an Asynchronous File Client</strong></p>
 *
 * {@codesnippet com.azure.storage.file.fileAsyncClient.instantiation}
 *
 * <p>View {@link FileClientBuilder this} for additional ways to construct the client.</p>
 *
 * @see FileClientBuilder
 * @see FileClient
 * @see StorageSharedKeyCredential
 */
@ServiceClient(builder = FileClientBuilder.class, isAsync = true)
public class FileAsyncClient {
    private final ClientLogger logger = new ClientLogger(FileAsyncClient.class);
    static final long FILE_DEFAULT_BLOCK_SIZE = 4 * 1024 * 1024L;
    private static final long DOWNLOAD_UPLOAD_CHUNK_TIMEOUT = 300;

    private final AzureFileStorageImpl azureFileStorageClient;
    private final String shareName;
    private final String filePath;
    private final String snapshot;
    private final String accountName;
    private final FileServiceVersion serviceVersion;

    /**
     * Creates a FileAsyncClient that sends requests to the storage file at {@link AzureFileStorageImpl#getUrl()
     * endpoint}. Each service call goes through the {@link HttpPipeline pipeline} in the {@code client}.
     *
     * @param azureFileStorageClient Client that interacts with the service interfaces
     * @param shareName Name of the share
     * @param filePath Path to the file
     * @param snapshot The snapshot of the share
     */
    FileAsyncClient(AzureFileStorageImpl azureFileStorageClient, String shareName, String filePath,
                    String snapshot, String accountName, FileServiceVersion serviceVersion) {
        Objects.requireNonNull(shareName, "'shareName' cannot be null.");
        Objects.requireNonNull(filePath, "'filePath' cannot be null.");
        this.shareName = shareName;
        this.filePath = filePath;
        this.snapshot = snapshot;
        this.azureFileStorageClient = azureFileStorageClient;
        this.accountName = accountName;
        this.serviceVersion = serviceVersion;
    }

    /**
     * Get the url of the storage file client.
     *
     * @return the URL of the storage file client
     */
    public String getFileUrl() {
        StringBuilder fileUrlstring = new StringBuilder(azureFileStorageClient.getUrl()).append("/")
            .append(shareName).append("/").append(filePath);
        if (snapshot != null) {
            fileUrlstring.append("?snapshot=").append(snapshot);
        }
        return fileUrlstring.toString();
    }

    /**
     * Gets the service version the client is using.
     *
     * @return the service version the client is using.
     */
    public FileServiceVersion getServiceVersion() {
        return serviceVersion;
    }

    /**
     * Creates a file in the storage account and returns a response of {@link FileInfo} to interact with it.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Create the file with size 1KB.</p>
     *
     * {@codesnippet com.azure.storage.file.fileClient.create}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/create-file">Azure Docs</a>.</p>
     *
     * @param maxSize The maximum size in bytes for the file, up to 1 TiB.
     * @return A response containing the file info and the status of creating the file.
     * @throws FileStorageException If the file has already existed, the parent directory does not exist or fileName
     * is an invalid resource name.
     */
    public Mono<FileInfo> create(long maxSize) {
        try {
            return createWithResponse(maxSize, null, null, null, null).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Creates a file in the storage account and returns a response of FileInfo to interact with it.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Create the file with length of 1024 bytes, some headers, file smb properties and metadata.</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.createWithResponse#long-filehttpheaders-filesmbproperties-string-map}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/create-file">Azure Docs</a>.</p>
     *
     * @param maxSize The maximum size in bytes for the file, up to 1 TiB.
     * @param httpHeaders The user settable file http headers.
     * @param smbProperties The user settable file smb properties.
     * @param filePermission The file permission of the file.
     * @param metadata Optional name-value pairs associated with the file as metadata.
     * @return A response containing the {@link FileInfo file info} and the status of creating the file.
     * @throws FileStorageException If the directory has already existed, the parent directory does not exist or
     * directory is an invalid resource name.
     */
    public Mono<Response<FileInfo>> createWithResponse(long maxSize, FileHttpHeaders httpHeaders,
        FileSmbProperties smbProperties, String filePermission, Map<String, String> metadata) {
        try {
            return withContext(context ->
                createWithResponse(maxSize, httpHeaders, smbProperties, filePermission, metadata, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<FileInfo>> createWithResponse(long maxSize, FileHttpHeaders httpHeaders,
        FileSmbProperties smbProperties, String filePermission, Map<String, String> metadata, Context context) {
        smbProperties = smbProperties == null ? new FileSmbProperties() : smbProperties;

        // Checks that file permission and file permission key are valid
        validateFilePermissionAndKey(filePermission, smbProperties.getFilePermissionKey());

        // If file permission and file permission key are both not set then set default value
        filePermission = smbProperties.setFilePermission(filePermission, FileConstants.FILE_PERMISSION_INHERIT);
        String filePermissionKey = smbProperties.getFilePermissionKey();

        String fileAttributes = smbProperties.setNtfsFileAttributes(FileConstants.FILE_ATTRIBUTES_NONE);
        String fileCreationTime = smbProperties.setFileCreationTime(FileConstants.FILE_TIME_NOW);
        String fileLastWriteTime = smbProperties.setFileLastWriteTime(FileConstants.FILE_TIME_NOW);

        return azureFileStorageClient.files()
            .createWithRestResponseAsync(shareName, filePath, maxSize, fileAttributes, fileCreationTime,
                fileLastWriteTime, null, metadata, filePermission, filePermissionKey, httpHeaders, context)
            .map(this::createFileInfoResponse);
    }

    /**
     * Copies a blob or file to a destination file within the storage account.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Copy file from source url to the {@code resourcePath} </p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.beginCopy#string-map-duration}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/copy-file">Azure Docs</a>.</p>
     *
     * @param sourceUrl Specifies the URL of the source file or blob, up to 2 KB in length.
     * @param pollInterval Duration between each poll for the copy status. If none is specified, a default of one second
     * is used.
     * @param metadata Optional name-value pairs associated with the file as metadata. Metadata names must adhere to the
     * naming rules.
     * @return A {@link Poller} that polls the file copy operation until it has completed or has been cancelled.
     * @see <a href="https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/">C# identifiers</a>
     */
    public Poller<FileCopyInfo, Void> beginCopy(String sourceUrl, Map<String, String> metadata, Duration pollInterval) {
        final AtomicReference<String> copyId = new AtomicReference<>();
        final Duration interval = pollInterval != null ? pollInterval : Duration.ofSeconds(1);

        return new Poller<>(interval,
            response -> {
                try {
                    return onPoll(response);
                } catch (RuntimeException ex) {
                    return monoError(logger, ex);
                }
            },
            Mono::empty,
            () -> {
                try {
                    return withContext(context -> azureFileStorageClient.files()
                    .startCopyWithRestResponseAsync(shareName, filePath, sourceUrl, null, metadata, context))
                    .map(response -> {
                        final FileStartCopyHeaders headers = response.getDeserializedHeaders();
                        copyId.set(headers.getCopyId());

                        return new FileCopyInfo(sourceUrl, headers.getCopyId(), headers.getCopyStatus(),
                            headers.getETag(), headers.getLastModified(), headers.getErrorCode());
                    });
                } catch (RuntimeException ex) {
                    return monoError(logger, ex);
                }
            },
            poller -> {
                final PollResponse<FileCopyInfo> response = poller.getLastPollResponse();

                if (response == null || response.getValue() == null) {
                    return Mono.error(logger.logExceptionAsError(
                        new IllegalArgumentException("Cannot cancel a poll response that never started.")));
                }

                final String copyIdentifier = response.getValue().getCopyId();

                if (!ImplUtils.isNullOrEmpty(copyIdentifier)) {
                    logger.info("Cancelling copy operation for copy id: {}", copyIdentifier);

                    return abortCopy(copyIdentifier).thenReturn(response.getValue());
                }

                return Mono.empty();
            });
    }

    private Mono<PollResponse<FileCopyInfo>> onPoll(PollResponse<FileCopyInfo> pollResponse) {
        if (pollResponse.getStatus() == OperationStatus.SUCCESSFULLY_COMPLETED
            || pollResponse.getStatus() == OperationStatus.FAILED) {
            return Mono.just(pollResponse);
        }

        final FileCopyInfo lastInfo = pollResponse.getValue();
        if (lastInfo == null) {
            logger.warning("FileCopyInfo does not exist. Activation operation failed.");
            return Mono.just(new PollResponse<>(OperationStatus.fromString("COPY_START_FAILED", true), null));
        }

        return getProperties()
            .map(response -> {
                final CopyStatusType status = response.getCopyStatus();
                final FileCopyInfo result = new FileCopyInfo(response.getCopySource(), response.getCopyId(), status,
                    response.getETag(), response.getCopyCompletionTime(), response.getCopyStatusDescription());

                OperationStatus operationStatus;
                switch (status) {
                    case SUCCESS:
                        operationStatus = OperationStatus.SUCCESSFULLY_COMPLETED;
                        break;
                    case FAILED:
                        operationStatus = OperationStatus.FAILED;
                        break;
                    case ABORTED:
                        operationStatus = OperationStatus.USER_CANCELLED;
                        break;
                    case PENDING:
                        operationStatus = OperationStatus.IN_PROGRESS;
                        break;
                    default:
                        throw logger.logExceptionAsError(new IllegalArgumentException(
                            "CopyStatusType is not supported. Status: " + status));
                }

                return new PollResponse<>(operationStatus, result);
            }).onErrorReturn(new PollResponse<>(OperationStatus.fromString("POLLING_FAILED", true), lastInfo));
    }

    /**
     * Aborts a pending Copy File operation, and leaves a destination file with zero length and full metadata.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Abort copy file from copy id("someCopyId") </p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.abortCopy#string}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/abort-copy-file">Azure Docs</a>.</p>
     *
     * @param copyId Specifies the copy id which has copying pending status associate with it.
     * @return An empty response.
     */
    public Mono<Void> abortCopy(String copyId) {
        try {
            return abortCopyWithResponse(copyId).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Aborts a pending Copy File operation, and leaves a destination file with zero length and full metadata.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Abort copy file from copy id("someCopyId") </p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.abortCopyWithResponse#string}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/abort-copy-file">Azure Docs</a>.</p>
     *
     * @param copyId Specifies the copy id which has copying pending status associate with it.
     * @return A response containing the status of aborting copy the file.
     */
    public Mono<Response<Void>> abortCopyWithResponse(String copyId) {
        try {
            return withContext(context -> abortCopyWithResponse(copyId, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<Void>> abortCopyWithResponse(String copyId, Context context) {
        return azureFileStorageClient.files().abortCopyWithRestResponseAsync(shareName, filePath, copyId, context)
            .map(response -> new SimpleResponse<>(response, null));
    }

    /**
     * Downloads a file from the system, including its metadata and properties into a file specified by the path.
     *
     * <p>The file will be created and must not exist, if the file already exists a {@link FileAlreadyExistsException}
     * will be thrown.</p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Download the file to current folder. </p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.downloadToFile#string}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-file">Azure Docs</a>.</p>
     *
     * @param downloadFilePath The path where store the downloaded file
     * @return An empty response.
     */
    public Mono<FileProperties> downloadToFile(String downloadFilePath) {
        try {
            return downloadToFileWithResponse(downloadFilePath, null).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Downloads a file from the system, including its metadata and properties into a file specified by the path.
     *
     * <p>The file will be created and must not exist, if the file already exists a {@link FileAlreadyExistsException}
     * will be thrown.</p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Download the file from 1024 to 2048 bytes to current folder. </p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.downloadToFileWithResponse#string-filerange}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-file">Azure Docs</a>.</p>
     *
     * @param downloadFilePath The path where store the downloaded file
     * @param range Optional byte range which returns file data only from the specified range.
     * @return An empty response.
     */
    public Mono<Response<FileProperties>> downloadToFileWithResponse(String downloadFilePath, FileRange range) {
        try {
            return withContext(context -> downloadToFileWithResponse(downloadFilePath, range, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<FileProperties>> downloadToFileWithResponse(String downloadFilePath, FileRange range,
                                                                Context context) {
        return Mono.using(() -> channelSetup(downloadFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW),
            channel -> getPropertiesWithResponse(context).flatMap(response ->
                downloadResponseInChunk(response, channel, range, context)), this::channelCleanUp);
    }

    private Mono<Response<FileProperties>> downloadResponseInChunk(Response<FileProperties> response,
                                                                   AsynchronousFileChannel channel,
                                                                   FileRange range, Context context) {
        return Mono.justOrEmpty(range).switchIfEmpty(Mono.just(new FileRange(0, response.getValue()
            .getContentLength())))
            .map(currentRange -> {
                List<FileRange> chunks = new ArrayList<>();
                for (long pos = currentRange.getStart(); pos < currentRange.getEnd(); pos += FILE_DEFAULT_BLOCK_SIZE) {
                    long count = FILE_DEFAULT_BLOCK_SIZE;
                    if (pos + count > currentRange.getEnd()) {
                        count = currentRange.getEnd() - pos;
                    }
                    chunks.add(new FileRange(pos, pos + count - 1));
                }
                return chunks;
            }).flatMapMany(Flux::fromIterable).flatMap(chunk ->
                downloadWithResponse(chunk, false, context)
                .map(FileDownloadAsyncResponse::getValue)
                .subscribeOn(Schedulers.elastic())
                .flatMap(fbb -> FluxUtil
                    .writeFile(fbb, channel, chunk.getStart() - (range == null ? 0 : range.getStart()))
                    .subscribeOn(Schedulers.elastic())
                    .timeout(Duration.ofSeconds(DOWNLOAD_UPLOAD_CHUNK_TIMEOUT))
                    .retry(3, throwable -> throwable instanceof IOException
                        || throwable instanceof TimeoutException)))
            .then(Mono.just(response));
    }

    private AsynchronousFileChannel channelSetup(String filePath, OpenOption... options) {
        try {
            return AsynchronousFileChannel.open(Paths.get(filePath), options);
        } catch (IOException e) {
            throw logger.logExceptionAsError(new UncheckedIOException(e));
        }
    }

    private void channelCleanUp(AsynchronousFileChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            throw logger.logExceptionAsError(Exceptions.propagate(new UncheckedIOException(e)));
        }
    }

    /**
     * Downloads a file from the system, including its metadata and properties
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Download the file with its metadata and properties. </p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.download}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-file">Azure Docs</a>.</p>
     *
     * @return A reactive response containing the file data.
     */
    public Flux<ByteBuffer> download() {
        try {
            return downloadWithResponse(null, null).flatMapMany(FileDownloadAsyncResponse::getValue);
        } catch (RuntimeException ex) {
            return fluxError(logger, ex);
        }
    }

    /**
     * Downloads a file from the system, including its metadata and properties
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Download the file from 1024 to 2048 bytes with its metadata and properties and without the contentMD5. </p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.downloadWithResponse#filerange-boolean}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-file">Azure Docs</a>.</p>
     *
     * @param range Optional byte range which returns file data only from the specified range.
     * @param rangeGetContentMD5 Optional boolean which the service returns the MD5 hash for the range when it sets to
     * true, as long as the range is less than or equal to 4 MB in size.
     * @return A reactive response containing response data and the file data.
     */
    public Mono<FileDownloadAsyncResponse> downloadWithResponse(FileRange range, Boolean rangeGetContentMD5) {
        try {
            return withContext(context -> downloadWithResponse(range, rangeGetContentMD5, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<FileDownloadAsyncResponse> downloadWithResponse(FileRange range, Boolean rangeGetContentMD5,
        Context context) {
        String rangeString = range == null ? null : range.toString();

        return azureFileStorageClient.files()
            .downloadWithRestResponseAsync(shareName, filePath, null, rangeString, rangeGetContentMD5, context)
            .map(response -> new FileDownloadAsyncResponse(response.getRequest(), response.getStatusCode(),
                response.getHeaders(), response.getValue(), response.getDeserializedHeaders()));
    }

    /**
     * Deletes the file associate with the client.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Delete the file</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.delete}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/delete-file2">Azure Docs</a>.</p>
     *
     * @return An empty response
     * @throws FileStorageException If the directory doesn't exist or the file doesn't exist.
     */
    public Mono<Void> delete() {
        try {
            return deleteWithResponse(null).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Deletes the file associate with the client.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Delete the file</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.deleteWithResponse}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/delete-file2">Azure Docs</a>.</p>
     *
     * @return A response that only contains headers and response status code
     * @throws FileStorageException If the directory doesn't exist or the file doesn't exist.
     */
    public Mono<Response<Void>> deleteWithResponse() {
        try {
            return withContext(this::deleteWithResponse);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<Void>> deleteWithResponse(Context context) {
        return azureFileStorageClient.files().deleteWithRestResponseAsync(shareName, filePath, context)
            .map(response -> new SimpleResponse<>(response, null));
    }

    /**
     * Retrieves the properties of the storage account's file. The properties includes file metadata, last modified
     * date, is server encrypted, and eTag.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Retrieve file properties</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.getProperties}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-file-properties">Azure Docs</a>.</p>
     *
     * @return {@link FileProperties Storage file properties}
     */
    public Mono<FileProperties> getProperties() {
        try {
            return getPropertiesWithResponse().flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Retrieves the properties of the storage account's file. The properties includes file metadata, last modified
     * date, is server encrypted, and eTag.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Retrieve file properties</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.getPropertiesWithResponse}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-file-properties">Azure Docs</a>.</p>
     *
     * @return A response containing the {@link FileProperties storage file properties} and response status code
     */
    public Mono<Response<FileProperties>> getPropertiesWithResponse() {
        try {
            return withContext(this::getPropertiesWithResponse);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<FileProperties>> getPropertiesWithResponse(Context context) {
        return azureFileStorageClient.files()
            .getPropertiesWithRestResponseAsync(shareName, filePath, snapshot, null, context)
            .map(this::getPropertiesResponse);
    }

    /**
     * Sets the user-defined file properties to associate to the file.
     *
     * <p>If {@code null} is passed for the fileProperties.httpHeaders it will clear the httpHeaders associated to the
     * file.
     * If {@code null} is passed for the fileProperties.filesmbproperties it will preserve the filesmb properties
     * associated with the file.</p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Set the httpHeaders of contentType of "text/plain"</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.setProperties#long-filehttpheaders-filesmbproperties-string}
     *
     * <p>Clear the metadata of the file and preserve the SMB properties</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.setProperties#long-filehttpheaders-filesmbproperties-string.clearHttpHeaderspreserveSMBProperties}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-file-properties">Azure Docs</a>.</p>
     *
     * @param newFileSize New file size of the file
     * @param httpHeaders The user settable file http headers.
     * @param smbProperties The user settable file smb properties.
     * @param filePermission The file permission of the file
     * @return The {@link FileInfo file info}
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     */
    public Mono<FileInfo> setProperties(long newFileSize, FileHttpHeaders httpHeaders, FileSmbProperties smbProperties,
        String filePermission) {
        try {
            return setPropertiesWithResponse(newFileSize, httpHeaders, smbProperties, filePermission)
                .flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Sets the user-defined file properties to associate to the file.
     *
     * <p>If {@code null} is passed for the httpHeaders it will clear the httpHeaders associated to the file.
     * If {@code null} is passed for the filesmbproperties it will preserve the filesmbproperties associated with the
     * file.</p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Set the httpHeaders of contentType of "text/plain"</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.setPropertiesWithResponse#long-filehttpheaders-filesmbproperties-string}
     *
     * <p>Clear the metadata of the file and preserve the SMB properties</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.setPropertiesWithResponse#long-filehttpheaders-filesmbproperties-string.clearHttpHeaderspreserveSMBProperties}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-file-properties">Azure Docs</a>.</p>
     *
     * @param newFileSize New file size of the file.
     * @param httpHeaders The user settable file http headers.
     * @param smbProperties The user settable file smb properties.
     * @param filePermission The file permission of the file.
     * @return Response containing the {@link FileInfo file info} and response status code.
     * @throws IllegalArgumentException thrown if parameters fail the validation.
     */
    public Mono<Response<FileInfo>> setPropertiesWithResponse(long newFileSize, FileHttpHeaders httpHeaders,
        FileSmbProperties smbProperties, String filePermission) {
        try {
            return withContext(context ->
                setPropertiesWithResponse(newFileSize, httpHeaders, smbProperties, filePermission, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<FileInfo>> setPropertiesWithResponse(long newFileSize, FileHttpHeaders httpHeaders,
        FileSmbProperties smbProperties, String filePermission, Context context) {
        smbProperties = smbProperties == null ? new FileSmbProperties() : smbProperties;

        // Checks that file permission and file permission key are valid
        validateFilePermissionAndKey(filePermission, smbProperties.getFilePermissionKey());

        // If file permission and file permission key are both not set then set default value
        filePermission = smbProperties.setFilePermission(filePermission, FileConstants.PRESERVE);
        String filePermissionKey = smbProperties.getFilePermissionKey();

        String fileAttributes = smbProperties.setNtfsFileAttributes(FileConstants.PRESERVE);
        String fileCreationTime = smbProperties.setFileCreationTime(FileConstants.PRESERVE);
        String fileLastWriteTime = smbProperties.setFileLastWriteTime(FileConstants.PRESERVE);

        return azureFileStorageClient.files()
            .setHTTPHeadersWithRestResponseAsync(shareName, filePath, fileAttributes, fileCreationTime,
                fileLastWriteTime, null, newFileSize, filePermission, filePermissionKey, httpHeaders, context)
            .map(this::setPropertiesResponse);
    }

    /**
     * Sets the user-defined metadata to associate to the file.
     *
     * <p>If {@code null} is passed for the metadata it will clear the metadata associated to the file.</p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Set the metadata to "file:updatedMetadata"</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.setMetadata#map}
     *
     * <p>Clear the metadata of the file</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.setMetadataWithResponse#map.clearMetadata}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-file-metadata">Azure Docs</a>.</p>
     *
     * @param metadata Options.Metadata to set on the file, if null is passed the metadata for the file is cleared
     * @return {@link FileMetadataInfo file meta info}
     * @throws FileStorageException If the file doesn't exist or the metadata contains invalid keys
     */
    public Mono<FileMetadataInfo> setMetadata(Map<String, String> metadata) {
        try {
            return setMetadataWithResponse(metadata).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Sets the user-defined metadata to associate to the file.
     *
     * <p>If {@code null} is passed for the metadata it will clear the metadata associated to the file.</p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Set the metadata to "file:updatedMetadata"</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.setMetadataWithResponse#map}
     *
     * <p>Clear the metadata of the file</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.setMetadataWithResponse#map.clearMetadata}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-file-metadata">Azure Docs</a>.</p>
     *
     * @param metadata Options.Metadata to set on the file, if null is passed the metadata for the file is cleared
     * @return A response containing the {@link FileMetadataInfo file meta info} and status code
     * @throws FileStorageException If the file doesn't exist or the metadata contains invalid keys
     */
    public Mono<Response<FileMetadataInfo>> setMetadataWithResponse(Map<String, String> metadata) {
        try {
            return withContext(context -> setMetadataWithResponse(metadata, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<FileMetadataInfo>> setMetadataWithResponse(Map<String, String> metadata, Context context) {
        try {
            return azureFileStorageClient.files()
                .setMetadataWithRestResponseAsync(shareName, filePath, null, metadata, context)
                .map(this::setMetadataResponse);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Uploads a range of bytes to the beginning of a file in storage file service. Upload operations performs an
     * in-place write on the specified file.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Upload data "default" to the file in Storage File Service. </p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.upload#flux-long}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/put-range">Azure Docs</a>.</p>
     *
     * @param data The data which will upload to the storage file.
     * @param length Specifies the number of bytes being transmitted in the request body.
     * @return A response that only contains headers and response status code
     */
    public Mono<FileUploadInfo> upload(Flux<ByteBuffer> data, long length) {
        try {
            return uploadWithResponse(data, length, 0L).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Uploads a range of bytes to specific of a file in storage file service. Upload operations performs an in-place
     * write on the specified file.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Upload the file from 1024 to 2048 bytes with its metadata and properties and without the contentMD5. </p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.uploadWithResponse#flux-long-long}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/put-range">Azure Docs</a>.</p>
     *
     * @param data The data which will upload to the storage file.
     * @param length Specifies the number of bytes being transmitted in the request body. When the FileRangeWriteType is
     * set to clear, the value of this header must be set to zero.
     * @param offset Optional starting point of the upload range. It will start from the beginning if it is
     * {@code null}.
     * @return A response containing the {@link FileUploadInfo file upload info} with headers and response status code
     * @throws FileStorageException If you attempt to upload a range that is larger than 4 MB, the service returns
     * status code 413 (Request Entity Too Large)
     */
    public Mono<Response<FileUploadInfo>> uploadWithResponse(Flux<ByteBuffer> data, long length, Long offset) {
        try {
            return withContext(context -> uploadWithResponse(data, length, offset, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<FileUploadInfo>> uploadWithResponse(Flux<ByteBuffer> data, long length, Long offset,
        Context context) {
        long rangeOffset = (offset == null) ? 0L : offset;
        FileRange range = new FileRange(rangeOffset, rangeOffset + length - 1);
        return azureFileStorageClient.files()
            .uploadRangeWithRestResponseAsync(shareName, filePath, range.toString(), FileRangeWriteType.UPDATE,
                length, data, null, null, context)
            .map(this::uploadResponse);
    }

    /**
     * Uploads a range of bytes from one file to another file.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Upload a number of bytes from a file at defined source and destination offsets </p>
     *
     * {@codesnippet com.azure.storage.file.FileAsyncClient.uploadRangeFromUrl#long-long-long-String}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/put-range">Azure Docs</a>.</p>
     *
     * @param length Specifies the number of bytes being transmitted in the request body.
     * @param destinationOffset Starting point of the upload range on the destination.
     * @param sourceOffset Starting point of the upload range on the source.
     * @param sourceUrl Specifies the URL of the source file.
     * @return The {@link FileUploadRangeFromUrlInfo file upload range from url info}
     */
    // TODO: (gapra) Fix put range from URL link. Service docs have not been updated to show this API
    public Mono<FileUploadRangeFromUrlInfo> uploadRangeFromUrl(long length, long destinationOffset, long sourceOffset,
        String sourceUrl) {
        try {
            return uploadRangeFromUrlWithResponse(length, destinationOffset, sourceOffset, sourceUrl)
                .flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Uploads a range of bytes from one file to another file.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Upload a number of bytes from a file at defined source and destination offsets </p>
     *
     * {@codesnippet com.azure.storage.file.FileAsyncClient.uploadRangeFromUrlWithResponse#long-long-long-String}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/put-range">Azure Docs</a>.</p>
     *
     * @param length Specifies the number of bytes being transmitted in the request body.
     * @param destinationOffset Starting point of the upload range on the destination.
     * @param sourceOffset Starting point of the upload range on the source.
     * @param sourceUrl Specifies the URL of the source file.
     * @return A response containing the {@link FileUploadRangeFromUrlInfo file upload range from url info} with headers
     * and response status code.
     */
    // TODO: (gapra) Fix put range from URL link. Service docs have not been updated to show this API
    public Mono<Response<FileUploadRangeFromUrlInfo>> uploadRangeFromUrlWithResponse(long length,
        long destinationOffset, long sourceOffset, String sourceUrl) {
        try {
            return withContext(context ->
                uploadRangeFromUrlWithResponse(length, destinationOffset, sourceOffset, sourceUrl, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<FileUploadRangeFromUrlInfo>> uploadRangeFromUrlWithResponse(long length, long destinationOffset,
        long sourceOffset, String sourceUrl, Context context) {
        FileRange destinationRange = new FileRange(destinationOffset, destinationOffset + length - 1);
        FileRange sourceRange = new FileRange(sourceOffset, sourceOffset + length - 1);

        return azureFileStorageClient.files()
            .uploadRangeFromURLWithRestResponseAsync(shareName, filePath, destinationRange.toString(), sourceUrl, 0,
                null, sourceRange.toString(), null, null, context)
            .map(this::uploadRangeFromUrlResponse);
    }

    /**
     * Clear a range of bytes to specific of a file in storage file service. Clear operations performs an in-place write
     * on the specified file.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Clears the first 1024 bytes. </p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.clearRange#long}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/put-range">Azure Docs</a>.</p>
     *
     * @param length Specifies the number of bytes being cleared.
     * @return The {@link FileUploadInfo file upload info}
     */
    public Mono<FileUploadInfo> clearRange(long length) {
        try {
            return clearRangeWithResponse(length, 0).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Clear a range of bytes to specific of a file in storage file service. Clear operations performs an in-place write
     * on the specified file.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Clear the range starting from 1024 with length of 1024. </p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.clearRange#long-long}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/put-range">Azure Docs</a>.</p>
     *
     * @param length Specifies the number of bytes being cleared in the request body.
     * @param offset Optional starting point of the upload range. It will start from the beginning if it is
     * {@code null}
     * @return A response of {@link FileUploadInfo file upload info} that only contains headers and response status code
     */
    public Mono<Response<FileUploadInfo>> clearRangeWithResponse(long length, long offset) {
        try {
            return withContext(context -> clearRangeWithResponse(length, offset, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<FileUploadInfo>> clearRangeWithResponse(long length, long offset, Context context) {
        FileRange range = new FileRange(offset, offset + length - 1);
        return azureFileStorageClient.files()
            .uploadRangeWithRestResponseAsync(shareName, filePath, range.toString(), FileRangeWriteType.CLEAR, 0L,
                null, null, null, context)
            .map(this::uploadResponse);
    }

    /**
     * Uploads file to storage file service.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p> Upload the file from the source file path. </p>
     *
     * (@codesnippet com.azure.storage.file.fileAsyncClient.uploadFromFile#string}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/create-file">Azure Docs Create File</a>
     * and
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/put-range">Azure Docs Upload</a>.</p>
     *
     * @param uploadFilePath The path where store the source file to upload
     * @return An empty response.
     * @throws UncheckedIOException If an I/O error occurs.
     */
    public Mono<Void> uploadFromFile(String uploadFilePath) {
        try {
            return Mono.using(() -> channelSetup(uploadFilePath, StandardOpenOption.READ),
                channel -> Flux.fromIterable(sliceFile(uploadFilePath))
                    .flatMap(chunk -> uploadWithResponse(FluxUtil.readFile(channel, chunk.getStart(),
                        chunk.getEnd() - chunk.getStart() + 1), chunk.getEnd() - chunk.getStart() + 1, chunk.getStart())
                        .timeout(Duration.ofSeconds(DOWNLOAD_UPLOAD_CHUNK_TIMEOUT))
                        .retry(3,
                            throwable -> throwable instanceof IOException || throwable instanceof TimeoutException))
                    .then(), this::channelCleanUp);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    private List<FileRange> sliceFile(String path) {
        File file = new File(path);
        assert file.exists();
        List<FileRange> ranges = new ArrayList<>();
        for (long pos = 0; pos < file.length(); pos += FILE_DEFAULT_BLOCK_SIZE) {
            long count = FILE_DEFAULT_BLOCK_SIZE;
            if (pos + count > file.length()) {
                count = file.length() - pos;
            }
            ranges.add(new FileRange(pos, pos + count - 1));
        }
        return ranges;
    }

    /**
     * List of valid ranges for a file.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>List all ranges for the file client.</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.listRanges}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/list-ranges">Azure Docs</a>.</p>
     *
     * @return {@link FileRange ranges} in the files.
     */
    public PagedFlux<FileRange> listRanges() {
        try {
            return listRanges(null);
        } catch (RuntimeException ex) {
            return pagedFluxError(logger, ex);
        }
    }

    /**
     * List of valid ranges for a file.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>List all ranges within the file range from 1KB to 2KB.</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.listRanges#filerange}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/list-ranges">Azure Docs</a>.</p>
     *
     * @param range Optional byte range which returns file data only from the specified range.
     * @return {@link FileRange ranges} in the files that satisfy the requirements
     */
    public PagedFlux<FileRange> listRanges(FileRange range) {
        try {
            return listRangesWithOptionalTimeout(range, null, Context.NONE);
        } catch (RuntimeException ex) {
            return pagedFluxError(logger, ex);
        }
    }

    PagedFlux<FileRange> listRangesWithOptionalTimeout(FileRange range, Duration timeout, Context context) {
        String rangeString = range == null ? null : range.toString();
        Function<String, Mono<PagedResponse<FileRange>>> retriever =
            marker -> StorageImplUtils.applyOptionalTimeout(this.azureFileStorageClient.files()
                .getRangeListWithRestResponseAsync(shareName, filePath, snapshot, null, rangeString, context), timeout)
                .map(response -> new PagedResponseBase<>(response.getRequest(),
                    response.getStatusCode(),
                    response.getHeaders(),
                    response.getValue().stream().map(FileRange::new).collect(Collectors.toList()),
                    null,
                    response.getDeserializedHeaders()));

        return new PagedFlux<>(() -> retriever.apply(null), retriever);
    }

    /**
     * List of open handles on a file.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>List all handles for the file client.</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.listHandles}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/list-handles">Azure Docs</a>.</p>
     *
     * @return {@link HandleItem handles} in the files that satisfy the requirements
     */
    public PagedFlux<HandleItem> listHandles() {
        try {
            return listHandles(null);
        } catch (RuntimeException ex) {
            return pagedFluxError(logger, ex);
        }
    }

    /**
     * List of open handles on a file.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>List 10 handles for the file client.</p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.listHandles#integer}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/list-handles">Azure Docs</a>.</p>
     *
     * @param maxResultsPerPage Optional maximum number of results will return per page
     * @return {@link HandleItem handles} in the file that satisfy the requirements
     */
    public PagedFlux<HandleItem> listHandles(Integer maxResultsPerPage) {
        try {
            return listHandlesWithOptionalTimeout(maxResultsPerPage, null, Context.NONE);
        } catch (RuntimeException ex) {
            return pagedFluxError(logger, ex);
        }
    }

    PagedFlux<HandleItem> listHandlesWithOptionalTimeout(Integer maxResultsPerPage, Duration timeout, Context context) {
        Function<String, Mono<PagedResponse<HandleItem>>> retriever =
            marker -> StorageImplUtils.applyOptionalTimeout(this.azureFileStorageClient.files()
                .listHandlesWithRestResponseAsync(shareName, filePath, marker, maxResultsPerPage, null, snapshot,
                    context), timeout)
                .map(response -> new PagedResponseBase<>(response.getRequest(),
                    response.getStatusCode(),
                    response.getHeaders(),
                    response.getValue().getHandleList(),
                    response.getValue().getNextMarker(),
                    response.getDeserializedHeaders()));

        return new PagedFlux<>(() -> retriever.apply(null), retriever);
    }

    /**
     * Closes a handle on the file. This is intended to be used alongside {@link #listHandles()}.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Force close handles returned by list handles.</p>
     *
     * {@codesnippet com.azure.storage.file.FileAsyncClient.forceCloseHandle#String}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/force-close-handles">Azure Docs</a>.</p>
     *
     * @param handleId Handle ID to be closed.
     * @return An empty response.
     */
    public Mono<Void> forceCloseHandle(String handleId) {
        try {
            return withContext(context -> forceCloseHandleWithResponse(handleId, context)).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Closes a handle on the file. This is intended to be used alongside {@link #listHandles()}.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Force close handles returned by list handles.</p>
     *
     * {@codesnippet com.azure.storage.file.FileAsyncClient.forceCloseHandleWithResponse#String}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/force-close-handles">Azure Docs</a>.</p>
     *
     * @param handleId Handle ID to be closed.
     * @return A response that only contains headers and response status code.
     */
    public Mono<Response<Void>> forceCloseHandleWithResponse(String handleId) {
        try {
            return withContext(context -> forceCloseHandleWithResponse(handleId, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<Void>> forceCloseHandleWithResponse(String handleId, Context context) {
        return azureFileStorageClient.files()
            .forceCloseHandlesWithRestResponseAsync(shareName, filePath, handleId, null, null, snapshot, context)
            .map(response -> new SimpleResponse<>(response, null));
    }

    /**
     * Closes all handles opened on the file at the service.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Force close all handles.</p>
     *
     * {@codesnippet com.azure.storage.file.FileAsyncClient.forceCloseAllHandles}
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/force-close-handles">Azure Docs</a>.</p>
     *
     * @return The number of handles closed.
     */
    public Mono<Integer> forceCloseAllHandles() {
        try {
            return withContext(context -> forceCloseAllHandlesWithOptionalTimeout(null, context)
                .reduce(0, Integer::sum));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    PagedFlux<Integer> forceCloseAllHandlesWithOptionalTimeout(Duration timeout, Context context) {
        Function<String, Mono<PagedResponse<Integer>>> retriever =
            marker -> StorageImplUtils.applyOptionalTimeout(this.azureFileStorageClient.files()
                .forceCloseHandlesWithRestResponseAsync(shareName, filePath, "*", null, marker,
                    snapshot, context), timeout)
                .map(response -> new PagedResponseBase<>(response.getRequest(),
                    response.getStatusCode(),
                    response.getHeaders(),
                    Collections.singletonList(response.getDeserializedHeaders().getNumberOfHandlesClosed()),
                    response.getDeserializedHeaders().getMarker(),
                    response.getDeserializedHeaders()));

        return new PagedFlux<>(() -> retriever.apply(null), retriever);
    }

    /**
     * Get snapshot id which attached to {@link FileAsyncClient}. Return {@code null} if no snapshot id attached.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Get the share snapshot id. </p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.getShareSnapshotId}
     *
     * @return The snapshot id which is a unique {@code DateTime} value that identifies the share snapshot to its base
     * share.
     */
    public String getShareSnapshotId() {
        return this.snapshot;
    }

    /**
     * Get the share name of file client.
     *
     * <p>Get the share name. </p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.getShareName}
     *
     * @return The share name of the file.
     */
    public String getShareName() {
        return shareName;
    }

    /**
     * Get file path of the client.
     *
     * <p>Get the file path. </p>
     *
     * {@codesnippet com.azure.storage.file.fileAsyncClient.getFilePath}
     *
     * @return The path of the file.
     */
    public String getFilePath() {
        return filePath;
    }


    /**
     * Get associated account name.
     *
     * @return account name associated with this storage resource.
     */
    public String getAccountName() {
        return this.accountName;
    }

    private Response<FileInfo> createFileInfoResponse(final FilesCreateResponse response) {
        String eTag = response.getDeserializedHeaders().getETag();
        OffsetDateTime lastModified = response.getDeserializedHeaders().getLastModified();
        boolean isServerEncrypted = response.getDeserializedHeaders().isServerEncrypted();
        FileSmbProperties smbProperties = new FileSmbProperties(response.getHeaders());
        FileInfo fileInfo = new FileInfo(eTag, lastModified, isServerEncrypted, smbProperties);
        return new SimpleResponse<>(response, fileInfo);
    }

    private Response<FileInfo> setPropertiesResponse(final FilesSetHTTPHeadersResponse response) {
        String eTag = response.getDeserializedHeaders().getETag();
        OffsetDateTime lastModified = response.getDeserializedHeaders().getLastModified();
        boolean isServerEncrypted = response.getDeserializedHeaders().isServerEncrypted();
        FileSmbProperties smbProperties = new FileSmbProperties(response.getHeaders());
        FileInfo fileInfo = new FileInfo(eTag, lastModified, isServerEncrypted, smbProperties);
        return new SimpleResponse<>(response, fileInfo);
    }

    private Response<FileProperties> getPropertiesResponse(final FilesGetPropertiesResponse response) {
        FileGetPropertiesHeaders headers = response.getDeserializedHeaders();
        String eTag = headers.getETag();
        OffsetDateTime lastModified = headers.getLastModified();
        Map<String, String> metadata = headers.getMetadata();
        String fileType = headers.getFileType();
        Long contentLength = headers.getContentLength();
        String contentType = headers.getContentType();
        byte[] contentMD5;
        try {
            contentMD5 = headers.getContentMD5();
        } catch (NullPointerException e) {
            contentMD5 = null;
        }
        String contentEncoding = headers.getContentEncoding();
        String cacheControl = headers.getCacheControl();
        String contentDisposition = headers.getContentDisposition();
        OffsetDateTime copyCompletionTime = headers.getCopyCompletionTime();
        String copyStatusDescription = headers.getCopyStatusDescription();
        String copyId = headers.getCopyId();
        String copyProgress = headers.getCopyProgress();
        String copySource = headers.getCopySource();
        CopyStatusType copyStatus = headers.getCopyStatus();
        Boolean isServerEncrpted = headers.isServerEncrypted();
        FileSmbProperties smbProperties = new FileSmbProperties(response.getHeaders());
        FileProperties fileProperties = new FileProperties(eTag, lastModified, metadata, fileType, contentLength,
            contentType, contentMD5, contentEncoding, cacheControl, contentDisposition, copyCompletionTime,
            copyStatusDescription, copyId, copyProgress, copySource, copyStatus, isServerEncrpted, smbProperties);
        return new SimpleResponse<>(response, fileProperties);
    }

    private Response<FileUploadInfo> uploadResponse(final FilesUploadRangeResponse response) {
        FileUploadRangeHeaders headers = response.getDeserializedHeaders();
        String eTag = headers.getETag();
        OffsetDateTime lastModified = headers.getLastModified();
        byte[] contentMD5;
        try {
            contentMD5 = headers.getContentMD5();
        } catch (NullPointerException e) {
            contentMD5 = null;
        }
        Boolean isServerEncrypted = headers.isServerEncrypted();
        FileUploadInfo fileUploadInfo = new FileUploadInfo(eTag, lastModified, contentMD5, isServerEncrypted);
        return new SimpleResponse<>(response, fileUploadInfo);
    }

    private Response<FileUploadRangeFromUrlInfo> uploadRangeFromUrlResponse(
        final FilesUploadRangeFromURLResponse response) {
        FileUploadRangeFromURLHeaders headers = response.getDeserializedHeaders();
        String eTag = headers.getETag();
        OffsetDateTime lastModified = headers.getLastModified();
        Boolean isServerEncrypted = headers.isServerEncrypted();
        FileUploadRangeFromUrlInfo fileUploadRangeFromUrlInfo =
            new FileUploadRangeFromUrlInfo(eTag, lastModified, isServerEncrypted);
        return new SimpleResponse<>(response, fileUploadRangeFromUrlInfo);
    }

    private Response<FileMetadataInfo> setMetadataResponse(final FilesSetMetadataResponse response) {
        String eTag = response.getDeserializedHeaders().getETag();
        Boolean isServerEncrypted = response.getDeserializedHeaders().isServerEncrypted();
        FileMetadataInfo fileMetadataInfo = new FileMetadataInfo(eTag, isServerEncrypted);
        return new SimpleResponse<>(response, fileMetadataInfo);
    }

    /**
     * Verifies that the file permission and file permission key are not both set and if the file permission is set,
     * the file permission is of valid length.
     * @param filePermission The file permission.
     * @param filePermissionKey The file permission key.
     * @throws IllegalArgumentException for invalid file permission or file permission keys.
     */
    private void validateFilePermissionAndKey(String filePermission, String  filePermissionKey) {
        if (filePermission != null && filePermissionKey != null) {
            throw logger.logExceptionAsError(new IllegalArgumentException(
                FileConstants.MessageConstants.FILE_PERMISSION_FILE_PERMISSION_KEY_INVALID));
        }

        if (filePermission != null) {
            StorageImplUtils.assertInBounds("filePermission",
                filePermission.getBytes(StandardCharsets.UTF_8).length, 0, 8 * Constants.KB);
        }
    }
}
