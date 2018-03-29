package edu.wustl.mir.erl.ihe.certificate.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.wustl.mir.erl.ihe.certificate.CertificateFactory;
import edu.wustl.mir.erl.ihe.util.Util;


@ManagedBean(eager = true)
@ApplicationScoped
public class ApplicationBean implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private static String runDirectory = "runDirectory";
	private static String viewDirectory;
	private static String archiveDirectory = "/opt/certificate/archive";
	private static Logger log = null;
	private static Properties properties = null;
	private static String name = "Test Certificate Generator / Viewer";
	private static SelectItem[] caNames;
	
	public ApplicationBean() {
		try {
			//---------- Get runDirectory path
			String runDirectory = this.getClass().getClassLoader().getResource("runDirectory").getPath();
			viewDirectory = runDirectory + File.separator + "view";
			//--------- initialize logging
			Util.setLogDirectoryPath(Paths.get(runDirectory, "logs"));
			PropertyConfigurator.configure(runDirectory + File.separator + 
					"log4j.properties");
			log = Logger.getLogger("syslog");
			log.info("instatiating applicationBean");
			//--------- load properties
			properties = new Properties();
			FileInputStream in = new FileInputStream(runDirectory + 
					File.separator +"Certificate.properties");
			properties.load(in);
			in.close();
			initializeNextValue();
			//--- initialize static fields in other classes
			CertificateFactory.setRunDir(runDirectory);
			CertificateFactory.setLog(log);
			CertificateFactory.setProperties(properties);
			CertificateFactory.setArchiveDir(archiveDirectory);
			CertificateFactory.setApplicationBean(this);
			SessionBean.setLog(log);
			SessionBean.setProperties(properties);
			//---- set up ca certificate selections from properties file
			String[] cans = properties.getProperty("CACertificate").split("\\s+",0);
			caNames = new SelectItem[cans.length];
			for (int i = 0; i < cans.length; i++) {
				caNames[i] = new SelectItem((Object) cans[i], cans[i]);
			}
		} catch (Exception e) {
			System.out.println("Could not instatiated ApplicationBean: " + 
				e.getMessage());
		}
	}

	public static Logger getLog() {
		return log;
	}

	public static void setLog(Logger log) {
		ApplicationBean.log = log;
	}

	public static Properties getProperties() {
		return properties;
	}

	public static void setProperties(Properties properties) {
		ApplicationBean.properties = properties;
	}

	public String getRunDirectory() {
		return runDirectory;
	}
	
	public String getName() { return name; }
	public void setName(String n) { name = n;}

	public SelectItem[] getCaNames() {
		return caNames;
	}

	public void setCaNames(SelectItem[] caNames) {
		ApplicationBean.caNames = caNames;
	}

	public void setRunDirectory(String runDirectory) {
		ApplicationBean.runDirectory = runDirectory;
	}

	public static String getViewDirectory() {
		return viewDirectory;
	}

	/*
	 * Maintain atomic counter. The next value to use is stored in file on disk
	 * and loaded at initialization. The synchronized nextValue method 
	 * increments the number and stores it in the file, returning the original
	 * value.
	 */
	
	private Integer nextValue;
	private File counterFile = null;
	
	public synchronized Integer getNextValue () {
		int returnValue = nextValue;
		nextValue++;
		try {
			FileWriter writer = new FileWriter(counterFile);
			writer.write(nextValue.toString());
			writer.close();
		} catch (IOException io) {
			log.warn("IO Error writing next value: [" + nextValue + 
					"] - " + io.getMessage());
		}
		return returnValue;
	}
	
	private void initializeNextValue() {
		//---------- get and validate archive directory (which contains file)
		archiveDirectory = properties.getProperty("archiveDirectory", archiveDirectory);
		File ad = new File(archiveDirectory);
		try {
			if (!ad.exists()) throw new Exception ("not found");
			if (!ad.isDirectory()) throw new Exception ("not directory");
			if (!ad.canRead()) throw new Exception ("not readable");
			if (!ad.canWrite()) throw new Exception("not writable");
		} catch (Exception e) {
			log.error("Archive Directory [" + archiveDirectory + 
				"] not valid: " + e.getMessage());
			System.exit(1);
		}
		//--------------- get and validate counter file
		String pfn = archiveDirectory + File.separator + "counter";
		counterFile = new File(pfn);
		try {
			if (!counterFile.exists()) throw new Exception ("not found");
			if (!counterFile.isFile()) throw new Exception ("not file");
			if (!counterFile.canRead()) throw new Exception ("not readable");
			if (!counterFile.canWrite()) throw new Exception("not writable");
		} catch (Exception e) {
			log.error("Counter File [" + pfn + "] not valid: " + 
				e.getMessage());
			System.exit(1);
		}
		//-------- load the current value
		try {
			nextValue = new Scanner(counterFile).nextInt();
		} catch (Exception e) {
			log.error("Failed to load next value: " + e.getMessage());
			System.exit(1);
		}
	} // EO initializeNextValue()
	
	
}  // EO ApplicationBean class







