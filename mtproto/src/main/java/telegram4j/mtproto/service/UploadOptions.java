/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.InputFileBig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static telegram4j.mtproto.service.UploadService.*;

public class UploadOptions {
    private final Publisher<? extends ByteBuf> data;
    private final long size;
    private final int partSize;
    private final int partsCount;
    private final String name;
    private final int parallelism;
    private final long fileId;

    UploadOptions(Builder builder) {
        this.data = builder.data;
        this.size = builder.size;
        this.partSize = builder.partSize;
        this.name = builder.name;
        this.parallelism = builder.parallelism;
        this.partsCount = builder.partsCount;
        this.fileId = builder.fileId;
    }

    UploadOptions(Publisher<? extends ByteBuf> data, long size, String name) {
        this.data = Objects.requireNonNull(data);
        this.size = size;
        this.name = Objects.requireNonNull(name);
        this.parallelism = UploadService.suggestParallelism(size);
        this.partSize = UploadService.suggestPartSize(size, -1);
        this.partsCount = (int) Math.ceil((double) size / partSize);
        this.fileId = CryptoUtil.random.nextLong();
    }

    /**
     * Gets {@code ByteBuf} source for this file.
     *
     * @return A {@link Publisher} with file source.
     */
    public Publisher<? extends ByteBuf> getData() {
        return data;
    }

    /**
     * Gets whether file size is more than {@value UploadService#BIG_FILE_THRESHOLD} bytes and
     * {@link InputFileBig} file id should be returned.
     *
     * @return {@code true} if file size is more than {@value UploadService#BIG_FILE_THRESHOLD} bytes.
     */
    public boolean isBigFile() {
        return size > BIG_FILE_THRESHOLD;
    }

    /**
     * Gets exact size of uploading file.
     *
     * @return The exact size of uploading file.
     */
    public long getSize() {
        return size;
    }

    /**
     * Gets size of uploading part.
     *
     * @return The size of uploading part.
     */
    public int getPartSize() {
        return partSize;
    }

    /**
     * Gets name which will be assigned to file after uploading.
     *
     * @return The name which will be assigned to file after uploading.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets number of uploading in parallel media clients.
     *
     * @return The number of uploading in parallel media clients.
     */
    public int getParallelism() {
        return parallelism;
    }

    /**
     * Gets total number of file parts for specified {@link #getPartSize()}.
     *
     * @return The total number of file parts for specified {@link #getPartSize()}.
     */
    public int getPartsCount() {
        return partsCount;
    }

    /**
     * Gets random file identifier.
     *
     * @return The random file identifier.
     */
    public long getFileId() {
        return fileId;
    }

    /**
     * Creates new {@code UploadOptions} with specified mandatory parameters.
     * All other attributes will initialize depends on specified values.
     *
     * @throws IllegalArgumentException if {@code size} is negative or more than {@link UploadService#MAX_FILE_SIZE}.
     * @param data The publisher emitting data for uploading.
     * @param size The exact size of file.
     * @param name The name for uploaded file.
     */
    public static UploadOptions create(Publisher<? extends ByteBuf> data, long size, String name) {
        if (size <= 0 || size > MAX_FILE_SIZE)
            throw new IllegalArgumentException("Invalid file size: " + size);
        return new UploadOptions(data, size, name);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        static final byte INIT_BIT_DATA = 1 << 0;
        static final byte INIT_BIT_SIZE = 1 << 1;
        static final byte INIT_BIT_NAME = 1 << 2;
        static final byte OPT_BIT_FILE_ID = 1 << 0;

        private byte initBits = INIT_BIT_DATA | INIT_BIT_SIZE | INIT_BIT_NAME;
        private byte optBits = OPT_BIT_FILE_ID;

        private int partsCount;
        private Publisher<? extends ByteBuf> data;
        private long size;
        private String name;
        private int partSize = -1;
        private int parallelism = -1;
        private long fileId;

        private Builder() {}

        public Builder data(Publisher<? extends ByteBuf> data) {
            this.data = Objects.requireNonNull(data);
            initBits &= ~INIT_BIT_DATA;
            return this;
        }

        public Builder size(long size) {
            if (size <= 0 || size > MAX_FILE_SIZE)
                throw new IllegalArgumentException("Invalid file size: " + size);
            this.size = size;
            initBits &= ~INIT_BIT_SIZE;
            return this;
        }

        public Builder partSize(int partSize) {
            if (partSize < -1 || partSize % 1024 != 0 || MAX_PART_SIZE % partSize != 0)
                throw new IllegalArgumentException("Invalid part size: " + partSize);
            this.partSize = partSize;
            return this;
        }

        public Builder name(String name) {
            this.name = Objects.requireNonNull(name);
            initBits &= ~INIT_BIT_NAME;
            return this;
        }

        public Builder parallelism(int parallelism) {
            if (parallelism < 1 && parallelism != -1)
                throw new IllegalArgumentException("Invalid parallelism");
            this.parallelism = parallelism;
            return this;
        }

        public Builder fileId(long fileId) {
            this.fileId = fileId;
            optBits &= ~OPT_BIT_FILE_ID;
            return this;
        }

        public UploadOptions build() {
            if (initBits != 0) {
                List<String> attributes = new ArrayList<>(Integer.bitCount(initBits));
                if ((initBits & INIT_BIT_DATA) != 0) attributes.add("data");
                if ((initBits & INIT_BIT_SIZE) != 0) attributes.add("size");
                if ((initBits & INIT_BIT_NAME) != 0) attributes.add("name");
                throw new IllegalStateException("Can not create UploadOptions, some of required attributes are not set: " + attributes);
            }
            if (partSize == -1)
                partSize = UploadService.suggestPartSize(size, partSize);
            partsCount = (int) Math.ceil((double) size / partSize);
            if (partsCount > MAX_PARTS_COUNT) {
                throw new IllegalArgumentException("Invalid size and part size parameters, parts count is too big." +
                        "size: " + size + ", part size: " + partSize);
            }
            if (parallelism == -1) {
                parallelism = UploadService.suggestParallelism(size);
            }
            if (size <= BIG_FILE_THRESHOLD && parallelism > 1) {
                throw new IllegalArgumentException("Parallelism option is disabled for small files");
            }

            if ((optBits & OPT_BIT_FILE_ID) != 0)
                fileId = CryptoUtil.random.nextLong();

            return new UploadOptions(this);
        }
    }
}
