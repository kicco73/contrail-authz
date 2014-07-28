package org.ow2.contrail.authorization.cnr.core.db;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.ow2.contrail.authorization.cnr.utils.UconCategory;
import org.ow2.contrail.authorization.cnr.utils.XacmlAttribute;

@Entity
@Table(name = "Holder")
public class UconHolder implements XacmlAttribute {

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
	return "UconHolder [id=" + id + ", category=" + category + ", xacml_id=" + xacml_id + ", value=" + value + ", issuer=" + issuer + ", type="
		+ type + ", subscribed=" + subscribed + ", attributes=" + Arrays.toString(attributes.toArray()) + "]";
    }


    public static final String ID = "id_holder";
    public static final String CATEGORY = "category";
    public static final String TYPE = "type";
    public static final String NAME = "xacml_id";
    public static final String ISSUER = "issuer";
    public static final String VALUE = "value";
    public static final String SUBSCRIBED = "subscribed";
    public static final String ATTRIBUTES = "attributes";
    public static final String SESSIONS = "sessions";
    
    @Id
    @GeneratedValue
    @Column(name = ID)
    private int id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = CATEGORY)
    private UconCategory category;
    @Column(nullable = false, name = NAME)
    private String xacml_id;
    @Column(nullable = false, name = VALUE)
    private String value;
    @Column(nullable = false, name = ISSUER)
    private String issuer;
    @Column(nullable = false, name = TYPE)
    private String type;
    @Column(nullable = false, columnDefinition = "TINYINT(1)", name = SUBSCRIBED)
    private boolean subscribed;
//    @OneToMany(fetch = FetchType.LAZY, mappedBy = UconSession."Holder")
//    @Cascade({ CascadeType.DELETE })
//    private Set<UconSession> sessions = new HashSet<UconSession>();
  
    
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = UconSession.HOLDERS)
    @Cascade({ CascadeType.ALL })
    @Column(name = SESSIONS)
    private Set<UconSession> sessions = new HashSet<UconSession>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = UconAttribute.HOLDER)
    @Cascade({ CascadeType.DELETE })//@Cascade({CascadeType.ALL})
    @Column(name=ATTRIBUTES)
    private Set<UconAttribute> attributes = new HashSet<UconAttribute>();

    public UconHolder() {
	super();
    }

    public static UconHolder makeHolderFromAttribute(UconAttribute attribute, UconCategory category) {
	UconHolder holder = new UconHolder();
	holder.setCategory(category);
	holder.setIssuer(attribute.getIssuer());
	holder.setType(attribute.getType());
	holder.setValue(attribute.getType());
	holder.setXacml_id(attribute.getXacml_id());
	return holder;
    }

    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
    }

    public UconCategory getCategory() {
	return category;
    }

    public void setCategory(UconCategory category) {
	this.category = category;
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

    public String getIssuer() {
	return issuer;
    }

    public void setIssuer(String issuer) {
	this.issuer = issuer;
    }

    public String getType() {
	return type;
    }

    public void setType(String type) {
	this.type = type;
    }

    public Set<UconSession> getSessions() {
	return sessions;
    }

    public void setSessions(Set<UconSession> sessions) {
	this.sessions = sessions;
	for(UconSession s: sessions) {
	    s.getHolders().add(this);
	}
    }

    /**
     * set subscriber and remove the other
     * 
     * @param subscriber
     */
    public void setSingleSession(UconSession session) {
	this.sessions = new HashSet<UconSession>();
	this.sessions.add(session);
	session.getHolders().add(this);
    }

    public void addSession(UconSession session) {
	this.sessions.add(session);
	session.getHolders().add(this);
    }

    public void addSessionAndSetUniqueLink(UconSession session) {
	this.sessions.add(session);
	session.setSingleHolder(this); //CHECKME
    }

    public UconSession getUniqueSession() {
	if (this.sessions.size() == 1) {
	    return this.sessions.iterator().next();
	} else {
	    return null;
	}
    }

//    public boolean removeSession(UconSession session) {
//	String name = subscriber.getSubscriber();
//	for (PipSubscriber mySub : subscribers) {
//	    if (name.equals(mySub.getSubscriber())) {
//		subscribers.remove(mySub);
//		mySub.getSubscriptions().remove(this);
//		return true;
//	    }
//	}
//	return false;
//    }
    
    // CHECKME in PipSubscriber there's setSingleSession

    public Set<UconAttribute> getAttributes() {
	return attributes;
    }

    public void setAttributes(Set<UconAttribute> attributes) {
	this.attributes = attributes;
    }

    public void addAttribute(UconAttribute attribute) {
	this.attributes.add(attribute);
	attribute.setHolder(this);
    }

    public boolean isSubscribed() {
	return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
	this.subscribed = subscribed;
    }

    public void addAttribute(Set<UconAttribute> attributes) {
	for (UconAttribute attr : attributes)
	    addAttribute(attr);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((category == null) ? 0 : category.hashCode());
        result = prime * result + id;
        result = prime * result + ((issuer == null) ? 0 : issuer.hashCode());
        result = prime * result + ((sessions == null) ? 0 : sessions.hashCode());
        result = prime * result + (subscribed ? 1231 : 1237);
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
        UconHolder other = (UconHolder) obj;
        if (attributes == null) {
            if (other.attributes != null)
        	return false;
        } else
            if (!attributes.equals(other.attributes))
        	return false;
        if (category != other.category)
            return false;
        if (id != other.id)
            return false;
        if (issuer == null) {
            if (other.issuer != null)
        	return false;
        } else
            if (!issuer.equals(other.issuer))
        	return false;
        if (sessions == null) {
            if (other.sessions != null)
        	return false;
        } else
            if (!sessions.equals(other.sessions))
        	return false;
        if (subscribed != other.subscribed)
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
}
