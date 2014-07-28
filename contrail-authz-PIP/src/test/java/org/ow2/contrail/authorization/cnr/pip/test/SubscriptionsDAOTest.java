package org.ow2.contrail.authorization.cnr.pip.test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ow2.contrail.authorization.cnr.pip.PipDataEntity;
import org.ow2.contrail.authorization.cnr.pip.db.PipAttribute;
import org.ow2.contrail.authorization.cnr.pip.db.PipOwner;
import org.ow2.contrail.authorization.cnr.pip.db.PipSubscriber;
import org.ow2.contrail.authorization.cnr.pip.db.SubscriptionsDAO;
import org.ow2.contrail.authorization.cnr.utils.UconCategory;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

public class SubscriptionsDAOTest {
    private static Logger logger = Logger.getLogger(SubscriptionsDAOTest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    private static String issuer = "PipTest";
    private static String string = "string";

    private PipOwner createA() {
	PipOwner a = new PipOwner();
	a.setName("A");
	a.setAuto(false);
	a.setIssuer(issuer);
	a.setCategory(UconCategory.SUBJECT);
	return a;
    }

    private PipOwner createB() {
	PipOwner a = new PipOwner();
	a.setName("B");
	a.setAuto(false);
	a.setIssuer(issuer);
	a.setCategory(UconCategory.RESOURCE);
	return a;
    }

    private PipAttribute createA1() {
	PipAttribute a1 = new PipAttribute();
	a1.setXacml_id("a1");
	a1.setValue("a1_value");
	a1.setIssuer(issuer);
	a1.setType(string);
	return a1;
    }

    private PipAttribute createA1_2() {
	PipAttribute a1 = new PipAttribute();
	a1.setXacml_id("a1");
	a1.setValue("a1_value2");
	a1.setIssuer(issuer);
	a1.setType(string);
	return a1;
    }

    private PipAttribute createA2() {
	PipAttribute a1 = new PipAttribute();
	a1.setValue("a2_value");
	a1.setXacml_id("a2");
	a1.setIssuer(issuer);
	a1.setType(string);
	return a1;
    }

    private PipAttribute createB1() {
	PipAttribute a1 = new PipAttribute();
	a1.setValue("b1_value");
	a1.setXacml_id("b1");
	a1.setIssuer(issuer);
	a1.setType(string);
	return a1;
    }

    private PipAttribute createB1_2() {
	PipAttribute a1 = new PipAttribute();
	a1.setValue("b1_value2");
	a1.setXacml_id("b1");
	a1.setIssuer(issuer);
	a1.setType(string);
	return a1;
    }

    private PipSubscriber createS1() {
	PipSubscriber s = new PipSubscriber();
	s.setSubscriber("s1");
	return s;
    }

    private PipSubscriber createS2() {
	PipSubscriber s = new PipSubscriber();
	s.setSubscriber("s2");
	return s;
    }

    @Test
    public void test() {

	try {
	    logger.info("subscription");
	    SubscriptionsDAO sDAO = new SubscriptionsDAO(null);

	    PipOwner o1 = createA();
	    o1.addAttribute(createA1());
	    o1.addSubscriber(createS1());
	    PipDataEntity s1 = PipDataEntity.getInstanceFromDao(null, o1);

	    PipOwner o2 = createB();
	    o2.addAttribute(createB1());
	    o2.addSubscriber(createS2());
	    PipDataEntity s2 = PipDataEntity.getInstanceFromDao(null, o2);

	    PipOwner o3 = createA();
	    o3.addAttribute(createA1());
	    o3.addSubscriber(createS2());
	    PipDataEntity s3 = PipDataEntity.getInstanceFromDao(null, o3);

	    PipOwner o4 = createB();
	    o4.addAttribute(createB1());
	    o4.addSubscriber(createS1());
	    PipDataEntity s4 = PipDataEntity.getInstanceFromDao(null, o4);

	    logger.info("adding");
	    sDAO.add(s1);
	    sDAO.add(s2);
	    sDAO.add(s3);
	    sDAO.add(s4);
	    // sDAO.remove(Collections.singletonList(s4), "s1");
	    List<PipDataEntity> list = sDAO.getNotAuto();
	    logger.info("not auto list:");
	    for (PipDataEntity d : list) {
		PipOwner o = d.getDao();
		logger.info(" - " + o.getId() + " " + o.getCategory() + ": " + o.getName() + " (" + o.getIssuer() + ") = " + o.getValue() + " ("
			+ o.getType() + ") ");
		for (PipAttribute a : o.getAttributes()) {
		    logger.info(" | - " + a.getId() + " " + ": " + a.getXacml_id() + " (" + a.getIssuer() + ") = " + a.getValue() + " ("
			    + a.getType() + ") ");
		}
	    }
	    PipOwner o1_2 = createA();
	    o1_2.addAttribute(createA1_2());
	    // o1_2.addSubscriber(createS1());
	    PipDataEntity s1_2 = PipDataEntity.getInstanceFromDao(null, o1_2);
	    PipOwner o4_2 = createB();
	    o4_2.addAttribute(createB1_2());
	    // o4_2.addSubscriber(createS1());
	    PipDataEntity s4_2 = PipDataEntity.getInstanceFromDao(null, o4_2);
	    // PipDataEntity[] latestData = {s1_2, s4_2};
	    PipDataEntity[] latestData = { s1, s4 };
	    List<PipDataEntity> list2 = sDAO.update(list, latestData);
	    logger.info("--------------------------------------");
	    logger.info("updated:");
	    for (PipDataEntity d : list2) {
		PipOwner o = d.getDao();
		logger.info(" - " + o.getId() + " " + o.getCategory() + ": " + o.getName() + " (" + o.getIssuer() + ") = " + o.getValue() + " ("
			+ o.getType() + ") ");
		for (PipAttribute a : o.getAttributes()) {
		    logger.info(" | - " + a.getId() + " " + ": " + a.getXacml_id() + " (" + a.getIssuer() + ") = " + a.getValue() + " ("
			    + a.getType() + ") ");
		}
	    }
	    logger.info("DONE");
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (XacmlSamlException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

}
