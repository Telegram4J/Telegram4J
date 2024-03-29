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
package telegram4j.core.spec.media;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.BitFlag;
import telegram4j.core.util.ImmutableEnumSet;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.*;
import telegram4j.tl.api.TlEncodingUtil;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class InputMediaUploadedDocumentSpec implements InputMediaSpec {
    private final InputFile file;
    private final InputFile thumb;
    private final String mimeType;
    private final List<DocumentAttribute> attributes;
    private final List<FileReferenceId> stickers;
    private final Duration autoDeleteDuration;
    private final ImmutableEnumSet<Flag> flags;

    private InputMediaUploadedDocumentSpec(InputFile file, String mimeType,
                                           Iterable<? extends DocumentAttribute> attributes) {
        this.file = Objects.requireNonNull(file);
        this.mimeType = Objects.requireNonNull(mimeType);
        this.thumb = null;
        this.attributes = TlEncodingUtil.copyList(attributes);
        this.stickers = null;
        this.autoDeleteDuration = null;
        this.flags = ImmutableEnumSet.of(Flag.class, 0);
    }

    private InputMediaUploadedDocumentSpec(Builder builder) {
        this.file = builder.file;
        this.thumb = builder.thumb;
        this.mimeType = builder.mimeType;
        this.attributes = List.copyOf(builder.attributes);
        this.stickers = builder.stickers != null ? List.copyOf(builder.stickers) : null;
        this.autoDeleteDuration = builder.autoDeleteDuration;
        this.flags = ImmutableEnumSet.of(Flag.class, builder.flags);
    }

    private InputMediaUploadedDocumentSpec(ImmutableEnumSet<Flag> flags,
                                           InputFile file, @Nullable InputFile thumb, String mimeType,
                                           List<DocumentAttribute> attributes, @Nullable List<FileReferenceId> stickers,
                                           @Nullable Duration autoDeleteDuration) {
        this.flags = flags;
        this.file = file;
        this.thumb = thumb;
        this.mimeType = mimeType;
        this.attributes = attributes;
        this.stickers = stickers;
        this.autoDeleteDuration = autoDeleteDuration;
    }

    public ImmutableEnumSet<Flag> flags() {
        return flags;
    }

    public InputFile file() {
        return file;
    }

    public Optional<InputFile> thumb() {
        return Optional.ofNullable(thumb);
    }

    public String mimeType() {
        return mimeType;
    }

    public List<DocumentAttribute> attributes() {
        return attributes;
    }

    public Optional<List<FileReferenceId>> stickers() {
        return Optional.ofNullable(stickers);
    }

    public Optional<Duration> autoDeleteDuration() {
        return Optional.ofNullable(autoDeleteDuration);
    }

    @Override
    public Mono<ImmutableInputMediaUploadedDocument> resolve(MTProtoTelegramClient client) {
        return Mono.fromSupplier(() -> InputMediaUploadedDocument.builder()
                .flags(flags.getValue())
                .file(file)
                .thumb(thumb)
                .mimeType(mimeType)
                .attributes(attributes)
                .stickers(stickers()
                        .map(list -> list.stream()
                                .map(FileReferenceId::asInputDocument)
                                .collect(Collectors.toList()))
                        .orElse(null))
                .ttlSeconds(autoDeleteDuration()
                        .map(Duration::getSeconds)
                        .map(Math::toIntExact)
                        .orElse(null))
                .build());
    }

    public InputMediaUploadedDocumentSpec withFile(Iterable<Flag> value) {
        if (value.equals(flags)) return this;
        var newFlags = ImmutableEnumSet.of(Flag.class, value);
        return new InputMediaUploadedDocumentSpec(newFlags, file, thumb, mimeType, attributes, stickers, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withFile(InputFile value) {
        Objects.requireNonNull(value);
        if (file == value) return this;
        return new InputMediaUploadedDocumentSpec(flags, value, thumb, mimeType, attributes, stickers, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withThumb(@Nullable InputFile value) {
        if (thumb == value) return this;
        return new InputMediaUploadedDocumentSpec(flags, file, value, mimeType, attributes, stickers, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withThumb(Optional<? extends InputFile> opt) {
        return withThumb(opt.orElse(null));
    }

    public InputMediaUploadedDocumentSpec withMimeType(String value) {
        Objects.requireNonNull(value);
        if (mimeType.equals(value)) return this;
        return new InputMediaUploadedDocumentSpec(flags, file, thumb, value, attributes, stickers, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withAttributes(DocumentAttribute... elements) {
        var newValue = List.of(elements);
        if (attributes == newValue) return this;
        return new InputMediaUploadedDocumentSpec(flags, file, thumb, mimeType, newValue, stickers, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withAttributes(Iterable<? extends DocumentAttribute> elements) {
        Objects.requireNonNull(elements);
        if (attributes == elements) return this;
        List<DocumentAttribute> newValue = TlEncodingUtil.copyList(elements);
        if (attributes == newValue) return this;
        return new InputMediaUploadedDocumentSpec(flags, file, thumb, mimeType, newValue, stickers, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withStickers(@Nullable Iterable<FileReferenceId> value) {
        if (stickers == value) return this;
        var newStickers = value != null ? TlEncodingUtil.copyList(value) : null;
        if (stickers == newStickers) return this;
        return new InputMediaUploadedDocumentSpec(flags, file, thumb, mimeType, attributes, newStickers, autoDeleteDuration);
    }

    public InputMediaUploadedDocumentSpec withStickers(Optional<? extends Iterable<FileReferenceId>> opt) {
        return withStickers(opt.orElse(null));
    }

    public InputMediaUploadedDocumentSpec withAutoDeleteDuration(@Nullable Duration value) {
        if (Objects.equals(autoDeleteDuration, value)) return this;
        return new InputMediaUploadedDocumentSpec(flags, file, thumb, mimeType, attributes, stickers, value);
    }

    public InputMediaUploadedDocumentSpec withAutoDeleteDuration(Optional<? extends Duration> opt) {
        return withAutoDeleteDuration(opt.orElse(null));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InputMediaUploadedDocumentSpec that)) return false;
        return flags.equals(that.flags)
                && file.equals(that.file)
                && Objects.equals(thumb, that.thumb)
                && mimeType.equals(that.mimeType)
                && attributes.equals(that.attributes)
                && Objects.equals(stickers, that.stickers)
                && Objects.equals(autoDeleteDuration, that.autoDeleteDuration);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + flags.hashCode();
        h += (h << 5) + file.hashCode();
        h += (h << 5) + Objects.hashCode(thumb);
        h += (h << 5) + mimeType.hashCode();
        h += (h << 5) + attributes.hashCode();
        h += (h << 5) + Objects.hashCode(stickers);
        h += (h << 5) + Objects.hashCode(autoDeleteDuration);
        return h;
    }

    @Override
    public String toString() {
        return "InputMediaUploadedDocumentSpec{" +
                "flags=" + flags +
                ", file=" + file +
                ", thumb=" + thumb +
                ", mimeType='" + mimeType + '\'' +
                ", attributes=" + attributes +
                ", stickers=" + stickers +
                ", autoDeleteDuration=" + autoDeleteDuration +
                '}';
    }

    public static InputMediaUploadedDocumentSpec of(InputFile file, String mimeType,
                                                    Iterable<? extends DocumentAttribute> attributes) {
        return new InputMediaUploadedDocumentSpec(file, mimeType, attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final byte INIT_BIT_FILE = 0x1;
        private static final byte INIT_BIT_MIME_TYPE = 0x2;
        private static final byte INIT_BIT_ATTRIBUTES = 0x4;

        private byte initBits = 0x7;

        private Set<Flag> flags = EnumSet.noneOf(Flag.class);
        private InputFile file;
        private InputFile thumb;
        private String mimeType;
        private List<DocumentAttribute> attributes;
        private List<FileReferenceId> stickers;
        private Duration autoDeleteDuration;

        private Builder() {
        }

        public Builder from(InputMediaUploadedDocumentSpec instance) {
            flags(instance.flags);
            file(instance.file);
            thumb(instance.thumb);
            mimeType(instance.mimeType);
            attributes(instance.attributes);
            stickers(instance.stickers);
            autoDeleteDuration(instance.autoDeleteDuration);
            return this;
        }

        public Builder flags(Set<Flag> flags) {
            this.flags = EnumSet.copyOf(flags);
            return this;
        }

        public Builder addFlag(Flag flag) {
            flags.add(flag);
            return this;
        }

        public Builder addFlags(Flag... flags) {
            Collections.addAll(this.flags, flags);
            return this;
        }

        public Builder addFlags(Iterable<Flag> flags) {
            for (Flag flag : flags) {
                this.flags.add(flag);
            }
            return this;
        }

        public Builder file(InputFile file) {
            this.file = Objects.requireNonNull(file);
            initBits &= ~INIT_BIT_FILE;
            return this;
        }

        public Builder thumb(@Nullable InputFile thumb) {
            this.thumb = thumb;
            return this;
        }

        public Builder thumb(Optional<? extends InputFile> thumb) {
            this.thumb = thumb.orElse(null);
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = Objects.requireNonNull(mimeType);
            initBits &= ~INIT_BIT_MIME_TYPE;
            return this;
        }

        public Builder addAttribute(DocumentAttribute element) {
            Objects.requireNonNull(element);
            if (attributes == null) {
                attributes = new ArrayList<>();
                initBits &= ~INIT_BIT_ATTRIBUTES;
            }
            this.attributes.add(element);
            return this;
        }

        public Builder addAttributes(DocumentAttribute... elements) {
            var copy = Arrays.stream(elements)
                    .map(Objects::requireNonNull)
                    .collect(Collectors.toList());
            if (attributes == null) {
                attributes = copy;
                initBits &= ~INIT_BIT_ATTRIBUTES;
            } else {
                attributes.addAll(copy);
            }
            return this;
        }

        public Builder attributes(Iterable<? extends DocumentAttribute> elements) {
            attributes = StreamSupport.stream(elements.spliterator(), false)
                    .map(Objects::requireNonNull)
                    .collect(Collectors.toList());
            initBits &= ~INIT_BIT_ATTRIBUTES;
            return this;
        }

        public Builder addAllAttributes(Iterable<? extends DocumentAttribute> elements) {
            List<DocumentAttribute> copy = StreamSupport.stream(elements.spliterator(), false)
                    .map(Objects::requireNonNull)
                    .collect(Collectors.toList());
            if (attributes == null) {
                attributes = copy;
                initBits &= ~INIT_BIT_ATTRIBUTES;
            } else {
                attributes.addAll(copy);
            }
            return this;
        }

        public Builder stickers(@Nullable Iterable<FileReferenceId> stickers) {
            this.stickers = stickers != null ? TlEncodingUtil.copyList(stickers) : null;
            return this;
        }

        public Builder stickers(Optional<? extends Iterable<FileReferenceId>> stickers) {
            this.stickers = stickers.map(TlEncodingUtil::copyList).orElse(null);
            return this;
        }

        public Builder autoDeleteDuration(@Nullable Duration autoDeleteDuration) {
            this.autoDeleteDuration = autoDeleteDuration;
            return this;
        }

        public Builder autoDeleteDuration(Optional<Duration> autoDeleteDuration) {
            this.autoDeleteDuration = autoDeleteDuration.orElse(null);
            return this;
        }

        public InputMediaUploadedDocumentSpec build() {
            if (initBits != 0) {
                throw new IllegalStateException(formatRequiredAttributesMessage());
            }
            return new InputMediaUploadedDocumentSpec(this);
        }

        private String formatRequiredAttributesMessage() {
            List<String> attributes = new ArrayList<>();
            if ((initBits & INIT_BIT_FILE) != 0) attributes.add("file");
            if ((initBits & INIT_BIT_MIME_TYPE) != 0) attributes.add("mimeType");
            if ((initBits & INIT_BIT_ATTRIBUTES) != 0) attributes.add("attributes");
            return "Cannot build InputMediaUploadedDocumentSpec, some of required attributes are not set " + attributes;
        }
    }

    public enum Flag implements BitFlag {
        NO_SOUND_VIDEO(InputMediaUploadedDocument.NOSOUND_VIDEO_POS),
        FORCE_FILE(InputMediaUploadedDocument.FORCE_FILE_POS),
        SPOILER(InputMediaUploadedDocument.SPOILER_POS);

        private final byte position;

        Flag(byte position) {
            this.position = position;
        }

        @Override
        public byte position() {
            return position;
        }
    }
}
