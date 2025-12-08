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
import walkingkooka.convert.ConverterContexts;
import walkingkooka.datetime.HasNow;
import walkingkooka.environment.EnvironmentContext;
import walkingkooka.environment.EnvironmentContexts;
import walkingkooka.locale.LocaleContexts;
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
import walkingkooka.plugin.ProviderContext;
import walkingkooka.plugin.ProviderContexts;
import walkingkooka.plugin.store.PluginStores;
import walkingkooka.predicate.Predicates;
import walkingkooka.spreadsheet.SpreadsheetStrings;
import walkingkooka.spreadsheet.compare.provider.SpreadsheetComparatorProviders;
import walkingkooka.spreadsheet.convert.provider.SpreadsheetConvertersConverterProviders;
import walkingkooka.spreadsheet.engine.SpreadsheetEngineContexts;
import walkingkooka.spreadsheet.engine.SpreadsheetMetadataMode;
import walkingkooka.spreadsheet.environment.SpreadsheetEnvironmentContext;
import walkingkooka.spreadsheet.environment.SpreadsheetEnvironmentContexts;
import walkingkooka.spreadsheet.export.provider.SpreadsheetExporterProviders;
import walkingkooka.spreadsheet.expression.function.provider.SpreadsheetExpressionFunctionProviders;
import walkingkooka.spreadsheet.format.provider.SpreadsheetFormatterProvider;
import walkingkooka.spreadsheet.format.provider.SpreadsheetFormatterProviders;
import walkingkooka.spreadsheet.importer.provider.SpreadsheetImporterProviders;
import walkingkooka.spreadsheet.meta.SpreadsheetMetadata;
import walkingkooka.spreadsheet.meta.SpreadsheetMetadataContexts;
import walkingkooka.spreadsheet.meta.SpreadsheetMetadataPropertyName;
import walkingkooka.spreadsheet.meta.store.SpreadsheetMetadataStore;
import walkingkooka.spreadsheet.meta.store.SpreadsheetMetadataStores;
import walkingkooka.spreadsheet.parser.provider.SpreadsheetParserProvider;
import walkingkooka.spreadsheet.parser.provider.SpreadsheetParserProviders;
import walkingkooka.spreadsheet.provider.SpreadsheetProvider;
import walkingkooka.spreadsheet.provider.SpreadsheetProviders;
import walkingkooka.spreadsheet.server.SpreadsheetHttpServer;
import walkingkooka.spreadsheet.server.SpreadsheetServerContexts;
import walkingkooka.spreadsheet.server.SpreadsheetServerStartup;
import walkingkooka.spreadsheet.store.repo.SpreadsheetStoreRepositories;
import walkingkooka.terminal.TerminalContexts;
import walkingkooka.terminal.server.TerminalServerContexts;
import walkingkooka.text.CharSequences;
import walkingkooka.text.Indentation;
import walkingkooka.text.LineEnding;
import walkingkooka.tree.expression.ExpressionNumberKind;
import walkingkooka.tree.json.marshall.JsonNodeMarshallContexts;
import walkingkooka.tree.json.marshall.JsonNodeMarshallUnmarshallContexts;
import walkingkooka.tree.json.marshall.JsonNodeUnmarshallContexts;
import walkingkooka.validation.form.provider.FormHandlerProviders;
import walkingkooka.validation.provider.ValidatorProviders;

import java.math.MathContext;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

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
        SpreadsheetServerStartup.init();;

        final LineEnding lineEnding = LineEnding.NL;
        final HasNow now = LocalDateTime::now;
        final Locale locale = Locale.forLanguageTag("en-AU");
        final EmailAddress user = EmailAddress.parse("user@example.com");
        final SpreadsheetMetadataStore metadataStore = SpreadsheetMetadataStores.treeMap();

        final SpreadsheetHttpServer server = SpreadsheetHttpServer.with(
            MediaTypeDetectors.fake(),
            fileServer(),
            browserHttpServer(worker),
            (u) -> SpreadsheetServerContexts.basic(
                (id) -> SpreadsheetStoreRepositories.treeMap(metadataStore),
                systemSpreadsheetProvider(),
                (c) -> SpreadsheetEngineContexts.basic(
                    SpreadsheetMetadataMode.FORMULA,
                    c,
                    TerminalContexts.fake()
                ),
                SpreadsheetEnvironmentContexts.basic(
                    EnvironmentContexts.map(
                        EnvironmentContexts.empty(
                            lineEnding,
                            locale,
                            now,
                            Optional.of(user)
                        )
                    ).setEnvironmentValue(
                        SpreadsheetEnvironmentContext.SERVER_URL,
                        Url.parseAbsolute("https://example.com")
                    )
                ),
                LocaleContexts.jre(locale),
                SpreadsheetMetadataContexts.basic(
                    (uu, l) -> SpreadsheetMetadata.EMPTY,
                    metadataStore
                ),
                HateosResourceHandlerContexts.basic(
                    Indentation.SPACES2,
                    lineEnding,
                    JsonNodeMarshallUnmarshallContexts.basic(
                        JsonNodeMarshallContexts.basic(),
                        JsonNodeUnmarshallContexts.basic(
                            ExpressionNumberKind.DEFAULT,
                            MathContext.DECIMAL32
                        )
                    )
                ),
                ProviderContexts.basic(
                    ConverterContexts.fake(),
                    EnvironmentContexts.empty(
                        lineEnding,
                        locale,
                        now,
                        Optional.of(user)
                    ),
                    PluginStores.treeMap()
                ),
                TerminalServerContexts.fake()
            ),
            (r) -> EnvironmentContext.ANONYMOUS
        );
        server.start();
    }

    private static SpreadsheetProvider systemSpreadsheetProvider() {
        final SpreadsheetFormatterProvider spreadsheetFormatterProvider = SpreadsheetFormatterProviders.spreadsheetFormatters();
        final SpreadsheetParserProvider spreadsheetParserProvider = SpreadsheetParserProviders.spreadsheetParsePattern(
            spreadsheetFormatterProvider
        );

        return SpreadsheetProviders.basic(
            SpreadsheetConvertersConverterProviders.spreadsheetConverters(
                (ProviderContext p) -> SpreadsheetMetadata.EMPTY.set(
                    SpreadsheetMetadataPropertyName.LOCALE,
                    Locale.forLanguageTag("EN-AU")
                ).dateTimeConverter(
                    spreadsheetFormatterProvider,
                    spreadsheetParserProvider,
                    p
                )
            ), // converterProvider
            SpreadsheetExpressionFunctionProviders.expressionFunctionProvider(SpreadsheetStrings.CASE_SENSITIVITY),
            SpreadsheetComparatorProviders.spreadsheetComparators(),
            SpreadsheetExporterProviders.spreadsheetExport(),
            spreadsheetFormatterProvider,
            FormHandlerProviders.validation(),
            SpreadsheetImporterProviders.spreadsheetImport(),
            spreadsheetParserProvider,
            ValidatorProviders.validators()
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
