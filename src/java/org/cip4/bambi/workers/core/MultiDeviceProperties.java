/*
 *
 * The CIP4 Software License, Version 1.0
 *
 *
 * Copyright (c) 2001-2007 The International Cooperation for the Integration of 
 * Processes in  Prepress, Press and Postpress (CIP4).  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        The International Cooperation for the Integration of 
 *        Processes in  Prepress, Press and Postpress (www.cip4.org)"
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "CIP4" and "The International Cooperation for the Integration of 
 *    Processes in  Prepress, Press and Postpress" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact info@cip4.org.
 *
 * 5. Products derived from this software may not be called "CIP4",
 *    nor may "CIP4" appear in their name, without prior written
 *    permission of the CIP4 organization
 *
 * Usage of this software in commercial products is subject to restrictions. For
 * details please consult info@cip4.org.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE INTERNATIONAL COOPERATION FOR
 * THE INTEGRATION OF PROCESSES IN PREPRESS, PRESS AND POSTPRESS OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the The International Cooperation for the Integration 
 * of Processes in Prepress, Press and Postpress and was
 * originally based on software 
 * copyright (c) 1999-2001, Heidelberger Druckmaschinen AG 
 * copyright (c) 1999-2001, Agfa-Gevaert N.V. 
 *  
 * For more information on The International Cooperation for the 
 * Integration of Processes in  Prepress, Press and Postpress , please see
 * <http://www.cip4.org/>.
 *  
 * 
 */
package org.cip4.bambi.workers.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.core.VElement;


/**
 * container for the properties of Bambi devices
 * 
 * @author boegerni
 */
public class MultiDeviceProperties
{
	/**
	 * properties for a single device
	 * @author boegerni
	 *
	 */
	public static class DeviceProperties {
		private String deviceID=null;
		private String deviceURL=null;
		private String proxyURL=null;
		private String deviceType=null;
		private String deviceProcessorClass=null;
		private String appDir=null;
		
		/**
		 * create device properties
		 * @param theDeviceID   the ID of the device
		 * @param theDeviceURL  the URL of the device
		 * @param theProxyURL   the URL of its proxy/controller
		 * @param theDeviceType the device type (e.g. "General Foo Device")
		 * @param theDeviceProcessorClass the class of the device (e.g. "org.cip4.bambi.workers.sim.SimWorker")
		 * @param theAppDir     the location of the web app (e.g. "C:/tomcat/webapps/bambi")
		 */
		public DeviceProperties(String theDeviceID, String theDeviceURL, String theProxyURL,
				String theDeviceType, String theDeviceProcessorClass, String theAppDir) {
			setDeviceID(theDeviceID);
			setDeviceURL(theDeviceURL);
			setProxyURL(theProxyURL);
			setDeviceType(theDeviceType);
			setDeviceProcessorClass(theDeviceProcessorClass);
			setAppDir(theAppDir);
		}

		/**
		 * @param deviceURL the deviceURL to set
		 */
		public void setDeviceURL(String deviceURL) {
			this.deviceURL = deviceURL;
		}

		/**
		 * @return the deviceURL
		 */
		public String getDeviceURL() {
			return deviceURL;
		}

		/**
		 * @param deviceID the deviceID to set
		 */
		private void setDeviceID(String deviceID) {
			this.deviceID = deviceID;
		}

		/**
		 * @return the deviceID
		 */
		public String getDeviceID() {
			return deviceID;
		}

		/**
		 * @param controllerURL the controllerURL to set
		 */
		private void setProxyURL(String controllerURL) {
			this.proxyURL = controllerURL;
		}

		/**
		 * @return the controllerURL
		 */
		public String getProxyURL() {
			return proxyURL;
		}

		/**
		 * @param deviceType the deviceType to set
		 */
		private void setDeviceType(String deviceType) {
			this.deviceType = deviceType;
		}

		/**
		 * @return the deviceType
		 */
		public String getDeviceType() {
			return deviceType;
		}

		/**
		 * @param deviceProcessorClass the deviceProcessorClass to set
		 */
		private void setDeviceProcessorClass(String deviceProcessorClass) {
			this.deviceProcessorClass = deviceProcessorClass;
		}

		/**
		 * @return the deviceProcessorClass
		 */
		public String getDeviceProcessorClass() {
			return deviceProcessorClass;
		}

		/**
		 * @param appDir the location of the web application on the hard disk
		 */
		private void setAppDir(String appDir) {
			this.appDir = appDir;
		}

		/**
		 * @return the appURL
		 */
		public String getAppDir() {
			return appDir;
		}
	}
	
	protected static final Log log = LogFactory.getLog(MultiDeviceProperties.class.getName());
	private Map _devices=null;

	/**
	 * create device properties for the devices defined in the config file
	 * @param appDir TODO
	 * @param fileName full path to the config file
	 */
	public MultiDeviceProperties(String appDir, String fileName) {
		_devices = new HashMap();
		
		File f = new File(fileName);
		if (f==null || !f.exists()) {
			log.fatal("config file '"+fileName+"' does not exist");
		}
		
		JDFParser p = new JDFParser();
	    JDFDoc doc = p.parseFile(fileName);
	    if (doc == null) {
	    	log.fatal( "failed to parse "+fileName );
	    	return;
	    }
	    
	    KElement e = doc.getRoot();
	    if (e==null) {
	    	log.fatal( "failed to parse "+fileName+", root is null" );
	    	return;
	    }
	    VElement v = e.getXPathElementVector("//devices/*", 99);
	    for (int i = 0; i < v.size(); i++)
	    {
	    	KElement device = (KElement)v.elementAt(i);
	    	String deviceID = device.getXPathAttribute("@DeviceID", null);
	    	if (deviceID==null) {
	    		log.error("cannot create device without device ID");
	    		break;
	    	}
	    	String deviceType = device.getXPathAttribute("@DeviceType", null);
	    	String deviceURL = device.getXPathAttribute("@DeviceURL", "");
	    	String controllerURL = device.getXPathAttribute("@ControllerURL", "");
	    	String deviceProcessorClass = device.getXPathAttribute("@DeviceProcessorClass", "");

	    	DeviceProperties dev = new DeviceProperties(deviceID,deviceURL,controllerURL,
	    			deviceType,deviceProcessorClass, appDir);
	    	_devices.put(deviceID, dev);
	    }
	}
	
	/**
	 * get the number of the devices
	 * @return the number of devices, zero if no devices have been found
	 */
	public int count() {
		return _devices.size();
	}
	
	/**
	 * get the a Set with the device IDs of all device properties stored
	 * @return a Set of device IDs, an empty set of nothing has been found
	 */
	public Set getDeviceIDs() {
		return _devices.keySet();
	}
	
	/**
	 * get the device properties for a single device
	 * @param deviceID the device ID of the device to look for
	 * @return the device properties, null if not found
	 */
	public DeviceProperties getDevice(String deviceID) {
		return (DeviceProperties)_devices.get(deviceID);
	}

}