package org.ow2.contrail.authorization.cnr.utils;

public class XacmlSamlException extends Exception {

    private static final long serialVersionUID = 1L;

    public XacmlSamlException(String message) {
	super(message);
    }

    public XacmlSamlException(String message, Throwable t) {
	super(message, t);
    }

    public XacmlSamlException(Throwable t) {
	super(t);
    }
}
