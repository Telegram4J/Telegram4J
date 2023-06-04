package telegram4j.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.util.IllegalReferenceCountException;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

// COPIED FROM REACTOR NETTY CORE
// TODO: REMOVE. Write replacement
public class ByteBufFlux extends FluxOperator<ByteBuf, ByteBuf> {

    private static final Logger log = Loggers.getLogger(ByteBufFlux.class);

    static final Consumer<? super FileChannel> fileCloser = fc -> {
        try {
            fc.close();
        } catch (Throwable e) {
            if (log.isTraceEnabled()) {
                log.trace("", e);
            }
        }
    };

    /**
     * Open a {@link java.nio.channels.FileChannel} from a path and stream
     * {@link ByteBuf} chunks with a default maximum size of 500K into
     * the returned {@link ByteBufFlux}
     *
     * @param path the path to the resource to stream
     * @return a {@link ByteBufFlux}
     */
    public static ByteBufFlux fromPath(Path path) {
        return fromPath(path, MAX_CHUNK_SIZE);
    }

    /**
     * Open a {@link java.nio.channels.FileChannel} from a path and stream
     * {@link ByteBuf} chunks with a given maximum size into the returned {@link ByteBufFlux}
     *
     * @param path the path to the resource to stream
     * @param maxChunkSize the maximum per-item ByteBuf size
     * @return a {@link ByteBufFlux}
     */
    public static ByteBufFlux fromPath(Path path, int maxChunkSize) {
        return fromPath(path, maxChunkSize, ByteBufAllocator.DEFAULT);
    }

    /**
     * Open a {@link java.nio.channels.FileChannel} from a path and stream
     * {@link ByteBuf} chunks with a default maximum size of 500K into the returned
     * {@link ByteBufFlux}, using the provided {@link ByteBufAllocator}.
     *
     * @param path the path to the resource to stream
     * @param allocator the channel {@link ByteBufAllocator}
     * @return a {@link ByteBufFlux}
     */
    public static ByteBufFlux fromPath(Path path, ByteBufAllocator allocator) {
        return fromPath(path, MAX_CHUNK_SIZE, allocator);
    }

    /**
     * Open a {@link java.nio.channels.FileChannel} from a path and stream
     * {@link ByteBuf} chunks with a given maximum size into the returned
     * {@link ByteBufFlux}, using the provided {@link ByteBufAllocator}.
     *
     * @param path the path to the resource to stream
     * @param maxChunkSize the maximum per-item ByteBuf size
     * @param allocator the channel {@link ByteBufAllocator}
     * @return a {@link ByteBufFlux}
     */
    public static ByteBufFlux fromPath(Path path,
                                       int maxChunkSize,
                                       ByteBufAllocator allocator) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(allocator, "allocator");
        if (maxChunkSize < 1) {
            throw new IllegalArgumentException("chunk size must be strictly positive, " + "was: " + maxChunkSize);
        }
        return maybeFuse(
                Flux.generate(() -> FileChannel.open(path),
                        (fc, sink) -> {
                            ByteBuf buf = allocator.buffer();
                            try {
                                if (buf.writeBytes(fc, maxChunkSize) < 0) {
                                    buf.release();
                                    sink.complete();
                                } else {
                                    sink.next(buf);
                                }
                            } catch (IOException e) {
                                buf.release();
                                sink.error(e);
                            }
                            return fc;
                        },
                        fileCloser),
                allocator);
    }

    /**
     * Convert to a {@link ByteBuffer} inbound {@link Flux}
     *
     * @return a {@link ByteBuffer} inbound {@link Flux}
     */
    public final Flux<ByteBuffer> asByteBuffer() {
        return handle((bb, sink) -> {
            try {
                sink.next(bb.nioBuffer());
            } catch (IllegalReferenceCountException e) {
                sink.complete();
            }
        });
    }

    /**
     * Convert to a {@literal byte[]} inbound {@link Flux}
     *
     * @return a {@literal byte[]} inbound {@link Flux}
     */
    public final Flux<byte[]> asByteArray() {
        return handle((bb, sink) -> {
            try {
                byte[] bytes = new byte[bb.readableBytes()];
                bb.readBytes(bytes);
                sink.next(bytes);
            } catch (IllegalReferenceCountException e) {
                sink.complete();
            }
        });
    }

    /**
     * Convert to a {@link String} inbound {@link Flux} using the default {@link Charset}.
     *
     * @return a {@link String} inbound {@link Flux}
     */
    public final Flux<String> asString() {
        return asString(Charset.defaultCharset());
    }

    /**
     * Convert to a {@link String} inbound {@link Flux} using the provided {@link Charset}.
     *
     * @param charset the decoding charset
     * @return a {@link String} inbound {@link Flux}
     */
    public final Flux<String> asString(Charset charset) {
        Objects.requireNonNull(charset, "charset");
        return handle((bb, sink) -> {
            try {
                sink.next(bb.readCharSequence(bb.readableBytes(), charset).toString());
            } catch (IllegalReferenceCountException e) {
                sink.complete();
            }
        });
    }

    /**
     * Disable auto memory release on each buffer published, retaining in order to prevent
     * premature recycling when buffers are accumulated downstream (async).
     *
     * @return {@link ByteBufFlux} of retained {@link ByteBuf}
     */
    public final ByteBufFlux retain() {
        return maybeFuse(doOnNext(ByteBuf::retain), alloc);
    }

    final ByteBufAllocator alloc;

    ByteBufFlux(Flux<ByteBuf> source, ByteBufAllocator allocator) {
        super(source);
        this.alloc = allocator;
    }

    static final class ByteBufFluxFuseable extends ByteBufFlux implements Fuseable {

        ByteBufFluxFuseable(Flux<ByteBuf> source, ByteBufAllocator allocator) {
            super(source, allocator);
        }
    }

    @Override
    public void subscribe(CoreSubscriber<? super ByteBuf> s) {
        source.subscribe(s);
    }

    static ByteBufFlux maybeFuse(Flux<ByteBuf> source, ByteBufAllocator allocator) {
        if (source instanceof Fuseable) {
            return new ByteBufFluxFuseable(source, allocator);
        }
        return new ByteBufFlux(source, allocator);
    }

    /**
     * A channel object to {@link ByteBuf} transformer
     */
    final static Function<Object, ByteBuf> bytebufExtractor = o -> {
        if (o instanceof ByteBuf) {
            return (ByteBuf) o;
        }
        if (o instanceof ByteBufHolder) {
            return ((ByteBufHolder) o).content();
        }
        if (o instanceof byte[]) {
            return Unpooled.wrappedBuffer((byte[]) o);
        }
        throw new IllegalArgumentException("Object " + o + " of type " + o.getClass() + " " + "cannot be converted to ByteBuf");
    };

    final static int MAX_CHUNK_SIZE = 1024 * 512; //500k

    static void safeRelease(ByteBuf byteBuf) {
        if (byteBuf.refCnt() > 0) {
            try {
                byteBuf.release();
            } catch (IllegalReferenceCountException e) {
                if (log.isDebugEnabled()) {
                    log.debug("", e);
                }
            }
        }
    }
}
