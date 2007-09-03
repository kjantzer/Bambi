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

package org.cip4.bambi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cip4.bambi.AbstractDeviceProcessor.JobPhase;

/**
 * a simple JDF device.<br>
 * A CustomDevice does not contain a fixed list of JobPhases. After a QueueEntry has been 
 * submitted, it starts with an idle job phase. Following job phases have to be added via 
 * the web interface. Processing of the QueueEntry finishes when ordered by the user. <br>
 * This class should remain final: if it is ever subclassed, the DeviceProcessor thread 
 * would be started before the constructor from the subclass has a chance to fire.
 * 
 * 
 * @author boegerni
 * 
 */
public final class CustomDevice extends AbstractDevice   {
	

	private static Log log = LogFactory.getLog(CustomDevice.class.getName());
	
	public CustomDevice(String deviceType, String deviceID, String deviceClass)
	{
		super(deviceType, deviceID, deviceClass);
		log.info("created CustomDevice");
	}

	public JobPhase getCurrentJobPhase()
	{
		return _theDeviceProcessor.getCurrentJobPhase();
	}
	
	public void doNextJobPhase(JobPhase nextPhase)
	{
		if (_theDeviceProcessor instanceof  CustomDeviceProcessor)
		{
			log.info("ordering next job phase: "+nextPhase.toString());
			((CustomDeviceProcessor)_theDeviceProcessor).doNextPhase(nextPhase);
		}
		else
		{
			String errorMsg = "device processor has wrong type\r\n"+
				"expected: org.cip4.Bambi.CustomDeviceProcessor\r\n" +
				"actual: " +_theDeviceProcessor.getClass().getName();
			log.fatal(errorMsg);
		}
	}
	
	public void finalizeCurrentQueueEntry()
	{
		if (_theDeviceProcessor instanceof  CustomDeviceProcessor)
		{
			log.info("processing of the current QueueEntry on device "+_deviceID+" is being finished");
			((CustomDeviceProcessor)_theDeviceProcessor).finalizeQueueEntry();
		}
		else
		{
			String errorMsg = "device processor has wrong type\r\n"+
				"expected: org.cip4.Bambi.CustomDeviceProcessor\r\n" +
				"actual: " +_theDeviceProcessor.getClass().getName();
			log.fatal(errorMsg);
		}
		
		
	}
}