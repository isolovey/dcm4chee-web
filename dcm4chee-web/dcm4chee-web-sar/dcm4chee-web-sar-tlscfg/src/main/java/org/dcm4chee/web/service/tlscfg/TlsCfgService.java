/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gwi-ag.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
package org.dcm4chee.web.service.tlscfg;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.dcm4che2.audit.util.SSLUtils;
import org.dcm4che2.net.Device;
import org.dcm4che2.net.NetworkConnection;
import org.dcm4che2.util.StringUtils;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author franz.willer@gmail.com
 * @version $Revision$ $Date$
 * @since Feb 11, 2010
 */
public class TlsCfgService extends ServiceMBeanSupport {

    private String keyStoreURL;
    private String trustStoreURL;
    private char[] keyStorePassword;
    private char[] trustStorePassword;
    private char[] keyPassword;
    private String keyStoreType;
    private String trustStoreType;
    private String[] tlsProtocol;
    private boolean needClientAuth;
    
    protected static final String NONE ="NONE";
    
    public TlsCfgService() {
    }

    public String getKeyStoreURL() {
        return keyStoreURL;
    }

    public void setKeyStoreURL(String keyStoreURL) {
        this.keyStoreURL = keyStoreURL;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = none2null(keyStorePassword);
    }

    public String getTrustStoreURL() {
        return trustStoreURL;
    }

    public void setTrustStoreURL(String trustStoreURL) {
        this.trustStoreURL = trustStoreURL;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = none2null(trustStorePassword);
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = none2null(keyPassword);
    }
    public String getKeyPassword() {
        return keyPassword == null ? NONE : "******";
    }

    public String getTlsProtocol() {
        return StringUtils.join(tlsProtocol, ',');
    }

    public void setTlsProtocol(String tlsProtocol) {
        this.tlsProtocol = StringUtils.split(tlsProtocol, ',');
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String type) {
        this.keyStoreType = type;
    }

    public String getTrustStoreType() {
        return trustStoreType;
    }

    public void setTrustStoreType(String type) {
        this.trustStoreType = type;
    }

    public boolean isNeedClientAuth() {
        return needClientAuth;
    }

    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }

    private char[] none2null(String s) {
        return NONE.equals(s) ? null : s.toCharArray();
    }
        
    public void initTLS(NetworkConnection remoteConn, Device device, String[] ciphers) throws IOException, GeneralSecurityException {
        if (ciphers!= null && ciphers.length > 0) {
            NetworkConnection localConn = device.getNetworkConnection()[0];
            remoteConn.setTlsCipherSuite(ciphers);
            localConn.setTlsCipherSuite(ciphers);
            localConn.setTlsProtocol(tlsProtocol);
            localConn.setTlsNeedClientAuth(needClientAuth);
            KeyStore keyStore = loadKeyStore(keyStoreURL, keyStorePassword, keyStoreType);
            KeyStore trustStore = loadKeyStore(trustStoreURL, trustStorePassword, trustStoreType);
            device.initTLS(keyStore, keyPassword == null ? keyStorePassword : keyPassword, trustStore);
            device.getNetworkConnection();
        }
    }
    
    private KeyStore loadKeyStore(String keyStoreURL, char[] password, String type) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        InputStream in;
        try {
            in = new URL(keyStoreURL).openStream();
        } catch (MalformedURLException e) {
            in = new FileInputStream(keyStoreURL);
        }
        KeyStore key = KeyStore.getInstance(type);
        try {
            key.load(in, password);
        } finally {
            in.close();
        }
        return key;
    }
 
    public Socket initSocket(String host, int port, String[] ciphers, String bindAddress, int connectTimeout) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException, UnrecoverableKeyException {
        Socket s = ciphers != null && ciphers.length > 0 ? createTLSSocket(ciphers) : new Socket();
        InetAddress addr = bindAddress == null ? null : InetAddress.getByName(bindAddress);
        InetSocketAddress bindPoint = new InetSocketAddress(
                (addr != null && addr.isLoopbackAddress()) ? null : addr, 0);
        InetSocketAddress endpoint = new InetSocketAddress(InetAddress.getByName(host), port);
        s.bind(bindPoint);
        s.connect(endpoint, connectTimeout);
        return s;
    }
    
    protected Socket createTLSSocket(String[] ciphers) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        KeyStore keyStore = loadKeyStore(keyStoreURL, keyStorePassword, keyStoreType);
        KeyStore trustStore = loadKeyStore(trustStoreURL, trustStorePassword, trustStoreType);
        SSLContext ctx = SSLUtils.getSSLContext(keyStore, keyPassword == null ? keyStorePassword : keyPassword, trustStore, null);
        if (ctx == null)
            throw new IllegalStateException("TLS Context not initialized!");
        SSLSocketFactory sf = ctx.getSocketFactory();
        SSLSocket s = (SSLSocket) sf.createSocket();
        s.setEnabledProtocols(tlsProtocol);
        s.setEnabledCipherSuites(ciphers);
        return s;
    }
   
}

