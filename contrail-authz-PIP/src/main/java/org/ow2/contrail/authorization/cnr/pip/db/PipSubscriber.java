package org.ow2.contrail.authorization.cnr.pip.db;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

@Entity
@Table(name = "Subscriber")
public class PipSubscriber {

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + id;
	result = prime * result + ((subscriber == null) ? 0 : subscriber.hashCode());
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
	PipSubscriber other = (PipSubscriber) obj;
	if (id != other.id)
	    return false;
	if (subscriber == null) {
	    if (other.subscriber != null)
		return false;
	} else
	    if (!subscriber.equals(other.subscriber))
		return false;
	return true;
    }

    @Override
    public String toString() {
	return "PipSubscriber [id=" + id + ", subscriber=" + subscriber + "]";
    }

    public static final String ID = "id_subscriber";
    public static final String SUBSCRIBER = "subscriber";
    public static final String SUBSCRIPTIONS = "subscriptions";
    public static final String OWNER_SUBSCRIBER = "Owner_Subscriber";

    @Id
    @GeneratedValue
    @Column(name = ID)
    private int id;
    @Column(name = SUBSCRIBER, nullable = false)
    private String subscriber;

    @ManyToMany(fetch = FetchType.LAZY)
    @Cascade({ CascadeType.ALL })
    @JoinTable(name = OWNER_SUBSCRIBER, joinColumns = { @JoinColumn(name = ID) }, inverseJoinColumns = { @JoinColumn(name = PipOwner.ID) })
    @Column(name = SUBSCRIPTIONS, nullable = false)
    private Set<PipOwner> subscriptions = new HashSet<PipOwner>();

    public PipSubscriber() {
    }

    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
    }

    public String getSubscriber() {
	return subscriber;
    }

    public void setSubscriber(String subscriber) {
	this.subscriber = subscriber;
    }

    public Set<PipOwner> getSubscriptions() {
	return subscriptions;
    }

    public void setSubscriptions(Set<PipOwner> subscriptions) {
	this.subscriptions = subscriptions;
    }

    //used by PipOwner
    public void setSingleSubscriptions(PipOwner subscriptions) {
	this.subscriptions = new HashSet<PipOwner>();
	this.subscriptions.add(subscriptions);
    }

}
