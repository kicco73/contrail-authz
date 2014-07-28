package org.ow2.contrail.authorization.cnr.pip.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.axis2.context.ServiceContext;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.ow2.contrail.authorization.cnr.pip.PipDataEntity;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscriptionsDAO {

    private static Logger log = LoggerFactory.getLogger(SubscriptionsDAO.class);

    private SessionFactory sf;
    private ServiceContext serviceContext;

    public SubscriptionsDAO(ServiceContext serviceContext) throws SQLException {
	this.sf = HibernateUtil.getSessionFactory(serviceContext);
	this.serviceContext = serviceContext;
    }

    public List<PipDataEntity> getNotAuto() throws SQLException, XacmlSamlException {
	Session session = null;
	// Transaction transaction = null;
	try {
	    session = sf.openSession();

	    // transaction = session.beginTransaction(); //CHECKME: ?

	    List<PipOwner> notAuto = this.queryNotAutoOwner(session);

	    // transaction.commit(); //CHECKME: ?

	    List<PipDataEntity> result = new ArrayList<PipDataEntity>(notAuto.size());
	    for (PipOwner o : notAuto) {
		result.add(PipDataEntity.getInstanceFromDao(serviceContext, o));
	    }
	    return result;
	} catch (HibernateException e) {
	    // if (transaction !=null)
	    // transaction.rollback();
	    throw new SQLException(e);
	} finally {
	    session.close();
	}
    }

    // public PipOwner read(String owner, String name) throws SQLException {
    // Session session = null;
    // Transaction transaction = null;
    // try {
    // session = sf.openSession();
    // transaction = session.beginTransaction();
    // Query query = session.createQuery("FROM Subscriptions S WHERE S.owner = :owner AND S.name = :name");
    // query.setParameter("owner", owner);
    // query.setParameter("name", name);
    // @SuppressWarnings("rawtypes")
    // List res = query.list();
    // if(res.isEmpty())
    // return null;
    // else
    // return (PipOwner) res.get(0);
    // } catch(HibernateException e) {
    // if (transaction !=null)
    // transaction.rollback();
    // throw new SQLException(e);
    // } finally {
    // session.close();
    // }
    // }

    public List<PipDataEntity> update(List<PipDataEntity> toUpdate, PipDataEntity[] latestData) throws SQLException, XacmlSamlException {
	Session session = null;
	Transaction transaction = null;
	try {
	    session = sf.openSession();
	    transaction = session.beginTransaction();
	    List<PipDataEntity> updated = new LinkedList<PipDataEntity>();
	    int i = 0;

	    for (PipDataEntity oldest : toUpdate) {
		// for each data entity, take the relates latest entity
		PipDataEntity newest = latestData[i++];

		PipOwner w = oldest.getDao();
		// logger.info("Prima di controllare attributi old");
		// logger.info(" - "+w.getId()+" "+w.getCategory()+": "+w.getName()+" ("+w.getIssuer()+") = "+w.getValue()+" ("+w.getType()+") ");
		// for(PipAttribute a: w.getAttributes()) {
		// logger.info(" | - "+a.getId()+" "+": "+a.getXacml_id()+" ("+a.getIssuer()+") = "+a.getValue()+" ("+a.getType()+") ");
		// }
		// logger.info("Prima di controllare attributi new");
		// PipOwner b = newest.getDao();
		// logger.info(" - "+b.getId()+" "+b.getCategory()+": "+b.getName()+" ("+b.getIssuer()+") = "+b.getValue()+" ("+b.getType()+") ");
		// for(PipAttribute a: b.getAttributes()) {
		// logger.info(" | - "+a.getId()+" "+": "+a.getXacml_id()+" ("+a.getIssuer()+") = "+a.getValue()+" ("+a.getType()+") ");
		// }

		boolean change = false;
		for (PipAttribute n_attr : newest.getDao().getAttributes()) {
		    // logger.info("new: "+n_attr.getXacml_id()+" "+n_attr.getValue());
		    for (PipAttribute o_attr : oldest.getDao().getAttributes()) {
			// logger.info("old: "+o_attr.getXacml_id()+" "+o_attr.getValue());
			if (o_attr.isTheSameAttribute(n_attr)) {
			    // logger.info("new: "+n_attr.getXacml_id()+" "+n_attr.getValue());
			    if (!(o_attr.getValue().equals(n_attr.getValue()))) {
				o_attr.setValue(n_attr.getValue());
				session.update(o_attr);
				change = true;
			    }
			    break;
			}
		    }
		}
		if (change)
		    updated.add(oldest);
	    }
	    transaction.commit();
	    return updated;
	} catch (HibernateException e) {
	    if (transaction != null)
		transaction.rollback();
	    throw new SQLException(e);
	} finally {
	    session.close();
	}
    }

    public PipDataEntity get(PipDataEntity data) throws SQLException, XacmlSamlException {
	Session session = null;
	Transaction transaction = null;
	try {
	    session = sf.openSession();
	    transaction = session.beginTransaction();

	    PipOwner owner = data.getDao();
	    PipOwner db_own = queryOwnerByNameIssuerCategory(session, owner);

	    if (db_own == null) {
		return null;
	    } else {
		return PipDataEntity.getInstanceFromDao(serviceContext, db_own);
	    }

	} catch (HibernateException e) {
	    if (transaction != null)
		transaction.rollback();
	    throw new SQLException(e);
	} finally {
	    session.close();
	}
    }

    public boolean add(PipDataEntity... dataGot) throws SQLException, XacmlSamlException {
	Session session = null;
	Transaction transaction = null;
	try {
	    session = sf.openSession();
	    transaction = session.beginTransaction();

	    int i = 0;
	    for (PipDataEntity data : dataGot) {
		PipOwner owner = data.getDao();
		// existing Owner
		PipOwner db_own = this.queryOwnerByNameIssuerCategory(session, owner);
		// existing Subscriber
		PipSubscriber db_sub = this.querySubscriberByName(session, owner.getUniqueSubscriber());

		if (db_own != null) {
		    // logger.info("db_own is not null");

		    if (db_sub != null) {
			// logger.info("db_sub is not null");
			db_own.setSingleSubscriber(db_sub);
		    } else {
			// logger.info("db_sub is null");
			PipSubscriber ps = owner.getUniqueSubscriber();
			db_own.addSubscriberAndSetUniqueLink(ps);
		    }
		    session.save(db_own);
		} else {
		    // logger.info("db_own is null");
		    if (db_sub != null) {
			// logger.info("db_sub is not null");
			owner.setSingleSubscriber(db_sub);
		    } else {
			// logger.info("db_sub is null");
		    }
		    session.saveOrUpdate(owner);
		}
		i++;
		session.flush();
	    }
	    if (i == dataGot.length) {
		transaction.commit();
		return true;
	    } else {
		transaction.rollback();
		// logger.error("error");
		return false;
	    }
	} catch (HibernateException e) {
	    if (transaction != null)
		transaction.rollback();
	    throw new SQLException(e);
	} finally {
	    session.close();
	}
    }

    public boolean remove(List<PipDataEntity> dataRequests, String subscriber) throws SQLException, XacmlSamlException {
	Session session = sf.openSession();
	Transaction transaction = null;
	try {
	    transaction = session.beginTransaction();

	    boolean res = true;
	    PipSubscriber sub = new PipSubscriber();
	    sub.setSubscriber(subscriber);
	    PipSubscriber db_sub = this.querySubscriberByName(session, sub);
	    if (db_sub == null) {
		return false;
	    }
	    for (PipDataEntity data : dataRequests) {
		PipOwner db_own = this.queryOwnerByNameIssuerCategory(session, data.getDao());
		res = res && db_own.removeSubscription(db_sub);
		if (db_own.getSubscribers().isEmpty())
		    session.delete(db_own);
	    }
	    if (db_sub.getSubscriptions().isEmpty())
		session.delete(db_sub);
	    session.flush();

	    if (res)
		transaction.commit();
	    else
		transaction.rollback();
	    return res;
	} catch (HibernateException e) {
	    if (transaction != null)
		transaction.rollback();
	    throw new SQLException(e);
	} finally {
	    session.close();
	}
    }

    public List<PipDataEntity> insertSubscriptionIfOwnerExist(List<PipDataEntity> dataRequested, String subscriber) throws SQLException,
	    XacmlSamlException {
	Session session = sf.openSession();
	Transaction transaction = null;
	try {
	    session = sf.openSession();
	    transaction = session.beginTransaction();
	    // the list of not existing data
	    List<PipDataEntity> notUpdated = new LinkedList<PipDataEntity>();

	    for (PipDataEntity data : dataRequested) {
		// get owner
		PipOwner owner = data.getDao();
		// query owner on db
		PipOwner db_own = queryOwnerByNameIssuerCategory(session, owner);
		if (db_own == null) {
		    // add to list
		    notUpdated.add(data);
		} else {
		    // check for existing Subscriber
		    PipSubscriber db_sub = this.querySubscriberByName(session, owner.getUniqueSubscriber());
		    if (db_sub != null) {
			// set subscriber (removing the other from object, in order to avoid repetition)
			db_own.setSingleSubscriber(db_sub);
		    } else {
			// create subscriber and set to object
			PipSubscriber ps = new PipSubscriber();
			ps.setSubscriber(data.getSubscriber());
			db_own.addSubscriber(ps);
		    }
		    session.save(db_own);
		    session.flush();
		}
	    }
	    transaction.commit();
	    return notUpdated;
	} catch (HibernateException e) {
	    if (transaction != null)
		transaction.rollback();
	    throw new SQLException(e);
	} finally {
	    session.close();
	}
    }

    @SuppressWarnings("unchecked")
    private List<PipOwner> queryNotAutoOwner(Session session) {
	Criteria crit_ow = session.createCriteria(PipOwner.class);
	crit_ow.setFetchMode(PipOwner.ATTRIBUTES, FetchMode.JOIN);
	Criterion own = Restrictions.eq(PipOwner.AUTO, false);
	List<PipOwner> db_own = crit_ow.add(own).list();
	return db_own;
    }

    private PipOwner queryOwnerByNameIssuerCategory(Session session, PipOwner owner) {
	Criteria crit_ow = session.createCriteria(PipOwner.class);
	Map<String, Object> restrictions_sub = new HashMap<String, Object>();
	restrictions_sub.put(PipOwner.NAME, owner.getName());
	restrictions_sub.put(PipOwner.ISSUER, owner.getIssuer());
	restrictions_sub.put(PipOwner.CATEGORY, owner.getCategory());
	Criterion own = Restrictions.allEq(restrictions_sub);
	PipOwner db_own = (PipOwner) crit_ow.add(own).uniqueResult();
	return db_own;
    }

    private PipSubscriber querySubscriberByName(Session session, PipSubscriber sub) {
	Criteria crit_sub = session.createCriteria(PipSubscriber.class);
	Criterion crit = Restrictions.eq(PipSubscriber.SUBSCRIBER, sub.getSubscriber());
	crit_sub.add(crit);
	PipSubscriber db_sub = (PipSubscriber) crit_sub.uniqueResult();
	return db_sub;
    }
}
