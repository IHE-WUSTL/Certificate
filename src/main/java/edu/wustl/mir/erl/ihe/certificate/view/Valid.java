package edu.wustl.mir.erl.ihe.certificate.view;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

/**
 * Provides a simple validation context for web application screen input values
 * in JSF 2 web applications.
 * <p/>
 * Example of use:
 * <p/>
 * 
 * <pre>
 * Valid v = new Valid();
 * v.NB("First Name", bean.getFirstName());
 * v.NB("Last Name, bean.getLastName());
 * v.Email("Email address", bean.getEmail());
 * if (bean.isEmailDuplicate() == true) {
 *    v.error("Email already on file");
 * }
 * if (v.isErrors()) return;
 * // further processing...
 * </pre>
 * 
 * @author rmoult01
 */
public class Valid implements Serializable {

	private static final long serialVersionUID = 1L;

	private FacesContext context = FacesContext.getCurrentInstance();
	private boolean errors = false;
	
	public Valid() {}

	/**
	 * Returns the value of the error flag. This flag is initially set to false,
	 * then set to true by any validation method which detects an error.
	 * @return boolean have one or more errors occurred in this validation 
	 * context.
	 */
	public boolean isErrors() {
		return errors;
	}

	/**
	 * Sets the value of the error flag. In most cases, setting the error flag
	 * is handled automatically by the validation and error reporting methods.
	 * @param errors
	 */
	public void setErrors(boolean errors) {
		this.errors = errors;
	}
	
	/**
	 * Resets the validation context, setting the error flag to false and 
	 * reloading the Faces context. In normal use, the developer simply creates
	 * a new instance of Valid.
	 */
	public void reset() {
		context = FacesContext.getCurrentInstance();
		errors = false;
	}

	/**
	 * Used to record validation errors which were found outside of Valid.
	 * Adds message and sets error flag to true.
	 * @param id text indicator to user of field related to error.
	 * @param msg Error message
	 */
	public void error(String id, String msg) {
		addErrorMessage(id + " " + msg);
		errors = true;
	}
	
	/**
	 * Used to record validation errors which were found outside of Valid.
	 * Adds message and sets error flag to true.
	 * @param msg Error message
	 */
	public void error(String msg) {
		addErrorMessage(msg);
		errors = true;
	}

	/**
	 * Tests string value which must be non-blank. If the value to be tested is
	 * blank, that is, that it contains only whitespace, is the empty string, or
	 * is null, the error flag is set to true and an appropriate error message
	 * is displayed.
	 * @param id text indicator to user of field related to error.
	 * @param v String value to be tested.
	 */
	public void NB(String id, String v) {
		if (StringUtils.isBlank(v)) {
			error(id, " Can't be null, empty, or just whitespace");
		}
	}

	/**
	 * Tests that an integer value represents a valid port number, that is, that
	 * it is in the range 1-65535. if not, the error flag is set to true and an 
	 * appropriate error message is displayed.
	 * @param id text indicator to user of field related to error.
	 * @param p int port value to be validated.
	 * @param required boolean, is the entry required. If false, 0 will also be
	 * accepted as a valid port number
	 */
	public void Port(String id, int p, boolean required) {
		if (!required && p == 0)
			return;
		if (p < 1 || p > 65535) {
			error(id, "Invalid port number");
		}
	}
	/**
	 * Tests that a string represents a valid URL. if not, the error flag is set 
	 * to true and an appropriate error message is displayed.
	 * @param id text indicator to user of field related to error.
	 * @param v string URL representation to be validated.
	 * @param required boolean, is the entry required. If false, a blank string
	 * will also be accepted as a valid entry.
	 */
	public void URL(String id, String v, boolean required) {
		if (!required && StringUtils.isBlank(v))
			return;
		try {
			new URL(v);
		} catch (MalformedURLException ex) {
			error(id, "Invalid URL");
		}
	}
	/**
	 * Tests that a string represents a valid IPV4 address in dot notation, for
	 * example, 127.0.0.1.
	 * If not, the error flag is set to true and an appropriate error message is 
	 * displayed.
	 * @param id text indicator to user of field related to error.
	 * @param v string IPV4 address representation to be validated.
	 * @param required boolean, is the entry required. If false, a blank string
	 * will also be accepted as a valid entry.
	 */
	public void Ip(String id, String v, boolean required) {
		if (!required && StringUtils.isBlank(v))
			return;
		boolean valid = true;
		String[] tuples = v.split("\\.");
		if (tuples.length == 4) {
			for (String tuple : tuples) {
				int i = Integer.parseInt(tuple);
				if (i >= 0 && i <= 255) {
					continue;
				}
				valid = false;
				break;
			}
		} else {
			valid = false;
		}
		if (!valid) {
			error(id, "Not a valid IP V4 address");
		}
	}
	/**
	 * Tests that a string represents a valid DICOM AE title, that is, that it
	 * consists entirely of upper or lower case letters from the English 
	 * alphabet, decimal digits, and the underscore character "_", for example, 
	 * AE_TITLE.
	 * If not, the error flag is set to true and an appropriate error message is 
	 * displayed.
	 * @param id text indicator to user of field related to error.
	 * @param v string DICOM AE title to be validated.
	 * @param required boolean, is the entry required. If false, a blank string
	 * will also be accepted as a valid entry.
	 */
	public void AeTitle(String id, String v, boolean required) {
		if (!required && StringUtils.isBlank(v))
			return;
		if (!StringUtils.trimToEmpty(v).matches("\\w{1,16}"))
			error(id, "Invalid AE Title");
	}

	/**
	 * Tests that a string represents a valid email address, using {@link 
	   org.apache.commons.validator.routinesEmailValidator#isValid() 
	   EmailValidator}.
	 * If not, the error flag is set to true and an appropriate error message is 
	 * displayed.
	 * @param id text indicator to user of field related to error.
	 * @param v string email address representation to be validated.
	 * @param required boolean, is the entry required. If false, a blank string
	 * will also be accepted as a valid entry.
	 */
	public void Email(String id, String v, boolean required) {
		if (!required && StringUtils.isBlank(v))
			return;
		if (!EmailValidator.getInstance().isValid(StringUtils.trimToEmpty(v))) {
			error(id, "Invalid Email Address");
		}
	}

	/**
	 * Add a general (screen level) error message. 
	 * @param msg the error message
	 */
	public void addErrorMessage(String msg) {
		addErrorMessage(null, msg);
	}

	/**
	 * Add error message to a specific client.
	 * @param clientId the client id
	 * @param msg the error message
	 */
	public void addErrorMessage(String clientId, String msg) {
		context.addMessage(clientId, new FacesMessage(
				FacesMessage.SEVERITY_ERROR, msg, msg));
	}

} // EO Class Valid