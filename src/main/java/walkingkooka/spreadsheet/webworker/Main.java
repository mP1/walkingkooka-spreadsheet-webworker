/*
 * Copyright 2020 Miroslav Pokorny (github.com/mP1)
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
 *
 */

package walkingkooka.spreadsheet.webworker;

import com.google.gwt.core.client.EntryPoint;
import elemental2.dom.MessagePort;
import elemental2.dom.WorkerGlobalScope;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.base.Js;
import walkingkooka.Either;
import walkingkooka.collect.map.Maps;
import walkingkooka.convert.ConverterContexts;
import walkingkooka.convert.provider.ConverterProviders;
import walkingkooka.datetime.HasNow;
import walkingkooka.environment.EnvironmentContexts;
import walkingkooka.net.Url;
import walkingkooka.net.UrlParameterName;
import walkingkooka.net.UrlPath;
import walkingkooka.net.UrlQueryString;
import walkingkooka.net.email.EmailAddress;
import walkingkooka.net.header.MediaTypeDetectors;
import walkingkooka.net.http.HttpStatus;
import walkingkooka.net.http.HttpStatusCode;
import walkingkooka.net.http.server.HttpHandler;
import walkingkooka.net.http.server.HttpServer;
import walkingkooka.net.http.server.WebFile;
import walkingkooka.net.http.server.browser.BrowserHttpServers;
import walkingkooka.net.http.server.hateos.HateosResourceHandlerContexts;
import walkingkooka.plugin.ProviderContexts;
import walkingkooka.plugin.store.PluginStores;
import walkingkooka.predicate.Predicates;
import walkingkooka.spreadsheet.SpreadsheetExpressionFunctionNames;
import walkingkooka.spreadsheet.SpreadsheetId;
import walkingkooka.spreadsheet.compare.SpreadsheetComparatorProviders;
import walkingkooka.spreadsheet.convert.SpreadsheetConvertersConverterProviders;
import walkingkooka.spreadsheet.export.SpreadsheetExporterProviders;
import walkingkooka.spreadsheet.expression.function.SpreadsheetExpressionFunctionProviders;
import walkingkooka.spreadsheet.format.SpreadsheetFormatterProvider;
import walkingkooka.spreadsheet.format.SpreadsheetFormatterProviders;
import walkingkooka.spreadsheet.importer.SpreadsheetImporterProviders;
import walkingkooka.spreadsheet.meta.SpreadsheetMetadata;
import walkingkooka.spreadsheet.meta.SpreadsheetMetadataPropertyName;
import walkingkooka.spreadsheet.meta.store.SpreadsheetMetadataStore;
import walkingkooka.spreadsheet.meta.store.SpreadsheetMetadataStores;
import walkingkooka.spreadsheet.parser.SpreadsheetParserProvider;
import walkingkooka.spreadsheet.parser.SpreadsheetParserProviders;
import walkingkooka.spreadsheet.provider.SpreadsheetProvider;
import walkingkooka.spreadsheet.provider.SpreadsheetProviders;
import walkingkooka.spreadsheet.security.store.SpreadsheetGroupStores;
import walkingkooka.spreadsheet.security.store.SpreadsheetUserStores;
import walkingkooka.spreadsheet.server.SpreadsheetHttpServer;
import walkingkooka.spreadsheet.store.SpreadsheetCellRangeStores;
import walkingkooka.spreadsheet.store.SpreadsheetCellReferencesStores;
import walkingkooka.spreadsheet.store.SpreadsheetCellStores;
import walkingkooka.spreadsheet.store.SpreadsheetColumnStores;
import walkingkooka.spreadsheet.store.SpreadsheetExpressionReferenceStores;
import walkingkooka.spreadsheet.store.SpreadsheetLabelStores;
import walkingkooka.spreadsheet.store.SpreadsheetRowStores;
import walkingkooka.spreadsheet.store.repo.SpreadsheetStoreRepositories;
import walkingkooka.spreadsheet.store.repo.SpreadsheetStoreRepository;
import walkingkooka.storage.StorageStores;
import walkingkooka.text.CaseSensitivity;
import walkingkooka.text.CharSequences;
import walkingkooka.text.Indentation;
import walkingkooka.text.LineEnding;
import walkingkooka.tree.expression.ExpressionNumberKind;
import walkingkooka.tree.expression.function.provider.ExpressionFunctionProviders;
import walkingkooka.tree.json.marshall.JsonNodeMarshallContexts;
import walkingkooka.tree.json.marshall.JsonNodeMarshallUnmarshallContexts;
import walkingkooka.tree.json.marshall.JsonNodeUnmarshallContexts;
import walkingkooka.validation.provider.ValidatorProviders;

import java.math.MathContext;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Starts the application as a webworker including waiting for messages from the main window.
 */
public final class Main implements EntryPoint {
    @Override
    public void onModuleLoad() {
        startServer(self());
    }

    /**
     * Elemental appears to be missing a getter or field that returns a {@link WorkerGlobalScope}
     */
    @JsMethod(name = "self", namespace = JsPackage.GLOBAL)
    private static native WorkerGlobalScope self();

    // VisibleForTesting
    static void startServer(final WorkerGlobalScope worker) {
        final HasNow now = LocalDateTime::now;
        final SpreadsheetMetadataStore metadataStore = SpreadsheetMetadataStores.treeMap(
            SpreadsheetMetadata.EMPTY.set(
                SpreadsheetMetadataPropertyName.LOCALE,
                Locale.forLanguageTag("EN-AU")
            ),
            now
        );

        final SpreadsheetHttpServer server = SpreadsheetHttpServer.with(
            Url.parseAbsolute("http://localhost"),
            Indentation.SPACES2,
            LineEnding.SYSTEM,
            MediaTypeDetectors.fake(),
            systemSpreadsheetProvider(),
            ProviderContexts.basic(
                ConverterContexts.fake(),
                EnvironmentContexts.empty(
                    now,
                    Optional.of(
                        EmailAddress.parse("user123@example.com")
                    )
                ),
                PluginStores.treeMap()
            ),
            metadataStore,
            HateosResourceHandlerContexts.basic(
                JsonNodeMarshallUnmarshallContexts.basic(
                    JsonNodeMarshallContexts.basic(),
                    JsonNodeUnmarshallContexts.basic(
                        ExpressionNumberKind.DEFAULT,
                        MathContext.DECIMAL32
                    )
                )
            ),
            (id) -> SpreadsheetProviders.basic(
                ConverterProviders.converters(),
                ExpressionFunctionProviders.empty(
                    SpreadsheetExpressionFunctionNames.CASE_SENSITIVITY
                ),
                SpreadsheetComparatorProviders.spreadsheetComparators(),
                SpreadsheetExporterProviders.spreadsheetExport(),
                SpreadsheetFormatterProviders.spreadsheetFormatPattern(),
                SpreadsheetImporterProviders.spreadsheetImport(),
                SpreadsheetParserProviders.spreadsheetParsePattern(
                    SpreadsheetFormatterProviders.spreadsheetFormatPattern()
                ),
                ValidatorProviders.validators()
            ),
            spreadsheetIdToRepository(Maps.sorted(), storeRepositorySupplier(metadataStore)),
            fileServer(),
            browserHttpServer(worker)
        );
        server.start();
    }

    private static SpreadsheetProvider systemSpreadsheetProvider() {
        final SpreadsheetFormatterProvider spreadsheetFormatterProvider = SpreadsheetFormatterProviders.spreadsheetFormatPattern();
        final SpreadsheetParserProvider spreadsheetParserProvider = SpreadsheetParserProviders.spreadsheetParsePattern(
            spreadsheetFormatterProvider
        );

        return SpreadsheetProviders.basic(
            SpreadsheetConvertersConverterProviders.spreadsheetConverters(
                SpreadsheetMetadata.EMPTY.set(
                    SpreadsheetMetadataPropertyName.LOCALE,
                    Locale.forLanguageTag("EN-AU")
                ),
                spreadsheetFormatterProvider,
                spreadsheetParserProvider
            ), // converterProvider
            SpreadsheetExpressionFunctionProviders.expressionFunctionProvider(CaseSensitivity.INSENSITIVE),
            SpreadsheetComparatorProviders.spreadsheetComparators(),
            SpreadsheetExporterProviders.spreadsheetExport(),
            spreadsheetFormatterProvider,
            SpreadsheetImporterProviders.spreadsheetImport(),
            spreadsheetParserProvider,
            ValidatorProviders.validators()
        );
    }

    /**
     * Retrieves from the cache or lazily creates a {@link SpreadsheetStoreRepository} for the given {@link SpreadsheetId}.
     */
    private static Function<SpreadsheetId, SpreadsheetStoreRepository> spreadsheetIdToRepository(final Map<SpreadsheetId, SpreadsheetStoreRepository> idToRepository,
                                                                                                 final Supplier<SpreadsheetStoreRepository> repositoryFactory) {
        return (id) -> {
            SpreadsheetStoreRepository repository = idToRepository.get(id);
            if (null == repository) {
                repository = repositoryFactory.get();
                idToRepository.put(id, repository); // TODO add locks etc.
            }
            return repository;
        };
    }

    /**
     * Creates a new {@link SpreadsheetStoreRepository} on demand
     */
    private static Supplier<SpreadsheetStoreRepository> storeRepositorySupplier(final SpreadsheetMetadataStore metadataStore) {
        return () -> SpreadsheetStoreRepositories.basic(
            SpreadsheetCellStores.treeMap(),
            SpreadsheetCellReferencesStores.treeMap(),
            SpreadsheetColumnStores.treeMap(),
            SpreadsheetGroupStores.treeMap(),
            SpreadsheetLabelStores.treeMap(),
            SpreadsheetExpressionReferenceStores.treeMap(),
            metadataStore,
            SpreadsheetCellRangeStores.treeMap(),
            SpreadsheetCellRangeStores.treeMap(),
            SpreadsheetRowStores.treeMap(),
            StorageStores.empty(),
            SpreadsheetUserStores.treeMap()
        );
    }

    /**
     * A fileserver that always returns {@link HttpStatusCode#NOT_FOUND} for any requested file.
     */
    private static Function<UrlPath, Either<WebFile, HttpStatus>> fileServer() {
        return (p) -> Either.right(HttpStatusCode.NOT_FOUND.status());
    }

    /**
     * Creates a {@link BrowserHttpServers}.
     */
    private static Function<HttpHandler, HttpServer> browserHttpServer(final WorkerGlobalScope worker) {
        final MessagePort port = Js.uncheckedCast(worker);

        return (processor) -> BrowserHttpServers.messagePort(
            processor,
            port,
            Predicates.always(),
            targetOrigin(worker.getLocation().getSearch())); // TODO accept parameter.
    }

    /**
     * Retrieves the targetOrigin query parameter, failing if it is absent.
     */
    private static String targetOrigin(final String queryString) {
        final UrlQueryString urlQueryString = UrlQueryString.parse(queryString);
        final UrlParameterName parameter = UrlParameterName.with("targetOrigin");
        return urlQueryString.parameter(parameter).orElseThrow(() -> new IllegalArgumentException("Missing query parameter " + CharSequences.quoteAndEscape(parameter.value()) + " from " + queryString));
    }
}
