/*
 *
 * The CIP4 Software License, Version 1.0
 *
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
package org.cip4.bambi.core.messaging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cip4.bambi.core.BambiServlet;
import org.cip4.bambi.core.IConverterCallback;
import org.cip4.bambi.core.messaging.JMFFactory.CallURL;
import org.cip4.jdflib.core.AttributeName;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.core.VElement;
import org.cip4.jdflib.core.XMLDoc;
import org.cip4.jdflib.extensions.XJDF20;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.util.ByteArrayIOStream;
import org.cip4.jdflib.util.ContainerUtil;
import org.cip4.jdflib.util.DumpDir;
import org.cip4.jdflib.util.FastFiFo;
import org.cip4.jdflib.util.FileUtil;
import org.cip4.jdflib.util.JDFDate;
import org.cip4.jdflib.util.MimeUtil;
import org.cip4.jdflib.util.StringUtil;
import org.cip4.jdflib.util.ThreadUtil;
import org.cip4.jdflib.util.UrlUtil;
import org.cip4.jdflib.util.VectorMap;
import org.cip4.jdflib.util.MimeUtil.MIMEDetails;
import org.cip4.jdflib.util.ThreadUtil.MyMutex;
import org.cip4.jdflib.util.UrlUtil.HTTPDetails;

/**
 * allow a JMF message to be send in its own thread
 * 
 * @author boegerni
 */
public class MessageSender implements Runnable
{
	private final CallURL callURL;

	private enum SendReturn
	{
		sent, empty, error, removed
	}

	/**
	 * @return the callURL associated with this
	 */
	public CallURL getCallURL()
	{
		return callURL;
	}

	private boolean doShutDown = false;
	private boolean doShutDownGracefully = false;
	private Vector<MessageDetails> _messages = null;
	protected FastFiFo<MessageDetails> sentMessages = null;
	protected static Log log = LogFactory.getLog(MessageSender.class.getName());
	private static VectorMap<String, DumpDir> vDumps = new VectorMap<String, DumpDir>();
	private final MyMutex mutexDispatch = new MyMutex();
	private final MyMutex mutexPause = new MyMutex();
	private int sent = 0;
	private int idle = 0;
	private long created = 0;
	private long lastQueued = 0;
	private long lastSent = 0;
	private boolean pause = false;
	private static File baseLocation = null;

	/**
	 * MessageDetails describes one jmf or mime package that is queued for a given url
	 * 
	 * @author Dr. Rainer Prosi, Heidelberger Druckmaschinen AG
	 * 
	 * before May 26, 2009
	 */
	protected class MessageDetails
	{
		protected JDFJMF jmf = null;
		protected Multipart mime = null;
		protected IResponseHandler respHandler;
		protected MIMEDetails mimeDet;
		protected String senderID = null;
		protected String url = null;
		protected IConverterCallback callback;
		protected long createTime;

		/**
		 * constructor for a single jmf message
		 * @param _jmf the jmf to send
		 * @param _respHandler the response handler to handle the response after the message is queued
		 * @param _callback the callback to apply to the message prior to sending it
		 * @param hdet the http details
		 * @param detailedURL the complete, fully expanded url to send to
		 */
		protected MessageDetails(final JDFJMF _jmf, final IResponseHandler _respHandler, final IConverterCallback _callback, final HTTPDetails hdet, final String detailedURL)
		{
			jmf = _jmf;
			senderID = jmf == null ? null : jmf.getSenderID();
			url = detailedURL;
			createTime = System.currentTimeMillis();
			setRespHandler(_respHandler, _callback);
			if (hdet == null)
			{
				mimeDet = null;
			}
			else
			{
				mimeDet = new MIMEDetails();
				mimeDet.httpDetails = hdet;
			}
		}

		/**
		 * 
		 * @param _mime
		 * @param _respHandler the response handler to handle the response after the message is queued
		 * @param _callback the callback to apply to the message prior to sending it
		 * @param hdet the http details
		 * @param _senderID the senderID of the sender
		 * @param _url the complete, fully expanded url to send to
		 */
		protected MessageDetails(final Multipart _mime, final IResponseHandler _respHandler, final IConverterCallback _callback, final MIMEDetails mdet, final String _senderID, final String _url)
		{
			mime = _mime;
			mimeDet = mdet;
			senderID = _senderID;
			url = _url;
			setRespHandler(_respHandler, _callback);

		}

		/**
		 * @param _respHandler
		 * @param _callback
		 */
		private void setRespHandler(final IResponseHandler _respHandler, final IConverterCallback _callback)
		{
			respHandler = _respHandler;
			callback = _callback;
			if (respHandler != null && _callback != null)
			{
				respHandler.setCallBack(_callback);
			}
		}

		/**
		 * constructor when deserializing from a file <br/>
		 * note that the handlers are NOT reconstructed at startup - some synchronization may be lost
		 * 
		 * @param element the serialized representation
		 */
		public MessageDetails(final KElement element)
		{
			url = element.getAttribute(AttributeName.URL, null, null);
			senderID = element.getAttribute(AttributeName.SENDERID, null, null);
			final String cbClass = element.getAttribute("CallbackClass", null, null);
			if (cbClass != null)
			{
				try
				{
					final Class<?> c = Class.forName(cbClass);
					callback = (IConverterCallback) c.newInstance();
				}
				catch (final Exception x)
				{
					log.warn("Illegal callback class - limp along with null: " + cbClass);// nop
				}
			}
			final KElement jmf1 = element.getElement(ElementName.JMF);
			// must clone the root
			jmf = (JDFJMF) (jmf1 == null ? null : new JDFDoc(ElementName.JMF).getRoot().mergeElement(jmf1, false));
			if (jmf == null)
			{
				final String mimeURL = element.getAttribute("MimeUrl", null, null);
				if (mimeURL != null)
				{
					mime = MimeUtil.getMultiPart(mimeURL);
				}
				if (mime != null)
				{
					final File mimFile = UrlUtil.urlToFile(mimeURL);
					final boolean bZapp = mimFile.delete();
					if (!bZapp)
					{
						mimFile.deleteOnExit();
					}

				}
				final String encoding = element.getAttribute("TransferEncoding", null, null);
				if (encoding != null)
				{
					mimeDet = new MIMEDetails();
					mimeDet.transferEncoding = encoding;
				}
			}
		}

		/**
		 * @param messageList
		 * @param i
		 */
		void appendToXML(final KElement messageList, final int i)
		{
			final KElement message = messageList.appendElement("Message");
			message.setAttribute(AttributeName.URL, url);
			message.setAttribute(AttributeName.SENDERID, senderID);
			if (i >= 0)
			{
				if (callback != null)
				{
					message.setAttribute("CallbackClass", callback.getClass().getCanonicalName());
				}
				if (jmf != null)
				{
					final KElement makeNewJMF = displayJMF(jmf);
					message.copyElement(makeNewJMF, null);
				}
				else
				// mime
				{
					if (mimeDet != null)
					{
						message.setAttribute("TransferEncoding", mimeDet.transferEncoding);
					}
					BodyPart bp = null;
					try
					{
						bp = mime == null ? null : mime.getBodyPart(0);
					}
					catch (final MessagingException e)
					{
						// nop
					}
					final JDFDoc jmfBP = MimeUtil.getJDFDoc(bp);
					final JDFJMF _jmf = jmfBP == null ? null : jmfBP.getJMFRoot();
					final KElement makeNewJMF = displayJMF(_jmf);
					message.copyElement(makeNewJMF, null);
				}
			}
			else
			{
				final JDFDate d = new JDFDate(createTime);
				message.setAttribute(AttributeName.TIMESTAMP, d.getDateTimeISO());
			}
		}

		/**
		 * @param _jmf
		 */
		private KElement displayJMF(final JDFJMF _jmf)
		{
			final XJDF20 xjdf20 = new XJDF20();
			xjdf20.bUpdateVersion = false;
			final KElement makeNewJMF = xjdf20.makeNewJMF(_jmf);
			return makeNewJMF;
		}
	}

	/**
	 * trivial response handler that simply grabs the response and passes it back through getResponse() / isHandled()
	 * 
	 * @author Rainer Prosi
	 * 
	 */
	public static class MessageResponseHandler implements IResponseHandler
	{
		protected JDFResponse resp;
		protected JDFMessage finalMessage;
		private HttpURLConnection connect;
		protected ByteArrayIOStream bufferedInput;
		private MyMutex mutex = new MyMutex();
		private int abort = 0; // 0 no abort handling, 1= abort on timeout, 2= has been aborted
		protected String refID;
		private IConverterCallback callBack = null;
		private final long startTime;

		/**
		 * @return the callBack
		 */
		public IConverterCallback getCallBack()
		{
			return callBack;
		}

		/**
		 * @param _callBack the callBack to set
		 */
		public void setCallBack(final IConverterCallback _callBack)
		{
			this.callBack = _callBack;
		}

		/**
		 * @param _refID the ID of the sent message
		 * 
		 */
		public MessageResponseHandler(final String _refID)
		{
			super();
			refID = _refID;
			resp = null;
			finalMessage = null;
			connect = null;
			bufferedInput = null;
			startTime = System.currentTimeMillis();
		}

		/**
		 * @see org.cip4.bambi.core.messaging.IResponseHandler#handleMessage()
		 * @return true if handled, even if not finalized
		 */
		public boolean handleMessage()
		{
			if (finalMessage == null)
			{
				if (bufferedInput != null)
				{
					JDFDoc d = MimeUtil.getJDFDoc(bufferedInput.getInputStream(), 0);
					if (callBack != null && d != null)
					{
						log.info("preparing jmf response");
						d = callBack.prepareJMFForBambi(d);
					}
					if (d != null)
					{
						final JDFJMF jmf = d.getJMFRoot();
						if (jmf != null)
						{
							resp = jmf.getResponse(refID);
							if (resp != null)
							{
								if (checkAcknowledge())
								{
									return true;
								}
							}
							else
							{
								finalMessage = jmf.getAcknowledge(refID);
							}
						}
					}
				}
				else if (resp != null && checkAcknowledge())
				{
					return true;
				}
			}
			finalizeHandling();
			return true;
		}

		/**
		 * @param r
		 */
		private boolean checkAcknowledge()
		{
			final JDFResponse r = resp;
			final boolean isAcknowledgeResponse = r.getAcknowledged();
			if (isAcknowledgeResponse) // must wait for an acknowledge
			{
				String refIDMes = StringUtil.getNonEmpty(r.getrefID());
				if (refIDMes == null)
				{
					refIDMes = refID;
				}
				final AcknowledgeMap aMap = AcknowledgeMap.getMap();
				aMap.addHandler(refIDMes, this);
				return true;
			}
			else
			// the response is "the" final response
			{
				finalMessage = resp;
			}
			return false;
		}

		/**
		 * 
		 */
		protected void finalizeHandling()
		{
			if (mutex == null)
			{
				return;
			}
			abort = 0;
			ThreadUtil.notifyAll(mutex);
			mutex = null;
			if (resp != null)
			{
				String rID = resp.getAttribute(AttributeName.REFID, null, null);
				if (refID == null)
				{
					rID = refID;
				}
				if (rID != null)
				{
					final AcknowledgeMap aMap = AcknowledgeMap.getMap();
					aMap.removeHandler(rID);
				}
			}
		}

		/**
		 * @return the Acknowledge or Response that was handled
		 */
		public JDFMessage getFinalMessage()
		{
			return finalMessage;
		}

		/**
		 * @param message the Acknowledge or Response that was handled
		 */
		public void setMessage(final JDFMessage message)
		{
			finalMessage = message;
		}

		/**
		 * @see org.cip4.bambi.core.messaging.IResponseHandler#getConnection()
		 */
		public HttpURLConnection getConnection()
		{
			return connect;
		}

		/**
		 * @see org.cip4.bambi.core.messaging.IResponseHandler#setConnection(java.net.HttpURLConnection)
		 */
		public void setConnection(final HttpURLConnection uc)
		{
			connect = uc;
		}

		/**
		 * @see org.cip4.bambi.core.messaging.IResponseHandler#setBufferedStream(java.io.InputStream)
		 */
		public void setBufferedStream(final ByteArrayIOStream bis)
		{
			bufferedInput = bis;
		}

		/**
		 * @return the buffered input stream, may also be null in case of snafu
		 */
		public InputStream getBufferedStream()
		{
			if (bufferedInput != null)
			{
				return bufferedInput.getInputStream();
			}
			if (connect == null)
			{
				return null;
			}
			try
			{
				final InputStream inputStream = connect.getInputStream();
				bufferedInput = new ByteArrayIOStream(inputStream);
				inputStream.close();
			}
			catch (final IOException x)
			{
				// nop
			}
			return bufferedInput.getInputStream();
		}

		/**
		 * @param wait1 milliseconds to wait for a connection
		 * @param wait2 milliseconds to wait for the response after the connection has been established
		 * 
		 * @param bAbort if true, abort handling after timeout
		 */
		public void waitHandled(final int wait1, final int wait2, final boolean bAbort)
		{
			if (mutex == null)
			{
				return;
			}
			abort = bAbort ? 1 : 0;
			ThreadUtil.wait(mutex, wait1);
			if (mutex != null && connect != null && wait2 >= 0) // we have established a connection but have not yet read anything
			{
				ThreadUtil.wait(mutex, wait2);
			}
			if (abort == 1)
			{
				abort++;
			}
		}

		/**
		 * @see org.cip4.bambi.core.messaging.IResponseHandler#isAborted()
		 */
		public boolean isAborted()
		{
			final long t = System.currentTimeMillis();
			if (t - startTime > 1000 * 24 * 60 * 60)
			{
				return true;
			}
			return mutex == null ? false : abort == 2;
		}

		/**
		 * @see org.cip4.bambi.core.messaging.IResponseHandler#getResponse()
		 */
		public JDFResponse getResponse()
		{
			return resp;
		}
	}

	/**
	 * constructor
	 * 
	 * @param cu the URL to send the message to
	 */

	public MessageSender(final CallURL cu)
	{
		_messages = new Vector<MessageDetails>();
		sentMessages = new FastFiFo<MessageDetails>(42);
		callURL = cu;
		created = System.currentTimeMillis();
	}

	/**
	 * the sender loop. <br/>
	 * Checks whether its vector of pending messages is empty. If it is not empty, the first message is sent and removed from the map.
	 */
	public void run()
	{
		readFromBase();
		// wait a while before sending messages so that all processors are alive before we start throwing messages
		ThreadUtil.sleep(10000);
		while (!doShutDown)
		{
			if (pause)
			{
				ThreadUtil.wait(mutexPause, 0);
			}
			SendReturn sentFirstMessage;
			try
			{
				sentFirstMessage = sendFirstMessage();
				if (sentFirstMessage == SendReturn.sent)
				{
					synchronized (_messages)
					{
						_messages.remove(0);
						sent++;
						lastSent = System.currentTimeMillis();
						idle = 0;
					}
				}

				if (doShutDownGracefully && (_messages.isEmpty() || idle > 10)) // idle>10 kills - we are having problems...
				{
					doShutDown = true;
				}
			}
			catch (final Exception x)
			{
				sentFirstMessage = SendReturn.error;
				log.error("Error sending message: ", x);
			}
			if (sentFirstMessage != SendReturn.sent && sentFirstMessage != SendReturn.removed)
			{
				if (idle++ > 3600)
				{
					// no success or idle for an hour...
					doShutDownGracefully = true;
					log.info("Shutting down thread for base url: " + callURL.getBaseURL());
				}
				else
				{ // stepwise increment - try every second 10 times, then every 15 seconds
					final int wait = (SendReturn.error == sentFirstMessage && idle > 10) ? 15000 : 1000;
					ThreadUtil.wait(mutexDispatch, wait);
				}
			}
		}
		write2Base();
	}

	/**
	 * 
	 */
	private void write2Base()
	{
		final File f = getPersistLocation();
		if (f == null)
		{
			return;
		}
		synchronized (_messages)
		{

			if (_messages.size() == 0)
			{
				f.delete(); // it's empty we can zapp it
				return;
			}
			final KElement root = appendToXML(null, true, -1);
			root.getOwnerDocument_KElement().write2File(f, 2, false);
			_messages.clear();
		}

	}

	/**
	 * pause this sender until resume is called the thread still exists
	 */
	public void pause()
	{
		pause = true;
	}

	/**
	 * resume this sender after pause was called
	 */
	public void resume()
	{
		pause = false;
		ThreadUtil.notifyAll(mutexPause);
	}

	/**
	 * read all queued messages
	 */
	private void readFromBase()
	{
		final File f = getPersistLocation();
		if (f == null)
		{
			log.error("cannot read persistant message file, bailing out");
			return;
		}
		if (!f.exists()) // nothing queued ,ciao
		{
			log.warn("no persistant message file exists, bailing out! " + f);
			return;
		}

		final JDFDoc d = new JDFParser().parseFile(f);
		synchronized (_messages)
		{
			final Vector<MessageDetails> vTmp = new Vector<MessageDetails>();
			vTmp.addAll(_messages);
			_messages.clear();
			// adding existing messages prior to vTmp - they must be sent first
			if (d != null)
			{
				final KElement root = d.getRoot();
				pause = root.getBoolAttribute("pause", null, false);
				sent = root.getIntAttribute("NumSent", null, 0);
				lastQueued = root.getLongAttribute("iLastQueued", null, 0);
				lastSent = root.getLongAttribute("iLastSent", null, 0);
				created = root.getLongAttribute("i" + AttributeName.CREATIONDATE, null, 0);
				final VElement v = root.getChildElementVector("Message", null);
				for (int i = 0; i < v.size(); i++)
				{
					_messages.add(new MessageDetails(v.get(i)));
				}
			}
			_messages.addAll(vTmp);
		}
	}

	/**
	 * @return the file where we persist
	 */
	protected File getPersistLocation()
	{
		String loc = callURL.getBaseURL();
		if (loc == null)
		{
			log.error("cannot persist jmf to null");
			return null;
		}
		loc = StringUtil.replaceCharSet(loc, ":\\", "/", 0);
		while (loc.indexOf("//") >= 0)
		{
			loc = StringUtil.replaceString(loc, "//", "/");
		}
		loc += ".xml";
		final File f = FileUtil.getFileInDirectory(baseLocation, new File(loc));
		final File locParent = f.getParentFile();
		if (locParent != null)
		{
			locParent.mkdirs();
		}
		if (locParent == null || !locParent.isDirectory())
		{
			log.error("cannot create directory to persist jmf: " + f.getAbsolutePath());
			return null;
		}
		return f;
	}

	/**
	 * send the first enqueued message and return true if all went well also update any returned responses for Bambi internally
	 * 
	 * @return boolean true if the message is assumed sent false if an error was detected and the Message must remain in the queue
	 */
	private SendReturn sendFirstMessage()
	{
		MessageDetails mesDetails;
		JDFJMF jmf;
		Multipart mp;

		// don't synchronize the whole thing - otherwise the get handler may be blocked
		synchronized (_messages)
		{

			if (_messages.isEmpty())
			{
				return SendReturn.empty;
			}

			mesDetails = _messages.get(0);
			if (mesDetails == null)
			{
				_messages.remove(0);
				log.warn("removed null queued message in message queue");
				return SendReturn.removed; // should never happen
			}

			jmf = mesDetails.jmf;
			mp = mesDetails.mime;
			if (KElement.isWildCard(mesDetails.url))
			{
				log.error("Sending to bad url - bailing out! " + mesDetails.url);
				return SendReturn.error; // snafu anyhow but not sent but no retry useful
			}
			if (jmf == null && mp == null)
			{
				log.error("Sending neither mime nor jmf - bailing out?");
				_messages.remove(0);
				return SendReturn.removed; // need no resend - will remove
			}

			if (mesDetails.respHandler != null && mesDetails.respHandler.isAborted())
			{
				_messages.remove(0);
				log.warn("removed aborted message to: " + mesDetails.url);
				return SendReturn.removed;
			}
			if (jmf == null && mp == null)
			{
				log.error("Sending neither mime nor jmf - bailing out?");
				_messages.remove(0);
				return SendReturn.removed; // nothing to send; remove it
			}

		}
		sentMessages.push(mesDetails);
		return sendHTTP(mesDetails, jmf, mp);
	}

	/**
	 * @param mh the messagedetails
	 * @param jmf the jmf to send
	 * @param mp the mime to send
	 * @return the success as a sendreturn enum
	 */
	private SendReturn sendHTTP(final MessageDetails mh, final JDFJMF jmf, final Multipart mp)
	{
		SendReturn b = SendReturn.sent;
		final URL url = mh == null ? null : UrlUtil.stringToURL(mh.url);
		if (url == null || mh == null || (!UrlUtil.isHttp(mh.url) && !UrlUtil.isHttps(mh.url)))
		{
			log.error("Invalid url: " + url);
			_messages.remove(0);
			return SendReturn.removed;
		}
		try
		{
			HttpURLConnection con;
			String header = "URL: " + url;
			final DumpDir outDump = getOutDump(mh.senderID);
			final DumpDir inDump = getInDump(mh.senderID);
			if (jmf != null)
			{
				final JDFDoc jmfDoc = jmf.getOwnerDocument_JDFElement();
				final HTTPDetails hd = mh.mimeDet == null ? null : mh.mimeDet.httpDetails;
				con = jmfDoc.write2HTTPURL(url, hd);
				if (outDump != null)
				{
					final File dump = outDump.newFile(header);
					if (dump != null)
					{
						final FileOutputStream fos = new FileOutputStream(dump, true);
						jmfDoc.write2Stream(fos, 0, true);
						fos.close();
					}
				}
			}
			else
			// mime
			{
				con = MimeUtil.writeToURL(mp, mh.url, mh.mimeDet);
				if (outDump != null)
				{
					final File dump = outDump.newFile(header);
					if (dump != null)
					{
						final FileOutputStream fos = new FileOutputStream(dump, true);
						MimeUtil.writeToStream(mp, fos, mh.mimeDet);
						fos.close();
					}
				}
			}

			if (con != null)
			{
				if (mh.respHandler != null)
				{
					mh.respHandler.setConnection(con);
				}
				con.setReadTimeout(30000); // 30 seconds should suffice
				header += "\nResponse code:" + con.getResponseCode();
				header += "\nContent type:" + con.getContentType();
				header += "\nContent length:" + con.getContentLength();
			}

			if (con != null && con.getResponseCode() == 200)
			{
				final ByteArrayIOStream bis = new ByteArrayIOStream(con.getInputStream());
				con.getInputStream().close(); // copy and close so that the connection stream can be reused by keep-alive

				if (inDump != null)
				{
					inDump.newFileFromStream(header, bis.getInputStream());
				}
				if (mh.respHandler != null)
				{
					mh.respHandler.setConnection(con);
					mh.respHandler.setBufferedStream(bis);
					b = mh.respHandler.handleMessage() ? SendReturn.sent : SendReturn.error;
				}
			}
			else
			{
				b = SendReturn.error;
				if (idle == 0)
				{
					log.warn("could not send message to " + mh.url + " rc= " + ((con == null) ? -1 : con.getResponseCode()));
				}
				if (con != null)
				{
					if (inDump != null)
					{
						inDump.newFile(header);
					}
					con.getInputStream().close();
				}
				if (mh.respHandler != null)
				{
					mh.respHandler.setConnection(con);
					mh.respHandler.handleMessage(); // make sure we tell anyone who is waiting that the wait is over...
				}
			}
		}
		catch (final Exception e)
		{
			if (log != null) // shutdown
			{
				log.error("Exception in sendfirstmessage", e);
			}
			b = SendReturn.error;
		}
		return b;
	}

	/**
	 * stop sending new messages immediately and shut down
	 * 
	 * @param gracefully true - process remaining messages first, then shut down. <br/>
	 * false - shut down immediately, skip remaining messages.
	 */
	public void shutDown(final boolean gracefully)
	{
		if (gracefully)
		{
			doShutDownGracefully = true;
		}
		else
		{
			doShutDown = true;
		}
		JMFFactory.getJMFFactory().senders.remove(callURL);
		ThreadUtil.notifyAll(mutexDispatch);
	}

	/**
	 * return true if the thread is still running
	 * 
	 * @return true if running
	 */
	public boolean isRunning()
	{
		return !doShutDown;
	}

	/**
	 * return true if tesatURL fits this url
	 * 
	 * @param testURL the url to check against
	 * @return true if running
	 */
	public boolean matchesURL(final String testURL)
	{
		return ContainerUtil.equals(testURL, callURL.getBaseURL());
	}

	private DumpDir getInDump(final String senderID)
	{
		return vDumps.getOne(senderID, 0);
	}

	private DumpDir getOutDump(final String senderID)
	{
		return vDumps.getOne(senderID, 1);
	}

	/**
	 * add debug dump directories for a given senderID
	 * 
	 * @param senderID
	 * @param inDump
	 * @param outDump
	 */
	public static void addDumps(final String senderID, final DumpDir inDump, final DumpDir outDump)
	{
		vDumps.putOne(senderID, inDump);
		vDumps.putOne(senderID, outDump);
	}

	/**
	 * queues a message for the URL that this MessageSender belongs to also updates the message for a given recipient if required
	 * 
	 * @param jmf the message to send
	 * @param handler
	 * @param url
	 * @param _callBack
	 * @return true, if the message is successfully queued. false, if this MessageSender is unable to accept further messages (i. e. it is shutting down).
	 */
	public boolean queueMessage(final JDFJMF jmf, final IResponseHandler handler, final String url, final IConverterCallback _callBack)
	{
		if (doShutDown || doShutDownGracefully)
		{
			return false;
		}

		if (_callBack != null)
		{
			_callBack.updateJMFForExtern(jmf.getOwnerDocument_JDFElement());
		}

		final MessageDetails messageDetails = new MessageDetails(jmf, handler, _callBack, null, url);
		queueMessageDetails(messageDetails);
		return true;
	}

	/**
	 * @param messageDetails
	 */
	private void queueMessageDetails(final MessageDetails messageDetails)
	{
		synchronized (_messages)
		{
			_messages.add(messageDetails);
			lastQueued = System.currentTimeMillis();
		}
		if (!pause)
		{
			ThreadUtil.notifyAll(mutexDispatch);
		}
	}

	/**
	 * queues a message for the URL that this MessageSender belongs to also updates the message for a given recipient if required
	 * 
	 * @param multpart
	 * @param handler
	 * @param callback
	 * @param md
	 * @param senderID
	 * @param url
	 * @return true, if the message is successfully queued. false, if this MessageSender is unable to accept further messages (i. e. it is shutting down).
	 */
	public boolean queueMimeMessage(final Multipart multpart, final IResponseHandler handler, final IConverterCallback callback, final MIMEDetails md, final String senderID, final String url)
	{
		if (doShutDown || doShutDownGracefully)
		{
			return false;
		}

		final MessageDetails messageDetails = new MessageDetails(multpart, handler, callback, md, senderID, url);
		queueMessageDetails(messageDetails);
		return true;
	}

	/**
	 * @see java.lang.Object#toString()
	 * @return the string
	 */
	@Override
	public String toString()
	{
		return "MessageSender - URL: " + callURL.url + " size: " + _messages.size() + " total: " + sent + " last queued at " + BambiServlet.formatLong(lastQueued) + " last sent at "
				+ BambiServlet.formatLong(lastSent) + "\n" + _messages;
	}

	/**
	 * creates a descriptive xml Object for this MessageSender
	 * 
	 * @param root the parent into which I append myself, if null create a new document
	 * @param writePendingMessages if true, write out the messages
	 * @param posQueuedMessages
	 * @return the appended element
	 */
	public KElement appendToXML(final KElement root, final boolean writePendingMessages, final int posQueuedMessages)
	{

		final KElement ms = root == null ? new XMLDoc("MessageSender", null).getRoot() : root.appendElement("MessageSender");
		synchronized (_messages)
		{
			ms.setAttribute(AttributeName.URL, callURL.url);
			ms.setAttribute(AttributeName.SIZE, _messages.size(), null);
			ms.setAttribute("NumSent", sent, null);
			ms.setAttribute("LastQueued", BambiServlet.formatLong(lastQueued), null);
			ms.setAttribute("LastSent", BambiServlet.formatLong(lastSent), null);
			ms.setAttribute(AttributeName.CREATIONDATE, BambiServlet.formatLong(created), null);

			ms.setAttribute("pause", pause, null);
			ms.setAttribute("idle", idle, null);
			ms.setAttribute("Active", !doShutDown, null);
			ms.setAttribute("iLastQueued", StringUtil.formatLong(lastQueued), null);
			ms.setAttribute("iLastSent", StringUtil.formatLong(lastSent), null);
			ms.setAttribute("i" + AttributeName.CREATIONDATE, StringUtil.formatLong(created), null);

			if (writePendingMessages)
			{
				for (int i = 0; i < _messages.size(); i++)
				{
					_messages.get(i).appendToXML(ms, i);
				}
			}
			else if (posQueuedMessages == 0)
			{
				final MessageDetails[] old = sentMessages.peekArray();
				if (old != null)
				{
					for (int i = old.length - 1; i >= 0; i--)
					{
						old[i].appendToXML(ms, -1);
					}
				}
			}
			else if (posQueuedMessages > 0)
			{
				final MessageDetails old = sentMessages.peek(sentMessages.getFill() - posQueuedMessages);
				old.appendToXML(ms, posQueuedMessages);
			}
		}
		return ms;
	}

	/**
	 * set the base directory for serializing and deserializing messages
	 * 
	 * @param _baseLocation the baseLocation to set
	 */
	public static void setBaseLocation(final File _baseLocation)
	{
		// this is static and can therefore only be set once for consistency
		if (baseLocation == null)
		{
			MessageSender.baseLocation = _baseLocation;
		}
	}

	/**
	 * remove all unsent messages without sending them
	 */
	public void flushMessages()
	{
		synchronized (_messages)
		{
			_messages.clear();
			final File pers = getPersistLocation().getParentFile();
			FileUtil.deleteAll(pers);
		}
	}

}
