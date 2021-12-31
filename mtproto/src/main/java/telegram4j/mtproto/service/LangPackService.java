package telegram4j.mtproto.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.LangPackDifference;
import telegram4j.tl.LangPackLanguage;
import telegram4j.tl.LangPackString;
import telegram4j.tl.request.langpack.*;

import java.util.function.Function;

public class LangPackService extends RpcService {

    public LangPackService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    public Mono<LangPackDifference> getLangPack(String langPack, String langCode) {
        return client.sendAwait(ImmutableGetLangPack.of(langPack, langCode));
    }

    public Flux<LangPackString> getStrings(String langPack, String langCode, Iterable<String> keys) {
        return client.sendAwait(GetStrings.builder()
                        .langPack(langPack)
                        .langCode(langCode)
                        .keys(keys)
                        .build())
                .flatMapIterable(Function.identity());
    }

    public Mono<LangPackDifference> getDifference(String langPack, String langCode, int fromVersion) {
        return client.sendAwait(ImmutableGetDifference.of(langPack, langCode, fromVersion));
    }

    public Flux<LangPackLanguage> getLanguages(String langPack) {
        return client.sendAwait(ImmutableGetLanguages.of(langPack))
                .flatMapIterable(Function.identity());
    }

    public Mono<LangPackLanguage> getLanguage(String langPack, String langCode) {
        return client.sendAwait(ImmutableGetLanguage.of(langPack, langCode));
    }
}
