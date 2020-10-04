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

import elemental2.core.JsArray;
import elemental2.core.Transferable;
import elemental2.dom.Event;
import elemental2.dom.EventListener;
import elemental2.dom.MessagePort;
import elemental2.dom.RequestInit;
import elemental2.dom.Response;
import elemental2.dom.WorkerGlobalScope;
import elemental2.dom.WorkerLocation;
import elemental2.dom.WorkerNavigator;
import elemental2.dom.WorkerPerformance;
import elemental2.promise.Promise;
import org.junit.jupiter.api.Test;

public final class MainTest {

    @Test
    public void testStart() {
        // want to verify starts without exceptions, real webworker start required.
        Main.startServer(new TestWorkerGlobalScope() {

        });
    }

    static class TestWorkerGlobalScope extends MessagePort implements WorkerGlobalScope {
        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Promise<Response> fetch(final FetchInputUnionType input, final RequestInit init) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Promise<Response> fetch(final FetchInputUnionType input) {
            return null;
        }

        @Override
        public WorkerLocation getLocation() {
            return new WorkerLocation() {
                @Override
                public String getHash() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getHost() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getHostname() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getHref() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getOrigin() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getPathname() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getPort() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getProtocol() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getSearch() {
                    return "targetOrigin=*";
                }

                @Override
                public void setHash(final String hash) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void setHost(final String host) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void setHostname(final String hostname) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void setHref(final String href) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void setOrigin(final String origin) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void setPathname(final String pathname) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void setPort(final String port) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void setProtocol(final String protocol) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void setSearch(final String search) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public WorkerNavigator getNavigator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public OnerrorFn getOnerror() {
            throw new UnsupportedOperationException();
        }

        @Override
        public OnofflineFn getOnoffline() {
            throw new UnsupportedOperationException();
        }

        @Override
        public OnonlineFn getOnonline() {
            throw new UnsupportedOperationException();
        }

        @Override
        public WorkerPerformance getPerformance() {
            throw new UnsupportedOperationException();
        }

        @Override
        public WorkerGlobalScope getSelf() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void postMessage(final Object message, final JsArray<Transferable> transfer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void postMessage(final Object message) {

        }

        @Override
        public void setLocation(final WorkerLocation location) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setNavigator(final WorkerNavigator navigator) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOnerror(final OnerrorFn onerror) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOnoffline(final OnofflineFn onoffline) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOnonline(final OnonlineFn ononline) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPerformance(WorkerPerformance performance) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSelf(final WorkerGlobalScope self) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addEventListener(final String type,
                                     final EventListener listener,
                                     final boolean a) {

        }

        @Override
        public void addEventListener(final String type,
                                     final EventListener listener,
                                     final AddEventListenerOptionsUnionType options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addEventListener(final String type,
                                     final EventListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean dispatchEvent(final Event evt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeEventListener(final String type,
                                        final EventListener listener,
                                        final RemoveEventListenerOptionsUnionType options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeEventListener(final String type,
                                        final EventListener listener) {
            throw new UnsupportedOperationException();
        }
    }
}
