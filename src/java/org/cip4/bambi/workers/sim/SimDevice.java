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

package org.cip4.bambi.workers.sim;

import java.util.Enumeration;
import java.util.Set;

import org.cip4.bambi.core.AbstractDeviceProcessor;
import org.cip4.bambi.core.BambiServletRequest;
import org.cip4.bambi.core.BambiServletResponse;
import org.cip4.bambi.core.IDeviceProperties;
import org.cip4.bambi.core.IGetHandler;
import org.cip4.bambi.workers.JobPhase;
import org.cip4.bambi.workers.UIModifiableDevice;
import org.cip4.jdflib.core.AttributeName;
import org.cip4.jdflib.util.ContainerUtil;
import org.cip4.jdflib.util.ThreadUtil;

/**
 * a simple JDF device with a fixed list of job phases. <br>
 * Job phases are defined in <code>/WebContend/config/devices.xml</code> and loaded in the constructor. They can be randomized, and random error phases can be
 * added. An example job phase is provided in <code>example_job.xml</code>.<br>
 * This class should remain final: if it is ever subclassed, the DeviceProcessor thread would be started before the constructor from the subclass has a chance
 * to fire.
 * @author boegerni
 */
public class SimDevice extends UIModifiableDevice implements IGetHandler
{
	/**
	 * 
	 */

	private static final long serialVersionUID = -8412710163767830461L;

	/**
	 * @author prosirai
	 */
	protected class XMLSimDevice extends XMLWorkerDevice
	{
		/**
		 * XML representation of this simDevice fore use as html display using an XSLT
		 * @param bProc
		 * @param request
		 */
		public XMLSimDevice(final boolean bProc, final BambiServletRequest request)
		{
			super(bProc, request);
			final JobPhase currentJobPhase = getCurrentJobPhase();
			if (currentJobPhase != null)
			{
				currentJobPhase.writeToParent(getRoot());
			}
		}
	}

	/**
	 * @param bProc if true add processors
	 * @param request
	 * @return
	 */
	@Override
	public XMLDevice getXMLDevice(final boolean bProc, final BambiServletRequest request)
	{
		final XMLDevice simDevice = this.new XMLSimDevice(bProc, request);
		return simDevice;
	}

	@Override
	protected boolean processNextPhase(final BambiServletRequest request, final BambiServletResponse response)
	{
		final JobPhase nextPhase = buildJobPhaseFromRequest(request);
		((SimDeviceProcessor) _deviceProcessors.get(0)).doNextPhase(nextPhase);
		ThreadUtil.sleep(1000); // allow device to switch phases before displaying page
		showDevice(request, response, false);
		return true;
	}

	/**
	 * @param prop the properties of the device
	 */
	public SimDevice(final IDeviceProperties prop)
	{
		super(prop);
	}

	@Override
	protected AbstractDeviceProcessor buildDeviceProcessor()
	{
		return new SimDeviceProcessor();
	}

	/**
	 * 
	 */
	private void updateTypeExpression(final String newTypeX)
	{
		final IDeviceProperties properties = getProperties();
		final String old = properties.getTypeExpression();
		if (!ContainerUtil.equals(old, newTypeX))
		{
			properties.setTypeExpression(newTypeX);
			properties.serialize();
		}
	}

	/**
	 * @param request
	 */
	@Override
	protected void updateDevice(final BambiServletRequest request)
	{
		super.updateDevice(request);

		final Enumeration<String> en = request.getParameterNames();
		final Set<String> s = ContainerUtil.toHashSet(en);

		final String exp = request.getParameter(AttributeName.TYPEEXPRESSION);
		if (exp != null && s.contains(AttributeName.TYPEEXPRESSION))
		{
			updateTypeExpression(exp);
		}
	}
}