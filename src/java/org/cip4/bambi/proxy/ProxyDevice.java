/*
 *
 * The CIP4 Software License, Version 1.0
 *
 *
 * Copyright (c) 2001-2010 The International Cooperation for the Integration of 
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

package org.cip4.bambi.proxy;

import java.util.Vector;

import org.cip4.bambi.core.AbstractDeviceProcessor;
import org.cip4.bambi.core.BambiNSExtension;
import org.cip4.bambi.core.IDeviceProperties;
import org.cip4.bambi.core.StatusListener;
import org.cip4.bambi.core.messaging.JMFHandler;
import org.cip4.bambi.core.messaging.SignalHandler;
import org.cip4.bambi.core.messaging.JMFHandler.AbstractHandler;
import org.cip4.bambi.core.queues.IQueueEntry;
import org.cip4.jdflib.auto.JDFAutoDeviceInfo.EnumDeviceStatus;
import org.cip4.jdflib.auto.JDFAutoNotification.EnumClass;
import org.cip4.jdflib.auto.JDFAutoQueueEntry.EnumQueueEntryStatus;
import org.cip4.jdflib.core.AttributeName;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.core.VElement;
import org.cip4.jdflib.core.JDFElement.EnumNodeStatus;
import org.cip4.jdflib.jmf.JDFDeviceInfo;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFJobPhase;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.jmf.JDFRequestQueueEntryParams;
import org.cip4.jdflib.jmf.JDFResourceQuParams;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFReturnQueueEntryParams;
import org.cip4.jdflib.jmf.JDFStatusQuParams;
import org.cip4.jdflib.jmf.JMFBuilder;
import org.cip4.jdflib.jmf.JDFMessage.EnumFamily;
import org.cip4.jdflib.jmf.JDFMessage.EnumType;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.node.NodeIdentifier;
import org.cip4.jdflib.util.StatusCounter;
import org.cip4.jdflib.util.StringUtil;

/**
 * @author Dr. Rainer Prosi, Heidelberger Druckmaschinen AG
 * 
 * 04.12.2008
 */
public class ProxyDevice extends AbstractProxyDevice
{
	/**
	 * class that handles merging of messages
	 */
	protected class ResponseMerger
	{

		/**
		 * 
		 * @param m
		 * @return
		 */
		protected JDFDoc getStatusResponse(final JDFMessage m)
		{
			final Vector<StatusListener> counters = getStatusListeners();
			if (counters == null)
			{
				return null;
			}
			JDFDoc dReturnPhaseTime = null;
			JDFResponse response = null;
			boolean first = true;
			for (int i = 0; i < counters.size(); i++)
			{
				final StatusListener listener = counters.elementAt(i);
				if (!listener.matchesQuery(m))
				{
					continue;
				}
				final EnumDeviceStatus subStatus = listener.getDeviceStatus();
				if (subStatus == null || EnumDeviceStatus.Idle.equals(subStatus))
				{
					continue; // skip waiting in queue or idle
				}

				final StatusCounter counter = listener.getStatusCounter();
				if (dReturnPhaseTime == null)
				{
					dReturnPhaseTime = counter.getDocJMFPhaseTime();
					response = dReturnPhaseTime.getJMFRoot().getResponse(0);
				}

				final JDFDoc docJMFPhaseTime = counter.getDocJMFPhaseTime();
				if (docJMFPhaseTime == null || response == null)
				{
					continue;
				}
				if (!first)
				{
					final JDFResponse response2 = docJMFPhaseTime.getJMFRoot().getResponse(0);
					final JDFDeviceInfo di2 = response2.getDeviceInfo(0);
					if (di2 == null)
					{
						getLog().warn("Counter Status Response with null deviceInfo???");
					}
					else
					{
						final String devID = di2.getDeviceID();
						final JDFDeviceInfo di3 = KElement.isWildCard(devID) ? null : (JDFDeviceInfo) response.getChildWithAttribute(ElementName.DEVICEINFO, AttributeName.DEVICEID, null, devID, 0, true);
						if (di3 != null)
						{
							final VElement phases = di2.getChildElementVector(ElementName.JOBPHASE, null, null, true, -1, false);
							for (int j = 0; j < phases.size(); j++)
							{
								di3.copyElement(phases.elementAt(j), null);
							}
						}
						else
						{
							response.copyElement(di2, null);
						}
					}
				}
				first = false;
			}
			return dReturnPhaseTime;
		}

		/**
		 * @param m the query input message
		 * @param response 
		 * @return JDFDoc the response message
		 */
		public boolean fillResourceResponse(final JDFMessage m, final JDFResponse response)
		{
			final Vector<StatusListener> counters = getStatusListeners();
			if (counters == null)
			{
				return false;
			}
			boolean bRet = false;
			for (int i = 0; i < counters.size(); i++)
			{
				final StatusListener listener = counters.elementAt(i);
				if (!listener.matchesQuery(m))
				{
					continue;
				}

				final StatusCounter counter = listener.getStatusCounter();
				final JDFDoc docJMFResource = counter.getDocJMFResource();
				if (docJMFResource == null)
				{
					continue;
				}
				JDFMessage response2 = docJMFResource.getJMFRoot().getResponse(0);
				JDFResourceQuParams signalRQP = null;
				if (response2 == null)
				{
					response2 = docJMFResource.getJMFRoot().getSignal(0);
					if (response2 != null)
					{
						signalRQP = response2.getResourceQuParams();
						if (signalRQP != null)
						{
							final JDFResourceQuParams queryRQP = m.getResourceQuParams();
							if (queryRQP == null)
							{
								m.copyElement(signalRQP, null);
							}
							else
							{
								queryRQP.mergeElement(signalRQP, false);
							}
						}
					}
				}
				final VElement v = response2 == null ? null : response2.getChildElementVector(ElementName.RESOURCEINFO, null);
				if (v != null)
				{
					final int riSize = v.size();
					for (int ii = 0; ii < riSize; ii++)
					{
						response.moveElement(v.get(ii), null);
						bRet = true; // we have one
					}
				}
			}
			return bRet;
		}

		/**
		 * @param m
		 * @param resp
		 * @return true if a notification was filled
		 */
		public boolean fillNotifications(final JDFMessage m, final JDFResponse resp)
		{
			final Vector<ProxyDeviceProcessor> procs = getProxyProcessors();
			if (procs == null)
			{
				return false;
			}
			boolean bRet = false;
			final JDFJMF jmfm = m.getJMFRoot();
			//			final JDFJMF jmfr = resp.getJMFRoot();
			for (int i = 0; i < procs.size(); i++)
			{
				bRet = procs.get(i).handleNotificationQuery(m, resp) || bRet;
				// undo handler delete
				jmfm.moveElement(m, null);
				jmfm.moveElement(resp, null);
			}
			// final zapp om m and r from list of signals
			m.deleteNode();
			resp.deleteNode();
			return bRet;

		}
	} // end of inner class MultiCounter

	// ////////////////////////////////////////////////////////////////////////////////

	protected ResponseMerger statusContainer;

	/**
	 * simple dispatcher
	 * @author prosirai
	 */
	protected class RequestQueueEntryHandler extends AbstractHandler
	{
		protected int numSubmitThread;
		protected SubmitThread submitThread;

		private class SubmitThread extends Thread
		{
			/**
			 * @param iqe
			 * @param queueURL
			 */
			public SubmitThread(IQueueEntry iqe, String queueURL)
			{
				super("RequestQE_" + getDeviceID() + "_" + numSubmitThread++);
				this.iqe = iqe;
				this.queueURL = queueURL;
			}

			IQueueEntry iqe;
			String queueURL;

			/**
			 * @see java.lang.Thread#run()
			*/
			@Override
			public void run()
			{
				getLog().info("submitting for RequestQE");
				submitQueueEntry(iqe, queueURL);
				submitThread = null; // now we are done...
			}
		}

		public RequestQueueEntryHandler()
		{
			super(EnumType.RequestQueueEntry, new EnumFamily[] { EnumFamily.Command });
			numSubmitThread = 0;
		}

		/**
		 * 
		 * @see org.cip4.bambi.core.messaging.JMFHandler.AbstractHandler#handleMessage(org.cip4.jdflib.jmf.JDFMessage, org.cip4.jdflib.jmf.JDFResponse)
		 * @param m
		 * @param resp
		 * @return
		 */
		@Override
		public boolean handleMessage(final JDFMessage m, final JDFResponse resp)
		{
			if (m == null)
			{
				return false;
			}
			// TODO retain rqe in case we cannot submit now
			// check for valid RequestQueueEntryParams
			final JDFRequestQueueEntryParams qep = m.getRequestQueueEntryParams(0);
			if (qep == null)
			{
				JMFHandler.errorResponse(resp, "QueueEntryParams missing in RequestQueueEntry message", 7, EnumClass.Error);
				return true;
			}
			final String queueURL = qep.getQueueURL();
			if (queueURL == null || queueURL.length() < 1)
			{
				JMFHandler.errorResponse(resp, "QueueURL is missing", 7, EnumClass.Error);
				return true;
			}
			if (submitThread != null)
			{
				JMFHandler.errorResponse(resp, "Currently handling requestQueueEntry, try again later", 10, EnumClass.Warning);
				return true;
			}

			final NodeIdentifier nid = new NodeIdentifier(qep.getJobID(), qep.getJobPartID(), qep.getPartMapVector());
			// submit a specific QueueEntry
			final IQueueEntry iqe = getQueueProcessor().getWaitingQueueEntry(nid);
			final JDFQueueEntry qe = iqe == null ? null : iqe.getQueueEntry();
			if (qe != null && EnumQueueEntryStatus.Waiting.equals(qe.getQueueEntryStatus()) && KElement.isWildCard(qe.getDeviceID()))
			{
				qe.setDeviceID(m.getSenderID());
				submitThread = new SubmitThread(iqe, queueURL);
				submitThread.start();
			}
			else if (qe == null)
			{
				JMFHandler.errorResponse(resp, "No QueueEntry is available for request", 108, EnumClass.Error);
			}
			else
			{
				final String qeStatus = qe.getQueueEntryStatus().getName();
				JMFHandler.errorResponse(resp, "requested QueueEntry is " + qeStatus + " or on Device: " + qe.getDeviceID(), 106, EnumClass.Error);
			}
			return true;
		}
	}

	protected class ReturnQueueEntryHandler extends AbstractHandler
	{

		public ReturnQueueEntryHandler()
		{
			super(EnumType.ReturnQueueEntry, new EnumFamily[] { EnumFamily.Command });
		}

		/**
		 * 
		 * @see org.cip4.bambi.core.messaging.JMFHandler.AbstractHandler#handleMessage(org.cip4.jdflib.jmf.JDFMessage, org.cip4.jdflib.jmf.JDFResponse)
		 * @param m
		 * @param resp
		 * @return
		 */
		@Override
		public boolean handleMessage(final JDFMessage m, final JDFResponse resp)
		{
			if (m == null)
			{
				return false;
			}
			log.debug("Handling " + m.getType());
			final JDFReturnQueueEntryParams retQEParams = m.getReturnQueueEntryParams(0);
			if (retQEParams == null)
			{
				JMFHandler.errorResponse(resp, "ReturnQueueEntryParams missing in ReturnQueueEntry message", 7, EnumClass.Error);
				return true;
			}
			final String slaveQueueEntryID = StringUtil.getNonEmpty(retQEParams.getQueueEntryID());

			JDFDoc theDoc = retQEParams.getURLDoc();
			if (_slaveCallback != null)
			{
				theDoc = _slaveCallback.prepareJDFForBambi(theDoc);
			}
			if (theDoc == null)
			{
				log.error("No returned JDF in ReturnQueueEntry message: for slave queueentry: " + slaveQueueEntryID);
				//				JMFHandler.errorResponse(resp, "No returned JDF in ReturnQueueEntry message", 7, EnumClass.Error);
				//				return true;
			}

			final ProxyDeviceProcessor proc = getProcessorForReturnQE(retQEParams, resp, theDoc);
			if (proc != null)
			{
				final JDFQueueEntry qeBambi = proc.getQueueEntry();
				getQueueProcessor().extractFiles(qeBambi, theDoc);
				proc.returnFromSlave(m, resp, theDoc);
			}
			return true;
		}

		/**
		 * @param rqp 
		 * @param resp 
		 * @param theDoc 
		 * @return the ProxyDeviceProcessor that handles messages from slaveQEID
		 */
		protected ProxyDeviceProcessor getProcessorForReturnQE(final JDFReturnQueueEntryParams rqp, final JDFResponse resp, JDFDoc theDoc)
		{
			final String slaveQEID = rqp == null ? null : rqp.getQueueEntryID();
			ProxyDeviceProcessor proc = getProcessorForSlaveQEID(slaveQEID);

			NodeIdentifier nid = null;
			if (proc == null && rqp != null)
			{
				final JDFNode node = theDoc.getJDFRoot();
				nid = node == null ? null : node.getIdentifier();
				proc = getProcessorForNID(nid);
			}
			if (proc == null)
			{
				final String errorMsg = "QueueEntry with slave QueueEntryID = " + slaveQEID + ", job identifier: " + nid == null ? " - " : nid + " is not being processed";
				JMFHandler.errorResponse(resp, errorMsg, 2, EnumClass.Error);
			}
			return proc;
		}
	}

	protected class StatusQueryHandler extends StatusHandler
	{
		public StatusQueryHandler()
		{
			super();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.cip4.bambi.IMessageHandler#handleMessage(org.cip4.jdflib.jmf. JDFMessage, org.cip4.jdflib.jmf.JDFMessage)
		 */
		@Override
		public boolean handleMessage(final JDFMessage m, final JDFResponse resp)
		{
			if (statusContainer == null)
			{
				return false;
			}

			final JDFDoc docJMF = statusContainer.getStatusResponse(m);

			if (docJMF == null)
			{
				return super.handleMessage(m, resp); // idle is handled by super
			}

			final boolean bOK = copyPhaseTimeFromCounter(resp, docJMF);
			if (bOK)
			{
				addQueueToStatusResponse(m, resp);
			}
			return bOK;
		}

	}

	protected class NotificationQueryHandler extends AbstractHandler
	{
		public NotificationQueryHandler()
		{
			super(EnumType.Notification, new EnumFamily[] { EnumFamily.Query });
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.cip4.bambi.IMessageHandler#handleMessage(org.cip4.jdflib.jmf. JDFMessage, org.cip4.jdflib.jmf.JDFMessage)
		 */
		@Override
		public boolean handleMessage(final JDFMessage m, final JDFResponse resp)
		{
			if (statusContainer == null)
			{
				return false;
			}

			return statusContainer.fillNotifications(m, resp);
		}
	}// end of inner class NotificationQueryHandler

	protected class ResourceQueryHandler extends ResourceHandler
	{
		public ResourceQueryHandler()
		{
			super();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.cip4.bambi.IMessageHandler#handleMessage(org.cip4.jdflib.jmf. JDFMessage, org.cip4.jdflib.jmf.JDFMessage)
		 */
		@Override
		public boolean handleMessage(final JDFMessage m, final JDFResponse resp)
		{
			if (statusContainer == null)
			{
				return false;
			}

			final boolean bHandeled = statusContainer.fillResourceResponse(m, resp);
			return bHandeled ? bHandeled : super.handleMessage(m, resp);
		}
	}// end of inner class ResourceQueryHandler

	// //////////////////////////////////////////////////////////////////
	protected class NotificationSignalHandler extends SignalHandler
	{
		public NotificationSignalHandler()
		{
			super(ProxyDevice.this, EnumType.Notification, new EnumFamily[] { EnumFamily.Signal });
		}

		@Override
		public boolean handleMessage(final JDFMessage m, final JDFResponse resp)
		{
			if (!super.handleMessage(m, resp))
			{
				return false;
			}
			log.debug("Handling " + m.getType() + " " + m.getID());
			final Vector<ProxyDeviceProcessor> v = getProxyProcessors();
			boolean b = false;
			if (v != null)
			{
				final int size = v.size();
				for (int i = 0; i < size; i++)
				{
					b = v.get(i).handleNotificationSignal(m, resp) || b;
				}
			}
			return true; // handled if any was ok
		}
	}// end of inner class ResourceQueryHandler

	// //////////////////////////////////////////////////////////////////
	protected class ResourceSignalHandler extends SignalHandler
	{
		public ResourceSignalHandler()
		{
			super(ProxyDevice.this, EnumType.Resource, new EnumFamily[] { EnumFamily.Signal });
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.cip4.bambi.IMessageHandler#handleMessage(org.cip4.jdflib.jmf. JDFMessage, org.cip4.jdflib.jmf.JDFMessage)
		 */
		@Override
		public boolean handleMessage(final JDFMessage m, final JDFResponse resp)
		{
			if (!super.handleMessage(m, resp))
			{
				return false;
			}
			log.debug("Handling " + m.getType() + " " + m.getID());
			final Vector<ProxyDeviceProcessor> v = getProxyProcessors();
			boolean b = false;
			if (v != null)
			{
				final int size = v.size();
				for (int i = 0; i < size; i++)
				{
					b = v.get(i).handleResourceSignal(m, resp) || b;
				}
			}
			return true; // handled if any was ok
		}
	}// end of inner class ResourceQueryHandler

	// //////////////////////////////////////////////////////////////////
	/**
	 * for now a dummy
	 */
	protected class QueueStatusSignalHandler extends SignalHandler
	{

		public QueueStatusSignalHandler()
		{
			super(ProxyDevice.this, EnumType.QueueStatus, new EnumFamily[] { EnumFamily.Signal });
		}

		/**
		 * @see SignalHandler#handleMessage(org.cip4.jdflib.jmf.JDFMessage, org.cip4.jdflib.jmf.JDFResponse)
		 * @param inputMessage
		 * @param response
		 * @return
		 * TODO implement...
		*/
		@Override
		public boolean handleMessage(JDFMessage inputMessage, JDFResponse response)
		{
			super.handleMessage(inputMessage, response);
			response.setReturnCode(0);
			return true;
		}
	}

	// //////////////////////////////////////////////////////////////////
	protected class StatusSignalHandler extends SignalHandler
	{

		public StatusSignalHandler()
		{
			super(ProxyDevice.this, EnumType.Status, new EnumFamily[] { EnumFamily.Signal });
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.cip4.bambi.IMessageHandler#handleMessage(org.cip4.jdflib.jmf. JDFMessage, org.cip4.jdflib.jmf.JDFMessage)
		 */
		@Override
		public boolean handleMessage(final JDFMessage m, final JDFResponse resp)
		{
			if (!super.handleMessage(m, resp))
			{
				return false;
			}
			log.debug("Handling " + m.getType() + " " + m.getID());
			final Vector<ProxyDeviceProcessor> v = getProxyProcessors();
			boolean b = false;
			if (v != null)
			{
				final int size = v.size();
				for (int i = 0; i < size; i++)
				{
					b = v.get(i).handleStatusSignal(m, resp) || b;
				}
			}
			if (!b)
			{
				final JDFStatusQuParams sqp = m.getStatusQuParams();
				final String qeid = (sqp != null) ? sqp.getQueueEntryID() : null;
				NodeIdentifier ni = sqp == null ? null : sqp.getIdentifier();
				if (ni == null)
				{
					JDFDeviceInfo di = m.getDeviceInfo(0);
					JDFJobPhase jp = di == null ? null : di.getJobPhase(0);
					ni = jp == null ? null : jp.getIdentifier();
				}
				if (KElement.isWildCard(qeid) && new NodeIdentifier().equals(ni))
				{
					b = handleIdle(m, resp);
				}

				if (!b)
				{
					final ProxyDeviceProcessor dp = getProcessorForSignal(qeid, ni);
					if (dp != null)
					{
						b = dp.handleStatusSignal(m, resp) || b;
					}
				}
				if (!b)
				{
					JMFHandler.errorResponse(resp, "Unknown QueueEntry: " + qeid, 103, EnumClass.Error);
				}
			}
			else
			{
				resp.setReturnCode(0);
			}

			return true; // handled if any was ok
		}

		/**
		 * @param m 
		 * @param resp 
		 * @return true if handled
		 */
		private boolean handleIdle(final JDFMessage m, final JDFResponse resp)
		{
			log.debug("handling idle status signal...");
			return true;
		}
	}// end of inner class StatusSignalHandler

	// //////////////////////////////////////////////////////////////////////////
	// ///////

	// ////////////////////////////////////////////////////////////////

	/**
	 * @param properties
	 */
	public ProxyDevice(final IDeviceProperties properties)
	{
		super(properties);
		final IProxyProperties proxyProperties = getProxyProperties();
		statusContainer = new ResponseMerger();
		_jmfHandler.setFilterOnDeviceID(false);
		final int maxPush = proxyProperties.getMaxPush();
		if (maxPush > 0)
		{
			_theQueueProcessor.getQueue().setMaxRunningEntries(maxPush);
			// TODO correctly dispatch them
		}
	}

	/**
	 * @return vector of status listeners
	 */
	public Vector<StatusListener> getStatusListeners()
	{
		final Vector<ProxyDeviceProcessor> procs = getProxyProcessors();
		if (procs == null)
		{
			return null;
		}
		final int size = procs.size();
		if (size == 0)
		{
			return null;
		}
		final Vector<StatusListener> v = new Vector<StatusListener>(size);
		for (int i = 0; i < size; i++)
		{
			final ProxyDeviceProcessor pd = procs.get(i);
			final StatusListener l = pd.getStatusListener();
			if (l != null)
			{
				v.add(l);
			}
		}
		return v.size() == 0 ? null : v;

	}

	@Override
	protected void addHandlers()
	{
		super.addHandlers();
		_jmfHandler.addHandler(this.new RequestQueueEntryHandler());
		_jmfHandler.addHandler(this.new ReturnQueueEntryHandler());
		_jmfHandler.addHandler(this.new QueueStatusSignalHandler());
		_jmfHandler.addHandler(this.new StatusSignalHandler());
		_jmfHandler.addHandler(this.new StatusQueryHandler());
		_jmfHandler.addHandler(this.new ResourceQueryHandler());
		_jmfHandler.addHandler(this.new ResourceSignalHandler());
		_jmfHandler.addHandler(this.new NotificationSignalHandler());
		_jmfHandler.addHandler(this.new NotificationQueryHandler());
	}

	/**
	 * @param iqe
	 * @param queueURL
	 * @return true if the processor is added
	 */
	public ProxyDeviceProcessor submitQueueEntry(final IQueueEntry iqe, final String queueURL)
	{
		ProxyDeviceProcessor pdp = new ProxyDeviceProcessor(this, _theQueueProcessor, iqe);
		final boolean submit = pdp.submit(queueURL);
		if (submit && pdp.isActive())
		{
			addProcessor(pdp);
		}
		else
		{
			pdp = null;
		}
		return pdp;
	}

	/**
	 * returns null, since the ProxyDevice doesn't need a DeviceProcessor
	 */
	@Override
	protected AbstractDeviceProcessor buildDeviceProcessor()
	{
		if (getProxyProperties().getMaxPush() <= 0)
		{
			return null;
		}
		return new ProxyDispatcherProcessor(this);
	}

	/**
	 * get a simple file name for the JDF in a queueentry
	 * @param qe the JDFQueueEntry to name mangle
	 * @return the file name for the jdf
	 */
	public String getNameFromQE(final JDFQueueEntry qe)
	{
		return "q" + qe.getQueueEntryID() + ".jdf";
	}

	/**
	 * remove a processor from the list of active processors
	 * @param processor
	 */
	public void removeProcessor(final AbstractDeviceProcessor processor)
	{
		log.info("removing device proceesor");
		_deviceProcessors.remove(processor);
		final StatusListener statusListener = processor.getStatusListener();
		// zapp the subscription that we added for listening to the device
		// TODO
		// _parent.getSignalDispatcher().removeSubScription(slaveChannelID);

	}

	/**
	 * gets the device processor for a given queueEntry
	 * @return the processor that is processing queueEntryID, null if none matches
	 */
	protected Vector<ProxyDeviceProcessor> getProxyProcessors()
	{
		final int size = _deviceProcessors.size();
		if (size == 0)
		{
			return null;
		}
		final Vector<ProxyDeviceProcessor> v = new Vector<ProxyDeviceProcessor>(size);
		for (int i = 0; i < size; i++)
		{
			final AbstractDeviceProcessor theDeviceProcessor = _deviceProcessors.get(i);
			if (theDeviceProcessor instanceof ProxyDeviceProcessor)
			{
				v.add((ProxyDeviceProcessor) theDeviceProcessor);
			}
		}
		return v.size() == 0 ? null : v;
	}

	/**
	 * @param slaveQEID 
	 * @param nid 
	 * @return the ProxyDeviceProcessor that handles messages from slaveQEID
	 */
	protected ProxyDeviceProcessor getProcessorForSignal(final String slaveQEID, final NodeIdentifier nid)
	{

		ProxyDeviceProcessor proc = getProcessorForSlaveQEID(slaveQEID);
		if (proc == null)
		{
			proc = getProcessorForNID(nid);
		}
		if (proc == null)
		{
			final String errorMsg = "QueueEntry with ID=" + slaveQEID + " is not being processed";
			JMFHandler.errorResponse(null, errorMsg, 2, EnumClass.Error);
		}
		return proc;
	}

	/**
	 * @param nid
	 * @return
	 */
	protected ProxyDeviceProcessor getProcessorForNID(final NodeIdentifier nid)
	{
		if (nid == null)
		{
			return null;
		}
		final JDFQueueEntry qe = _theQueueProcessor.getQueueEntry(null, nid);
		final IQueueEntry iqe = _theQueueProcessor.getIQueueEntry(qe);
		return createExistingProcessor(iqe);
	}

	/**
	 * @param slaveQEID
	 * @return
	 */
	protected ProxyDeviceProcessor getProcessorForSlaveQEID(final String slaveQEID)
	{
		final String inQEID = getIncomingQEID(slaveQEID);
		ProxyDeviceProcessor proc = inQEID == null ? null : (ProxyDeviceProcessor) getProcessor(inQEID, 0);

		// we don't have an active proc, but this might be a multiple retQE - try to generate from old
		if (proc == null && inQEID != null)
		{
			final JDFQueueEntry qe = _theQueueProcessor.getQueueEntry(slaveQEID, null);
			final IQueueEntry iqe = _theQueueProcessor.getIQueueEntry(qe);
			proc = createExistingProcessor(iqe);
		}
		return proc;
	}

	/**
	 * add a processor to the list of active processors
	 * @param processor
	 */
	public void addProcessor(final AbstractDeviceProcessor processor)
	{
		log.info("adding device proceesor");
		_deviceProcessors.add(processor);
	}

	/**
	 * @param slaveQEID
	 * @return the bambi qeid for a given slave qeid
	 */
	protected String getIncomingQEID(final String slaveQEID)
	{
		if (slaveQEID == null || _deviceProcessors == null)
		{
			return null;
		}
		for (int i = 0; i < _deviceProcessors.size(); i++)
		{
			final AbstractDeviceProcessor aProc = _deviceProcessors.get(i);
			if (!(aProc instanceof ProxyDeviceProcessor))
			{
				continue;
			}
			final ProxyDeviceProcessor proc = (ProxyDeviceProcessor) aProc;
			final String procSlaveQEID = proc.getSlaveQEID();
			if (slaveQEID.equals(procSlaveQEID))
			{
				return proc.getCurrentQE().getQueueEntryID();
			}
		}
		final JDFQueueEntry qe = getQueueProcessor().getQueueEntry(slaveQEID, null);
		return qe == null ? null : qe.getQueueEntryID();
	}

	/**
	 * @param bambiQEID
	 * @return the queuentryID on the slave
	 */
	private String getSlaveQEID(final String bambiQEID)
	{
		if (bambiQEID == null || _deviceProcessors == null)
		{
			return null;
		}
		for (int i = 0; i < _deviceProcessors.size(); i++)
		{
			final AbstractDeviceProcessor aProc = _deviceProcessors.get(i);
			if (!(aProc instanceof ProxyDeviceProcessor))
			{
				continue;
			}
			final ProxyDeviceProcessor proc = (ProxyDeviceProcessor) aProc;
			final String qeID = proc.getCurrentQE().getQueueEntryID();
			if (bambiQEID.equals(qeID))
			{
				return proc.getSlaveQEID();
			}
		}
		final JDFQueueEntry qe = _theQueueProcessor.getQueue().getQueueEntry(bambiQEID);
		return BambiNSExtension.getSlaveQueueEntryID(qe);
	}

	/**
	 * reload the queue
	 */
	@Override
	protected void reloadQueue()
	{
		final JDFQueue q = _theQueueProcessor.getQueue();
		BambiNSExtension.setSlaveURL(q, "true");
		final VElement qev = q.getQueueEntryVector();

		if (qev != null)
		{
			final int qSize = qev.size();
			for (int i = 0; i < qSize; i++)
			{
				final JDFQueueEntry qe = (JDFQueueEntry) qev.get(i);
				final EnumQueueEntryStatus stat = qe.getQueueEntryStatus();
				if (!qe.isCompleted() && BambiNSExtension.getSlaveQueueEntryID(qe) != null)
				{
					final IQueueEntry iqe = _theQueueProcessor.getIQueueEntry(qe);
					if (iqe == null)
					{
						log.error("no Queue entry refreshing queue " + qe.getQueueEntryID() + " Status= " + stat == null ? "unknown" : stat.getName());
					}
					else
					{
						createExistingProcessor(iqe);
					}
				}
			}
		}
		final JDFJMF jmfQS = new JMFBuilder().buildQueueStatus();
		sendJMFToSlave(jmfQS, new QueueSynchronizeHandler(jmfQS));
	}

	/**
	 * @param iqe the queueentry to check for
	 * @return 
	 */
	protected ProxyDeviceProcessor createExistingProcessor(final IQueueEntry iqe)
	{
		final JDFQueueEntry qe = iqe == null ? null : iqe.getQueueEntry();
		if (qe == null || iqe == null) // redundant but eclipse nags about potential npa otherwise...
		{
			return null;
		}
		ProxyDeviceProcessor pdp = (ProxyDeviceProcessor) getProcessor(iqe.getQueueEntryID(), 0);
		if (pdp != null)
			return pdp;

		pdp = new ProxyDeviceProcessor(this, _theQueueProcessor, iqe);
		pdp.submitted(BambiNSExtension.getSlaveQueueEntryID(qe), qe.getQueueEntryStatus(), BambiNSExtension.getDeviceURL(qe), qe.getDeviceID());
		addProcessor(pdp);
		return pdp;
	}

	/**
	 * @see org.cip4.bambi.core.AbstractDevice#getNodeFromDoc(org.cip4.jdflib.core.JDFDoc)
	 */
	@Override
	public JDFNode getNodeFromDoc(final JDFDoc doc)
	{
		return doc.getJDFRoot();
	}

	/**
	 * @see org.cip4.bambi.core.AbstractDevice#stopProcessing(java.lang.String, org.cip4.jdflib.core.JDFElement.EnumNodeStatus)
	 */
	@Override
	public JDFQueueEntry stopProcessing(final String queueEntryID, final EnumNodeStatus status)
	{
		if (status == null)
		{
			final JDFJMF jmf = new JMFBuilder().buildRemoveQueueEntry(getSlaveQEID(queueEntryID));
			if (jmf != null)
			{
				final QueueEntryAbortHandler ah = new QueueEntryAbortHandler(status, jmf.getCommand(0).getID());
				sendJMFToSlave(jmf, ah);
				//				ah.waitHandled(5555, 30000, false);
				//				final EnumNodeStatus newStatus = ah.getFinalStatus();
				//				if (newStatus == null)
				//				{
				//					//					return null;
				//				}
			}
		}
		final JDFQueueEntry qe = super.stopProcessing(queueEntryID, status);
		return qe;
	}

	@Override
	protected void init()
	{
		super.init();
		if (_theStatusListener == null)
		{
			_theStatusListener = new StatusListener(_theSignalDispatcher, getDeviceID(), getICSVersions());
		}
	}

}