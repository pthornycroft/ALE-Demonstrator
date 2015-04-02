package com.arubanetworks.aledemonstrator;

import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.util.Log;

public class CustomVerifier implements HostnameVerifier {
	static String TAG = "CustomVerifier";
	
	@Override
	public boolean verify(String hostname, SSLSession session) {
        return true;
	}
	
	void trustAllHosts() {
    	// Create a trust manager that does not validate certificate chains
		
        TrustManager[] trustManagers = new TrustManager[] { new X509TrustManager() {
        	public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        		return new java.security.cert.X509Certificate[] {};
        	}

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType)
					throws java.security.cert.CertificateException {	
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType)
					throws java.security.cert.CertificateException {
				MainActivity.certData = chain[0].getIssuerDN().toString();
		    }
			
        } };

        // Install the all-trusting trust manager
        try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustManagers, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
                Log.e(TAG, "exception in ssl context "+e);
        }

      }

}
