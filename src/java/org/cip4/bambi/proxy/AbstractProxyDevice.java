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

package org.cip4.bambi.proxy;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cip4.bambi.core.AbstractDevice;
import org.cip4.bambi.core.BambiNSExtension;
import org.cip4.bambi.core.BambiServletRequest;
import org.cip4.bambi.core.BambiServletResponse;
import org.cip4.bambi.core.IConverterCallback;
import org.cip4.bambi.core.IDeviceProperties;
import org.cip4.jdflib.auto.JDFAutoQueueEntry.EnumQueueEntryStatus;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.jmf.JDFCommand;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFReturnQueueEntryParams;
import org.cip4.jdflib.jmf.JDFMessage.EnumFamily;
import org.cip4.jdflib.jmf.JDFMessage.EnumType;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.util.MimeUtil;
import org.cip4.jdflib.util.QueueHotFolder;
import org.cip4.jdflib.util.QueueHotFolderListener;
import org.cip4.jdflib.util.StringUtil;
import org.cip4.jdflib.util.UrlUtil;

public abstract class AbstractProxyDevice extends AbstractDevice {

    /**
     * 
     */
    public static final String SLAVEJMF = "slavejmf";
    private static final Log log = LogFactory.getLog(AbstractProxyDevice.class.getName());
    protected QueueHotFolder slaveJDFOutput=null;
    protected QueueHotFolder slaveJDFError=null;
    public enum EnumSlaveStatus {JMF,NODEINFO}
    protected EnumSlaveStatus slaveStatus=null;
    protected IConverterCallback _slaveCallback;
    protected String slaveURL=null;

    protected class ReturnHFListner implements QueueHotFolderListener
    {
        private EnumQueueEntryStatus hfStatus;
        /**
         * @param aborted
         */
        public ReturnHFListner(EnumQueueEntryStatus status)
        {
            hfStatus=status;
        }

        public void submitted(JDFJMF submissionJMF)
        {
            log.info("ReturnHFListner:submitted");
            JDFCommand command=submissionJMF.getCommand(0);
            JDFReturnQueueEntryParams rqp=command.getReturnQueueEntryParams(0);

            JDFDoc doc=rqp==null ? null : rqp.getURLDoc();
            if(doc==null)
            {
                log.warn("could not process JDF File");
                return;
            }
            if (_jmfHandler != null) {
                JDFNode n=doc.getJDFRoot();
                if(n==null)
                {
                    log.warn("could not process JDF File");
                    return;
                }

                // assume the rootDev was the executed baby...
                rqp.setAttribute(hfStatus.getName(), n.getID());
                // let the standard returnqe handler do the work
                JDFDoc responseJMF=_jmfHandler.processJMF(submissionJMF.getOwnerDocument_JDFElement());
                try
                {    
                    JDFJMF jmf=responseJMF.getJMFRoot();
                    JDFResponse r=jmf.getResponse(0);
                    if(r!=null && r.getReturnCode()==0)
                    {
                        UrlUtil.urlToFile(rqp.getURL()).delete();
                    }
                    else
                    {
                        log.error("could not process JDF File");
                    }
                }
                catch (Exception e)
                {
                    handleError(submissionJMF);
                }
            }        
        }

        /**
         * @param submissionJMF
         */
        private void handleError(JDFJMF submissionJMF)
        {
            log.error("error handling hf return");            
        }
    }

    /**
     * 
     * @author prosirai
     *
     */
    protected class XMLProxyDevice
    {

        XMLDevice d;
        /**
         * XML representation of this simDevice
         * fore use as html display using an XSLT
         * @param dev
         */
        public XMLProxyDevice(String contextPath, IProxyProperties prop)
        {
            d=new XMLDevice(true,contextPath);
            KElement root=d.getRoot();
            BambiNSExtension.setSlaveURL(root,prop.getSlaveURL());
        }        
    }

    public AbstractProxyDevice(IDeviceProperties properties) {
        super(properties);
        IProxyProperties proxyProperties=getProxyProperties();
        final File fDeviceJDFOutput = properties.getOutputHF();
        _slaveCallback=proxyProperties.getSlaveCallBackClass();
        slaveURL=proxyProperties.getSlaveURL();
        if(fDeviceJDFOutput!=null)
        {            
            File hfStorage=new File(_devProperties.getBaseDir()+File.separator+"HFDevTmpStorage"+File.separator+_devProperties.getDeviceID());
            log.info("Device output HF:"+fDeviceJDFOutput.getPath()+" device ID= "+proxyProperties.getSlaveDeviceID());
            JDFJMF rqCommand=JDFJMF.createJMF(EnumFamily.Command, EnumType.ReturnQueueEntry);
            slaveJDFOutput=new QueueHotFolder(fDeviceJDFOutput,hfStorage, null, new ReturnHFListner(EnumQueueEntryStatus.Completed), rqCommand);
        }

        final File fDeviceErrorOutput = properties.getErrorHF();
        if(fDeviceErrorOutput!=null)
        {
            File hfStorage=new File(_devProperties.getBaseDir()+File.separator+"HFDevTmpStorage"+File.separator+_devProperties.getDeviceID());
            log.info("Device error output HF:"+fDeviceErrorOutput.getPath()+" device ID= "+getSlaveDeviceID());
            JDFJMF rqCommand=JDFJMF.createJMF(EnumFamily.Command, EnumType.ReturnQueueEntry);
            slaveJDFError=new QueueHotFolder(fDeviceErrorOutput,hfStorage, null, new ReturnHFListner(EnumQueueEntryStatus.Aborted), rqCommand);
        } 
        _jmfHandler.setFilterOnDeviceID(false);
    }

    /* (non-Javadoc)
     * @see org.cip4.bambi.core.AbstractDevice#init()
     */
    @Override
    protected void init()
    {
        IProxyProperties dp=getProxyProperties();
        slaveURL=dp.getSlaveURL();
        super.init();
    }

    public IConverterCallback getSlaveCallback()
    {
        return _slaveCallback;
    }


    /* (non-Javadoc)
     * @see org.cip4.bambi.core.IDeviceProperties#getSlaveDeviceID()
     */
    public String getSlaveDeviceID()
    {
        // TODO - dynamically grab with knowndevices
        return getProxyProperties().getSlaveDeviceID();
    }


    /* (non-Javadoc)
     * @see org.cip4.bambi.core.AbstractDevice#shutdown()
     */
    @Override
    public void shutdown()
    {
        super.shutdown();
        if(slaveJDFError!=null)
            slaveJDFError.stop();
        if(slaveJDFOutput!=null)
            slaveJDFOutput.stop();
    }

    /**
     * @return
     */
    public EnumSlaveStatus getSlaveStatus()
    {
        String s=getProperties().getDeviceAttribute("SlaveStatus");
        if(s==null)
            return null;
        return EnumSlaveStatus.valueOf(s.toUpperCase());
    }

    /**
     * @return the slaveURL
     */
    public String getSlaveURL()
    {
        return slaveURL;
    }
    /**
     * @return the slaveURL
     */
    public String getDeviceURLForSlave()
    {
        return getProxyProperties().getDeviceURLForSlave();
    }
    
    @Override
    public IConverterCallback getCallback(String url)
    {
        if(StringUtil.hasToken(url, SLAVEJMF, "/",0))
            return _slaveCallback;
        return _callback;
    }

    /**
     * @return the proxyProperties
     */
    public IProxyProperties getProxyProperties()
    {
        return (IProxyProperties) _devProperties;
    }
    @Override
    protected boolean showDevice(BambiServletRequest request, BambiServletResponse response, boolean refresh)
    {
        XMLProxyDevice proxyDev=this.new XMLProxyDevice(request.getContextRoot(), getProxyProperties());
        try
        {
            proxyDev.d.write2Stream(response.getBufferedOutputStream(), 0,true);
        }
        catch (IOException x)
        {
            return false;
        }
        response.setContentType(MimeUtil.TEXT_XML);
        return true;
    }


}