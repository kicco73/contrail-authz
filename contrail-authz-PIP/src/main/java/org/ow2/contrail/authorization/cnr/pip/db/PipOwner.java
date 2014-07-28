package org.ow2.contrail.authorization.cnr.pip.db;

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
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XacmlAttribute;

@Entity
@Table(name = "Owner")
public class PipOwner implements XacmlAttribute {

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
	result = prime * result + (auto ? 1231 : 1237);
	result = prime * result + ((category == null) ? 0 : category.hashCode());
	result = prime * result + id;
	result = prime * result + ((issuer == null) ? 0 : issuer.hashCode());
	result = prime * result + ((name == null) ? 0 : name.hashCode());
	result = prime * result + ((subscribers == null) ? 0 : subscribers.hashCode());
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
	PipOwner other = (PipOwner) obj;
	if (attributes == null) {
	    if (other.attributes != null)
		return false;
	} else
	    if (!attributes.equals(other.attributes))
		return false;
	if (auto != other.auto)
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
	if (name == null) {
	    if (other.name != null)
		return false;
	} else
	    if (!name.equals(other.name))
		return false;
	if (subscribers == null) {
	    if (other.subscribers != null)
		return false;
	} else
	    if (!subscribers.equals(other.subscribers))
		return false;
	return true;
    }

    @Override
    public String toString() {
	return "PipOwner [id=" + id + ", category=" + category + ", name=" + name + ", issuer=" + issuer + ", auto=" + auto + ", attributes="
		+ Arrays.toString(attributes.toArray()) + ", subscribers=" + Arrays.toString(subscribers.toArray()) + "]";
    }

    // COLUMN NAME
    public static final String ID = "id_owner";
    public static final String CATEGORY = "category";
    public static final String NAME = "name";
    public static final String ISSUER = "issuer";
    public static final String AUTO = "auto";
    public static final String ATTRIBUTES = "attributes";
    public static final String SUBSCRIBERS = "subscribers";

    @Id
    @GeneratedValue
    @Column(name = ID)
    private int id;
    @Enumerated(EnumType.STRING)
    @Column(name = CATEGORY, nullable = false)
    private UconCategory category;
    @Column(name = NAME, nullable = false)
    private String name;
    @Column(name = ISSUER, nullable = false)
    private String issuer;
    @Column(name = AUTO, nullable = false, columnDefinition = "TINYINT(1)")
    private boolean auto;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = PipAttribute.OWNER)
    @Cascade({ CascadeType.ALL })
    @Column(name = ATTRIBUTES)
    private Set<PipAttribute> attributes = new HashSet<PipAttribute>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = PipSubscriber.SUBSCRIPTIONS)
    @Cascade({ CascadeType.ALL })
    @Column(name = SUBSCRIBERS)
    private Set<PipSubscriber> subscribers = new HashSet<PipSubscriber>();

    public PipOwner() {
	super();
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

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String getIssuer() {
	return issuer;
    }

    public void setIssuer(String issuer) {
	this.issuer = issuer;
    }

    public Set<PipSubscriber> getSubscribers() {
	return subscribers;
    }

    public void setSubscribers(Set<PipSubscriber> subscribers) {
	this.subscribers = subscribers;
	for (PipSubscriber ps : subscribers) {
	    ps.getSubscriptions().add(this);
	}
    }

    /**
     * set subscriber and remove the other
     * 
     * @param subscriber
     */
    public void setSingleSubscriber(PipSubscriber subscriber) {
	this.subscribers = new HashSet<PipSubscriber>();
	this.subscribers.add(subscriber);
	subscriber.getSubscriptions().add(this);
    }

    public void addSubscriber(PipSubscriber subscriber) {
	this.subscribers.add(subscriber);
	subscriber.getSubscriptions().add(this);
    }

    public void addSubscriberAndSetUniqueLink(PipSubscriber subscriber) {
	this.subscribers.add(subscriber);
	subscriber.setSingleSubscriptions(this);
    }

    public PipSubscriber getUniqueSubscriber() {
	if (this.subscribers.size() == 1) {
	    return this.subscribers.iterator().next();
	} else {
	    return null;
	}
    }

    public boolean removeSubscription(PipSubscriber subscriber) {
	String name = subscriber.getSubscriber();
	for (PipSubscriber mySub : subscribers) {
	    if (name.equals(mySub.getSubscriber())) {
		subscribers.remove(mySub);
		mySub.getSubscriptions().remove(this);
		return true;
	    }
	}
	return false;
    }

    public boolean getAuto() {
	return auto;
    }

    public void setAuto(boolean auto) {
	this.auto = auto;
    }

    public Set<PipAttribute> getAttributes() {
	return this.attributes;
    }

    public void setAttributes(Set<PipAttribute> attributes) {
	this.attributes = attributes;
    }

    public void addAttribute(PipAttribute attribute) {
	this.attributes.add(attribute);
	attribute.setOwner(this);
    }

    public boolean isTheSameEntity(PipOwner owner) {
	return (category.equals(owner.category) && name.equals(owner.name) && issuer.equals(owner.issuer) && this.hasTheSameAttributes(owner));
    }

    private boolean hasTheSameAttributes(PipOwner owner) {
	for (PipAttribute attribute : owner.attributes) {
	    if (!contains(attribute))
		return false;
	}
	return true;
    }

    private boolean contains(PipAttribute attr) {
	for (PipAttribute attribute : attributes) {
	    if (attribute.isTheSameAttributeValue(attr))
		return true;
	}
	return false;
    }

    // CHECKME

    // the owner has no xacml id
    public String getXacml_id() {
	return UconConstants.HOLDER_ELEMENT;
    }

    public void setXacml_id(String xacml_id) {
    }

    // the name is mapped with xacml value
    public String getValue() {
	return getName();
    }

    public void setValue(String value) {
	setName(value);
    }

    // the type is always(?) string
    public String getType() {
	return UconConstants.XML_STRING;
    }

    public void setType(String type) {

    }

}
