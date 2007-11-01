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
package org.cip4.bambi.core.queues;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.Vector;

import javax.mail.Multipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cip4.bambi.core.BambiNSExtension;
import org.cip4.bambi.core.messaging.IJMFHandler;
import org.cip4.bambi.core.messaging.IMessageHandler;
import org.cip4.jdflib.auto.JDFAutoQueue.EnumQueueStatus;
import org.cip4.jdflib.auto.JDFAutoQueueEntry.EnumQueueEntryStatus;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.core.VString;
import org.cip4.jdflib.core.JDFElement.EnumNodeStatus;
import org.cip4.jdflib.jmf.JDFCommand;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.jmf.JDFQueueEntryDef;
import org.cip4.jdflib.jmf.JDFQueueSubmissionParams;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFReturnQueueEntryParams;
import org.cip4.jdflib.jmf.JDFMessage.EnumFamily;
import org.cip4.jdflib.jmf.JDFMessage.EnumType;
import org.cip4.jdflib.util.MimeUtil;
import org.cip4.jdflib.util.UrlUtil;

/**
 *
 * @author rainer
 *
 *
 */
public abstract class AbstractQueueProcessor implements IQueueProcessor
{
    protected class SubmitQueueEntryHandler implements IMessageHandler
    {
        /* (non-Javadoc)
         * @see org.cip4.bambi.IMessageHandler#handleMessage(org.cip4.jdflib.jmf.JDFMessage, org.cip4.jdflib.jmf.JDFMessage)
         */
        public boolean handleMessage(JDFMessage m, JDFResponse resp)
        {
            if(m==null || resp==null) {
                return false;
            }
            EnumType typ=m.getEnumType();
            log.info( "Handling "+typ.getName() );
            if(EnumType.SubmitQueueEntry.equals(typ))
            {
                JDFQueueSubmissionParams qsp=m.getQueueSubmissionParams(0);
                if(qsp!=null) {
                    JDFDoc doc=qsp.getURLDoc();

                    if(doc!=null) {
                        JDFResponse r2=addEntry( (JDFCommand)m, doc, qsp.getHold() );
                        if(r2!=null) {
                            resp.mergeElement(r2, false);
                            return true;
                        }
                    }
                }
                log.error("QueueSubmissionParams missing or invalid");
                resp.setErrorText("QueueSubmissionParams missing or invalid");
                resp.setReturnCode(9);
                return true;
            }

            return false;        
        }
    
        /* (non-Javadoc)
         * @see org.cip4.bambi.IMessageHandler#getFamilies()
         */
        public EnumFamily[] getFamilies()
        {
            return new EnumFamily[]{EnumFamily.Command};
        }
    
        /* (non-Javadoc)
         * @see org.cip4.bambi.IMessageHandler#getMessageType()
         */
        public EnumType getMessageType()
        {
            return EnumType.SubmitQueueEntry;
        }
    }

    protected class QueueStatusHandler implements IMessageHandler
	{
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#handleMessage(org.cip4.jdflib.jmf.JDFMessage, org.cip4.jdflib.jmf.JDFMessage)
	     */
	    public boolean handleMessage(JDFMessage m, JDFResponse resp)
	    {
	        if(m==null || resp==null) {
	            return false;
	        }
	        log.info("Handling "+m.getType());
	        EnumType typ=m.getEnumType();
	        if(EnumType.QueueStatus.equals(typ)) {
	        	if (_theQueue != null) {
	        		JDFQueue q = (JDFQueue) resp.copyElement(_theQueue, null);
	        		//TODO filter some stuff?
//	        		JDFQueueFilter qf=m.getQueueFilter(0);
//	        		if (qf==null)
//	        			log.info("qf is null");
//	        		else 
//	        			log.info(qf.toString());
	        		handleQueueStatus(q);
	        		removeBambiNSExtensions(q);
	        		return true;
	        	} else {
	        		log.error("queue is null");
	        		// append an empty Queue to the response
	        		resp.appendQueue();
	        		return true;
	        	}
	        }
	
	        return false;        
	    }
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#getFamilies()
	     */
	    public EnumFamily[] getFamilies() {
	        return new EnumFamily[]{EnumFamily.Query};
	    }
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#getMessageType()
	     */
	    public EnumType getMessageType() {
	        return EnumType.QueueStatus;
	    }
	
	}

	protected class AbortQueueEntryHandler implements IMessageHandler
	{
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#handleMessage(org.cip4.jdflib.jmf.JDFMessage, org.cip4.jdflib.jmf.JDFMessage)
	     */
	    public boolean handleMessage(JDFMessage m, JDFResponse resp)
	    {
	        if(m==null || resp==null)
	        {
	            return false;
	        }
	        log.info("Handling "+m.getType());
	        EnumType typ=m.getEnumType();
	        if(EnumType.AbortQueueEntry.equals(typ))
	        {
                String qeid = getMessageQueueEntryID(m);
	            JDFQueueEntry qe =_theQueue.getQueueEntry(qeid);
	            if (qe==null)
	            {
	            	resp.setReturnCode(105);
	            	resp.setErrorText("found no QueueEntry with QueueEntryID="+qeid);
	            	return true;
	            } else {
	                EnumQueueEntryStatus status = qe.getQueueEntryStatus();
	                if ( EnumQueueEntryStatus.Completed.equals(status) )
	                {
	                    log.error("cannot abort QueueEntry with ID="+qeid+", it is already completed");
	                    resp.setReturnCode(114);
	                    resp.setErrorText("job is already completed");
	                    return true;
	                } else if ( EnumQueueEntryStatus.Aborted.equals(status) )
	                {
	                    log.error("cannot abort QueueEntry with ID="+qeid+", it is already aborted");
	                    resp.setReturnCode(113);
	                    resp.setErrorText("QueueEntry is already aborted");
	                    return true;
	                }

	                // has to be waiting, held, running or suspended: abort it!
	                handleAbortQueueEntry(resp, qeid, qe); 				
	                return true;
	            }
	        }
	
	        return false;
	    }

		
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#getFamilies()
	     */
	    public EnumFamily[] getFamilies()
	    {
	        return new EnumFamily[]{EnumFamily.Command};
	    }
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#getMessageType()
	     */
	    public EnumType getMessageType()
	    {
	        return EnumType.AbortQueueEntry;
	    }
	}

	protected class RemoveQueueEntryHandler implements IMessageHandler
	{
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#handleMessage(org.cip4.jdflib.jmf.JDFMessage, org.cip4.jdflib.jmf.JDFMessage)
	     */
	    public boolean handleMessage(JDFMessage m, JDFResponse resp)
	    {
	    	if(m==null || resp==null)
	        {
	            return false;
	        }
	        log.info("Handling "+m.getType());
	        EnumType typ=m.getEnumType();
	        if(EnumType.RemoveQueueEntry.equals(typ))
	        {
	            String qeid = getMessageQueueEntryID(m);
	            JDFQueueEntry qe =_theQueue.getQueueEntry(qeid);
	            if (qe==null)
	            {
	            	log.error("failed to remove QueueEntry with ID="+qeid+", QueueEntry does not exist.");
		            resp.setReturnCode(105);
		            resp.setErrorText("found no QueueEntry with QueueEntryID="+qeid);
	            	return true;
	            } else {
	                EnumQueueEntryStatus status = qe.getQueueEntryStatus();
	
	                if ( EnumQueueEntryStatus.Held.equals(status) || 
	                		EnumQueueEntryStatus.Completed.equals(status) || EnumQueueEntryStatus.Aborted.equals(status))
	                {
	                    qe.setQueueEntryStatus(EnumQueueEntryStatus.Removed);
	                    JDFQueue q = resp.appendQueue();
	                    q.copyElement(qe, null);
	                    q.setDeviceID( _theQueue.getDeviceID() );
	                    q.setStatus( _theQueue.getStatus() );
	                    removeBambiNSExtensions(q);
	                    updateEntry(qeid, EnumQueueEntryStatus.Removed);
	                    _theQueue.cleanup();
	                    log.info("removed QueueEntry with ID="+qeid);
	                    return true;
	                } else {
	                	String statName = status.getName();
	                    log.error("cannot remove QueueEntry with ID="+qeid+", it is "+statName);
	                    resp.setReturnCode(106);
	                    resp.setErrorText("QueueEntry is "+statName);
	                    return true;
	                }
	            }
	        }
	
	        return false;       
	    }
	
	
	
	
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#getFamilies()
	     */
	    public EnumFamily[] getFamilies()
	    {
	        return new EnumFamily[]{EnumFamily.Command};
	    }
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#getMessageType()
	     */
	    public EnumType getMessageType()
	    {
	        return EnumType.RemoveQueueEntry;
	    }
	}

	protected class ResumeQueueEntryHandler implements IMessageHandler
	{
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#handleMessage(org.cip4.jdflib.jmf.JDFMessage, org.cip4.jdflib.jmf.JDFMessage)
	     */
	    public boolean handleMessage(JDFMessage m, JDFResponse resp)
	    {
	    	if(m==null || resp==null)
	        {
	            return false;
	        }
	        log.info("Handling "+m.getType());
	        EnumType typ=m.getEnumType();
	        if(EnumType.ResumeQueueEntry.equals(typ))
	        {
	            String qeid = getMessageQueueEntryID(m);
	
	            JDFQueueEntry qe =_theQueue.getQueueEntry(qeid);
	            if (qe==null)
	            {
	            	resp.setReturnCode(105);
	            	resp.setErrorText("found no QueueEntry with QueueEntryID="+qeid);
	            	return true;
	            } else {
	                EnumQueueEntryStatus status = qe.getQueueEntryStatus();
	
	                if ( EnumQueueEntryStatus.Suspended.equals(status) || EnumQueueEntryStatus.Held.equals(status) )
	                {
	                	handleResumeQueueEntry(resp, qeid, qe); 				
	                    return true;
	                }
	
	                if ( EnumQueueEntryStatus.Running.equals(status) )
	                {
	                    log.error("cannot resume QueueEntry with ID="+qeid+", it is "+status.getName());
	                    resp.setReturnCode(113);
	                    resp.setErrorText("QueueEntry is "+status.getName());
	                    return true;
	                }
	
	                if ( EnumQueueEntryStatus.Completed.equals(status) || EnumQueueEntryStatus.Aborted.equals(status) )
	                {
	                    log.error("cannot resume QueueEntry with ID="+qeid+", it is already "+status.getName());
	                    resp.setReturnCode(115);
	                    resp.setErrorText("QueueEntry is already "+status.getName());
	                    return true;
	                }
	            }
	        }
	
	        return false;       
	    }
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#getFamilies()
	     */
	    public EnumFamily[] getFamilies()
	    {
	        return new EnumFamily[]{EnumFamily.Command};
	    }
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#getMessageType()
	     */
	    public EnumType getMessageType()
	    {
	        return EnumType.ResumeQueueEntry;
	    }
	}

	protected class SuspendQueueEntryHandler implements IMessageHandler
	{
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#handleMessage(org.cip4.jdflib.jmf.JDFMessage, org.cip4.jdflib.jmf.JDFMessage)
	     */
	    public boolean handleMessage(JDFMessage m, JDFResponse resp)
	    {
	    	if(m==null || resp==null)
	        {
	            return false;
	        }
	        log.info("Handling "+m.getType());
	        EnumType typ=m.getEnumType();
	        if(EnumType.SuspendQueueEntry.equals(typ))
	        {
	            String qeid = getMessageQueueEntryID(m);
	            JDFQueueEntry qe =_theQueue.getQueueEntry(qeid);
	            if (qe==null)
	            {
	            	resp.setReturnCode(105);
	            	resp.setErrorText("found no QueueEntry with QueueEntryID="+qeid);
	            	return true;
	            } else {
	                EnumQueueEntryStatus status = qe.getQueueEntryStatus();
	
	                if ( EnumQueueEntryStatus.Running.equals(status) )
	                {
	                	handleSuspendQueueEntry(resp, qeid, qe); 				
	                    return true;
	                }
	
	                if ( EnumQueueEntryStatus.Suspended.equals(status) )
	                {
	                    log.error("cannot suspend QueueEntry with ID="+qeid+", it is already suspended");
	                    resp.setReturnCode(113);
	                    resp.setErrorText("QueueEntry is already suspended");
	                    return true;
	                }
	
	                if ( EnumQueueEntryStatus.Waiting.equals(status)  || EnumQueueEntryStatus.Held.equals(status) )
	                {
	                    log.error("cannot suspend QueueEntry with ID="+qeid+", it is "+status.getName());
	                    resp.setReturnCode(115);
	                    resp.setErrorText("QueueEntry is "+status.getName());
	                    return true;
	                }
	
	                if ( EnumQueueEntryStatus.Completed.equals(status)  || EnumQueueEntryStatus.Aborted.equals(status) )
	                {
	                    log.error("cannot suspend QueueEntry with ID="+qeid+", it is already "+status.getName());
	                    resp.setReturnCode(114);
	                    resp.setErrorText("QueueEntry is already "+status.getName());
	                    return true;
	                }
	            }
	        }
	
	        return false;       
	    }

	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#getFamilies()
	     */
	    public EnumFamily[] getFamilies()
	    {
	        return new EnumFamily[]{EnumFamily.Command};
	    }
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#getMessageType()
	     */
	    public EnumType getMessageType()
	    {
	        return EnumType.SuspendQueueEntry;
	    }
	}

	protected class HoldQueueEntryHandler implements IMessageHandler
	{
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#handleMessage(org.cip4.jdflib.jmf.JDFMessage, org.cip4.jdflib.jmf.JDFMessage)
	     */
	    public boolean handleMessage(JDFMessage m, JDFResponse resp)
	    {
	    	if(m==null || resp==null)
	        {
	            return false;
	        }
	        log.info("Handling "+m.getType());
	        EnumType typ=m.getEnumType();
	        if(EnumType.HoldQueueEntry.equals(typ))
	        {
	            String qeid = getMessageQueueEntryID(m);
	            JDFQueueEntry qe =_theQueue.getQueueEntry(qeid);
	            if (qe==null) {
	            	resp.setReturnCode(105);
	            	resp.setErrorText("found no QueueEntry with QueueEntryID="+qeid);
	            	return true;
	            } else {
	                EnumQueueEntryStatus status = qe.getQueueEntryStatus();
	
	                if ( EnumQueueEntryStatus.Waiting.equals(status) ) {
	                	updateEntry(qe.getQueueEntryID(), EnumQueueEntryStatus.Held);
	                	JDFQueue q = resp.appendQueue();
	            		q.setDeviceID( _theQueue.getDeviceID() );
	            		q.setQueueStatus( _theQueue.getQueueStatus() );
	            		q.copyElement(qe, null);
	            		removeBambiNSExtensions(q);
	            		log.info("held QueueEntry with ID="+qeid);
	                    return true;
	                }
	
	                if ( EnumQueueEntryStatus.Held.equals(status) ) {
	                    log.error("cannot suspend QueueEntry with ID="+qeid+", it is already held");
	                    resp.setReturnCode(113);
	                    resp.setErrorText("QueueEntry is already held");
	                    return true;
	                }
	
	                if ( EnumQueueEntryStatus.Running.equals(status)  || EnumQueueEntryStatus.Suspended.equals(status) ) {
	                    log.error("cannot hold QueueEntry with ID="+qeid+", it is "+status.getName());
	                    resp.setReturnCode(106);
	                    resp.setErrorText("QueueEntry is "+status.getName());
	                    return true;
	                }
	
	                if ( EnumQueueEntryStatus.Completed.equals(status)  || EnumQueueEntryStatus.Aborted.equals(status) ) {
	                    log.error("cannot hold QueueEntry with ID="+qeid+", it is already "+status.getName());
	                    resp.setReturnCode(114);
	                    resp.setErrorText("QueueEntry is already "+status.getName());
	                    return true;
	                }
	            }
	        }
	
	        return false;       
	    }
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#getFamilies()
	     */
	    public EnumFamily[] getFamilies()
	    {
	        return new EnumFamily[]{EnumFamily.Command};
	    }
	
	    /* (non-Javadoc)
	     * @see org.cip4.bambi.IMessageHandler#getMessageType()
	     */
	    public EnumType getMessageType()
	    {
	        return EnumType.HoldQueueEntry;
	    }
	}

	protected static final Log log = LogFactory.getLog(AbstractQueueProcessor.class.getName());
    private File _queueFile;
    private static final long serialVersionUID = -876551736245089033L;
    protected JDFQueue _theQueue;
    private Vector<Object> _listeners;
    protected String _appDir=null;
    protected String _configDir=null;
    protected String _deviceURL=null;
    protected String _jdfDir=null;
     
    public AbstractQueueProcessor(String deviceID, String appDir)
    {
		super();
		_appDir=appDir;
		this.init(deviceID);
    }
    
    /**
     * @param jmfHandler
     */
    public void addHandlers(IJMFHandler jmfHandler) {
        jmfHandler.addHandler(this.new SubmitQueueEntryHandler());
        jmfHandler.addHandler(this.new QueueStatusHandler());
        jmfHandler.addHandler(this.new AbortQueueEntryHandler());
        jmfHandler.addHandler(this.new RemoveQueueEntryHandler());
        jmfHandler.addHandler(this.new SuspendQueueEntryHandler());
        jmfHandler.addHandler(this.new ResumeQueueEntryHandler());
        jmfHandler.addHandler(this.new HoldQueueEntryHandler());
    }

    private void init(String deviceID) {
        log.info("QueueProcessor construct for device '"+deviceID+"'");
        String baseDir=_appDir+"jmb/";
        _configDir=_appDir+"config/";
        _jdfDir=_appDir+"jmb/JDFDir/";
        loadProperties();
        
      	_queueFile=new File(baseDir+"theQueue_"+deviceID+".xml");       
        _queueFile.getParentFile().mkdirs();
        new File(baseDir+"JDFDir"+File.separator).mkdirs();
        JDFDoc d=JDFDoc.parseFile(_queueFile.getAbsolutePath());
        if(d!=null) {
            log.info("refreshing queue");
            _theQueue=(JDFQueue) d.getRoot();
        } else {
            d=new JDFDoc(ElementName.QUEUE);
            log.info("creating new queue");
            _theQueue=(JDFQueue) d.getRoot();
            _theQueue.setQueueStatus(EnumQueueStatus.Waiting);
        }
        _theQueue.setAutomated(true);
        _theQueue.setDeviceID(deviceID);
        _theQueue.setMaxCompletedEntries( 100 ); // remove just the selected QE when RemoveQE is called 
        _listeners=new Vector<Object>();
	}

    public IQueueEntry getNextEntry() {
        JDFQueueEntry qe=_theQueue.getNextExecutableQueueEntry();

        if(qe==null) {
        	return null;
        }
        
        // try to load from local file system first, then try URL
        String file=_jdfDir+qe.getQueueEntryID()+".jdf";
        if ( new File(file).canRead() ) {
        	JDFDoc doc=loadDocFromFile(file);
    		if ( doc!=null ) {
    			return new QueueEntry(doc,qe);
    		} else {
    			return null;
    		}
        } else {
            String docURL=BambiNSExtension.getDocURL(qe);
            if(docURL==null || docURL.length()<1) {
            	return null;
            }
        	JDFDoc theDoc = loadDocFromURL(docURL);
        	return new QueueEntry(theDoc,qe);      
        }
    }

    /**
     * get and parse a JDFDoc from an URL
     * @param docURL the location of the JDFDoc to get and parse. 
     *               May point to an HTTP URL or to a file.
     * @return a JDFDoc if successful, null if not
     */
	protected JDFDoc loadDocFromURL(String docURL) {
		JDFDoc theDoc=null;
        if (UrlUtil.isHttp(docURL)) {
        	String docStr="";
        	URL url;
        	BufferedReader br=null;
        	InputStream is=null;
			try {
				url = new URL(docURL);
				if (url==null) {
					log.error("can't create URL from String '"+docURL+"'");
				}
			
				URLConnection conn = (URLConnection) url.openConnection();
        		is=conn.getInputStream();
        		br=new BufferedReader( new InputStreamReader(is) );
        		String read=br.readLine();
        		while ( read!=null ) {
        			docStr+=read;
        			read=br.readLine();
        		}
        		
        		br.close();
        		is.close();   		
			} catch (MalformedURLException e) {
				log.error("failed to download file, invalid URL '"+docURL+"'");
			} catch (IOException e) {
				log.error("failed to download file from URL '"+docURL+"'. Error: "
						+e.getMessage());
			} finally {		
				try {
					if (is!=null) {
						is.close();
					}
					if (br!=null) {
						br.close();
					}
				} catch (IOException e) {
					log.error("failed to download file from URL '"+docURL+"'. Error: "
							+e.getMessage());
				}
			}
			
			JDFParser p=new JDFParser();
			theDoc=p.parseString(docStr);
        } else {
        	theDoc=JDFDoc.parseFile(docURL);
        }
		return theDoc;
	}
	
	/**
	 * get and parse a JDFDoc from a given file 
	 * @param file the full path of the  file to load the JDFDoc from
	 * @return a JDFDoc, null if the file could notbe read or an error occured
	 */
	protected JDFDoc loadDocFromFile(String file) {
		if ( !new File(file).canRead() ) {
			log.warn( "failed to load JDFDoc from file "+file );
			return null;
		}
		JDFDoc doc=JDFDoc.parseFile(file);
		return doc;
	}

    /* (non-Javadoc)
     * @see org.cip4.bambi.IQueueProcessor#addListener(java.lang.Object)
     */
    public void addListener(Object o)
    {
        log.info("adding new listener");
        _listeners.add(o);        
    }

     /* (non-Javadoc)
     * @see org.cip4.bambi.IQueueProcessor#addEntry(org.cip4.jdflib.jmf.JDFCommand, org.cip4.jdflib.core.JDFDoc)
     */
    public JDFResponse addEntry(JDFCommand submitQueueEntry, JDFDoc theJDF, boolean hold)
    {
        if(submitQueueEntry==null || theJDF==null)
        {
            log.error("error submitting new queueentry");
            return null;
        }
        if(!_theQueue.canAccept())
            return null;
        
        JDFQueueSubmissionParams qsp=submitQueueEntry.getQueueSubmissionParams(0);
        if(qsp==null)
        {
            log.error("error submitting new queueentry");
            return null;
        }
        
        JDFResponse r=qsp.addEntry(_theQueue, null);
        JDFQueueEntry newQE=r.getQueueEntry(0);
       
        if(r.getReturnCode()!=0 || newQE==null)
        {
            log.error("error submitting queueentry: "+r.getReturnCode());
            return r;
        }
         
        if(!storeDoc(newQE,theJDF,qsp.getReturnURL(),qsp.getReturnJMF()))
        {
            log.error("error storing queueentry: "+r.getReturnCode());
        }
        persist();
        notifyListeners();
        return r;
    }

    /**
     * @param newQE
     * @param theJDF
     */
    private boolean storeDoc(JDFQueueEntry newQE, JDFDoc theJDF, String returnURL, String returnJMF)
    {
        if(newQE==null || theJDF==null) {
            log.error("error storing queueentry");
            return false;
        }
        String newQEID=newQE.getQueueEntryID();
        newQE=_theQueue.getQueueEntry(newQEID);
        if(newQE==null) {
            log.error("error fetching queueentry: QueueEntryID="+newQEID);
            return false;
        }
        String baseDir=_appDir+"jmb/";
        String jdfDir=baseDir+"JDFDir/";
        String theDocFile=jdfDir+newQEID+".jdf";
        boolean ok=theJDF.write2File(theDocFile, 0, true);
        String docURL=_deviceURL+"?cmd=showJDFDoc&qeid="+newQEID;
        BambiNSExtension.setDocURL( newQE,docURL );
        if(!KElement.isWildCard(returnJMF)) {
        	BambiNSExtension.setReturnURL(newQE, returnJMF);
        } else if(!KElement.isWildCard(returnURL)) {
        	BambiNSExtension.setReturnURL(newQE, returnURL);
        }

        return ok;
    }

    private void notifyListeners()
    {
        for(int i=0;i<_listeners.size();i++) {
            final Object elementAt = _listeners.elementAt(i);
            synchronized (elementAt) {
                elementAt.notifyAll();               
            }
         }
    }

    /**
     * make the memory queue persistent
     *
     */
    protected synchronized void persist()
    {
        log.info("persisting queue to "+_queueFile.getAbsolutePath());
        _theQueue.getOwnerDocument_KElement().write2File(_queueFile.getAbsolutePath(), 0, true);
    }

    /* (non-Javadoc)
     * @see org.cip4.bambi.IQueueProcessor#getQueue()
     */
    public JDFQueue getQueue()
    {
        return _theQueue;
    }

    /* (non-Javadoc)
     * @see org.cip4.bambi.IQueueProcessor#updateEntry(java.lang.String, org.cip4.jdflib.auto.JDFAutoQueueEntry.EnumQueueEntryStatus)
     */
    public void updateEntry(String queueEntryID, EnumQueueEntryStatus status)
    {
        if(queueEntryID==null)
            return;
        JDFQueueEntry qe=getEntry(queueEntryID);
        if (qe == null)
        	return;
        qe.setQueueEntryStatus(status);
        if (status == EnumQueueEntryStatus.Completed || status == EnumQueueEntryStatus.Aborted) {
        	returnQueueEntry( qe,new VString("root",null) );
        }
        persist();
        notifyListeners();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        String s="[QueueProcessor: ] Status= "+_theQueue.getQueueStatus().getName()+" Num Entries: "+_theQueue.numEntries(null)+"\n Queue:\n";
        s+=_theQueue.toString();
        return s;
    }
    
    protected void returnQueueEntry(JDFQueueEntry qe, VString finishedNodes)
	{
		JDFDoc docJMF=new JDFDoc("JMF");
        JDFJMF jmf=docJMF.getJMFRoot();
        JDFCommand com=(JDFCommand) jmf.appendMessageElement(JDFMessage.EnumFamily.Command, JDFMessage.EnumType.ReturnQueueEntry);
        JDFReturnQueueEntryParams qerp = com.appendReturnQueueEntryParams();

        if (finishedNodes==null) {
        	finishedNodes=new VString("root",null);
        }
        
        if (qe.getStatus() == EnumNodeStatus.Completed) {
        	qerp.setCompleted( finishedNodes );
        } else if (qe.getStatus() == EnumNodeStatus.Aborted) {
        	qerp.setAborted( finishedNodes );
        }
        String returnURL=BambiNSExtension.getReturnURL(qe);
        qerp.setURL("cid:dummy"); // will be overwritten by buildMimePackage
        final String queueEntryID = qe.getQueueEntryID();
        qerp.setQueueEntryID(queueEntryID);
        String docFile=_jdfDir+qe.getQueueEntryID()+".jdf";
        JDFDoc docJDF = loadDocFromFile(docFile);
        if ( docJDF==null ) {
        	log.equals("cannot load the JDFDoc from "+docFile);
        	return;
        }
        Multipart mp = MimeUtil.buildMimePackage(docJMF, docJDF);
        if(returnURL!=null) {
        	HttpURLConnection response = null;
            try {
                response = MimeUtil.writeToURL(mp, returnURL);
                if (response.getResponseCode() == 200)
                    log.info("ReturnQueueEntry for "+queueEntryID+" has been sent.");
                else
                    log.error("failed to send ReturnQueueEntry. Response: "+response.toString());
            } catch (Exception e) {
                log.error("failed to send ReturnQueueEntry: "+e);
            }
            response = null;
        } else {
            // TODO write to default output
            log.warn("No return URL specified");
        }
	}

    protected String getMessageQueueEntryID(JDFMessage m)
    {
        JDFQueueEntryDef def = m.getQueueEntryDef(0);
        if (def == null) {
        	log.error("Message contains no QueueEntryDef");
            return null;
        }
        	
        String qeid = def.getQueueEntryID();
        if ( KElement.isWildCard(qeid) )  {
        	log.error("QueueEntryID does not contain any QueueEntryID");	
            return null;
        }
        log.info("processing getMessageQueueEntryID for "+qeid);
        return qeid;
    }

    private JDFQueueEntry getEntry(String queueEntryID) {
        JDFQueueEntry qe=_theQueue.getQueueEntry(queueEntryID);
		return qe;
	}
    
    /**
     * remove all Bambi namespace extensions from a given queue
     * @param queue the queue to filter
     * @return a queue without Bambi namespaces 
     */
    protected void removeBambiNSExtensions(JDFQueue queue) {   		
    	for (int i=0;i<queue.getQueueSize();i++) {
    		BambiNSExtension.removeBambiExtensions( queue.getQueueEntry(i) );
    	}
    }
    
    
    
    protected abstract void handleAbortQueueEntry(JDFResponse resp, String qeid,
			JDFQueueEntry qe);
    
    protected abstract void handleQueueStatus(JDFQueue q);
    
    protected abstract void handleSuspendQueueEntry(JDFResponse resp, String qeid,
			JDFQueueEntry qe);
    
    protected abstract void handleResumeQueueEntry(JDFResponse resp, String qeid,
			JDFQueueEntry qe);
    
	protected boolean loadProperties() {
		log.debug("loading properties");
		String configPath=_configDir+"device.properties";
		try  {
			Properties properties = new Properties();
			FileInputStream in = new FileInputStream(configPath);
			properties.load(in);
			
			JDFJMF.setTheSenderID(properties.getProperty("SenderID"));
			_deviceURL=properties.getProperty("DeviceURL");
			
			in.close();
		} catch (FileNotFoundException e) {
			log.fatal("properties not found at location "+configPath);
			return false;
		} catch (IOException e) {
			log.fatal("Error while loading properties from "+configPath);
			return false;
		}
		return true;
	}
    
}