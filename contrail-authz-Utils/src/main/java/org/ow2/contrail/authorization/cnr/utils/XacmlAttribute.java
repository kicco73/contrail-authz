package org.ow2.contrail.authorization.cnr.utils;

public interface XacmlAttribute {

    public String getXacml_id();

    public void setXacml_id(String xacml_id);

    public String getValue();

    public void setValue(String value);

    public String getType();

    public void setType(String type);

    public String getIssuer();

    public void setIssuer(String issuer);

}
