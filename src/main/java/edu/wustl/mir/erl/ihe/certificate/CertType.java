package edu.wustl.mir.erl.ihe.certificate;

import javax.faces.model.SelectItem;

public enum CertType {
	CERT  (false, "X509 certificate PEM format (cert.pem)",     "openssl x509 -in ${pfn} -noout -text"), 
	PKCS12(true,  "PKCS #12 certificate PEM format (cert.p12)", "openssl pkcs12 -info -nodes -in ${pfn} -passin pass:${pw}"), 
	JKS   (true,  "Java Key Storefile (keystore.jks)",          "keytool -list -v -keystore ${pfn} -storepass ${pw}");

	private final boolean pwRequired;
	private final String longName;
	private final String cmd;

	CertType(boolean pwr, String l, String c) {
		pwRequired = pwr;
		longName = l;
		cmd = c;
	}

	public boolean isPwRequired() {
		return pwRequired;
	}

	public String getLongName() {
		return longName;
	}
	
	public String getCmd() {
		return cmd;
	}

	public static SelectItem[] getItems() {
		CertType[] types = CertType.values();
		SelectItem[] items = new SelectItem[types.length];
		for (int i = 0; i < types.length; i++) {
			items[i] = new SelectItem(types[i].name(), types[i].getLongName());
		}
		return items;
	}
} // EO enum CertType
