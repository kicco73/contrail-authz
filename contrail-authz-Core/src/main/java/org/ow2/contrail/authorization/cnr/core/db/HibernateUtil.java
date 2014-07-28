package org.ow2.contrail.authorization.cnr.core.db;

import java.sql.SQLException;

import org.apache.axis2.context.ServiceContext;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;

public class HibernateUtil {

    private static HibernateUtil hu = null;

    private final SessionFactory sessionFactory;

    private HibernateUtil() throws SQLException {
	try {
	    Configuration configuration = new Configuration();
	    configuration.configure();
	    configuration.addPackage(this.getClass().getPackage().getName()); // the fully qualified package name
	    configuration.addAnnotatedClass(UconHolder.class); // the order is important! 
	    configuration.addAnnotatedClass(UconSession.class);
	    configuration.addAnnotatedClass(UconAttribute.class);
	    ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
	    sessionFactory = configuration.buildSessionFactory(serviceRegistry);
	} catch (Throwable ex) {
	    throw new SQLException(ex);
	}
    }

    public static void init(ServiceContext serviceContext) throws SQLException {
	if (serviceContext == null) {
	    if (hu == null) {
		hu = new HibernateUtil();
	    }
	} else {
	    Object obj = serviceContext.getProperty(UconConstants.DB_TOOL);
	    if (obj == null) {
		HibernateUtil hu = new HibernateUtil();
		serviceContext.setProperty(UconConstants.DB_TOOL, hu);
	    }
	}
    }

    public static SessionFactory getSessionFactory(ServiceContext serviceContext) throws SQLException {
	if (serviceContext == null) {
	    if (hu == null) {
		hu = new HibernateUtil();
	    }
	    return hu.sessionFactory;
	} else {
	    Object obj = serviceContext.getProperty(UconConstants.DB_TOOL);
	    if (obj != null) {
		return ((HibernateUtil) obj).sessionFactory;
	    } else {
		HibernateUtil hu = new HibernateUtil();
		serviceContext.setProperty(UconConstants.DB_TOOL, hu);
		return hu.sessionFactory;
	    }
	}
    }
}