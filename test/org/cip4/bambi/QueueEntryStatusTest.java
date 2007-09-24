package org.cip4.bambi;

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

import java.net.HttpURLConnection;

import javax.mail.Multipart;

import org.cip4.bambi.messaging.JMFFactory;
import org.cip4.jdflib.auto.JDFAutoQueueEntry.EnumQueueEntryStatus;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.core.VElement;
import org.cip4.jdflib.jmf.JDFCommand;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFMessage;
import org.cip4.jdflib.jmf.JDFQueue;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.jmf.JDFQueueSubmissionParams;
import org.cip4.jdflib.jmf.JDFResponse;
import org.cip4.jdflib.util.MimeUtil;

public class QueueEntryStatusTest extends BambiTestCase {
	
	/**
	 * tests are running on this device
	 */
	private final String deviceID = "device002";
	private String createdQueueEntryID=null;

	private JDFQueue getQueue() {
		JDFJMF jmf = JMFFactory.buildQueueStatus();
		JDFResponse resp = JMFFactory.send2Bambi(jmf,deviceID);
        assertTrue( resp!=null );
        assertEquals( 0,resp.getReturnCode() );

        JDFQueue qu = resp.getQueue(0);
        assertTrue( qu!=null );
		return qu;
	}
	
	private JDFQueueEntry getRunningQueueEntry() {
		JDFQueue qu = getQueue();
        VElement qev = qu.getQueueEntryVector();
        assertNotNull( qev );
        
        // find a runnig QueueEntry
        JDFQueueEntry runningQE = null;
        for (int i=0;i<qev.size();i++)
        {
        	JDFQueueEntry qe = (JDFQueueEntry) qev.get(i);
        	if ( EnumQueueEntryStatus.Running.equals(qe.getQueueEntryStatus()) ) {
        		runningQE = qe;
        		break;
        	}
        }
		return runningQE;
	}
	
	private EnumQueueEntryStatus getQueueEntryStatus(String queueEntryID)
	{
		JDFQueue qu = getQueue();
		JDFQueueEntry que = qu.getQueueEntry(queueEntryID);
		if ( que==null) {
		return null;
		} else {
			return que.getQueueEntryStatus();
		}
	}
	
	public void setUp() throws Exception 
	{
		super.setUp();
		
		JDFQueueEntry runningQE = getRunningQueueEntry();
        
        // submit a new qe if no running qe has been found
        if (runningQE == null) {
        	System.out.println("submitting new QueueEntry");
        	JDFDoc docJMF=new JDFDoc("JMF");
            JDFJMF jmfSubmit=docJMF.getJMFRoot();
            JDFCommand com = (JDFCommand)jmfSubmit.appendMessageElement(JDFMessage.EnumFamily.Command,JDFMessage.EnumType.SubmitQueueEntry);
            JDFQueueSubmissionParams qsp = com.appendQueueSubmissionParams();
            qsp.setURL( "cid:"+sm_dirTestData+"Elk_ConventionalPrinting.jdf" );
    	
    		JDFParser p = new JDFParser();
            JDFDoc docJDF = p.parseFile( sm_dirTestData+"Elk_ConventionalPrinting.jdf" );
            Multipart mp = MimeUtil.buildMimePackage(docJMF, docJDF);

            try {
            	HttpURLConnection response = MimeUtil.writeToURL( mp,BambiUrl+"/"+deviceID );
            	assertEquals( 200,response.getResponseCode() );
            	// give the device some time to start processing
                try {
        			Thread.sleep(1000);
        		} catch (InterruptedException e) {
        			e.printStackTrace();
        		}
            } catch (Exception e) {
            	fail( e.getMessage() ); 
            }
            runningQE = getRunningQueueEntry();
            if (runningQE!=null) {
            	createdQueueEntryID=runningQE.getQueueEntryID();
            }
        }
        
        assertNotNull( runningQE );
	}
	
	public void tearDown() throws Exception
	{
		// remove the created the created QueueEntry, if there is any
		if ( createdQueueEntryID!=null ) {
			JDFJMF jmf = JMFFactory.buildAbortQueueEntry(createdQueueEntryID);
			JDFResponse resp = JMFFactory.send2Bambi(jmf, deviceID);
			Thread.sleep(1000);
			if (resp.getReturnCode()!=105) {
				jmf = JMFFactory.buildRemoveQueueEntry(createdQueueEntryID);
				JMFFactory.send2Bambi(jmf, deviceID);
			}
		}
		
		super.tearDown();
	}
	
	public void testSuspendResumeQE()
	{
		JDFQueueEntry runningQE = getRunningQueueEntry();
		assertNotNull( runningQE);
		String qeID = runningQE.getQueueEntryID();
		
		JDFJMF jmf = JMFFactory.buildSuspendQueueEntry( qeID );
		JDFResponse resp = JMFFactory.send2Bambi(jmf, deviceID);
		assertEquals( 0,resp.getReturnCode() );
		// give the device some time to suspend the QE
        try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(EnumQueueEntryStatus.Suspended, getQueueEntryStatus(qeID) );
		
		jmf = JMFFactory.buildResumeQueueEntry( qeID );
		resp = JMFFactory.send2Bambi(jmf, deviceID);
		assertEquals( 0, resp.getReturnCode() );
		// give the device some time to resume the QE
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// now the qe should be either Waiting or Running
		EnumQueueEntryStatus status = getQueueEntryStatus(qeID);
		if (status.equals(EnumQueueEntryStatus.Running) || status.equals(EnumQueueEntryStatus.Waiting)) {
			assertTrue( true );
		} else {
			fail("status is "+status.getName()+", should be Running or Waiting");
		}
	}
	
	public void testAbortRemoveQE()
	{
		JDFQueueEntry runningQE = getRunningQueueEntry();
		assertTrue( runningQE!=null );
		String qeID = runningQE.getQueueEntryID();
		
		JDFJMF jmf = JMFFactory.buildAbortQueueEntry( qeID );
		JDFResponse resp = JMFFactory.send2Bambi(jmf, deviceID);
		assertEquals( 0, resp.getReturnCode() );
		// give the device some time to abort the QE
        try {
			Thread.sleep(2500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(EnumQueueEntryStatus.Aborted.getName(), getQueueEntryStatus(qeID).getName() );
		
		jmf = JMFFactory.buildRemoveQueueEntry( qeID );
		resp = JMFFactory.send2Bambi(jmf, deviceID);
		assertEquals( 0, resp.getReturnCode() );
		// give the device some time to remove the QE
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// now the qe should be gone
		assertNull("QueueEntry is still present",  getQueueEntryStatus(qeID));
	}
	
	public void testSuspendAbortQE()
	{
		JDFQueueEntry runningQE = getRunningQueueEntry();
		assertTrue( runningQE!=null );
		String qeID = runningQE.getQueueEntryID();
		
		JDFJMF jmf = JMFFactory.buildSuspendQueueEntry( qeID );
		JDFResponse resp = JMFFactory.send2Bambi(jmf, deviceID);
		assertEquals( 0, resp.getReturnCode() );
		// give the device some time to abort the QE
        try {
			Thread.sleep(2500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertEquals(EnumQueueEntryStatus.Suspended.getName(), getQueueEntryStatus(qeID).getName() );
		
		jmf = JMFFactory.buildAbortQueueEntry( qeID );
		resp = JMFFactory.send2Bambi(jmf, deviceID);
		assertEquals( 0, resp.getReturnCode() );
		// give the device some time to remove the QE
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// now the qe should be gone
		assertEquals(EnumQueueEntryStatus.Aborted.getName(), getQueueEntryStatus(qeID).getName());
	}
	
	public void testRogueWaves() throws Exception
	{
		for (int i=0;i<20;i++)
		{
			System.out.println("run #"+i);
			setUp();
			testSuspendResumeQE();
			tearDown();
			Thread.sleep(2000);
		}
	}
}