package edu.wustl.mir.erl.ihe.certificate.view;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Properties;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.icefaces.ace.component.fileentry.FileEntry;
import org.icefaces.ace.component.fileentry.FileEntryEvent;

import com.icesoft.faces.component.paneltabset.TabChangeEvent;
import com.icesoft.faces.context.FileResource;
import com.icesoft.faces.context.Resource;

import edu.wustl.mir.erl.ihe.certificate.CertType;
import edu.wustl.mir.erl.ihe.certificate.CertificateFactory;
import edu.wustl.mir.erl.ihe.certificate.CertificateView;


@ManagedBean
@SessionScoped
public class SessionBean implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@ManagedProperty(value = "#{applicationBean}")
	ApplicationBean applicationBean;
	public void setApplicationBean(ApplicationBean a) {
		applicationBean = a;
	}
	
	private String logMsg = "";
	public String getLogMsg() { return logMsg; }
	public void setLogMsg(String msg) { logMsg = msg; }
	public boolean isShowLog() {
		return logMsg.length() > 0;
	}
	
	
	private static Logger log;
	private static Properties properties;

	public static Logger getLog() {
		return log;
	}

	public static void setLog(Logger log) {
		SessionBean.log = log;
	}

	public static Properties getProperties() {
		return properties;
	}

	public static void setProperties(Properties properties) {
		SessionBean.properties = properties;
	}
	
	public SessionBean() {
		selectedTab = GENERATE_CERTS_TAB;
		initializeGen();
		initializeView();
	}	

	@PreDestroy
	public void preDestroy() {
		if (cert != null) cert.deleteTmpDir();
	}
	
	/****************************************************************************
	 *********************************** panelTabSet Controls
	 ***************************************************************************/

	private static final int GENERATE_CERTS_TAB = 0;
	private static final int VIEW_CERTS_TAB = 1;
	private int selectedTab;

	public int getSelectedTab() {
		return selectedTab;
	}

	public void setSelectedTab(int s) {
		selectedTab = s;
	}

	public void tabChangeListener(TabChangeEvent tabChangeEvent) {
		switch (selectedTab) {
		case GENERATE_CERTS_TAB:
			break;
		case VIEW_CERTS_TAB:
			initializeView();
			break;
		default:
			log.warn("xhtml returned invalid tab selected value: "
					+ selectedTab);
		}
	}
	
	/****************************************************************************
	 *********************************** Generate Certificates Tab
	 ***************************************************************************/
	
	private CertificateFactory cert = null;
	
	private String pw1, pw2;
	private int state;
	private String zipPfn;
	
	private void initializeGen() {
		if (cert != null) cert.deleteTmpDir();
		cert = new CertificateFactory(this);
		pw1 = "";
		pw2 = "";
		state = 1;
		zipPfn = null;
		logMsg = "";
	}
	
	public void reset() {
		initializeGen();
	}
	
	public void generateCertificate() {
		Valid v = new Valid();
		v.NB("Country Code", cert.getCountry());
		v.NB("State/Province", cert.getStateProvince());
		v.NB("Locality", cert.getCity());
		v.NB("Organization", cert.getOrganization());
		v.NB("Organizational Unit", cert.getUnit());
		v.NB("Name", cert.getName());
		if (!cert.getName().trim().matches("[A-Za-z0-9-]*")) {
			v.error("Common Name", "contains invalid character(s)");
		}
		v.Email("Email", cert.getEmail(), false);
		v.NB("Password", getPw1());
		if(!getPw1().equals(getPw2())) {
			v.error("Passwords do not match");
			setPw1("");
			setPw2("");
		}
		if (getPw1().length() < 6) {
			v.error("Password must be at least 6 characters");
		}
		if (v.isErrors()) return;
		cert.setPassword(pw1);
		try {
			zipPfn = cert.generateCertificate();
			state = 2;
		} catch (Exception e) {
			v.error("Error generating certificates: " + e.getMessage());
			return;
		}
	}

	public String getPw1() {
		return pw1;
	}

	public void setPw1(String pw1) {
		this.pw1 = pw1;
	}

	public String getPw2() {
		return pw2;
	}

	public void setPw2(String pw2) {
		this.pw2 = pw2;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public String getZipPfn() {
		return zipPfn;
	}

	public void setZipPfn(String zipPfn) {
		this.zipPfn = zipPfn;
	}

	public Resource getZipResource() {
		return new FileResource(new File(zipPfn));
	}

	public CertificateFactory getCert() {
		return cert;
	}

	/****************************************************************************
	 *********************************** View Certificates Tab
	 ***************************************************************************/
	
	private static SelectItem[] certTypes = CertType.getItems();
	
	private void initializeView() {
		certType = CertType.CERT;
		password = "";
		showPassword = false;
		viewStatus = 1;
		viewResults = "";
		deleteFile();
	}
	
	private void deleteFile() {
		if (file != null)
			try {
				Files.delete(file.toPath());
			} catch (IOException e) {
				log.warn("Error deleting " + file.getName() + " " + e.getMessage());
			} finally {	file = null; }
	}
		
	private CertType certType = CertType.CERT;
	private int viewStatus;
	private String viewResults;
	private File file = null;
	CertificateView certView = null;
	
	private String password;
	private boolean showPassword;

	public String getCertType() {
		return certType.name();
	}

	public void setCertType(String ct) {
		this.certType = CertType.valueOf(ct);
	}
	
	public boolean isPwNeeded() {
		return certType.isPwRequired();
	}
	
	public void viewFile(FileEntryEvent fee){
		Valid v = new Valid();
		try {
			if (certView == null) certView = new CertificateView();
			file = ((FileEntry)fee.getComponent()).getResults().getFiles().get(0).getFile();
			viewResults = certView.getCertificateView(certType, file.getAbsolutePath(), password);
			viewStatus = 2;
		} catch (Exception e) {
			v.addErrorMessage("Error: " + e.getMessage());
			viewStatus = 1;
		}
	}
	
	public String clearView() {
		initializeView();
		return null;
	}

	public int getViewStatus() {
		return viewStatus;
	}

	public void setViewStatus(int viewStatus) {
		this.viewStatus = viewStatus;
	}

	public String getViewResults() {
		return viewResults;
	}

	public void setViewResults(String viewResults) {
		this.viewResults = viewResults;
	}

	public String getFileName() {
		if (file != null) return file.getName();
		return "";
	}
	
	public SelectItem[] getCertTypes() { return certTypes; }

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isShowPassword() {
		return showPassword;
	}

	public void setShowPassword(boolean showPassword) {
		this.showPassword = showPassword;
	}
	
		
} // EO SessionBean class
