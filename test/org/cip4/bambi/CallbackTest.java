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

package org.cip4.bambi;

import java.io.File;

import org.cip4.bambi.core.AbstractDevice;
import org.cip4.bambi.core.IConverterCallback;
import org.cip4.bambi.core.SignalDispatcher;
import org.cip4.bambi.core.messaging.JMFHandler;
import org.cip4.bambi.workers.sim.SimDevice;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.JDFParser;
import org.cip4.jdflib.jmf.JDFJMF;
import org.cip4.jdflib.jmf.JDFQuery;
import org.cip4.jdflib.jmf.JDFSubscription;
import org.cip4.jdflib.jmf.JDFMessage.EnumFamily;
import org.cip4.jdflib.jmf.JDFMessage.EnumType;
import org.cip4.jdflib.util.StatusCounter;
import org.cip4.jdflib.util.UrlUtil;

public class CallbackTest extends BambiTestCase {


    /**
     * needed here to define the callback class
     * @author prosirai
     *
     */
    protected static class MyProp extends BambiTestProp
    {



        /* (non-Javadoc)
         * @see org.cip4.bambi.core.IDeviceProperties#getCallBackClassName()
         */
        public IConverterCallback getCallBackClass()
        {
            // TODO Auto-generated method stub

            String callback="org.cip4.bambi.MyTestCallback";
            if(callback!=null)
            {
                try
                {
//                  Class c=Class.forName("MyTestCallback");
                    Class c=Class.forName(callback);
                    return (IConverterCallback) c.newInstance();
                }
                catch (Exception x)
                {
                }
            }
            return null;

        }


        /* (non-Javadoc)
         * @see org.cip4.bambi.core.IDeviceProperties#getDeviceID()
         */
        public String getDeviceID()
        {
            // TODO Auto-generated method stub
            return "d1";
        }



        /* (non-Javadoc)
         * @see org.cip4.bambi.core.IDeviceProperties#getHotFolderURL()
         */
        public String getHotFolderURL()
        {
            // TODO Auto-generated method stub
            return sm_dirTestTemp+"HFURL";
        }

 

  
    }
    public void testAddCallback()
    {
        final MyProp myProp = new MyProp();
        JMFHandler h=new JMFHandler(null);
        SignalDispatcher d=new SignalDispatcher(h, null,null);
        System.out.println(new MyTestCallback().getClass().getCanonicalName());
        d.addHandlers(h);
        File f=new File(sm_dirTestData+"subscriptions.jmf");
        f.delete();
        JDFJMF jmf=JDFJMF.createJMF(EnumFamily.Query, EnumType.KnownMessages);
        JDFQuery q=jmf.getQuery(0);
        JDFSubscription s=q.appendSubscription();
        s.setRepeatTime(1.0);
        UrlUtil.urlToFile(getTestURL()).mkdirs();
        s.setURL(getTestURL()+"subscriptions.jmf");
        d.addSubscription(q, null);
        StatusCounter.sleep(2000);
        assertTrue( f.exists() ); 
        JDFDoc doc=new JDFParser().parseFile(f.getPath());
        JDFJMF jmf2=doc.getJMFRoot();
        assertEquals(jmf2.getAttribute("bambi:callback"), "updateJMFForExtern");
        d.shutdown();
        assertTrue( f.delete() );
    }

    public void testHFCallback() throws Exception
    {
        final MyProp myProp = new MyProp();
        AbstractDevice d=new SimDevice(myProp);
        String s=myProp.getHotFolderURL();
        new File(s).mkdirs();
        JDFDoc doc=new JDFDoc("JDF");
        doc.write2File(s+File.separator+"dummy.jdf", 2,true);
        Thread.sleep(2000);
        
        
    }
}
