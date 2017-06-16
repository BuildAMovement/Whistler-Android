/*
 * Copyright (c) 2016 CommonsWare, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.guardianproject.netcipher.client;

import android.content.Intent;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.TrustManager;

public interface StrongBuilder<T extends StrongBuilder, C> {
  /**
   * Callback to get a connection handed to you for use,
   * already set up for NetCipher.
   *
   * @param <C> the type of connection created by this builder
   */
  interface Callback<C> {
    /**
     * Called when the NetCipher-enhanced connection is ready
     * for use.
     *
     * @param connection the connection
     */
    void onConnected(C connection);

    /**
     * Called if we tried to connect through to Orbot but failed
     * for some reason
     *
     * @param e the reason
     */
    void onConnectionException(Exception e);

    /**
     * Called if our attempt to get a status from Orbot failed
     * after a defined period of time. See statusTimeout() on
     * OrbotInitializer.
     */
    void onTimeout();

    /**
     * Called if you requested validation that we are connecting
     * through Tor, and while we were able to connect to Orbot, that
     * validation failed.
     */
    void onInvalid();
  }

  /**
   * Call this to configure the Tor proxy from the results
   * returned by Orbot, using the best available proxy
   * (SOCKS if possible, else HTTP)
   *
   * @return the builder
   */
  T withBestProxy();

  /**
   * @return true if this builder supports HTTP proxies, false
   * otherwise
   */
  boolean supportsHttpProxy();

  /**
   * Call this to configure the Tor proxy from the results
   * returned by Orbot, using the HTTP proxy.
   *
   * @return the builder
   */
   T withHttpProxy();

  /**
   * @return true if this builder supports SOCKS proxies, false
   * otherwise
   */
   boolean supportsSocksProxy();

  /**
   * Call this to configure the Tor proxy from the results
   * returned by Orbot, using the SOCKS proxy.
   *
   * @return the builder
   */
  T withSocksProxy();

  /**
   * Applies your own custom TrustManagers, such as for
   * replacing the stock keystore support with a custom
   * keystore.
   *
   * @param trustManagers the TrustManagers to use
   * @return the builder
   */
  T withTrustManagers(TrustManager[] trustManagers)
    throws NoSuchAlgorithmException, KeyManagementException;

  /**
   * Call this if you want a weaker set of supported ciphers,
   * because you are running into compatibility problems with
   * some server due to a cipher mismatch. The better solution
   * is to fix the server.
   *
   * @return the builder
   */
  T withWeakCiphers();

  /**
   * Call this if you want the builder to confirm that we are
   * communicating over Tor, by reaching out to a Tor test
   * server and confirming our connection status. By default,
   * this is skipped. Adding this check adds security, but it
   * has the chance of false negatives (e.g., we cannot reach
   * that Tor server for some reason).
   *
   * @return the builder
   */
  T withTorValidation();

  /**
   * Builds a connection, applying the configuration already
   * specified in the builder.
   *
   * @param status status Intent from OrbotInitializer
   * @return the connection
   * @throws IOException
   */
  C build(Intent status) throws Exception;

  /**
   * Asynchronous version of build(), one that uses OrbotInitializer
   * internally to get the status and checks the validity of the Tor
   * connection (if requested). Note that your callback methods may
   * be invoked on any thread; do not assume that they will be called
   * on any particular thread.
   *
   * @param callback Callback to get a connection handed to you
   *                 for use, already set up for NetCipher
   */
  void build(Callback<C> callback);
}
