package org.ow2.contrail.authorization.cnr.old.core.ucon;

import static org.ow2.contrail.authorization.cnr.utils.UconConstants.VERBOSE_NONE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;
import org.ow2.contrail.authorization.cnr.utils.Communication;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;

public class OngoingTicker {

	public static void main(String[] args) {
		long cycle_pause_duration = 20000;

		// load configuration file
		Properties properties = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(UconConstantsCore.configFile);
			properties.load(in);
		} catch (FileNotFoundException e) {
			LOG(0, " ERROR: unable to find configuration file in " + UconConstantsCore.configFile, VERBOSE_NONE);
		} catch (IOException e) {
			e.printStackTrace();
			LOG(0, " ERROR: while reading configuration file in " + UconConstantsCore.configFile, VERBOSE_NONE);
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
				}
		}
		// value read from configuration file or set to default
		String temp = "";
		if ((temp = properties.getProperty(UconConstantsCore.CYCLE_PAUSE_DURATION)) != null) {
			try {
				cycle_pause_duration = Long.parseLong(temp);
			} catch (NumberFormatException e) {
				LOG(0, "Invalid value for " + UconConstantsCore.CYCLE_PAUSE_DURATION + ". Please, check the configuration file "
						+ UconConstantsCore.configFile, VERBOSE_NONE);
			}
		}

		// try to read debug file
		File configFile = new File(System.getProperty("user.home") + "/testmanagerconfig.properties");
		if (configFile.exists()) {
			properties = new Properties();
			try {
				in = new FileInputStream(configFile);
				properties.load(in);
				if ((temp = properties.getProperty(UconConstantsCore.CYCLE_PAUSE_DURATION)) != null)
					cycle_pause_duration = Long.parseLong(temp);

			} catch (FileNotFoundException e) {
				// It means no debug mode
			} catch (NumberFormatException e) {
				LOG(0, " ERROR: while reading debug configuration file in " + configFile + ". Found invalid number (properties "
						+ "for cycle pause duration are set to " + properties.getProperty(UconConstantsCore.CYCLE_PAUSE_DURATION) + ").\n"
						+ "Default values will be used.", VERBOSE_NONE);
			} catch (IOException e) {
				e.printStackTrace();
				LOG(0, " ERROR: while reading debug configuration file in " + configFile, VERBOSE_NONE);
			} finally {
				if (in != null)
					try {
						in.close();
					} catch (IOException e) {
					}
			}
		}

		ServiceClient synchSc = null;
		try {
			synchSc = new ServiceClient();
		} catch (AxisFault e1) {
		}
		// synchSc.engageModule("addressing");
		Options synchOpts = new Options();
		// setting target EPR
		synchOpts.setTo(new EndpointReference("http://localhost:8080/axis2/services/UconWs"));
		// synchOpts.setTo(new EndpointReference("http://146.48.96.76:8080/axis2/services/UconWs"));
		String actionName = "reevaluation";
		synchOpts.setAction("urn:" + actionName);
		// setting synchronous invocation
		synchOpts.setUseSeparateListener(false);
		synchOpts.setCallTransportCleanup(true); // ?
		// setting created option into service client
		synchSc.setOptions(synchOpts);

		while (true) {
			try {
				LOG(0, " sending reevaluation signal", VERBOSE_NONE);

				synchSc.fireAndForget(Communication.createPayload(UconConstants.UCON_NAMESPACE, actionName));
				synchSc.cleanupTransport();
			} catch (AxisFault e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(cycle_pause_duration);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private static void LOG(int verbosity, String text, int mode) {
		if (mode <= verbosity) {
			System.out.println("[UCON ongoing ticker]: " + text);
		}
	}

}
