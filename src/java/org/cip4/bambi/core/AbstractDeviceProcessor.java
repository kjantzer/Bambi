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
package org.cip4.bambi.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cip4.bambi.core.messaging.MessageSender.MessageResponseHandler;
import org.cip4.bambi.core.queues.IQueueEntry;
import org.cip4.bambi.core.queues.QueueEntry;
import org.cip4.bambi.core.queues.QueueProcessor;
import org.cip4.jdflib.auto.JDFAutoDeviceInfo.EnumDeviceStatus;
import org.cip4.jdflib.auto.JDFAutoQueueEntry.EnumQueueEntryStatus;
import org.cip4.jdflib.core.AttributeName;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.core.JDFResourceLink;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.core.VElement;
import org.cip4.jdflib.core.JDFElement.EnumNodeStatus;
import org.cip4.jdflib.core.JDFResourceLink.EnumUsage;
import org.cip4.jdflib.datatypes.JDFAttributeMap;
import org.cip4.jdflib.datatypes.VJDFAttributeMap;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.resource.JDFResource;
import org.cip4.jdflib.resource.JDFResource.EnumResourceClass;
import org.cip4.jdflib.resource.process.JDFUsageCounter;
import org.cip4.jdflib.util.MimeUtil;
import org.cip4.jdflib.util.StatusCounter;

/**
 * abstract parent class for device processors <br>
 * The device processor is the actual working part of a device. 
 * @author boegerni
 *
 */
public abstract class AbstractDeviceProcessor implements IDeviceProcessor
{
	private static Log log = LogFactory.getLog(AbstractDeviceProcessor.class.getName());
	/**
	 * note: the queue processor points to the queue processor of the device, 
	 * it !does not! copy it
	 */
	protected QueueProcessor _queueProcessor;
	protected StatusListener _statusListener;
	protected Object _myListener; // the mutex for waiting and reawakening
	protected IDeviceProperties _devProperties = null;
	protected boolean _doShutdown = false;
	protected IQueueEntry currentQE;
	protected String _trackResource = null;
	protected AbstractDevice _parent = null;

	protected class SubmitQueueEntryResponseHandler extends MessageResponseHandler
	{
		public JDFDoc doc = null;

		/**
		 * @param _type
		 * @param _families
		 */
		public SubmitQueueEntryResponseHandler()
		{
			super();
		}

		/* (non-Javadoc)
		 * @see org.cip4.bambi.IMessageHandler#handleMessage(org.cip4.jdflib.jmf.JDFMessage, org.cip4.jdflib.jmf.JDFMessage)
		 */
		@Override
		public boolean handleMessage()
		{
			HttpURLConnection uc = getConnection();
			boolean handled = false;
			if (uc == null)
			{
				finalizeHandling();
				return false; // file
			}
			int rc;
			try
			{
				rc = uc.getResponseCode();
				if (rc == 200)
				{
					if (bufferedInput == null)
					{
						final InputStream inputStream = uc.getInputStream();
						bufferedInput = new BufferedInputStream(inputStream);
						bufferedInput.mark(1000000);
					}
					bufferedInput.reset();

					Multipart mpRet = MimeUtil.getMultiPart(bufferedInput);
					if (mpRet != null)
					{
						try
						{
							BodyPart bp = mpRet.getBodyPart(0);
							doc = MimeUtil.getJDFDoc(bp);
							handled = true;
						}
						catch (MessagingException e)
						{
							// nop - try simple doc
						}
					}
					if (!handled)
					{
						bufferedInput.reset();
						doc = new JDFParser().parseStream(bufferedInput);
						handled = true;
					}
				}
			}
			catch (IOException x)
			{
				handled = false;
			}
			finalizeHandling();
			return handled;
		}

	}

	private class XMLDeviceProcessor
	{
		/**
		 * @param rootDev
		 */
		KElement root;

		public XMLDeviceProcessor(KElement _root)
		{
			root = _root;
		}

		/**
		 * @param currentJobPhase
		 * @return
		 */
		public void fill()
		{
			KElement processor = root.appendElement(BambiNSExtension.MY_NS_PREFIX + "Processor", BambiNSExtension.MY_NS);
			if (currentQE == null)
			{
				processor.setAttribute("DeviceStatus", "Idle");
				return;
			}

			final EnumDeviceStatus deviceStatus = _statusListener.getDeviceStatus();
			JDFNode n = currentQE.getJDF();
			VJDFAttributeMap vm = n.getNodeInfoPartMapVector();
			JDFAttributeMap map = vm == null ? null : vm.elementAt(0);
			final EnumNodeStatus nodeStatus = n.getPartStatus(map);
			if (deviceStatus != null && nodeStatus != null)
			{
				processor.setAttribute("NodeStatus", nodeStatus.getName(), null);
				processor.setAttribute("NodeStatusDetails", n.getStatusDetails());
				fillPhaseTime(processor);
			}
			else
			{
				log.error("null status - bailing out");
			}
		}

		/**
		 * @param processor
		 * @param pt
		 */
		private void fillPhaseTime(KElement processor)
		{
			StatusCounter statusCounter = _statusListener.getStatusCounter();
			if (processor == null || statusCounter == null)
				return;
			processor.setAttribute(AttributeName.STARTTIME, statusCounter.getStartDate().getDateTimeISO());
			processor.setAttribute(AttributeName.DEVICESTATUS, statusCounter.getStatus().getName());
			processor.setAttribute("Device" + AttributeName.STATUSDETAILS, statusCounter.getStatusDetails());
			processor.setAttribute(AttributeName.QUEUEENTRYID, currentQE.getQueueEntryID());
			final JDFNode node = currentQE.getJDF();
			String typ = node.getType();
			if (node.isTypesNode())
				typ += " - " + node.getAttribute(AttributeName.TYPES);

			processor.setAttribute("Type", typ);
			processor.copyAttribute(AttributeName.DESCRIPTIVENAME, node);
			processor.setAttribute("JobID", node.getJobID(true));
			processor.setAttribute("JobPartID", node.getJobPartID(false));

			JDFResourceLink rls[] = statusCounter.getAmountLinks();
			int siz = rls == null ? 0 : rls.length;
			for (int i = 0; i < siz; i++)
			{
				String linkedResName = rls[i].getLinkedResourceName();
				if (ElementName.USAGECOUNTER.equals(linkedResName))
				{
					JDFUsageCounter uc = (JDFUsageCounter) rls[i].getTarget();
					if (uc != null && !KElement.isWildCard(uc.getCounterID()))
					{
						linkedResName += ":" + uc.getCounterID();
					}
				}
				else if (ElementName.COMPONENT.equals(linkedResName))
				{
					linkedResName += ":" + rls[i].getUsage().getName();
				}
				addAmount(processor, rls[i].getrRef(), linkedResName);
			}
		}

		/**
		 * @param string
		 */
		private void addAmount(KElement jp, String resID, String resName)
		{
			if (jp == null)
				return;
			StatusCounter sc = _statusListener.getStatusCounter();
			final double phaseAmount = sc.getPhaseAmount(resID);
			final double totalAmount = sc.getTotalAmount(resID);
			final double totalWaste = sc.getTotalWaste(resID);
			final double phaseWaste = sc.getPhaseWaste(resID);
			final double plannedAmount = sc.getPlannedAmount(resID);
			final double plannedWaste = sc.getPlannedWaste(resID);

			if ((phaseAmount + phaseWaste + totalAmount + totalWaste > 0) || (plannedAmount > 0) || (plannedWaste > 0))
			{
				KElement amount = jp.appendElement(BambiNSExtension.MY_NS_PREFIX + "PhaseAmount", BambiNSExtension.MY_NS);
				amount.setAttribute("ResourceName", resName);
				amount.setAttribute("PlannedAmount", plannedAmount, null);
				amount.setAttribute("PlannedWaste", plannedWaste, null);
				amount.setAttribute("PhaseAmount", phaseAmount, null);
				amount.setAttribute("PhaseWaste", phaseWaste, null);
				amount.setAttribute("TotalAmount", totalAmount, null);
				amount.setAttribute("TotalWaste", totalWaste, null);
			}
		}
	}

	/**
	 * constructor
	 */
	public AbstractDeviceProcessor()
	{
		super();
	}

	/**
	 * this is the device processor loop
	 */
	final public void run()
	{
		while (!_doShutdown)
		{
			try
			{
				if (!processQueueEntry())
				{
					try
					{
						if (log != null)
							log.debug("waiting");
						if (_statusListener != null)
							_statusListener.signalStatus(EnumDeviceStatus.Idle, null, null, null, false);
						synchronized (_myListener)
						{
							_myListener.wait(10000); // just in case                        
						}
					}
					catch (InterruptedException x)
					{
						log.warn("interrupted while idle");
					}
				}
			}
			catch (Exception x)
			{
				log.error("unhandled exception in processor", x);
			}
		}
		if (_queueProcessor != null)
			_queueProcessor.removeListener(_myListener);
	}

	/**
	 * initialize the IDeviceProcessor
	 * @param _queueProcessor
	 * @param _statusListener
	 */
	public void init(QueueProcessor queueProcessor, StatusListener statusListener, IDeviceProperties devProperties)
	{
		log.info(this.getClass().getName() + " construct");
		_myListener = new Object();
		_queueProcessor = queueProcessor;
		if (_queueProcessor != null)
			_queueProcessor.addListener(_myListener);
		_statusListener = statusListener;
		_statusListener.getStatusCounter().setDeviceID(devProperties.getDeviceID());
		_devProperties = devProperties;
		_trackResource = devProperties.getTrackResource();
	}

	/**
	* process a queue entry
	* 
	* @param node the JDF node to process
	* @param qe the JDF queueentry that corresponds to this
	* @return EnumQueueEntryStatus the final status of the queuentry 
	*/
	public abstract EnumQueueEntryStatus processDoc(JDFNode n, JDFQueueEntry qe);

	final private boolean processQueueEntry()
	{
		currentQE = getQEFromParent();
		if (currentQE == null)
			return false;
		JDFQueueEntry qe = currentQE.getQueueEntry();
		if (qe == null)
			return false;
		final String queueEntryID = qe.getQueueEntryID();
		if (queueEntryID != null)
			log.debug("processing: " + queueEntryID);

		JDFNode node = currentQE.getJDF();
		if (node == null)
		{
			log.error("no JDF Node for: " + queueEntryID);
			return false;
		}
		boolean bOK = true;
		initializeProcessDoc(node, qe);

		EnumQueueEntryStatus qes;
		try
		{
			qes = processDoc(node, qe);
			if (qes == null)
			{
				if (log != null)
					log.error("QueueEntryStatus is null");
				bOK = false;
			}
		}
		catch (Exception x)
		{
			log.error("error processing JDF: " + x);
			qes = EnumQueueEntryStatus.Aborted;
			bOK = false;
		}

		// Always finalize even if exceptions are caught
		return finalizeProcessDoc(qes) && bOK;
	}

	/**
	 * 
	 */
	protected IQueueEntry getQEFromParent()
	{
		synchronized (_queueProcessor.getQueue())
		{
			currentQE = _queueProcessor.getNextEntry(null);
			if (currentQE == null)
			{
				if (_parent != null)
				{
					RootDevice rootDevice = _parent.get_rootDevice();
					if (rootDevice != null)
					{
						currentQE = rootDevice._theQueueProcessor.getNextEntry(_parent.getDeviceID());
						if (currentQE != null)
						{
							//grab the qe and pass it on to the devices queue...
							final JDFQueue queue = _parent._theQueueProcessor.getQueue();
							JDFQueueEntry queueEntry = currentQE.getQueueEntry();
							queueEntry = (JDFQueueEntry) queue.moveElement(queueEntry, null);
							// sort the root queue as it doesn't know that it lost a kid
							rootDevice._theQueueProcessor.getQueue().sortChildren();
							currentQE.setQueueEntry(queueEntry);
						}
					}
				}
				if (currentQE == null)
				{
					_parent.sendRequestQueueEntry();
				}
			}
			return currentQE;
		}
	}

	/**
	 * genric setup of processing 
	 * @param queueEntryID the queueEntryID of the job to process
	 */
	protected void initializeProcessDoc(JDFNode node, JDFQueueEntry qe)
	{
		currentQE = new QueueEntry(node, qe);
		_queueProcessor.updateEntry(qe, EnumQueueEntryStatus.Running, null, null);
		setupStatusListener(node, qe);

	}

	/**
	 * @param node
	 * @param qe
	 * @param queueEntryID
	 * @param vResLinks
	 * @param vSiz
	 */
	protected void setupStatusListener(JDFNode node, JDFQueueEntry qe)
	{
		if (node == null || qe == null)
			return;
		final String queueEntryID = qe.getQueueEntryID();
		VElement vResLinks = node.getResourceLinks(null);
		int vSiz = (vResLinks == null) ? 0 : vResLinks.size();
		VJDFAttributeMap vPartMap = qe.getPartMapVector();
		if (vPartMap == null)
			vPartMap = node.getNodeInfoPartMapVector();

		String trackResourceID = null;
		for (int i = 0; i < vSiz; i++)
		{
			JDFResourceLink rl = (JDFResourceLink) vResLinks.elementAt(i);
			if (trackResourceID == null && rl.matchesString(_trackResource))
			{
				trackResourceID = rl.getrRef();
				break;
			}
		}

		//heuristics in case we didn't find anything
		if (trackResourceID == null)
		{
			String inConsume = null;
			String outQuantity = null;
			for (int i = 0; i < vSiz; i++)
			{
				JDFResourceLink rl = (JDFResourceLink) vResLinks.elementAt(i);
				JDFResource r = rl.getLinkRoot();
				EnumResourceClass c = r.getResourceClass();
				if (EnumResourceClass.Consumable.equals(c) || EnumResourceClass.Handling.equals(c)
						|| EnumResourceClass.Quantity.equals(c))
				{
					EnumUsage inOut = rl.getUsage();
					if (EnumUsage.Input.equals(inOut))
					{
						if (EnumResourceClass.Consumable.equals(c))
							inConsume = rl.getrRef();
					}
					else
					{
						outQuantity = rl.getrRef();
					}
				}
			}
			trackResourceID = inConsume != null ? inConsume : outQuantity;
		}
		_statusListener.setNode(queueEntryID, node, vPartMap, trackResourceID);
	}

	protected void suspend()
	{
		_statusListener.signalStatus(EnumDeviceStatus.Idle, "Idle", EnumNodeStatus.Suspended, "job suspended", false);
	}

	protected void abort()
	{
		_statusListener.signalStatus(EnumDeviceStatus.Idle, "JobCanceledByUser", EnumNodeStatus.Aborted, "job canceled by user", true);
	}

	protected void complete()
	{
		_statusListener.signalStatus(EnumDeviceStatus.Idle, "JobCompleted", EnumNodeStatus.Completed, "job completed successfully", true);
	}

	/**
	 * signal that processing has finished and prepare the StatusCounter for the next process
	 * @param qes the final queue entry status of the entry
	 * @return true if successfully processed
	 */
	protected boolean finalizeProcessDoc(EnumQueueEntryStatus qes)
	{
		if (currentQE != null)
		{
			if (EnumQueueEntryStatus.Completed.equals(qes))
			{
				complete();
			}
			else if (EnumQueueEntryStatus.Suspended.equals(qes))
			{
				suspend();
			}
			else if (EnumQueueEntryStatus.Aborted.equals(qes))
			{
				abort();
			}
		}
		_statusListener.flush(null);
		_statusListener.setNode(null, null, null, null);
		JDFQueueEntry qe = currentQE.getQueueEntry();
		_queueProcessor.updateEntry(qe, qes, null, null);
		_queueProcessor.returnQueueEntry(qe, null, null);

		currentQE.getQueueEntry().removeAttribute(AttributeName.DEVICEID);
		currentQE = null;
		log.info("finalized processing JDF: ");
		return true;
	}

	/**
	 * stops the currently processed task, 
	 * called e.g. from the queueprocessor upon AbortQueueEntry
	 * @param newStatus
	 * @return the new status, null in case of snafu
	 */
	public abstract void stopProcessing(EnumNodeStatus newStatus);

	public void shutdown()
	{
		_doShutdown = true;
	}

	public IStatusListener getStatusListener()
	{
		return _statusListener;
	}

	/**
	 * get the currently processed IQueueEntry
	 * @return
	 */
	public IQueueEntry getCurrentQE()
	{
		return currentQE;
	}

	/**
	 * get the device properties
	 * @return
	 */
	public IDeviceProperties getProperties()
	{
		return _devProperties;
	}

	/**
	 * @param rootDev
	 */
	public void addToDisplayXML(KElement root)
	{
		this.new XMLDeviceProcessor(root).fill();
	}

	@Override
	public String toString()
	{
		return "Abstract Device Processor: Current Entry: "
				+ (currentQE != null ? currentQE.getQueueEntryID() : "no current entry") + "]";
	}

	/**
	 * @param device
	 */
	public void setParent(AbstractDevice device)
	{
		_parent = device;
	}

	@Override
	protected void finalize() throws Throwable
	{
		shutdown();
		super.finalize();
	}

}
