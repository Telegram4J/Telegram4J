package telegram4j.core.internal;

import telegram4j.core.retriever.EntityRetrievalStrategy;

public class RetrievalUtil {
    private RetrievalUtil() {}

    public static final EntityRetrievalStrategy IDENTITY = client -> client;
}
