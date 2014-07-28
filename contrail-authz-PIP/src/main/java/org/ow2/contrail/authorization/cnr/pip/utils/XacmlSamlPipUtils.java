package org.ow2.contrail.authorization.cnr.pip.utils;

import java.util.List;

import org.ow2.contrail.authorization.cnr.pip.db.PipOwner;
import org.ow2.contrail.authorization.cnr.utils.UconCategory;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.w3c.dom.Element;

public interface XacmlSamlPipUtils {

    public List<Element> getUconGenericRequests(Element request) throws XacmlSamlException;

    /**
     * Create a SAML AttributeQuery element without any attributes
     * "if no attributes are specified, it indicates that all attribute allowed by policy are requested"
     * 
     * @param name
     *            subject name
     * @return the attribute query object
     */
    public Element formSAMLAttributeQuery(String name) throws XacmlSamlException;

    /**
     * Create a SAML AttributeQuery element
     * 
     * @param name
     *            subject name
     * @param attributes
     *            string list of attributes to query
     * @return the attribute query object
     */
    public Element formSAMLAttributeQuery(String name, List<String> attributes) throws XacmlSamlException;

    public Element formPipResponse(List<Element> attributes) throws XacmlSamlException;

    /**
     * Create the XACML response message from identity provider update
     * 
     * @param updateMessage
     * @return
     * @throws XacmlSamlException
     */
    public Element formUconUpdateMessage(List<Element> attributes) throws XacmlSamlException;

    public Element convertSAMLtoXACML(Element samlResponse, UconCategory type) throws XacmlSamlException;

    /**
     * Get a Entity Subscriptions from a SAML response (to populate subscription table)
     * 
     * @param queryResponse
     * @return
     * @throws XacmlSamlException
     */
    public PipOwner convertSAMLtoSubscription(Element samlRespose, UconCategory type, String subscriber, boolean autoupdate) throws XacmlSamlException;

    public Element convertSubscriptionToXACML(PipOwner subscription) throws XacmlSamlException;

}