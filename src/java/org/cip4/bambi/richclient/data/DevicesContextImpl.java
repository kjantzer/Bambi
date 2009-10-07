/**
 * The CIP4 Software License, Version 1.0
 *
 * Copyright (c) 2001-2009 The International Cooperation for the Integration of 
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
package org.cip4.bambi.richclient.data;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.List;

import org.cip4.bambi.richclient.model.Device;
import org.cip4.bambi.richclient.model.DeviceList;
import org.cip4.bambi.richclient.model.Queue;
import org.cip4.bambi.richclient.value.DeviceListVO;
import org.cip4.bambi.richclient.value.DeviceVO;
import org.cip4.bambi.richclient.value.MsgSubscriptionVO;
import org.cip4.bambi.richclient.value.QueueVO;
import org.cip4.bambi.richclient.value.SubscriptionListVO;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.xml.XMLContext;

/**
 * Singlton class manages data caching and session based periodical updating form bambi.
 * @author smeissner
 * @date 30.09.2009
 */
class DevicesContextImpl implements DevicesContext, Runnable {

	private static final String PATH_MAPPING_FILE = "/org/cip4/bambi/richclient/mapping.xml";

	private static final String URL_DEVICES_OVERVIEW = "http://localhost:8080/richworker/overview";

	private static final String URL_DEVICES_QUEUE_ROOT = "http://localhost:8080/richworker/showQueue/";

	private static final String URL_DEVICES_SUBSCRIPTIONS = "http://localhost:8080/richworker/showSubscriptions/";

	private static final int UPDATE_INTERVAL = 2000;

	private static final String THREAD_UPDATER_NAME = "threadUpdater";

	private final Thread threadCacheUpdater;

	/**
	 * caches the device list
	 */
	private DeviceList cacheDeviceList;

	/**
	 * caches the queue for a device
	 */
	private final Hashtable<String, LazyQueue> cacheLazyQueues;

	/**
	 * Package private default constructor. Initializing component.
	 */
	DevicesContextImpl() {
		// initialize caches
		cacheLazyQueues = new Hashtable<String, LazyQueue>();

		// load devices / queues
		try {
			updateDevices();
			updateQueueEntries();
		} catch (Exception e) {
			throw new Error(e);
		}

		// initialize cacheUpdater thread
		threadCacheUpdater = new Thread(this);
		threadCacheUpdater.setName(THREAD_UPDATER_NAME);
		threadCacheUpdater.setDaemon(true);
		threadCacheUpdater.start();
	}

	/**
	 * @see org.cip4.bambi.richclient.data.DevicesContext#getDeviceList()
	 */
	public DeviceList getDeviceList() {
		return cacheDeviceList;
	}

	/**
	 * @see org.cip4.bambi.richclient.data.DevicesContext#getDevice(java.lang.String, java.lang.String)
	 */
	public Device getDevice(String deviceId, String sessionId) {
		// find device
		Device device = cacheDeviceList.findDeviceById(deviceId);

		// get lazy queue
		LazyQueue lazyQueue = cacheLazyQueues.get(deviceId);

		return new Device.Builder(device).queue(lazyQueue.getQueue()).build();
	}

	/**
	 * @see org.cip4.bambi.richclient.data.DevicesContext#getDeviceDiff(java.lang.String, java.lang.String)
	 */
	public Device getDeviceDiff(String deviceId, String sessionId) {
		// TODO Implement lazy loading
		return getDevice(deviceId, sessionId);
	}

	/**
	 * @see org.cip4.bambi.richclient.data.DevicesContext#queueClose(java.lang.String)
	 */
	public void queueClose(String deviceId) {
		// control parameter
		String ctlParam = "close=true";

		// submit
		submitQueueParameter(deviceId, ctlParam);
	}

	/**
	 * @see org.cip4.bambi.richclient.data.DevicesContext#queueFlush(java.lang.String)
	 */
	public void queueFlush(String deviceId) {
		// control parameter
		String ctlParam = "flush=true";

		// submit
		submitQueueParameter(deviceId, ctlParam);
	}

	/**
	 * @see org.cip4.bambi.richclient.data.DevicesContext#queueHold(java.lang.String)
	 */
	public void queueHold(String deviceId) {
		// control parameter
		String ctlParam = "hold=true";

		// submit
		submitQueueParameter(deviceId, ctlParam);
	}

	/**
	 * @see org.cip4.bambi.richclient.data.DevicesContext#queueOpen(java.lang.String)
	 */
	public void queueOpen(String deviceId) {
		// control parameter
		String ctlParam = "open=true";

		// submit
		submitQueueParameter(deviceId, ctlParam);
	}

	/**
	 * @see org.cip4.bambi.richclient.data.DevicesContext#queueResume(java.lang.String)
	 */
	public void queueResume(String deviceId) {
		// control parameter
		String ctlParam = "resume=true";

		// submit
		submitQueueParameter(deviceId, ctlParam);
	}

	/**
	 * Updates cache periodically.
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while (threadCacheUpdater != null) {

			try {
				// update devices
				updateDevices();

				// update queue entries
				updateQueueEntries();

				// sleep
				Thread.sleep(UPDATE_INTERVAL);
			} catch (Exception e) {
				// throw new error
				// throw new Error(e);
			}

		}
	}

	/**
	 * Updates devices list in cache.
	 * @throws MappingException
	 * @throws IOException
	 * @throws ValidationException
	 * @throws MarshalException
	 */
	private void updateDevices() throws Exception {

		DeviceListVO lstVO;

		// open connection and load stream
		URL url = new URL(URL_DEVICES_OVERVIEW);
		URLConnection cn = url.openConnection();
		cn.setDoOutput(true);

		// load device list details
		lstVO = (DeviceListVO) unmarshal(new InputStreamReader(cn.getInputStream()), DeviceListVO.class);

		// set subscriptions
		for (DeviceVO deviceVO : lstVO.getDevices()) {
			deviceVO.setMsgSubscriptions(updateSubscriptions(deviceVO.getId()));
		}

		// map to DeviceList and cache
		cacheDeviceList = new DeviceList.Builder(lstVO).build();
	}

	/**
	 * Update subscriptions for a device.
	 * @param deviceId
	 * @return
	 * @throws Exception
	 */
	private List<MsgSubscriptionVO> updateSubscriptions(String deviceId) throws Exception {
		// open connection and load stream
		URL url = new URL(URL_DEVICES_SUBSCRIPTIONS + deviceId);
		URLConnection cn = url.openConnection();
		cn.setDoOutput(true);

		// load device list details
		SubscriptionListVO lstVO = (SubscriptionListVO) unmarshal(new InputStreamReader(cn.getInputStream()), SubscriptionListVO.class);

		// return MsgSubscriptionsVOs
		return lstVO.getMsgSubscriptions();
	}

	/**
	 * Updates all queue entries of all devices and caches them.
	 * @throws MappingException
	 * @throws IOException
	 * @throws ValidationException
	 * @throws MarshalException
	 */
	private void updateQueueEntries() throws MarshalException, ValidationException, IOException, MappingException {
		// check for devices
		if (cacheDeviceList == null || cacheDeviceList.getDevices().size() == 0) {
			return;
		}

		// iterate over all devices
		for (Device device : cacheDeviceList.getDevices()) {
			// build url (URL_DEVICES_QUEUE_ROOT + deviceId)
			String sUrl = URL_DEVICES_QUEUE_ROOT + device.getId();

			// open connection and load stream
			URL url = new URL(sUrl);
			URLConnection cn = url.openConnection();
			cn.setDoOutput(true);

			// load device list details
			QueueVO queueVO = (QueueVO) unmarshal(new InputStreamReader(cn.getInputStream()), QueueVO.class);

			// build Queue object
			Queue queue = new Queue.Builder(queueVO).build();

			// cache as lazy queue
			LazyQueue lazyQueue;

			if (cacheLazyQueues.containsKey(queue.getDeviceId())) {
				// if exists, get queue
				lazyQueue = cacheLazyQueues.get(queue.getDeviceId());
			} else {
				// if NOT exists, create new object
				lazyQueue = new LazyQueue();

				// put into cache
				cacheLazyQueues.put(queue.getDeviceId(), lazyQueue);
			}

			// store queue
			lazyQueue.put(queue);
		}
	}

	/**
	 * Submit queue control parameter to bambi.
	 * @param deviceId DeviceId of queue
	 * @param parameter parameter to control
	 */
	void submitQueueParameter(String deviceId, String parameter) {
		// build control url
		String sUrl = URL_DEVICES_QUEUE_ROOT + deviceId + "?" + parameter;

		try {
			// open connection and load stream
			URL url = new URL(sUrl);
			URLConnection cn = url.openConnection();
			cn.setDoOutput(true);
			cn.getInputStream();
		} catch (Exception ex) {

		}
	}

	/**
	 * Unmarshalls a xml file to object model.
	 * @param reader stream to unmarshal
	 * @param clazz class type
	 * @return object of type clazz including all details
	 * @throws MappingException
	 * @throws IOException
	 * @throws ValidationException
	 * @throws MarshalException
	 */
	synchronized Object unmarshal(Reader reader, Class clazz) throws IOException, MappingException, MarshalException, ValidationException {
		// load mapping
		Mapping mapping = new Mapping();
		mapping.loadMapping(DevicesContextImpl.class.getResource(PATH_MAPPING_FILE));

		// initialize and configure XMLContext
		XMLContext context = new XMLContext();
		context.addMapping(mapping);

		// create a new unmarshaller
		Unmarshaller unmarshaller = context.createUnmarshaller();
		unmarshaller.setClass(clazz);

		// return unmarshalled object
		return unmarshaller.unmarshal(reader);
	}

}