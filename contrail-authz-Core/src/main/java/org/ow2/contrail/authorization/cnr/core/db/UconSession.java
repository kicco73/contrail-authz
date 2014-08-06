package org.ow2.contrail.authorization.cnr.core.db;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.ow2.contrail.authorization.cnr.core.utils.CorePhase;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
//import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
//import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "Session")
public class UconSession {

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
	return "UconSession [session_key=" + session_key + ", session_id_string=" + session_id_string + ", session_status=" + session_status
		+ ", message_id=" + message_id + ", reply_to=" + reply_to + ", last_reevaluation=" + last_reevaluation + ", holders=" + 
		Arrays.toString(holders.toArray()) + "]";
    }

    public static final String ID = "primary_key";
    public static final String SESSION_ID = "session_id";
    public static final String STATUS = "status";
    public static final String MESSAGE_ID = "message_id";
    public static final String REPLY_TO = "reply_to";
    public static final String LAST_REEVALUATION = "last_reevaluation";
    public static final String HOLDERS = "holders";
    public static final String HOLDER_SESSION = "holder_session";

    @Id
    @GeneratedValue
    @Column(name = ID)
    private int session_key; // db primary key
    @Column(name = SESSION_ID, nullable = false)
    private String session_id_string = null;
    @Enumerated(EnumType.STRING)
    @Column(name = STATUS, nullable = false)
    private CorePhase session_status;
    @Column(name = MESSAGE_ID, nullable = false)
    private String message_id; // soap id!!!
    @Column(name = REPLY_TO, nullable = false)
    private String reply_to;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = LAST_REEVALUATION, nullable = false)
    private Date last_reevaluation;
    // OLD VERSION WITH ONE subject, resource, action
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private UconHolder subject;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private UconHolder resource;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id", nullable = false)
    private UconHolder action;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @Cascade({ CascadeType.ALL })
    @JoinTable(name = HOLDER_SESSION, joinColumns = { @JoinColumn(name = ID) }, inverseJoinColumns = { @JoinColumn(name = UconHolder.ID) })
    @Column(name = HOLDERS, nullable = false)
    private Set<UconHolder> holders = new HashSet<UconHolder>();

    public UconSession() {
	session_status = CorePhase.PRE;
	reply_to = "";
    }

    public String getSession_id_string() {
	return session_id_string;
    }

    public void setSession_id_string(String session_id_string) {
	this.session_id_string = session_id_string;
    }

    public int getSessionKey() {
	return session_key;
    }

    public void setSessionKey(int sessionKey) {
	this.session_key = sessionKey;
    }

    public String getReplyTo() {
	return reply_to;
    }

    public void setReplyTo(String replyAddress) {
	reply_to = replyAddress;
    }

    public String getMessageId() {
	return message_id;
    }

    public void setMessageId(String messageId) {
	this.message_id = messageId;
    }

    public CorePhase getStatus() {
	return session_status;
    }

    public void setStatus(CorePhase status) {
	session_status = status;
    }

    public Date getLast_reevaluation() {
	return last_reevaluation;
    }

    public void setLast_reevaluation(Date last_reevaluation) {
	this.last_reevaluation = last_reevaluation;
    }

    public Set<UconHolder> getHolders() {
        return holders;
    }

    public void setHolders(Set<UconHolder> holders) {
        this.holders = holders;
    }
    
    //used by UconHolder
    public void setSingleHolder(UconHolder holder) {
	this.holders = new HashSet<UconHolder>();
	this.holders.add(holder);
    }
    
    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((holders == null) ? 0 : holders.size());
	result = prime * result + ((last_reevaluation == null) ? 0 : last_reevaluation.hashCode());
	result = prime * result + ((message_id == null) ? 0 : message_id.hashCode());
	result = prime * result + ((reply_to == null) ? 0 : reply_to.hashCode());
	result = prime * result + ((session_id_string == null) ? 0 : session_id_string.hashCode());
	result = prime * result + session_key;
	result = prime * result + ((session_status == null) ? 0 : session_status.hashCode());
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
	UconSession other = (UconSession) obj;
	if (holders == null) {
	    if (other.holders != null)
		return false;
	} else
	    if (!holders.equals(other.holders))
		return false;
	if (last_reevaluation == null) {
	    if (other.last_reevaluation != null)
		return false;
	} else
	    if (!last_reevaluation.equals(other.last_reevaluation))
		return false;
	if (message_id == null) {
	    if (other.message_id != null)
		return false;
	} else
	    if (!message_id.equals(other.message_id))
		return false;
	if (reply_to == null) {
	    if (other.reply_to != null)
		return false;
	} else
	    if (!reply_to.equals(other.reply_to))
		return false;
	if (session_id_string == null) {
	    if (other.session_id_string != null)
		return false;
	} else
	    if (!session_id_string.equals(other.session_id_string))
		return false;
	if (session_key != other.session_key)
	    return false;
	if (session_status != other.session_status)
	    return false;
	return true;
    }
    
//  //FIRST VERSION WITH ONE subject, resource, action
    public UconHolder getSubject() {
	return subject;
    }

    public void setSubject(UconHolder subject) {
	this.subject = subject;
    }

    public UconHolder getResource() {
	return resource;
    }

    public void setResource(UconHolder resource) {
    	this.resource = resource;
    }

    public UconHolder getAction() {
    	return action;
    }

    public void setAction(UconHolder action) {
    	this.action = action;
    }

    public static UconSession formCompleteSession(UconSession session, UconSession attributes) {

    	UconSession complete = new UconSession();

    	complete.setMessageId(session.message_id);
    	complete.setReplyTo(session.getReplyTo());
    	complete.setSession_id_string(session.getSession_id_string());
    	complete.setSessionKey(session.getSessionKey());
    	complete.setStatus(session.getStatus());

    	complete.setAction(session.getAction());
    	if(attributes.getAction() != null)
    		complete.getAction().addAttribute(attributes.getAction().getAttributes());

    	complete.setResource(session.getResource());
    	if(attributes.getResource() != null)
    		complete.getResource().addAttribute(attributes.getResource().getAttributes());

    	complete.setSubject(session.getSubject());
    	if(attributes.getSubject() != null)
    		complete.getSubject().addAttribute(attributes.getSubject().getAttributes());

    	return complete;
    }

}
