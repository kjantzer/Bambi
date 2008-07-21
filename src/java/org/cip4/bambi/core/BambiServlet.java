/*
 *
 * The CIP4 Software License, Version 1.0
 *
 *
 * Copyright (c) 2001-2008 The International Cooperation for the Integration of 
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.enums.ValuedEnum;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cip4.bambi.core.messaging.IJMFHandler;
import org.cip4.bambi.core.messaging.JMFFactory;
import org.cip4.bambi.core.messaging.MessageSender;
import org.cip4.jdflib.auto.JDFAutoQueueEntry.EnumQueueEntryStatus;
import org.cip4.jdflib.core.ElementName;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.core.VElement;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.jmf.JDFMessage.EnumFamily;
import org.cip4.jdflib.jmf.JDFMessage.EnumType;
import org.cip4.jdflib.util.DumpDir;
import org.cip4.jdflib.util.FileUtil;
import org.cip4.jdflib.util.MimeUtil;
import org.cip4.jdflib.util.StringUtil;
import org.cip4.jdflib.util.UrlUtil;

/**
 * Entrance point for Bambi servlets 
 * @author boegerni
 *
 */
public class BambiServlet extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 1926504814491033980L;

    /**
     * handler for final handler for any non-handled url
     * @author prosirai
     *
     */
    protected class UnknownErrorHandler implements IGetHandler
    {

        /* (non-Javadoc)
         * @see org.cip4.bambi.core.IGetHandler#handleGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
         */
        public boolean handleGet(BambiServletRequest request, BambiServletResponse response)
        {

            showErrorPage("No handler for URL", request.getPathInfo(), request, response);
            return true;
        }
    }

    /**
     * handler for the overview page
     * @author prosirai
     *
     */
    protected class OverviewHandler implements IGetHandler
    {
        /* (non-Javadoc)
         * @see org.cip4.bambi.core.IGetHandler#handleGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
         */
        public boolean handleGet(BambiServletRequest request, BambiServletResponse response)
        {
            String context=getContext(request);
            if(KElement.isWildCard(context)||context.equalsIgnoreCase("overview")&&(rootDev instanceof AbstractDevice))
            {
                return ((AbstractDevice)rootDev).showDevice(request, response,false);
            }
            else
                return false;
        }
    }

    //protected IConverterCallback _callBack = null;
    private static Log log = LogFactory.getLog(BambiServlet.class.getName());
    protected IDevice rootDev=null;
    private List<IGetHandler> _getHandlers=new Vector<IGetHandler>();
    protected DumpDir bambiDumpIn=null;
    protected DumpDir bambiDumpOut=null;
    public static int port=0;

    /** Initializes the servlet.
     * @throws MalformedURLException 
     */
    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        ServletContext context = config.getServletContext();       
        String dump=config.getInitParameter("bambiDump");
        log.info( "Initializing servlet for "+context.getServletContextName() );

        loadProperties(context,new File("/config/devices.xml"),dump);

        // doGet handlers
        _getHandlers.add(this.new OverviewHandler());
    }

    /**
     * create the specified directories, if the do not exist
     * @param dirs the directories to create
     */
    private void createDirs(Vector<File> dirs) {
        for (int i=0;i<dirs.size();i++) {
            File f=dirs.get(i);
            if (f!=null && !f.exists()) {
                if (!f.mkdirs())
                    log.error("failed to create directory "+f);
            }
        }
    }


    /**
     * display an error on error.jsp
     * @param errorMsg short message describing the error
     * @param errorDetails detailed error info
     * @param request required to forward the page
     * @param response required to forward the page
     */
    protected void showErrorPage(String errorMsg, String errorDetails, BambiServletRequest request, BambiServletResponse response)
    {
        request.setAttribute("errorOrigin", this.getClass().getName());
        request.setAttribute("errorMsg", errorMsg);
        request.setAttribute("errorDetails", errorDetails);

        try {
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        } catch (ServletException e) {
            System.err.println("failed to show error.jsp");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("failed to show error.jsp");
            e.printStackTrace();
        }
    }

    /**
     * @param request
     * @param response
     */
    protected void processError(BambiServletRequest request, BambiServletResponse response, EnumType messageType, int returnCode, String notification)
    {
        log.warn("processError- rc: "+returnCode+" "+notification==null ? "" : notification);
        JDFJMF error=JDFJMF.createJMF(EnumFamily.Response, messageType);
        JDFResponse r=error.getResponse(0);
        r.setReturnCode(returnCode);
        r.setErrorText(notification);
        response.setContentType(UrlUtil.VND_JMF);
        IConverterCallback _callBack=getCallBack(request);
        if(_callBack!=null)
            _callBack.updateJMFForExtern(error.getOwnerDocument_JDFElement());

        try {
            error.getOwnerDocument_KElement().write2Stream(response.getBufferedOutputStream(), 0, true);
        } catch (IOException x) {
            log.error("processError: cannot write response\n"+x.getMessage());
        }
    }

    /**
     * process a multipart request - including job submission
     * @param request
     * @param response
     */
    protected void processMultipleDocuments(BambiServletRequest request, BambiServletResponse response,BodyPart[] bp)
    {
        log.info("processMultipleDocuments- parts: "+(bp==null ? 0 : bp.length));
        if(bp==null || bp.length<2) {
            processError(request, response, EnumType.Notification, 2,"processMultipleDocuments- not enough parts, bailing out");
            return;
        }
        JDFDoc docJDF[]=MimeUtil.getJMFSubmission(bp[0].getParent());
        if(docJDF==null) {
            processError(request, response, EnumType.Notification, 2,"proccessMultipleDocuments- incorrect jmf/jdf parts, bailing out!");
            return;
        }
        IConverterCallback _callBack=getCallBack(request);

        if(_callBack!=null)
        {
            for(int i=0;i<docJDF.length;i++)
            {
                final JDFDoc doc = docJDF[i];
                KElement e=doc.getRoot();

                final String localName = e==null ? null : e.getLocalName();
                if(ElementName.JMF.equals(localName))
                {
                    _callBack.prepareJMFForBambi(doc);
                }
                else if(ElementName.JDF.equals(localName))
                {
                    _callBack.prepareJDFForBambi(doc);
                }                
            }
        }
        processJMFDoc(request, response, docJDF[0]);
    }

    /**
     * process zhe main, i.e. doc #0 JMF document
     * 
     * @param request the http request to service
     * @param response the http response to fill
     * @param jmfDoc the extracted first jmf bodypart or raw jmf
     */
    protected void processJMFDoc(BambiServletRequest request, BambiServletResponse response, JDFDoc jmfDoc) {
        if(jmfDoc==null) 
        {
            processError(request, response, null, 3, "Error Parsing JMF");
        } 
        else 
        {
            IConverterCallback _callBack=getCallBack(request);

            if(_callBack!=null)
            {
                _callBack.prepareJMFForBambi(jmfDoc);
            }

            // switch: sends the jmfDoc to correct device
            JDFDoc responseJMF = null;
            IJMFHandler handler = getTargetHandler(request);
            if (handler != null) {
                responseJMF=handler.processJMF(jmfDoc);
            } 

            if (responseJMF!=null) {
                response.setContentType(UrlUtil.VND_JMF);
                if(_callBack!=null)
                    _callBack.updateJMFForExtern(responseJMF);

                try 
                {
                    responseJMF.write2Stream(response.getBufferedOutputStream(), 0, true);
                } 
                catch (IOException e) 
                {
                    log.error("cannot write to stream: ",e);
                }
            } else {
//              processError(request, response, null, 3, "Error Parsing JMF");               
            }
        }
    }

    /**
     * @param request
     * @return
     */
    private IConverterCallback getCallBack(BambiServletRequest request)
    {
       if(rootDev instanceof AbstractDevice)
       {
           Map m=request.getParameterMap();
           return ((AbstractDevice)rootDev).getCallback(request.getRequestURI());
       }
       else
           return null;
    }

    protected IJMFHandler getTargetHandler(HttpServletRequest request)
    {
        IDevice device =  getDeviceFromRequest(request);
        if (device == null)
            return rootDev.getHandler(); // device not found
        return( device.getHandler() );
    }

    /**
     * Parses a multipart request.
     */
    protected void processMultipartRequest(BambiServletRequest request, BambiServletResponse response)
    throws IOException {
        InputStream inStream=request.getBufferedInputStream();
        BodyPart bp[]=MimeUtil.extractMultipartMime(inStream);
        log.info("Body Parts: "+((bp==null) ? 0 : bp.length));
        if(bp==null || bp.length==0) {
            processError(request,response,null,9,"No body parts in mime package");
            return;
        }
        try  {// messaging exceptions
            if(bp.length>1) {
                processMultipleDocuments(request,response,bp);
            } else {
                String s=bp[0].getContentType();
                if(UrlUtil.VND_JMF.equalsIgnoreCase(s)) {
                    processJMFRequest(request, response, bp[0].getInputStream());            
                }
            }
        } catch (MessagingException x) {
            processError(request, response, null, 9, "Messaging exception\n"+x.getLocalizedMessage());
        }
    }

    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws IOException 
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
    throws IOException	{
        log.debug("Processing post request for: "+request.getPathInfo());
        BambiServletRequest bufRequest=null;
        BambiServletResponse bufResponse=null;

        String header="Context Path: "+request.getContextPath();
        header+="\nMethod: Post\nContext Type: "+request.getContentType();
        if(bambiDumpIn!=null)
        {
            bufRequest=new BambiServletRequest(request,true);
            bufResponse=new BambiServletResponse(response,true);

            header+="\nContext Length: "+request.getContentLength();
            bambiDumpIn.newFileFromStream(header,bufRequest.getBufferedInputStream());
        }
        else
        {
            bufRequest=new BambiServletRequest(request,false);     
            bufResponse=new BambiServletResponse(response,false);

        }

        String contentType=request.getContentType();
        if(UrlUtil.VND_JMF.equals(contentType)) 
        {
            processJMFRequest(bufRequest,bufResponse,null);
        } 
        else if( UrlUtil.TEXT_XML.equals(contentType))
        {
            processXMLRequest(bufRequest, bufResponse);            
        }
        else 
        {
            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            if (isMultipart) {
                log.info("Processing multipart request... (ContentType: "+contentType+")");
                processMultipartRequest(bufRequest, bufResponse);
            } else {
                log.warn("Unknown ContentType: "+contentType);
                response.setContentType("text/plain");

                OutputStream os=bufResponse.getBufferedOutputStream();
                InputStream is=bufRequest.getBufferedInputStream();
                IOUtils.copy(is,os);
            }
        }
        if(bambiDumpOut!=null)
        {
            InputStream buf=bufResponse.getBufferedInputStream();
            File f=bambiDumpOut.newFileFromStream(header,buf);
        }
        bufResponse.flush();
    }

    /**
     * @param bufRequest
     * @param bufResponse
     */
    private void processXMLRequest(BambiServletRequest bufRequest, BambiServletResponse bufResponse)
    {
        //TODO some smarts whether JDF or JMF
        log.info("Processing text/xml");
        processJMFRequest(bufRequest,bufResponse,null);
    }

    /**
     * @param request
     * @param response
     */
    private void processJMFRequest(BambiServletRequest request, BambiServletResponse response,InputStream inStream) 
    {
        log.debug("processJMFRequest");
        JDFParser p=new JDFParser();
        if(inStream==null)
            inStream=request.getBufferedInputStream();
        JDFDoc jmfDoc=p.parseStream(inStream);
        processJMFDoc(request, response, jmfDoc);
    }

    /**
     * loads properties
     * @param context the serbvlet context information
     * @param config the name of the Java config xml file
     */
    protected void loadProperties(ServletContext context, File config, String dump)
    {
        MultiDeviceProperties props=new MultiDeviceProperties(context,config);
        createDevices(props, dump);
    }

    /** 
     * Destroys the servlet.
     */
    @Override
    public void destroy() 
    {
        rootDev.shutdown();
        JMFFactory.shutDown(null, true);
    }

    /**
     * @param request
     */
    protected IDevice getDeviceFromRequest(HttpServletRequest request)
    {
        String deviceID = getDeviceIDFromRequest(request);
        RootDevice root=getRootDevice();
        IDevice dev = root==null ? rootDev : root.getDevice(deviceID); 
        if (dev == null) {
            log.info("invalid request: device with id="+deviceID==null?"null":deviceID+" not found");
            return null;
        }		
        return dev;
    }

    /**
     * 
     * @param request the request to parse
     * @return
     */
    public static String getDeviceIDFromRequest(HttpServletRequest request)
    {
        String deviceID = request.getParameter("id");
        if (deviceID == null) {
            deviceID = request.getPathInfo();
            deviceID = getDeviceIDFromURL(deviceID);
        }
        return deviceID;        
    }

    /**
     * @param deviceID
     * @return
     */
    public static String getDeviceIDFromURL(String deviceID)
    {
        deviceID = StringUtil.token(deviceID, -1, "/");
        return deviceID;
    }
    /**
     * add a set of options to an xml file
     * @param e the default enum
     * @param it the iterator over all enums
     * @param parent the parent element to add the list to
     * @param name the name of the option list form
     */
    public static void addOptionList(ValuedEnum e, List<EnumQueueEntryStatus> l,KElement parent, String name)
    {
        if(e==null || parent==null)
            return;
        KElement list=parent.appendElement(BambiNSExtension.MY_NS_PREFIX+"OptionList",BambiNSExtension.MY_NS); 
        list.setAttribute("name", name);
        list.setAttribute("default", e.getName());
        Iterator<EnumQueueEntryStatus> it=l.iterator();
        while(it.hasNext())
        {
            ValuedEnum ve=it.next();
            KElement option=list.appendElement(BambiNSExtension.MY_NS_PREFIX+"Option",BambiNSExtension.MY_NS);
            option.setAttribute("name", ve.getName());
            option.setAttribute("selected", ve.equals(e)?"selected":null,null);
        }
    }



    /**
     * extract a double attribute from a given request
     * @param request
     * @param param
     * @return
     */
    public static double getDoubleFromRequest(HttpServletRequest request, String param)
    {
        final String val = request.getParameter(param);
        return StringUtil.parseDouble(val, 0.0);
    }

    /**
     * extract a boolean attribute from a given request
     * @param request
     * @param param
     * @return true if the parameter is"true", else false
     */
    public static boolean getBooleanFromRequest(HttpServletRequest request, String param)
    {
        final String val = request.getParameter(param);
        return StringUtil.parseBoolean(val, false);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    {
        boolean bHandled=false;
        BambiServletRequest bufRequest=null;
        BambiServletResponse bufResponse=null;

        String header="Context Path: "+request.getContextPath();
        header+="\nMethod: Get\nContext Type: "+request.getContentType();
        if(bambiDumpIn!=null)
        {
            bufRequest=new BambiServletRequest(request,true);
            bufResponse=new BambiServletResponse(response,true);

            header+="\nContext Length: "+request.getContentLength();
            bambiDumpIn.newFileFromStream(header,bufRequest.getBufferedInputStream());
        }
        else
        {
            bufRequest=new BambiServletRequest(request,false);     
            bufResponse=new BambiServletResponse(response,false);

        }

        try
        {
            final int size = _getHandlers.size();
            // simply loop over all handlers until you are done
            for(int i=0;i<size;i++)
            {
                IGetHandler ig=_getHandlers.get(i);
                bHandled=ig.handleGet(bufRequest, bufResponse);
                if(bHandled)
                    break;
            }
            // rootDev also dispatches to all other devices
            if(!bHandled && getRootDevice()!=null)
                bHandled = getRootDevice().handleGet(bufRequest, bufResponse);
        }
        catch (Exception x)
        {
            int i=0;
            // nop
        }
        if(!bHandled)
            this.new UnknownErrorHandler().handleGet(bufRequest, bufResponse);

        if(bambiDumpOut!=null)
        {
            InputStream buf=bufResponse.getBufferedInputStream();
            File f=bambiDumpOut.newFileFromStream(header,buf);
        }
        bufResponse.flush();

    }

    /**
     * @return
     */
    private RootDevice getRootDevice()
    {
        return (rootDev instanceof RootDevice) ? (RootDevice)rootDev : null;
    }

    /**
     * get the static context string
     * @param request
     * @return
     */
    public static String getContext(HttpServletRequest request)
    {
        String context = request.getParameter("cmd");
        if(context==null)
        {
            context=UrlUtil.getLocalURL(request.getContextPath(),request.getRequestURI());
        }
        return context;
    }
    /**
     * get the static context string
     * @param request
     * @return
     */
    public static String getBaseServletName(HttpServletRequest request)
    {
        return StringUtil.token(request.getRequestURI(), 0, "/");
    }

    public static boolean isMyRequest(HttpServletRequest request,final String deviceID)
    {
        if(deviceID==null)
            return true;
        final String reqDeviceID=getDeviceIDFromRequest(request);
        return reqDeviceID==null || deviceID.equals(reqDeviceID);           
    }
    /**
     * 
     * @param request
     * @param context
     * @return
     */
    public static boolean isMyContext(HttpServletRequest request, String context)
    {
        if(context==null)
            return true;

        String reqContext=getContext(request);
        return context.equals(StringUtil.token(reqContext, 0, "/"));

    }

    /**
     * create devices based on the list of devices given in a file
     * @param props 
     * @param configFile the file containing the list of devices 
     * @return true if successfull, otherwise false
     */
    protected boolean createDevices(MultiDeviceProperties props, String dump)
    {

        Vector<File> dirs=new Vector<File>();
        dirs.add( props.getBaseDir() );
        dirs.add( props.getJDFDir() );
        createDirs(dirs);

        VElement v=props.getDevices();
        Iterator<KElement> iter=v.iterator();
        boolean needController =  v.size()>1;
        while (iter.hasNext()) {
            IDeviceProperties prop=props.createDevice(iter.next());
            IDevice d=null;
            if(rootDev==null)
            {
                if(needController)
                {
                    d=prop.getDeviceInstance(); 
                    if(!(d instanceof RootDevice))
                    {
                        d.shutdown();
                        d=new RootDevice(prop);
                    }
                }
                else
                {
                    d=prop.getDeviceInstance(); 
                }
                rootDev=d;
            }
            else
            {
                RootDevice rd=getRootDevice();
                d=rd.createDevice(prop,this);

            }
            if(dump!=null )
            {
                bambiDumpIn=new DumpDir(FileUtil.getFileInDirectory(new File(dump), new File("in")));
                bambiDumpOut=new DumpDir(FileUtil.getFileInDirectory(new File(dump), new File("out")));
                MessageSender.addDumps(d!=null ? d.getDeviceID() : "Bambi",bambiDumpIn,bambiDumpOut);
            }
            if(d instanceof IGetHandler)
                _getHandlers.add(0,(IGetHandler)d);

        }
        return true;
    }

    @Override
    protected void service(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException
    {
        // TODO find correct server port at startup
        if(port==0) // quick hack
            port=arg0.getServerPort();
        super.service(arg0, arg1);

    }

}