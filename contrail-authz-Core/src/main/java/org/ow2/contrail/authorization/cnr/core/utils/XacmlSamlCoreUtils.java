package org.ow2.contrail.authorization.cnr.core.utils;

import java.util.List;

import org.ow2.contrail.authorization.cnr.core.db.UconSession;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.w3c.dom.Element;

public interface XacmlSamlCoreUtils {

    public UconSession getUconSessionFromSamlXacmlRequest(Element samlXacml) throws XacmlSamlException;

    public Element formAttributeQueryRequest(UconSession session);

    public Element formSubscriptionRequest(UconSession session);

    public Element formUnubscriptionRequest(UconSession session);

    public String formXacmlPDPRequest(UconSession session) throws XacmlSamlException;

    public UconSession getAttributeFromPipResponse(Element attributesXml) throws XacmlSamlException;

    public UconSession getUconSessionFromXacmlRetrieve(Element retrieve) throws XacmlSamlException;

    public List<UconSession> getHolderUpdate(Element updates) throws XacmlSamlException;

}
