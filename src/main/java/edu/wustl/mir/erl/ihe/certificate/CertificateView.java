package edu.wustl.mir.erl.ihe.certificate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.log4j.Logger;

import edu.wustl.mir.erl.ihe.certificate.view.ApplicationBean;

public class CertificateView implements Serializable {

	private static final long serialVersionUID = 1L;
	private static Logger log = ApplicationBean.getLog();
	
	private static Runtime runtime = Runtime.getRuntime();
	private static final String nl = System.getProperty("line.separator");
	
	
	public String getCertificateView(CertType type, String pfn, String pw) 
		throws Exception {
		results = "";
		Map<String, String> map = new HashMap<String, String>();
		map.put("pfn", pfn);
		map.put("pw", pw);
		StrSubstitutor subst = new StrSubstitutor(map);
		String cmd = subst.replace(type.getCmd());
		runCommand(cmd);
		return results;
	} // EO getCertificateView method
	
	private String results;
	private String runCommand(String cmd) throws Exception {
		String line;
		String[] command = {"/bin/sh", "-c", cmd};
		String RTN = "CertificateView.runCommand";
		log.trace(RTN + " called with: " + cmd);
		Process process = runtime.exec(command);
		int exitValue = process.waitFor();
		if (exitValue == 0) {
			BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));
			while ((line = out.readLine()) != null) results = results + line + nl;
			return results;
		}
		log.warn("Error running command: " + cmd);
		StringBuilder em = new StringBuilder("Error: ");
        BufferedReader buff = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        while ((line = buff.readLine()) != null) {
        	log.warn(line);
        	em.append(line).append(" ");
        }
		throw new Exception(em.toString());
	}

}
