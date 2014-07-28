import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.axis2.AxisFault;
import org.ow2.contrail.authorization.cnr.pep.PEP;
import org.ow2.contrail.authorization.cnr.pep.PEP_callout;
import org.ow2.contrail.authorization.cnr.pep.utils.PepRequestAttribute;
import org.ow2.contrail.authorization.cnr.utils.UconCategory;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;

import java.util.LinkedList;
import java.util.List;

import javax.xml.ws.WebServiceException;

public class SimpleTest {

    // private static String serverIp = "146.48.96.76:8080";
    //private static String serverIp = "146.48.99.167:8080";
    // private static String clientIp = "146.48.96.125"; //My IP
    private static String serverIp = "localhost:8080";
    private static String clientIp = "localhost"; // My IP
    // private static String clientIp = "192.168.220.142";
    private static String clientPort = "19065";

    public static void main(String[] args) throws InterruptedException {
	try {
	    String pdp_endpoint = "http://" + serverIp + "/axis2/services/UconWs";
	    String pep_host = clientIp;
	    String pep_port = clientPort;

	    PEP pep = new PEP(pdp_endpoint, pep_port, pep_host);

	    PEP_callout pep_callout = pep.getNewCallout();

	    // prepare request example request (at least SUBJECT - ACTION - OBJECT)
	    System.out.println("Test: prepare request");
	    // List<PepRequestAttribute> req = prepareRequest();

	    X509Certificate cert = null;
	    try {
		String certPos = System.getProperty("user.home") + "/cert.cer";

		InputStream inStream = new FileInputStream(certPos);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		cert = (X509Certificate) cf.generateCertificate(inStream);
		inStream.close();

	    } catch (Exception e) {
		e.printStackTrace();
	    }

	    String type = UconConstants.XML_STRING;
	    String issuer = "CNR-Federation";
	    List<PepRequestAttribute> req = new LinkedList<PepRequestAttribute>(); //pep_callout.getAttributesFromCertificate(cert);
	    PepRequestAttribute res = new PepRequestAttribute("urn:oasis:names:tc:xacml:1.0:resource:resource-id", type, "aa", issuer,
		    UconCategory.RESOURCE);
	    req.add(res);
	    PepRequestAttribute act = new PepRequestAttribute("urn:contrail:vep:action:id", type, "aa", issuer, UconCategory.ACTION);
	    req.add(act);

	    System.out.println("Test: send try access");
	    boolean accessDecision = pep_callout.tryaccess(cert, req);
	    System.out.println("Test: try access result " + accessDecision);
	    if (accessDecision) {

		// set a callback in case of access revocation
		System.out.println("Test: send startaccess");
		String uuid = "af22bc00-3f0b-480b-9da6-05110892e7ab";
		String appUuid = "b0bebfe7-343e-4a33-b586-3e7db3b9cc43";
		String urlBase = "http://146.48.81.249:8080/federation-api/";
		urlBase += "users/" + uuid + "/applications/" + appUuid + "/revoke";
		URL url = new URL(urlBase);
		System.out.println("URL: " + url.toString());
		pep_callout.startaccess(url);

		Thread.sleep(38000);
		System.out.println("Test: send endaccess");
		pep_callout.endaccess();

	    }

	} catch (WebServiceException e) {
	    System.err.println("" + e.getMessage());
	} catch (AxisFault e) {
	    e.printStackTrace();
	    // System.err.println(""+e.getMessage());
	} catch (MalformedURLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (CertificateException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

}