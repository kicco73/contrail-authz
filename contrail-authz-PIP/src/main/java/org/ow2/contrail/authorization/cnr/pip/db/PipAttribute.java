package org.ow2.contrail.authorization.cnr.pip.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.ow2.contrail.authorization.cnr.utils.XacmlAttribute;

@Entity
@Table(name = "Attribute")
public class PipAttribute implements XacmlAttribute {

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + id;
	result = prime * result + ((issuer == null) ? 0 : issuer.hashCode());
	result = prime * result + ((type == null) ? 0 : type.hashCode());
	result = prime * result + ((value == null) ? 0 : value.hashCode());
	result = prime * result + ((xacml_id == null) ? 0 : xacml_id.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	PipAttribute other = (PipAttribute) obj;
	if (id != other.id)
	    return false;
	if (issuer == null) {
	    if (other.issuer != null)
		return false;
	} else
	    if (!issuer.equals(other.issuer))
		return false;
	if (type == null) {
	    if (other.type != null)
		return false;
	} else
	    if (!type.equals(other.type))
		return false;
	if (value == null) {
	    if (other.value != null)
		return false;
	} else
	    if (!value.equals(other.value))
		return false;
	if (xacml_id == null) {
	    if (other.xacml_id != null)
		return false;
	} else
	    if (!xacml_id.equals(other.xacml_id))
		return false;
	return true;
    }

    @Override
    public String toString() {
	return "PipAttribute [id=" + id + ", xacml_id=" + xacml_id + ", value=" + value + ", type=" + type + ", issuer=" + issuer + "]";
    }

    public static final String ID = "id_attribute";
    public static final String OWNER = "owner";
    public static final String TYPE = "type";
    public static final String NAME = "xacml_id";
    public static final String ISSUER = "issuer";
    public static final String VALUE = "value";

    @Id
    @GeneratedValue
    @Column(name = ID)
    private int id;
    @Column(nullable = false, name = NAME)
    private String xacml_id;
    @Column(nullable = false, name = VALUE)
    private String value;
    @Column(nullable = false, name = TYPE)
    private String type;
    @Column(nullable = false, name = ISSUER)
    private String issuer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = PipOwner.ID)
    private PipOwner owner;

    public PipAttribute() {
	super();
    }

    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
    }

    public String getValue() {
	return value;
    }

    public void setValue(String value) {
	this.value = value;
    }

    public String getType() {
	return type;
    }

    public void setType(String type) {
	this.type = type;
    }

    public PipOwner getOwner() {
	return owner;
    }

    public void setOwner(PipOwner owner) {
	this.owner = owner;
    }

    public boolean isTheSameAttribute(PipAttribute attr) {
	// compare type name and value
	return (attr != null && type.equals(attr.type) && xacml_id.equals(attr.xacml_id) && issuer.equals(attr.issuer));
    }

    public boolean isTheSameAttributeValue(PipAttribute attr) {
	// compare type name and value
	return (attr != null && type.equals(attr.type) && xacml_id.equals(attr.xacml_id) && value.equals(attr.value) && issuer.equals(attr.issuer));
    }

    public String getXacml_id() {
	return this.xacml_id;
    }

    public void setXacml_id(String xacml_id) {
	this.xacml_id = xacml_id;
    }

    public String getIssuer() {
	return issuer;
    }

    public void setIssuer(String issuer) {
	this.issuer = issuer;
    }
}
