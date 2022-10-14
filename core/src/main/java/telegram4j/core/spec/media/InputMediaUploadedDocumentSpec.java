package telegram4j.core.spec.media;

import telegram4j.tl.DocumentAttribute;
import telegram4j.tl.InputFile;

import java.time.Duration;
import java.util.*;

/**
 * Immutable implementation of {@link InputMediaUploadedDocumentSpecDef}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code InputMediaUploadedDocumentSpec.builder()}.
 * Use the static factory method to create immutable instances:
 * {@code InputMediaUploadedDocumentSpec.of()}.
 */
@SuppressWarnings({"all"})
public final class InputMediaUploadedDocumentSpec
    implements InputMediaUploadedDocumentSpecDef {
  private final boolean noSoundVideo;
  private final boolean forceFile;
  private final InputFile file;
  private final InputFile thumb;
  private final String mimeType;
  private final List<DocumentAttribute> attributes;
  private final List<String> stickers;
  private final Duration autoDeleteDuration;

  private InputMediaUploadedDocumentSpec(InputFile file, String mimeType) {
    this.file = Objects.requireNonNull(file, "file");
    this.mimeType = Objects.requireNonNull(mimeType, "mimeType");
    this.thumb = null;
    this.attributes = Collections.emptyList();
    this.stickers = null;
    this.autoDeleteDuration = null;
    this.noSoundVideo = initShim.noSoundVideo();
    this.forceFile = initShim.forceFile();
    this.initShim = null;
  }

  private InputMediaUploadedDocumentSpec(Builder builder) {
    this.file = builder.file;
    this.thumb = builder.thumb;
    this.mimeType = builder.mimeType;
    this.attributes = createUnmodifiableList(true, builder.attributes);
    this.stickers = builder.stickers;
    this.autoDeleteDuration = builder.autoDeleteDuration;
    if (builder.noSoundVideoIsSet()) {
      initShim.noSoundVideo(builder.noSoundVideo);
    }
    if (builder.forceFileIsSet()) {
      initShim.forceFile(builder.forceFile);
    }
    this.noSoundVideo = initShim.noSoundVideo();
    this.forceFile = initShim.forceFile();
    this.initShim = null;
  }

  private InputMediaUploadedDocumentSpec(
      boolean noSoundVideo,
      boolean forceFile,
      InputFile file,
      InputFile thumb,
      String mimeType,
      List<DocumentAttribute> attributes,
      List<String> stickers,
      Duration autoDeleteDuration) {
    this.noSoundVideo = noSoundVideo;
    this.forceFile = forceFile;
    this.file = file;
    this.thumb = thumb;
    this.mimeType = mimeType;
    this.attributes = attributes;
    this.stickers = stickers;
    this.autoDeleteDuration = autoDeleteDuration;
    this.initShim = null;
  }

  private static final byte STAGE_INITIALIZING = -1;
  private static final byte STAGE_UNINITIALIZED = 0;
  private static final byte STAGE_INITIALIZED = 1;
  private transient volatile InitShim initShim = new InitShim();

  private final class InitShim {
    private byte noSoundVideoBuildStage = STAGE_UNINITIALIZED;
    private boolean noSoundVideo;

    boolean noSoundVideo() {
      if (noSoundVideoBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (noSoundVideoBuildStage == STAGE_UNINITIALIZED) {
        noSoundVideoBuildStage = STAGE_INITIALIZING;
        this.noSoundVideo = noSoundVideoInitialize();
        noSoundVideoBuildStage = STAGE_INITIALIZED;
      }
      return this.noSoundVideo;
    }

    void noSoundVideo(boolean noSoundVideo) {
      this.noSoundVideo = noSoundVideo;
      noSoundVideoBuildStage = STAGE_INITIALIZED;
    }

    private byte forceFileBuildStage = STAGE_UNINITIALIZED;
    private boolean forceFile;

    boolean forceFile() {
      if (forceFileBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (forceFileBuildStage == STAGE_UNINITIALIZED) {
        forceFileBuildStage = STAGE_INITIALIZING;
        this.forceFile = forceFileInitialize();
        forceFileBuildStage = STAGE_INITIALIZED;
      }
      return this.forceFile;
    }

    void forceFile(boolean forceFile) {
      this.forceFile = forceFile;
      forceFileBuildStage = STAGE_INITIALIZED;
    }

    private String formatInitCycleMessage() {
      List<String> attributes = new ArrayList<>();
      if (noSoundVideoBuildStage == STAGE_INITIALIZING) attributes.add("noSoundVideo");
      if (forceFileBuildStage == STAGE_INITIALIZING) attributes.add("forceFile");
      return "Cannot build InputMediaUploadedDocumentSpec, attribute initializers form cycle " + attributes;
    }
  }

  private boolean noSoundVideoInitialize() {
    return InputMediaUploadedDocumentSpecDef.super.noSoundVideo();
  }

  private boolean forceFileInitialize() {
    return InputMediaUploadedDocumentSpecDef.super.forceFile();
  }

  /**
   * @return The value of the {@code noSoundVideo} attribute
   */
  @Override
  public boolean noSoundVideo() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.noSoundVideo()
        : this.noSoundVideo;
  }

  /**
   * @return The value of the {@code forceFile} attribute
   */
  @Override
  public boolean forceFile() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.forceFile()
        : this.forceFile;
  }

  /**
   * @return The value of the {@code file} attribute
   */
  @Override
  public InputFile file() {
    return file;
  }

  /**
   * @return The value of the {@code thumb} attribute
   */
  @Override
  public Optional<InputFile> thumb() {
    return Optional.ofNullable(thumb);
  }

  /**
   * @return The value of the {@code mimeType} attribute
   */
  @Override
  public String mimeType() {
    return mimeType;
  }

  /**
   * @return The value of the {@code attributes} attribute
   */
  @Override
  public List<DocumentAttribute> attributes() {
    return attributes;
  }

  /**
   * @return The value of the {@code stickers} attribute
   */
  @Override
  public Optional<List<String>> stickers() {
    return Optional.ofNullable(stickers);
  }

  /**
   * @return The value of the {@code autoDeleteDuration} attribute
   */
  @Override
  public Optional<Duration> autoDeleteDuration() {
    return Optional.ofNullable(autoDeleteDuration);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link InputMediaUploadedDocumentSpec#noSoundVideo() noSoundVideo} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for noSoundVideo
   * @return A modified copy of the {@code this} object
   */
  public final InputMediaUploadedDocumentSpec withNoSoundVideo(boolean value) {
    if (this.noSoundVideo == value) return this;
    return new InputMediaUploadedDocumentSpec(
        value,
        this.forceFile,
        this.file,
        this.thumb,
        this.mimeType,
        this.attributes,
        this.stickers,
        this.autoDeleteDuration);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link InputMediaUploadedDocumentSpec#forceFile() forceFile} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for forceFile
   * @return A modified copy of the {@code this} object
   */
  public final InputMediaUploadedDocumentSpec withForceFile(boolean value) {
    if (this.forceFile == value) return this;
    return new InputMediaUploadedDocumentSpec(
        this.noSoundVideo,
        value,
        this.file,
        this.thumb,
        this.mimeType,
        this.attributes,
        this.stickers,
        this.autoDeleteDuration);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link InputMediaUploadedDocumentSpec#file() file} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for file
   * @return A modified copy of the {@code this} object
   */
  public final InputMediaUploadedDocumentSpec withFile(InputFile value) {
    if (this.file == value) return this;
    InputFile newValue = Objects.requireNonNull(value, "file");
    return new InputMediaUploadedDocumentSpec(
        this.noSoundVideo,
        this.forceFile,
        newValue,
        this.thumb,
        this.mimeType,
        this.attributes,
        this.stickers,
        this.autoDeleteDuration);
  }

  /**
   * Copy the current immutable object by setting a <i>present</i> value for the optional {@link InputMediaUploadedDocumentSpec#thumb() thumb} attribute.
   * @param value The value for thumb, {@code null} is accepted as {@code java.util.Optional.empty()}
   * @return A modified copy of {@code this} object
   */
  public final InputMediaUploadedDocumentSpec withThumb(InputFile value) {
    InputFile newValue = value;
    if (this.thumb == newValue) return this;
    return new InputMediaUploadedDocumentSpec(
        this.noSoundVideo,
        this.forceFile,
        this.file,
        newValue,
        this.mimeType,
        this.attributes,
        this.stickers,
        this.autoDeleteDuration);
  }

  /**
   * Copy the current immutable object by setting an optional value for the {@link InputMediaUploadedDocumentSpec#thumb() thumb} attribute.
   * A shallow reference equality check is used on unboxed optional value to prevent copying of the same value by returning {@code this}.
   * @param optional A value for thumb
   * @return A modified copy of {@code this} object
   */
  @SuppressWarnings("unchecked") // safe covariant cast
  public final InputMediaUploadedDocumentSpec withThumb(Optional<? extends InputFile> optional) {
    InputFile value = optional.orElse(null);
    if (this.thumb == value) return this;
    return new InputMediaUploadedDocumentSpec(
        this.noSoundVideo,
        this.forceFile,
        this.file,
        value,
        this.mimeType,
        this.attributes,
        this.stickers,
        this.autoDeleteDuration);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link InputMediaUploadedDocumentSpec#mimeType() mimeType} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for mimeType
   * @return A modified copy of the {@code this} object
   */
  public final InputMediaUploadedDocumentSpec withMimeType(String value) {
    String newValue = Objects.requireNonNull(value, "mimeType");
    if (this.mimeType.equals(newValue)) return this;
    return new InputMediaUploadedDocumentSpec(
        this.noSoundVideo,
        this.forceFile,
        this.file,
        this.thumb,
        newValue,
        this.attributes,
        this.stickers,
        this.autoDeleteDuration);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link InputMediaUploadedDocumentSpec#attributes() attributes}.
   * @param elements The elements to set
   * @return A modified copy of {@code this} object
   */
  public final InputMediaUploadedDocumentSpec withAttributes(DocumentAttribute... elements) {
    List<DocumentAttribute> newValue = createUnmodifiableList(false, createSafeList(Arrays.asList(elements), true, false));
    return new InputMediaUploadedDocumentSpec(
        this.noSoundVideo,
        this.forceFile,
        this.file,
        this.thumb,
        this.mimeType,
        newValue,
        this.stickers,
        this.autoDeleteDuration);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link InputMediaUploadedDocumentSpec#attributes() attributes}.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param elements An iterable of attributes elements to set
   * @return A modified copy of {@code this} object
   */
  public final InputMediaUploadedDocumentSpec withAttributes(Iterable<? extends DocumentAttribute> elements) {
    if (this.attributes == elements) return this;
    List<DocumentAttribute> newValue = createUnmodifiableList(false, createSafeList(elements, true, false));
    return new InputMediaUploadedDocumentSpec(
        this.noSoundVideo,
        this.forceFile,
        this.file,
        this.thumb,
        this.mimeType,
        newValue,
        this.stickers,
        this.autoDeleteDuration);
  }

  /**
   * Copy the current immutable object by setting a <i>present</i> value for the optional {@link InputMediaUploadedDocumentSpec#stickers() stickers} attribute.
   * @param value The value for stickers, {@code null} is accepted as {@code java.util.Optional.empty()}
   * @return A modified copy of {@code this} object
   */
  public final InputMediaUploadedDocumentSpec withStickers(List<String> value) {
    List<String> newValue = value;
    if (this.stickers == newValue) return this;
    return new InputMediaUploadedDocumentSpec(
        this.noSoundVideo,
        this.forceFile,
        this.file,
        this.thumb,
        this.mimeType,
        this.attributes,
        newValue,
        this.autoDeleteDuration);
  }

  /**
   * Copy the current immutable object by setting an optional value for the {@link InputMediaUploadedDocumentSpec#stickers() stickers} attribute.
   * A shallow reference equality check is used on unboxed optional value to prevent copying of the same value by returning {@code this}.
   * @param optional A value for stickers
   * @return A modified copy of {@code this} object
   */
  @SuppressWarnings("unchecked") // safe covariant cast
  public final InputMediaUploadedDocumentSpec withStickers(Optional<? extends List<String>> optional) {
    List<String> value = optional.orElse(null);
    if (this.stickers == value) return this;
    return new InputMediaUploadedDocumentSpec(
        this.noSoundVideo,
        this.forceFile,
        this.file,
        this.thumb,
        this.mimeType,
        this.attributes,
        value,
        this.autoDeleteDuration);
  }

  /**
   * Copy the current immutable object by setting a <i>present</i> value for the optional {@link InputMediaUploadedDocumentSpec#autoDeleteDuration() autoDeleteDuration} attribute.
   * @param value The value for autoDeleteDuration, {@code null} is accepted as {@code java.util.Optional.empty()}
   * @return A modified copy of {@code this} object
   */
  public final InputMediaUploadedDocumentSpec withAutoDeleteDuration(Duration value) {
    Duration newValue = value;
    if (this.autoDeleteDuration == newValue) return this;
    return new InputMediaUploadedDocumentSpec(
        this.noSoundVideo,
        this.forceFile,
        this.file,
        this.thumb,
        this.mimeType,
        this.attributes,
        this.stickers,
        newValue);
  }

  /**
   * Copy the current immutable object by setting an optional value for the {@link InputMediaUploadedDocumentSpec#autoDeleteDuration() autoDeleteDuration} attribute.
   * A shallow reference equality check is used on unboxed optional value to prevent copying of the same value by returning {@code this}.
   * @param optional A value for autoDeleteDuration
   * @return A modified copy of {@code this} object
   */
  @SuppressWarnings("unchecked") // safe covariant cast
  public final InputMediaUploadedDocumentSpec withAutoDeleteDuration(Optional<? extends Duration> optional) {
    Duration value = optional.orElse(null);
    if (this.autoDeleteDuration == value) return this;
    return new InputMediaUploadedDocumentSpec(
        this.noSoundVideo,
        this.forceFile,
        this.file,
        this.thumb,
        this.mimeType,
        this.attributes,
        this.stickers,
        value);
  }

  /**
   * This instance is equal to all instances of {@code InputMediaUploadedDocumentSpec} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(Object another) {
    if (this == another) return true;
    return another instanceof InputMediaUploadedDocumentSpec
        && equalTo(0, (InputMediaUploadedDocumentSpec) another);
  }

  private boolean equalTo(int synthetic, InputMediaUploadedDocumentSpec another) {
    return noSoundVideo == another.noSoundVideo
        && forceFile == another.forceFile
        && file.equals(another.file)
        && Objects.equals(thumb, another.thumb)
        && mimeType.equals(another.mimeType)
        && attributes.equals(another.attributes)
        && Objects.equals(stickers, another.stickers)
        && Objects.equals(autoDeleteDuration, another.autoDeleteDuration);
  }

  /**
   * Computes a hash code from attributes: {@code noSoundVideo}, {@code forceFile}, {@code file}, {@code thumb}, {@code mimeType}, {@code attributes}, {@code stickers}, {@code autoDeleteDuration}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 5381;
    h += (h << 5) + Boolean.hashCode(noSoundVideo);
    h += (h << 5) + Boolean.hashCode(forceFile);
    h += (h << 5) + file.hashCode();
    h += (h << 5) + Objects.hashCode(thumb);
    h += (h << 5) + mimeType.hashCode();
    h += (h << 5) + attributes.hashCode();
    h += (h << 5) + Objects.hashCode(stickers);
    h += (h << 5) + Objects.hashCode(autoDeleteDuration);
    return h;
  }

  /**
   * Prints the immutable value {@code InputMediaUploadedDocumentSpec} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("InputMediaUploadedDocumentSpec{");
    builder.append("noSoundVideo=").append(noSoundVideo);
    builder.append(", ");
    builder.append("forceFile=").append(forceFile);
    builder.append(", ");
    builder.append("file=").append(file);
    if (thumb != null) {
      builder.append(", ");
      builder.append("thumb=").append(thumb);
    }
    builder.append(", ");
    builder.append("mimeType=").append(mimeType);
    builder.append(", ");
    builder.append("attributes=").append(attributes);
    if (stickers != null) {
      builder.append(", ");
      builder.append("stickers=").append(stickers);
    }
    if (autoDeleteDuration != null) {
      builder.append(", ");
      builder.append("autoDeleteDuration=").append(autoDeleteDuration);
    }
    return builder.append("}").toString();
  }

  /**
   * Construct a new immutable {@code InputMediaUploadedDocumentSpec} instance.
   * @param file The value for the {@code file} attribute
   * @param mimeType The value for the {@code mimeType} attribute
   * @return An immutable InputMediaUploadedDocumentSpec instance
   */
  public static InputMediaUploadedDocumentSpec of(InputFile file, String mimeType) {
    return new InputMediaUploadedDocumentSpec(file, mimeType);
  }

  /**
   * Creates an immutable copy of a {@link InputMediaUploadedDocumentSpecDef} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable InputMediaUploadedDocumentSpec instance
   */
  static InputMediaUploadedDocumentSpec copyOf(InputMediaUploadedDocumentSpecDef instance) {
    if (instance instanceof InputMediaUploadedDocumentSpec) {
      return (InputMediaUploadedDocumentSpec) instance;
    }
    return InputMediaUploadedDocumentSpec.builder()
        .from(instance)
        .build();
  }

  /**
   * Creates a builder for {@link InputMediaUploadedDocumentSpec InputMediaUploadedDocumentSpec}.
   * <pre>
   * InputMediaUploadedDocumentSpec.builder()
   *    .noSoundVideo(boolean) // optional {@link InputMediaUploadedDocumentSpec#noSoundVideo() noSoundVideo}
   *    .forceFile(boolean) // optional {@link InputMediaUploadedDocumentSpec#forceFile() forceFile}
   *    .file(telegram4j.tl.InputFile) // required {@link InputMediaUploadedDocumentSpec#file() file}
   *    .thumb(telegram4j.tl.InputFile) // optional {@link InputMediaUploadedDocumentSpec#thumb() thumb}
   *    .mimeType(String) // required {@link InputMediaUploadedDocumentSpec#mimeType() mimeType}
   *    .addAttribute|addAllAttributes(telegram4j.tl.DocumentAttribute) // {@link InputMediaUploadedDocumentSpec#attributes() attributes} elements
   *    .stickers(List&amp;lt;String&amp;gt;) // optional {@link InputMediaUploadedDocumentSpec#stickers() stickers}
   *    .autoDeleteDuration(java.time.Duration) // optional {@link InputMediaUploadedDocumentSpec#autoDeleteDuration() autoDeleteDuration}
   *    .build();
   * </pre>
   * @return A new InputMediaUploadedDocumentSpec builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builds instances of type {@link InputMediaUploadedDocumentSpec InputMediaUploadedDocumentSpec}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  public static final class Builder {
    private static final long INIT_BIT_FILE = 0x1L;
    private static final long INIT_BIT_MIME_TYPE = 0x2L;
    private static final long OPT_BIT_NO_SOUND_VIDEO = 0x1L;
    private static final long OPT_BIT_FORCE_FILE = 0x2L;
    private long initBits = 0x3L;
    private long optBits;

    private boolean noSoundVideo;
    private boolean forceFile;
    private InputFile file;
    private InputFile thumb;
    private String mimeType;
    private List<DocumentAttribute> attributes = new ArrayList<DocumentAttribute>();
    private List<String> stickers;
    private Duration autoDeleteDuration;

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code InputMediaUploadedDocumentSpec} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * Collection elements and entries will be added, not replaced.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(InputMediaUploadedDocumentSpec instance) {
      return from((InputMediaUploadedDocumentSpecDef) instance);
    }

    /**
     * Copy abstract value type {@code InputMediaUploadedDocumentSpecDef} instance into builder.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    final Builder from(InputMediaUploadedDocumentSpecDef instance) {
      Objects.requireNonNull(instance, "instance");
      noSoundVideo(instance.noSoundVideo());
      forceFile(instance.forceFile());
      file(instance.file());
      Optional<InputFile> thumbOptional = instance.thumb();
      if (thumbOptional.isPresent()) {
        thumb(thumbOptional);
      }
      mimeType(instance.mimeType());
      addAllAttributes(instance.attributes());
      Optional<List<String>> stickersOptional = instance.stickers();
      if (stickersOptional.isPresent()) {
        stickers(stickersOptional);
      }
      Optional<Duration> autoDeleteDurationOptional = instance.autoDeleteDuration();
      if (autoDeleteDurationOptional.isPresent()) {
        autoDeleteDuration(autoDeleteDurationOptional);
      }
      return this;
    }

    /**
     * Initializes the value for the {@link InputMediaUploadedDocumentSpec#noSoundVideo() noSoundVideo} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link InputMediaUploadedDocumentSpec#noSoundVideo() noSoundVideo}.</em>
     * @param noSoundVideo The value for noSoundVideo
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder noSoundVideo(boolean noSoundVideo) {
      this.noSoundVideo = noSoundVideo;
      optBits |= OPT_BIT_NO_SOUND_VIDEO;
      return this;
    }

    /**
     * Initializes the value for the {@link InputMediaUploadedDocumentSpec#forceFile() forceFile} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link InputMediaUploadedDocumentSpec#forceFile() forceFile}.</em>
     * @param forceFile The value for forceFile
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder forceFile(boolean forceFile) {
      this.forceFile = forceFile;
      optBits |= OPT_BIT_FORCE_FILE;
      return this;
    }

    /**
     * Initializes the value for the {@link InputMediaUploadedDocumentSpec#file() file} attribute.
     * @param file The value for file
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder file(InputFile file) {
      this.file = Objects.requireNonNull(file, "file");
      initBits &= ~INIT_BIT_FILE;
      return this;
    }

    /**
     * Initializes the optional value {@link InputMediaUploadedDocumentSpec#thumb() thumb} to thumb.
     * @param thumb The value for thumb, {@code null} is accepted as {@code java.util.Optional.empty()}
     * @return {@code this} builder for chained invocation
     */
    public final Builder thumb(InputFile thumb) {
      this.thumb = thumb;
      return this;
    }

    /**
     * Initializes the optional value {@link InputMediaUploadedDocumentSpec#thumb() thumb} to thumb.
     * @param thumb The value for thumb
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder thumb(Optional<? extends InputFile> thumb) {
      this.thumb = thumb.orElse(null);
      return this;
    }

    /**
     * Initializes the value for the {@link InputMediaUploadedDocumentSpec#mimeType() mimeType} attribute.
     * @param mimeType The value for mimeType
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder mimeType(String mimeType) {
      this.mimeType = Objects.requireNonNull(mimeType, "mimeType");
      initBits &= ~INIT_BIT_MIME_TYPE;
      return this;
    }

    /**
     * Adds one element to {@link InputMediaUploadedDocumentSpec#attributes() attributes} list.
     * @param element A attributes element
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addAttribute(DocumentAttribute element) {
      this.attributes.add(Objects.requireNonNull(element, "attributes element"));
      return this;
    }

    /**
     * Adds elements to {@link InputMediaUploadedDocumentSpec#attributes() attributes} list.
     * @param elements An array of attributes elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addAttributes(DocumentAttribute... elements) {
      for (DocumentAttribute element : elements) {
        this.attributes.add(Objects.requireNonNull(element, "attributes element"));
      }
      return this;
    }


    /**
     * Sets or replaces all elements for {@link InputMediaUploadedDocumentSpec#attributes() attributes} list.
     * @param elements An iterable of attributes elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder attributes(Iterable<? extends DocumentAttribute> elements) {
      this.attributes.clear();
      return addAllAttributes(elements);
    }

    /**
     * Adds elements to {@link InputMediaUploadedDocumentSpec#attributes() attributes} list.
     * @param elements An iterable of attributes elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addAllAttributes(Iterable<? extends DocumentAttribute> elements) {
      for (DocumentAttribute element : elements) {
        this.attributes.add(Objects.requireNonNull(element, "attributes element"));
      }
      return this;
    }

    /**
     * Initializes the optional value {@link InputMediaUploadedDocumentSpec#stickers() stickers} to stickers.
     * @param stickers The value for stickers, {@code null} is accepted as {@code java.util.Optional.empty()}
     * @return {@code this} builder for chained invocation
     */
    public final Builder stickers(List<String> stickers) {
      this.stickers = stickers;
      return this;
    }

    /**
     * Initializes the optional value {@link InputMediaUploadedDocumentSpec#stickers() stickers} to stickers.
     * @param stickers The value for stickers
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder stickers(Optional<? extends List<String>> stickers) {
      this.stickers = stickers.orElse(null);
      return this;
    }

    /**
     * Initializes the optional value {@link InputMediaUploadedDocumentSpec#autoDeleteDuration() autoDeleteDuration} to autoDeleteDuration.
     * @param autoDeleteDuration The value for autoDeleteDuration, {@code null} is accepted as {@code java.util.Optional.empty()}
     * @return {@code this} builder for chained invocation
     */
    public final Builder autoDeleteDuration(Duration autoDeleteDuration) {
      this.autoDeleteDuration = autoDeleteDuration;
      return this;
    }

    /**
     * Initializes the optional value {@link InputMediaUploadedDocumentSpec#autoDeleteDuration() autoDeleteDuration} to autoDeleteDuration.
     * @param autoDeleteDuration The value for autoDeleteDuration
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder autoDeleteDuration(Optional<? extends Duration> autoDeleteDuration) {
      this.autoDeleteDuration = autoDeleteDuration.orElse(null);
      return this;
    }

    /**
     * Builds a new {@link InputMediaUploadedDocumentSpec InputMediaUploadedDocumentSpec}.
     * @return An immutable instance of InputMediaUploadedDocumentSpec
     * @throws IllegalStateException if any required attributes are missing
     */
    public InputMediaUploadedDocumentSpec build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return new InputMediaUploadedDocumentSpec(this);
    }

    private boolean noSoundVideoIsSet() {
      return (optBits & OPT_BIT_NO_SOUND_VIDEO) != 0;
    }

    private boolean forceFileIsSet() {
      return (optBits & OPT_BIT_FORCE_FILE) != 0;
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = new ArrayList<>();
      if ((initBits & INIT_BIT_FILE) != 0) attributes.add("file");
      if ((initBits & INIT_BIT_MIME_TYPE) != 0) attributes.add("mimeType");
      return "Cannot build InputMediaUploadedDocumentSpec, some of required attributes are not set " + attributes;
    }
  }

  private static <T> List<T> createSafeList(Iterable<? extends T> iterable, boolean checkNulls, boolean skipNulls) {
    ArrayList<T> list;
    if (iterable instanceof Collection<?>) {
      int size = ((Collection<?>) iterable).size();
      if (size == 0) return Collections.emptyList();
      list = new ArrayList<>();
    } else {
      list = new ArrayList<>();
    }
    for (T element : iterable) {
      if (skipNulls && element == null) continue;
      if (checkNulls) Objects.requireNonNull(element, "element");
      list.add(element);
    }
    return list;
  }

  private static <T> List<T> createUnmodifiableList(boolean clone, List<T> list) {
    switch(list.size()) {
    case 0: return Collections.emptyList();
    case 1: return Collections.singletonList(list.get(0));
    default:
      if (clone) {
        return Collections.unmodifiableList(new ArrayList<>(list));
      } else {
        if (list instanceof ArrayList<?>) {
          ((ArrayList<?>) list).trimToSize();
        }
        return Collections.unmodifiableList(list);
      }
    }
  }
}
