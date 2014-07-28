package org.ow2.contrail.authorization.cnr.old.pep;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Statement;
import org.opensaml.xacml.ctx.DecisionType;
import org.opensaml.xacml.ctx.ResponseType;
import org.opensaml.xacml.ctx.ResultType;
import org.opensaml.xacml.profile.saml.XACMLAuthzDecisionStatementType;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.parse.BasicParserPool;
import org.w3c.dom.Document;

@Deprecated
public class ResponseCtx {

private static Logger logger = Logger.getLogger("cnr.contrail.pep.responseCtx");
private static UnmarshallerFactory unMarshallerFactory = org.opensaml.xml.Configuration
						.getUnmarshallerFactory();

public static String getAccessDecisionFromSAMLResp(String samlResp){
	String decision = new String("Deny");
	try {		
	    InputStream in = new ByteArrayInputStream(samlResp.getBytes());
	    	    
	    BasicParserPool pool = new BasicParserPool();
	    Document doc = pool.parse(in);

	    org.opensaml.xml.io.Unmarshaller queryUnmarshaller = unMarshallerFactory
		    .getUnmarshaller(Response.DEFAULT_ELEMENT_NAME);

	    Response response = (Response) queryUnmarshaller.unmarshall(doc
		    .getDocumentElement());

	    Assertion assertion = response.getAssertions().get(0);
	    List<Statement> statements = assertion
		    .getStatements(XACMLAuthzDecisionStatementType.TYPE_NAME_XACML20);

	    XACMLAuthzDecisionStatementType xacmlS = (XACMLAuthzDecisionStatementType) statements
		    .get(0);

	    ResponseType xacmlResponse = xacmlS.getResponse();

	    ResultType result = xacmlResponse.getResult();

	    DecisionType pdpDecision = result.getDecision();
	    
	    decision = pdpDecision.getDecision().toString();
	    
	} catch (Exception e) {
		//DO SOMETHING
		//System.out.println("LocatorPDP->constructor:" + e.toString());
		logger.log(Level.WARNING, "ResponseCtx->getAccessDecisionFromSAMLResp:", e);
	}	
	return decision;
}

}
