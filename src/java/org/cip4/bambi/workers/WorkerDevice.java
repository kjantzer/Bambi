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

package org.cip4.bambi.workers;

import java.io.File;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.cip4.bambi.core.AbstractDevice;
import org.cip4.bambi.core.BambiServlet;
import org.cip4.bambi.core.BambiServletRequest;
import org.cip4.bambi.core.BambiServletResponse;
import org.cip4.bambi.core.IDeviceProperties;
import org.cip4.bambi.core.IGetHandler;
import org.cip4.bambi.core.messaging.JMFHandler;
import org.cip4.jdflib.auto.JDFAutoNotification.EnumClass;
import org.cip4.jdflib.auto.JDFAutoQueueEntry.EnumQueueEntryStatus;
import org.cip4.jdflib.core.AttributeName;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.core.JDFResourceLink;
import org.cip4.jdflib.core.KElement;
import org.cip4.jdflib.core.VElement;
import org.cip4.jdflib.core.VString;
import org.cip4.jdflib.core.XMLDoc;
import org.cip4.jdflib.jmf.JDFQueueEntry;
import org.cip4.jdflib.node.JDFNode;
import org.cip4.jdflib.resource.process.JDFEmployee;
import org.cip4.jdflib.util.ContainerUtil;
import org.cip4.jdflib.util.FileUtil;
import org.cip4.jdflib.util.StringUtil;

/**
 * a simple data input terminal/console JDF device . <br>
 * @author Rainer Prosi
 */
public abstract class WorkerDevice extends AbstractDevice implements IGetHandler
{
	/**
	 * 
	 */

	private static final long serialVersionUID = -8412710163767830461L;
	protected String _trackResource = null; // the "major" resource to track
	protected VString amountResources = null;
	protected String _typeExpression = null; // the regexp that defines the valid types
	protected EmployeeList employees;

	/**
	 * @see org.cip4.bambi.core.AbstractDevice#canAccept(org.cip4.jdflib.core.JDFDoc, java.lang.String)
	 */
	@Override
	public int canAccept(final JDFDoc doc, final String queueEntryID)
	{
		if (queueEntryID != null)
		{
			final JDFQueueEntry qe = getQueueProcessor().getQueue().getQueueEntry(queueEntryID);
			if (qe == null)
			{
				log.error("no qe: " + queueEntryID);
				return 105;
			}
			if (EnumQueueEntryStatus.Running.equals(qe.getQueueEntryStatus()))

			{
				JMFHandler.errorResponse(null, "Queuentry already running - QueueEntryID: " + queueEntryID, 106, EnumClass.Error);
			}
		}

		if (doc != null && _typeExpression == null)
		{
			return 0;
		}
		return getAcceptableNodes(doc) == null ? 101 : 0;
	}

	/**
	 * @see org.cip4.bambi.core.AbstractDevice#getNodeFromDoc(org.cip4.jdflib.core.JDFDoc)
	 */
	@Override
	public JDFNode getNodeFromDoc(final JDFDoc doc)
	{
		final VElement v = getAcceptableNodes(doc);
		return (JDFNode) (v == null ? null : v.get(0));
	}

	/**
	 * @param doc
	 * @return
	 */
	public VElement getAcceptableNodes(final JDFDoc doc)
	{
		// TODO plug in devcaps
		if (doc == null)
		{
			return null;
		}

		final JDFNode n = doc.getJDFRoot();
		if (n == null)
		{
			return null;
		}
		final VElement v = n.getvJDFNode(null, null, false);
		for (int i = v.size() - 1; i >= 0; i--)
		{
			final JDFNode n2 = (JDFNode) v.elementAt(i);
			if (!canAccept(n2))
			{
				v.remove(n2);
			}
		}
		return v.size() == 0 ? null : v;
	}

	/**
	 * @param n2
	 */
	private boolean canAccept(final JDFNode n2)
	{
		final String types = n2.getTypesString();
		return StringUtil.matches(types, _typeExpression);
	}

	/**
	 * @author prosirai
	 */
	protected class XMLWorkerDevice extends XMLDevice
	{
		/**
		 * XML representation of this simDevice fore use as html display using an XSLT
		 * @param bProc
		 * @param request
		 */
		public XMLWorkerDevice(final boolean bProc, final BambiServletRequest request)
		{
			super(bProc, request);
			final KElement deviceRoot = getRoot();
			deviceRoot.setAttribute(AttributeName.TYPEEXPRESSION, getProperties().getTypeExpression());
			deviceRoot.setAttribute("login", true, null);
			addEmployees();
			addKnownEmployees();
		}

		/**
		 * 
		 */
		private void addKnownEmployees()
		{
			final KElement deviceRoot = getRoot();
			final KElement knownEmps = deviceRoot.appendElement("KnownEmployees");
			final Vector<JDFEmployee> vEmpLoggedIn = _theStatusListener.getStatusCounter().getEmpoyees();
			final Vector<JDFEmployee> vEmp = employees.vEmp;
			for (int i = 0; i < vEmp.size(); i++)
			{
				final JDFEmployee knownEmp = vEmp.get(i);
				boolean bAdd = true;
				if (vEmpLoggedIn != null)
				{
					for (final Iterator iterator = vEmpLoggedIn.iterator(); iterator.hasNext();)
					{
						final JDFEmployee employee = (JDFEmployee) iterator.next();
						if (knownEmp.matches(employee))
						{
							bAdd = false;
							break;
						}

					}
				}
				if (bAdd)
				{
					knownEmps.copyElement(knownEmp, null);
				}
			}
		}

		/**
		 * add the currently logged employees, duh
		 */
		private void addEmployees()
		{
			final KElement deviceRoot = getRoot();
			final Vector<JDFEmployee> vEmp = _theStatusListener.getStatusCounter().getEmpoyees();
			for (final Iterator<JDFEmployee> iterator = vEmp.iterator(); iterator.hasNext();)
			{
				final JDFEmployee employee = iterator.next();
				deviceRoot.copyElement(employee, null);
			}
		}
	}

	protected class EmployeeList
	{

		/**
		 * @param emp
		 */
		public EmployeeList(final Vector<JDFEmployee> emp)
		{
			super();
			vEmp = emp;
		}

		protected Vector<JDFEmployee> vEmp; // the list of all employees

		/**
		 * @param personalID
		 * @return
		 */
		public JDFEmployee getEmployee(final String personalID)
		{
			if (vEmp == null)
			{
				return null;
			}
			for (final Iterator<JDFEmployee> iterator = vEmp.iterator(); iterator.hasNext();)
			{
				final JDFEmployee emp = iterator.next();
				if (emp.matches(personalID))
				{
					return emp;
				}
			}
			return null;
		}

	}

	/**
	 * 
	 * @author Dr. Rainer Prosi, Heidelberger Druckmaschinen AG
	 * 
	 * Sep 29, 2009
	 */
	protected class EmployeeLoader
	{
		private final File employeeFile;
		private File employeePath;

		/**
		 * 
		 */
		public EmployeeLoader()
		{
			employeeFile = new File("employees.xml");
			employeePath = null;
		}

		protected EmployeeList load()
		{
			File deviceDir = getProperties().getBaseDir();
			deviceDir = FileUtil.getFileInDirectory(deviceDir, new File("config"));
			Vector<JDFEmployee> v = loadFile(deviceDir);
			if (v == null)
			{
				v = loadFile(getProperties().getConfigDir());
			}
			if (v != null)
			{
				FileUtil.copyFileToDir(employeePath, deviceDir);
			}
			else
			{
				v = new Vector<JDFEmployee>();
			}
			return new EmployeeList(v);
		}

		/**
		 * @param deviceDir
		 * @return
		 */
		private Vector<JDFEmployee> loadFile(final File deviceDir)
		{
			final File employeePathLocal = FileUtil.getFileInDirectory(deviceDir, employeeFile);
			final XMLDoc d = JDFDoc.parseFile(employeePathLocal.getAbsolutePath());
			final KElement root = d == null ? null : d.getRoot();
			if (root == null)
			{
				return null;
			}
			final VElement v = root.getChildElementVector("Employee", null);
			if (v == null)
			{
				return null;
			}
			employeePath = employeePathLocal;
			final Vector<JDFEmployee> vPA = new Vector<JDFEmployee>(v.size());
			for (int i = 0; i < v.size(); i++)
			{
				final JDFEmployee pa = (JDFEmployee) v.get(i);
				vPA.add(pa);
			}
			return vPA;

		}

	}

	/**
	 * @return
	 */
	public String getTrackResource()
	{
		return _trackResource;
	}

	/**
	 * @see org.cip4.bambi.core.AbstractDevice#handleGet(org.cip4.bambi.core.BambiServletRequest, org.cip4.bambi.core.BambiServletResponse)
	 * @param request
	 * @param response
	 * @return true if handled
	 */
	@Override
	public boolean handleGet(final BambiServletRequest request, final BambiServletResponse response)
	{
		if (isMyRequest(request))
		{
			if (BambiServlet.isMyContext(request, "processNextPhase"))
			{
				return processNextPhase(request, response);
			}
			else if (BambiServlet.isMyContext(request, "login"))
			{
				return handleLogin(request, response);
			}
		}
		return super.handleGet(request, response);
	}

	/**
	 * handle login/logout of employees
	 * @param request
	 * @return
	 */
	private boolean handleLogin(final BambiServletRequest request, final BambiServletResponse response)
	{
		String personalID = StringUtil.getNonEmpty(request.getParameter(AttributeName.PERSONALID));
		if (personalID != null)
		{
			final boolean bLogout = "logout".equals(request.getParameter("inout"));
			personalID = StringUtil.token(personalID, 0, " ");
			if (bLogout)
			{
				_theStatusListener.removeEmployee(employees.getEmployee(personalID));
			}
			else
			{
				_theStatusListener.addEmployee(employees.getEmployee(personalID));
			}
		}

		showDevice(request, response, false);
		return true;
	}

	/**
	 * @param request
	 * @param response
	 * @return
	 */
	protected abstract boolean processNextPhase(BambiServletRequest request, BambiServletResponse response);

	/**
	 * check whether this resource should track amounts
	 * @param resLink
	 * @return
	 */
	public boolean isAmountResource(final JDFResourceLink resLink)
	{
		if (resLink == null || amountResources == null)
		{
			return false;
		}
		for (int i = 0; i < amountResources.size(); i++)
		{
			if (resLink.matchesString(amountResources.get(i)))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * reload the queue
	 */
	@Override
	protected void reloadQueue()
	{
		// nop
	}

	/**
	 * @param prop the properties of the device
	 */
	public WorkerDevice(final IDeviceProperties prop)
	{
		super(prop);
		_trackResource = prop.getTrackResource();
		_typeExpression = prop.getTypeExpression();
		amountResources = prop.getAmountResources();
		employees = new EmployeeLoader().load();
		log.info("created WorkerDevice '" + prop.getDeviceID() + "'");
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

	/**
	 * @see org.cip4.bambi.core.AbstractDevice#getXSLT(java.lang.String, org.cip4.bambi.core.BambiServletRequest)
	 */
	@Override
	public String getXSLT(final BambiServletRequest request)
	{
		final String command = request.getCommand();
		final String contextPath = request.getContextPath();
		if ("login".equals(command))
		{
			return getXSLTBaseFromContext(contextPath) + "/login.xsl";
		}
		return super.getXSLT(request);
	}
}