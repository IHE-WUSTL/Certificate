package edu.wustl.mir.erl.ihe.certificate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.wustl.mir.erl.ihe.certificate.view.ApplicationBean;
import edu.wustl.mir.erl.ihe.certificate.view.SessionBean;

// TODO Does not accept pwds with special characters, arcane error message.
public class CertificateFactory implements Serializable {

	private static final long serialVersionUID = 1L;

	private static ApplicationBean applicationBean;

	public static void setApplicationBean(ApplicationBean a) {
		applicationBean = a;
	}

	private SessionBean sessionBean = null;

	private static Logger log = null;
	private static Properties properties;
	private static String runDir = "runDirectory";
	private static String archiveDir = null;
	private Path tmpDir = null;

	private Map<String, String> map = new HashMap<String, String>();
	private String[] CACertificateNames;

	private static final String ARCHIVE_DIRECTORY = "archiveDirectory";
	private static final String COUNTRY = "Country";
	private static final String STATE_PROVINCE = "StateProvince";
	private static final String CITY = "City";
	private static final String ORGANIZATION = "Organization";
	private static final String UNIT = "Unit";
	private static final String NAME = "Name";
	private static final String EMAIL = "Email";
	private static final String KEY_LENGTH = "KeyLength";
	private static final String CERTIFICATE_DAYS = "CertificateDays";
	private static final String DIR = "dir";
	private static final String CA_CERTIFICATE = "CACertificate";
	private static final String CA_CERTIFICATE_PFN = "CACertificatePfn";
	private static final String CA_CERTIFICATE_KEY_PFN = "CAkey";
	private static final String SERIAL_NUMBER = "serialNumber";
	private static final String CA_CERT_DIR = "CACertificate.directory";
	private static final String PASSWORD = "Password";
	private static final String CA_NAME = "CAName";
	private static final String ZIP_FILE_NAME = "cert.zip";

	private static final FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions
			.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx"));

	private static Runtime runtime = Runtime.getRuntime();

	private static final String nl = System.getProperty("line.separator");

	private static final String DN_FILE = "${" + COUNTRY + "}" + nl + "${"
			+ STATE_PROVINCE + "}" + nl + "${" + CITY + "}" + nl + "${"
			+ ORGANIZATION + "}" + nl + "${" + UNIT + "}" + nl + "${" + NAME
			+ "}" + nl + "${" + EMAIL + "}" + nl + "${" + PASSWORD + "}" + nl
			+ "IHE Connectathon" + nl;

	private static final String PW_FILE = "#*****************************************************"
			+ nl
			+ "# This is your password for cert.p12 and keystore.jks"
			+ nl
			+ "#*****************************************************"
			+ nl
			+ "${" + PASSWORD + "}" + nl;

	private static final String GENERATE_PRIVATE_KEY = "openssl genrsa -out key.pem ${"
			+ KEY_LENGTH + "}";

	private static final String GENERATE_CERTIFICATE_REQUEST = "openssl req -new -key key.pem -out csr <dn.txt";

	private static final String SIGN_CERTIFICATE = "openssl x509 -req -inform PEM -in csr -days ${"
			+ CERTIFICATE_DAYS
			+ "} -CA ${"
			+ CA_CERTIFICATE_PFN
			+ "} "
			+ "-CAkey ${"
			+ CA_CERTIFICATE_KEY_PFN
			+ "} "
			+ "-keyform PEM -set_serial ${"
			+ SERIAL_NUMBER
			+ "} "
			+ "-out cert.pem";

	private static final String GENERATE_PKCS12 = "openssl pkcs12 -export -in cert.pem -inkey key.pem -out cert.p12 "
			+ "-passout pass:${" + PASSWORD + "}";

	private static final String IMPORT_CA_CERTIFICATE = "keytool -import -trustcacerts -file cacert.${"
			+ CA_NAME
			+ "}.pem -keystore keystore.jks -storepass ${"
			+ PASSWORD
			+ "} -alias ${" + CA_NAME + "} < yes.txt";

	private static final String IMPORT_KEYSTORE = "keytool -importkeystore -srckeystore cert.p12 "
			+ "-destkeystore keystore.jks -srcstoretype PKCS12 "
			+ "-deststoreType JKS -srcstorepass ${"
			+ PASSWORD
			+ "} "
			+ "-deststorepass ${" + PASSWORD + "}";

	private static final String ARCHIVE_CERTIFICATE = "cp cert.pem ${"
			+ ARCHIVE_DIRECTORY + "}" + File.separator + "cert${"
			+ SERIAL_NUMBER + "}.pem";

	private static final String DELETE_TMP_DIRECTORY = "rm -R ${" + DIR + "}";

	/**
	 * Constructor sets up default values for certificate fields.
	 * 
	 * @param sBean
	 */
	public CertificateFactory(SessionBean sBean) {
		sessionBean = sBean;
		log.trace("CertificateFactory()");
		map.put(ARCHIVE_DIRECTORY, archiveDir);
		load(COUNTRY);
		load(STATE_PROVINCE);
		load(CITY);
		load(ORGANIZATION);
		load(UNIT);
		load(NAME);
		load(EMAIL);
		load(KEY_LENGTH);
		load(PASSWORD);
		CACertificateNames = properties.getProperty(CA_CERTIFICATE).split(
				"\\s+", 0);
		map.put(CA_CERTIFICATE, CACertificateNames[0]);
		setCertificateDays();

	} // EO CertificateFactory() constructor

	/*
	 * Calculates Certificate days from expiration date for certificates
	 * generated for this CA in properties. This works with UTC and ignores some
	 * fine points, so the expiration is "on or about".
	 */
	private void setCertificateDays() {
		String caName = map.get(CA_CERTIFICATE);
		String exp = "2014-10-31";
		try {
			exp = properties.getProperty(caName + ".expirationDate", exp);
			Date expDate = new SimpleDateFormat("yyyy-MM-dd").parse(exp);
			Integer days = (int) ((expDate.getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24));
			map.put(CERTIFICATE_DAYS, days.toString());
		} catch (ParseException pe) {
			log.error("Could not parse Epiration date in properties file: "
					+ exp);
			System.exit(1);
		}

	}

	private void load(String cd) {
		String str = properties.getProperty("default." + cd, "");
		map.put(cd, str);
	}

	private String context = "";
	private File workingDirectory;

	/**
	 * Generates a .zip file containing private key, signed certificate, and
	 * related files in .PEM, PKCS12, and java keystore format for downloading
	 * to participant.
	 * 
	 * @return String - pfn of .zip file, if successful.
	 * @throws Exception
	 *             on error.
	 */
	public String generateCertificate() throws Exception {
		String caName = map.get(CA_CERTIFICATE);
		String serialNumber = null;
		deleteTmpDir();
		String zipFilePfn = null;
		String RTN = "CertificateFactory.generateCertificate ";
		log.trace("called " + RTN);
		String cmd;
		try {
			context = "create temp directory";
			tmpDir = Files.createTempDirectory(
					Paths.get(runDir + File.separator + "tmp"), "certAPI",
					attrs);
			workingDirectory = tmpDir.toFile();
			map.put(DIR, tmpDir.toString() + File.separator);

			context = "copy in boiler plate";
			String b = properties.getProperty(caName + ".boilerPlateDirectory",
					"boiler");
			Path boiler = Paths.get(runDir, b);
			File blr = boiler.toFile();
			if (!blr.isDirectory())
				throw new Exception(boiler.toString() + " is not a directory");
			if (!blr.canRead())
				throw new Exception(boiler.toString() + " is not readable");
			DirectoryStream<Path> bdir = Files.newDirectoryStream(boiler);
			for (Path sp : bdir) {
				if (sp.toFile().isFile()) {
					Path dp = tmpDir.resolve(sp.getFileName());
					Files.copy(sp, dp, StandardCopyOption.REPLACE_EXISTING,
							StandardCopyOption.COPY_ATTRIBUTES);
				}
			}
			bdir.close();

			context = "write distinct name parameters file";
			Path dn = tmpDir.resolve("dn.txt");
			String dnStr = StrSubstitutor.replace(DN_FILE, map);
			log.trace("dn parameters:" + nl + dnStr);
			Files.write(dn, dnStr.getBytes());

			context = "write password file";
			Path pwf = tmpDir.resolve("password.txt");
			String pwfStr = StrSubstitutor.replace(PW_FILE, map);
			log.trace("password: " + map.get(PASSWORD));
			Files.write(pwf, pwfStr.getBytes());

			context = "generate private key pem";
			cmd = StrSubstitutor.replace(GENERATE_PRIVATE_KEY, map);
			runCommand(cmd);

			context = "generate certificate request";
			cmd = StrSubstitutor.replace(GENERATE_CERTIFICATE_REQUEST, map);
			runCommand(cmd);

			// ---------- We get serial number here, to minimize unused ones
			serialNumber = applicationBean.getNextValue().toString();
			map.put(SERIAL_NUMBER, serialNumber);

			context = "generate signed certificate";
			getCAPfns();
			cmd = StrSubstitutor.replace(SIGN_CERTIFICATE, map);
			runCommand(cmd);

			cmd = StrSubstitutor.replace(ARCHIVE_CERTIFICATE, map);
			runCommand(cmd);

			context = "copy in ca public key";
			/*
			 * For each cert name, copy cacert(name).pem from the cacert
			 * directory to zip directory.
			 */
			Path caDir = Paths.get(runDir,
					properties.getProperty(CA_CERT_DIR, "ca"));
			File f = caDir.toFile();
			if (!f.isDirectory())
				throw new Exception(f.getCanonicalPath() + " not a directory");
			if (!f.canRead())
				throw new Exception(f.getCanonicalPath() + " not readable");

			String can = "cacert." + caName + ".pem";
			Path source = caDir.resolve(can);
			File caCert = source.toFile();
			if (!caCert.isFile())
				throw new Exception(caCert.getCanonicalPath() + " not a file");
			if (!caCert.canRead())
				throw new Exception(caCert.getCanonicalPath() + " not readable");
			Path dest = tmpDir.resolve(caCert.getName());
			Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.COPY_ATTRIBUTES);

			context = "generate PKCS#12 key file";
			cmd = StrSubstitutor.replace(GENERATE_PKCS12, map);
			runCommand(cmd);

			context = "import CA cert into java keystore";
			map.put(CA_NAME, caName);
			cmd = StrSubstitutor.replace(IMPORT_CA_CERTIFICATE, map);
			runCommand(cmd);

			context = "import pkcs12 keystore into java keystore";
			cmd = StrSubstitutor.replace(IMPORT_KEYSTORE, map);
			runCommand(cmd);

			context = "delete certificate request";
			if (!(new File(map.get(DIR) + "csr")).delete())
				log.info("Delete of " + DIR + "csr file failed");

			context = "delete yes.txt";
			if (!(new File(map.get(DIR) + "yes.txt")).delete())
				log.info("Delete of " + DIR + "yes.txt file failed");

			context = "create zip file";
			Path zipPath = tmpDir.resolve(ZIP_FILE_NAME);
			URI uri = URI.create("jar:file:" + zipPath.toUri().getPath());
			Map<String, String> env = new HashMap<String, String>();
			env.put("create", "true");
			FileSystem zipFileSystem = FileSystems.newFileSystem(uri, env);
			Path zipRoot = zipFileSystem.getPath("/");
			DirectoryStream<Path> dirStream = Files.newDirectoryStream(tmpDir);
			for (Path src : dirStream) {
				String fileName = src.getFileName().toString();
				if (fileName.equals(ZIP_FILE_NAME))
					continue;
				Path dst = zipRoot.resolve(fileName);
				Files.copy(src, dst);
			}
			dirStream.close();
			zipFileSystem.close();
			zipFilePfn = zipPath.toString();

		} catch (Exception e) {
			String em = "Exception in " + RTN + context + " " + e.getCause()
					+ e.getMessage();
			log.warn(em);
			throw new Exception(em);
		}
		return zipFilePfn;
	}

	private void runCommand(String cmd) throws Exception {
		Map<String, String> envs = System.getenv();
		String pathValue = "<not set>";
		if (envs.containsKey("PATH"))
			pathValue = envs.get("PATH");
		if (pathValue == null)
			pathValue = "<null>";
		log.trace("PATH=" + pathValue);
		String line;
		String[] command = { "/bin/sh", "-c", cmd };
		String RTN = "CertificateFactory.runCommand";
		log.trace(RTN + " called with: " + cmd);
		Process process = runtime.exec(command, null, workingDirectory);
		int exitValue = process.waitFor();
		if (exitValue == 0)
			return;
		log.warn("Error running command: " + cmd);
		StringBuffer sb = new StringBuffer();
		BufferedReader buff = new BufferedReader(new InputStreamReader(
				process.getErrorStream()));
		while ((line = buff.readLine()) != null) {
			log.warn(line);
			String escapedLine = StringEscapeUtils.escapeHtml(line);
			sb.append(escapedLine).append("<br/>");
		}
		if (sessionBean != null)
			sessionBean.setLogMsg(sb.toString());
		throw new Exception(RTN + " returned " + exitValue);
	}

	public String getCountry() {
		return map.get(COUNTRY);
	}

	public void setCountry(String country) {
		map.put(COUNTRY, country);
	}

	public String getStateProvince() {
		return map.get(STATE_PROVINCE);
	}

	public void setStateProvince(String stateProvince) {
		map.put(STATE_PROVINCE, stateProvince);
	}

	public String getCity() {
		return map.get(CITY);
	}

	public void setCity(String city) {
		map.put(CITY, city);
	}

	public String getOrganization() {
		return map.get(ORGANIZATION);
	}

	public void setOrganization(String organization) {
		map.put(ORGANIZATION, organization);
	}

	public String getUnit() {
		return map.get(UNIT);
	}

	public void setUnit(String unit) {
		map.put(UNIT, unit);
	}

	public String getName() {
		return map.get(NAME);
	}

	public void setName(String name) {
		map.put(NAME, name);
	}

	public String getEmail() {
		return map.get(EMAIL);
	}

	public void setEmail(String email) {
		map.put(EMAIL, email);
	}

	public int getKeyLength() {
		return Integer.parseInt(map.get(KEY_LENGTH));
	}

	public void setKeyLength(int keyLength) {
		map.put(KEY_LENGTH, Integer.toString(keyLength));
	}

	// public int getCertificateDays() {
	// return Integer.parseInt(map.get(CERTIFICATE_DAYS));
	// }
	// public void setCertificateDays(int certificateDays) {
	// map.put(CERTIFICATE_DAYS, Integer.toString(certificateDays));
	// }

	public String getPassword() {
		return map.get(PASSWORD);
	}

	public void setPassword(String pw) {
		map.put(PASSWORD, pw);
	}

	public Object getCa() {
		return (Object) map.get(CA_CERTIFICATE);
	}

	public void setCa(Object ca) {
		map.put(CA_CERTIFICATE, (String) ca);
		setCertificateDays();
	}

	public static Logger getLog() {
		return log;
	}

	public static void setLog(Logger log) {
		CertificateFactory.log = log;
	}

	public static Properties getProperties() {
		return properties;
	}

	public static void setProperties(Properties properties) {
		CertificateFactory.properties = properties;
	}

	public static String getRunDir() {
		return runDir;
	}

	public static void setRunDir(String runDir) {
		CertificateFactory.runDir = runDir;
	}

	public static String getArchiveDir() {
		return archiveDir;
	}

	public static void setArchiveDir(String archiveDir) {
		CertificateFactory.archiveDir = archiveDir;
	}

	/**
	 * Puts ca certificate pfn in map. pfn is of the format:
	 * runDirectory/ca/certName/cacert.certName.pem
	 * 
	 * @throws Exception
	 */
	private void getCAPfns() throws Exception {
		String a = map.get(CA_CERTIFICATE);
		if (StringUtils.isBlank(a))
			throw new Exception("invalid CA Certificate name");
		String b = runDir + File.separator
				+ properties.getProperty(CA_CERT_DIR, "ca") + File.separator
				+ "cacert." + a + ".pem";
		map.put(CA_CERTIFICATE_PFN, b);
		b = runDir + File.separator + properties.getProperty(CA_CERT_DIR, "ca")
				+ File.separator + "cakey." + a + ".pem";
		map.put(CA_CERTIFICATE_KEY_PFN, b);
	}

	public void deleteTmpDir() {
		if (tmpDir != null) {
			tmpDir = null;
			String cmd = StrSubstitutor.replace(DELETE_TMP_DIRECTORY, map);
			try {
				runCommand(cmd);
			} catch (Exception e) {
				log.warn(cmd + " faile: " + e.getMessage());
			}
			;
		}
	}

	public int getSerialNumber() {
		return Integer.parseInt(map.get(SERIAL_NUMBER));
	}

	public void setSerialNumber(int s) {
		map.put(SERIAL_NUMBER, Integer.toString(s));
	}

	public static void main(String[] args) {
		try {
			CertificateFactory
					.setRunDir("/home/rmoult01/workspace/test-certificates/target/classes/runDirectory");
			PropertyConfigurator.configure(CertificateFactory.runDir
					+ "/log4j.properties");
			CertificateFactory.setLog(Logger.getLogger("syslog"));
			Properties p = new Properties();
			FileInputStream in = new FileInputStream(runDir
					+ "/Certificate.properties");
			p.load(in);
			in.close();
			CertificateFactory.setProperties(p);
			CertificateFactory cf = new CertificateFactory(null);
			cf.generateCertificate();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

} // EO CertificateFactory class
