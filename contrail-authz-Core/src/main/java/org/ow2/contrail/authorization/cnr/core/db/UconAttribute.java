package org.ow2.contrail.authorization.cnr.core.db;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.ow2.contrail.authorization.cnr.utils.XacmlAttribute;

@Entity
@Table(name = "Attribute")
public class UconAttribute implements XacmlAttribute {

    @Override
    public String toString() {
	return "UconAttribute [attribute_id=" + attribute_id + ", xacml_id=" + xacml_id + ", value=" + value + ", issuer=" + issuer + ", type="
		+ type + ", last_change=" + last_change + "]";
    }

    public static final String ID = "id_attribute";
    public static final String CATEGORY = "category";
    public static final String NAME = "xacml_id";
    public static final String VALUE = "value";
    public static final String ISSUER = "issuer";
    public static final String TYPE = "type";
    public static final String LAST_CHANGE = "last_change";
    public static final String HOLDER = "holder"; // I can't use @Column

    @Id
    @GeneratedValue
    @Column(name = ID)
    private int attribute_id; // primary key in db
    @Column(name = NAME, nullable = false)
    private String xacml_id;
    @Column(name = VALUE, nullable = false)
    private String value;
    @Column(name = ISSUER, nullable = false)
    private String issuer;
    @Column(name = TYPE, nullable = false)
    private String type;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = LAST_CHANGE, nullable = false)
    private Date last_change;
    // @Enumerated(EnumType.STRING)
    // private Category category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = UconHolder.ID, nullable = false)
//    @Column(name = HOLDER, nullable = false)
    private UconHolder holder;

    public UconAttribute() {
	super();
    }

    public int getAttribute_id() {
	return attribute_id;
    }

    public void setAttribute_id(int attribute_id) {
	this.attribute_id = attribute_id;
    }

    public String getXacml_id() {
	return xacml_id;
    }

    public void setXacml_id(String xacml_id) {
	this.xacml_id = xacml_id;
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

    public String getIssuer() {
	return issuer;
    }

    public void setIssuer(String issuer) {
	this.issuer = issuer;
    }

    public Date getLast_change() {
	return last_change;
    }

    public void setLast_change(Date last_change) {
	this.last_change = last_change;
    }

    public UconHolder getHolder() {
	return holder;
    }

    public void setHolder(UconHolder holder) {
	this.holder = holder;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + attribute_id;
        result = prime * result + ((holder == null) ? 0 : 1);
        result = prime * result + ((issuer == null) ? 0 : issuer.hashCode());
        result = prime * result + ((last_change == null) ? 0 : last_change.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + ((xacml_id == null) ? 0 : xacml_id.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UconAttribute other = (UconAttribute) obj;
        if (attribute_id != other.attribute_id)
            return false;
        if (holder == null) {
            if (other.holder != null)
        	return false;
        } else
            if (!holder.equals(other.holder))
        	return false;
        if (issuer == null) {
            if (other.issuer != null)
        	return false;
        } else
            if (!issuer.equals(other.issuer))
        	return false;
        if (last_change == null) {
            if (other.last_change != null)
        	return false;
        } else
            if (!last_change.equals(other.last_change))
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

    // public Category getCategory() {
    // return category;
    // }
    //
    // public void setCategory(Category category) {
    // this.category = category;
    // }
    
    
    
}
