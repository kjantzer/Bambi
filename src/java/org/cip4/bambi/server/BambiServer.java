package org.cip4.bambi.server;

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
import java.io.File;
import java.net.MalformedURLException;

import org.apache.log4j.BasicConfigurator;
import org.cip4.bambi.core.BambiException;
import org.cip4.bambi.core.BambiLogFactory;
import org.cip4.bambi.core.BambiServlet;
import org.cip4.bambi.core.MultiDeviceProperties;
import org.cip4.jdflib.core.KElement;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

/**
 * standalone app for bambi using an embedded jetty server
 * @author rainer prosi
 * @date Dec 9, 2010
 */
public final class BambiServer extends BambiLogFactory
{

	private final int port;
	private String context;

	/**
	 * @throws BambiException if config file is not readable
	 */
	public BambiServer() throws BambiException
	{
		super();
		BasicConfigurator.configure();
		MultiDeviceProperties mp = new MultiDeviceProperties(new File("."), null, new File("config/devices.xml"));
		KElement root = mp.getRoot();
		if (root == null)
		{
			String logString = "cannot find config file at :" + new File("config/devices.xml").getAbsolutePath();
			log.fatal(logString);
			throw new BambiException(logString);
		}
		port = root.getIntAttribute("Port", null, 8080);
		context = root.getAttribute("Context", null, null);
		if (context == null)
		{
			String logString = "no context specified for servlet, bailing out";
			log.fatal(logString);
			throw new BambiException(logString);
		}
		if (!context.startsWith("/"))
			context = "/" + context;
		log.info("starting BambiServer at context: " + context + " port: " + port);
	}

	/**
	 * 
	 * TODO Please insert comment!
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		new BambiServer().runServer();
	}

	/**
	 * 
	 * the doing routine to run a jetty server
	 * @throws Exception
	 * @throws InterruptedException
	 */
	public final void runServer() throws Exception, InterruptedException
	{
		Server server = new Server(port);

		HandlerList handlers = new HandlerList();
		server.setHandler(handlers);

		ResourceHandler resourceHandler = createResourceHandler();
		handlers.addHandler(resourceHandler);

		ServletContextHandler context = createServletHandler();
		handlers.addHandler(context);

		server.start();
		server.join();
	}

	/**
	 * 
	 * simple resource (file) handler that tweeks the url to match the context, thus allowing servlets to emulate a war file without actually requiring the war file
	 * 
	 * @author rainer prosi
	 * @date Dec 10, 2010
	 */
	private class MyResourceHandler extends ResourceHandler
	{
		protected MyResourceHandler(String strip)
		{
			super();
			this.strip = strip;
		}

		private final String strip;

		@Override
		public Resource getResource(String arg0) throws MalformedURLException
		{
			if (arg0.startsWith(strip))
				arg0 = arg0.substring(strip.length());
			return super.getResource(arg0);
		}
	}

	protected ResourceHandler createResourceHandler()
	{
		ResourceHandler resourceHandler = new MyResourceHandler(context);
		resourceHandler.setResourceBase(".");
		return resourceHandler;
	}

	protected ServletContextHandler createServletHandler()
	{
		ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		contextHandler.setContextPath(context);
		contextHandler.setWelcomeFiles(new String[] { "index.jsp" });
		BambiServlet myServlet = new BambiServlet();
		contextHandler.addServlet(new ServletHolder(myServlet), "/*");
		return contextHandler;
	}

}