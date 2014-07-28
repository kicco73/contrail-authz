package org.ow2.contrail.authorization.cnr.pep.utils;

import org.ow2.contrail.authorization.cnr.utils.UconCategory;
import org.ow2.contrail.authorization.cnr.utils.XacmlAttribute;

public class PepRequestAttribute implements XacmlAttribute {

    private String xacml_attribute_id, type, value, issuer;
    private UconCategory category;

    public PepRequestAttribute(String xacmlid, String type, String value, String issuer, UconCategory category) {
	this.xacml_attribute_id = xacmlid;
	this.type = type;
	this.value = value;
	this.issuer = issuer;
	this.category = category;
    }

    public String getType() {
	return type;
    }

    @Override
    public void setType(String type) {
	this.type = type;

    }

    public String getIssuer() {
	return issuer;
    }

    @Override
    public void setIssuer(String issuer) {
	this.issuer = issuer;
    }

    public UconCategory getCategory() {
	return category;
    }

    @Override
    public String getXacml_id() {
	return xacml_attribute_id;
    }

    @Override
    public void setXacml_id(String xacml_id) {
	this.xacml_attribute_id = xacml_id;

    }

    @Override
    public void setValue(String value) {
	this.value = value;
    }

    public String getValue() {
	return value;
    }

}
