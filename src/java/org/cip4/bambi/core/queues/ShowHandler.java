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
package org.cip4.bambi.core.queues;

import java.io.File;

import org.cip4.bambi.core.AbstractDevice;
import org.cip4.bambi.core.BambiContainer.UnknownErrorHandler;
import org.cip4.bambi.core.ContainerRequest;
import org.cip4.bambi.core.IGetHandler;
import org.cip4.bambi.core.XMLResponse;
import org.cip4.jdflib.core.AttributeName;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.KElement;

/**
 * @author Dr. Rainer Prosi, Heidelberger Druckmaschinen AG
 * 
 * 13.01.2009
 */
public abstract class ShowHandler implements IGetHandler
{
	/**
	 * @param device
	 */
	public ShowHandler(final AbstractDevice device)
	{
		super();
		_parentDevice = device;
	}

	protected final AbstractDevice _parentDevice;

	/**
	 * @see org.cip4.bambi.core.IGetHandler#handleGet(org.cip4.bambi.core.BambiServletRequest, org.cip4.bambi.core.BambiServletResponse)
	 */
	public XMLResponse handleGet(final ContainerRequest request)
	{
		if (!isMyRequest(request))
		{
			return null;
		}
		final String qeID = request.getParameter(QueueProcessor.QE_ID);
		final String fil = _parentDevice.getUpdatedFile(qeID);
		if (fil == null)
		{
			return errorShow(request, qeID);
		}
		final File f = new File(fil);
		if (!f.canRead())
		{
			return errorShow(request, qeID);
		}

		return processFile(request, f);
	}

	/**
	 * @param request
	 * @param qeid
	 * @return
	 */
	private XMLResponse errorShow(final ContainerRequest request, final String qeid)
	{
		final UnknownErrorHandler eh = new UnknownErrorHandler();
		eh.setDetails("Unknown QueueEntry: " + qeid);
		eh.setMessage("Cannot Show JDF");
		return eh.handleGet(request);
	}

	/**
	 * @param request
	 * @param f
	 * @return 
	 */
	protected abstract XMLResponse processFile(final ContainerRequest request, final File f);

	/**
	 * @param request
	 * @return 
	 */
	protected abstract boolean isMyRequest(final ContainerRequest request);

	/**
	 * prepare the root jdf for display with xslt
	 * @param doc
	 * @param request 
	 * @return 
	 */
	protected JDFDoc prepareRoot(JDFDoc doc, final ContainerRequest request)
	{
		final KElement root = (doc == null) ? null : doc.getRoot();
		final boolean raw = request.getBooleanParam("raw");
		if (raw || root == null)
		{
			return doc;
		}
		final boolean fix = request.getBooleanParam("fix");
		if (fix && _parentDevice.getCallback(null) != null)
		{
			doc = _parentDevice.getCallback(null).updateJDFForExtern(doc);
		}

		root.setAttribute(AttributeName.DEVICEID, _parentDevice.getDeviceID());
		if (doc == null)
		{
			return null;
		}
		doc.setXSLTURL(_parentDevice.getXSLT(request));
		root.setAttribute(AttributeName.CONTEXT, request.getContextRoot());
		final String qeID = request.getParameter(QueueProcessor.QE_ID);
		root.setAttribute(AttributeName.QUEUEENTRYID, qeID);
		return doc;
	}
}
