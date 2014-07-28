package org.ow2.contrail.authorization.cnr.core.db;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.axis2.context.ServiceContext;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.ow2.contrail.authorization.cnr.core.ucon.UconDataEntity;
import org.ow2.contrail.authorization.cnr.core.utils.CorePhase;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

public class SessionDAO {

    private SessionFactory sf;
    private ServiceContext serviceContext;

    public SessionDAO(ServiceContext serviceContext) throws SQLException {
	this.sf = HibernateUtil.getSessionFactory(serviceContext);
	this.serviceContext = serviceContext;
    }

    // public UconDataEntity getSessionByID(UconDataEntity dataRequested) throws XacmlSamlException, SQLException {
    // Session session = null;
    // // Transaction transaction = null;
    // try {
    // session = sf.openSession();
    // UconSession db_ses = this.querySessionById(session, dataRequested.getSessionId());
    //
    // return UconDataEntity.getInstanceFromUconSession(serviceContext, db_ses);
    // } catch (HibernateException e) {
    // // if (transaction != null)
    // // transaction.rollback();
    // throw new SQLException(e);
    // } finally {
    // session.close();
    // }
    // }

    public UconDataEntity setStatusById(UconDataEntity dataRequested, CorePhase status) throws SQLException, XacmlSamlException {
	Session session = null;
	Transaction transaction = null;
	try {
	    session = sf.openSession();
	    transaction = session.beginTransaction();
	    UconSession db_ses = this.querySessionById(session, dataRequested.getSessionId()); // load holders too
	    db_ses.setStatus(status);
	    session.saveOrUpdate(db_ses);
	    session.flush();
	    transaction.commit();
	    return UconDataEntity.getInstanceFromUconSession(serviceContext, db_ses);
	} catch (HibernateException e) {
	    if (transaction != null)
		transaction.rollback();
	    throw new SQLException(e);
	} finally {
	    session.close();
	}
    }

    @Deprecated
    public UconDataEntity setStatusByIdAndGetUnsubscribedHolders(UconDataEntity dataRequested, CorePhase status) throws SQLException,
	    XacmlSamlException {
	Session session = null;
	Transaction transaction = null;
	try {
	    session = sf.openSession();
	    transaction = session.beginTransaction();
	    UconSession db_ses = this.querySessionById(session, dataRequested.getSessionId()); // load holders too
	    db_ses.setStatus(status);
	    // UconSession holders = new UconSession();
	    // for(UconHolder db_holder: db_ses.getHolders()) {
	    //
	    // }
	    // //OLD
	    // if(!db_ses.getAction().isSubscribed()) {
	    // holders.setAction(db_ses.getAction());
	    // db_ses.getAction().setSubscribed(true);
	    // }
	    // if(!db_ses.getResource().isSubscribed()) {
	    // holders.setResource(db_ses.getResource());
	    // db_ses.getResource().setSubscribed(true);
	    // }
	    // if(!db_ses.getSubject().isSubscribed()) {
	    // holders.setSubject(db_ses.getSubject());
	    // db_ses.getSubject().setSubscribed(true);
	    // }
	    session.saveOrUpdate(db_ses);
	    session.flush();
	    transaction.commit();
	    return UconDataEntity.getInstanceFromUconSession(serviceContext, db_ses);
	} catch (HibernateException e) {
	    if (transaction != null)
		transaction.rollback();
	    throw new SQLException(e);
	} finally {
	    session.close();
	}
    }

    @Deprecated
    public UconDataEntity setStatusByIdAndGetSubscribedHolders(UconDataEntity dataRequested, CorePhase post) {
	// TODO Auto-generated method stub
	return null;
    }

    public boolean saveNewSessionAndHolders(UconDataEntity data) throws SQLException, XacmlSamlException {
	Session session = null;
	Transaction transaction = null;
	try {
	    session = sf.openSession();
	    transaction = session.beginTransaction();

	    // check if holder is already stored on database
	    UconSession uconSession = data.getFinalSession();
	    // the actual session holders to store in database
	    Set<UconHolder> holdersToPut = new HashSet<UconHolder>();
	    // for each holders
	    for (UconHolder new_holder : uconSession.getHolders()) {
		// check if it already exists
		UconHolder db_holder = this.queryHolderByNameIssuerCategoryType(session, new_holder);
		if (db_holder != null) {
		    // if it is already subscribed, I don't need to add also attributes
		    if (!db_holder.isSubscribed()) {
			db_holder.setAttributes(new_holder.getAttributes());
		    }
		    holdersToPut.add(db_holder);
		} else {
		    holdersToPut.add(new_holder);
		}
	    }
	    // set holders collection to uconsession
	    uconSession.setHolders(holdersToPut);

	    session.save(uconSession);
	    session.flush();
	    transaction.commit();
	    return true;
	} catch (HibernateException e) {
	    if (transaction != null)
		transaction.rollback();
	    throw new SQLException(e);
	} finally {
	    session.close();
	}
    }

    public List<UconDataEntity> updateSession(UconDataEntity data) {
	// TODO Auto-generated method stub
	return null;
    }

    public Set<UconDataEntity> updateHolderAndGetInvolvedSession(UconDataEntity holder_data) throws SQLException, XacmlSamlException {
	Session session = null;
	Transaction transaction = null;
	try {
	    session = sf.openSession();
	    transaction = session.beginTransaction();
	    
	    Set<UconSession> involvedSessions = new HashSet<UconSession>();
	    
	    // get holders list
	    Set<UconHolder> holders = holder_data.getFinalSession().getHolders();
	    for(UconHolder holder: holders) {
		// query holder on database
		UconHolder db_holder = queryHolderByNameIssuerCategoryType(session, holder);
		// set the new value
		db_holder.setValue(holder.getValue());
		// add the holder sessions to involved sessions set
		involvedSessions.addAll(db_holder.getSessions());
	    }
	    Set<UconDataEntity> session_data = new HashSet<UconDataEntity>();
	    for(UconSession sess: involvedSessions) {
		session_data.add(UconDataEntity.getInstanceFromUconSession(serviceContext, sess));
	    }
	    return session_data;
	} catch (HibernateException e) {
	    if (transaction != null)
		transaction.rollback();
	    throw new SQLException(e);
	} finally {
	    session.close();
	}
    }

    private UconSession querySessionById(Session session, String sessionId) {
	Criteria criteria_ses = session.createCriteria(UconSession.class);
	Criterion crit = Restrictions.eq(UconSession.SESSION_ID, sessionId);
	criteria_ses.add(crit);
	// criteria_ses.setFetchMode(UconSession.HOLDERS, FetchMode.JOIN); // CHECKME?
	UconSession db_ses = (UconSession) criteria_ses.uniqueResult();
	return db_ses;
    }

    private UconHolder queryHolderByNameIssuerCategoryType(Session session, UconHolder holder) {
	Criteria crit_hold = session.createCriteria(UconHolder.class);
	Map<String, Object> restrictions = new HashMap<String, Object>();
	restrictions.put(UconHolder.NAME, holder.getXacml_id());
	restrictions.put(UconHolder.ISSUER, holder.getIssuer());
	restrictions.put(UconHolder.CATEGORY, holder.getCategory());
	restrictions.put(UconHolder.TYPE, holder.getType());
	Criterion hold = Restrictions.allEq(restrictions);
	UconHolder db_holder = (UconHolder) crit_hold.add(hold).uniqueResult();
	return db_holder;
    }
}
