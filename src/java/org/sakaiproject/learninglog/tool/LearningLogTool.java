package org.sakaiproject.learninglog.tool;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.learninglog.SakaiProxy;
import org.sakaiproject.util.ResourceLoader;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public class LearningLogTool extends HttpServlet
{
	private Logger logger = Logger.getLogger(getClass());

	private SakaiProxy sakaiProxy;
	
	private Template bootstrapTemplate = null;
	
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);

		if (logger.isDebugEnabled()) logger.debug("init");
		
        ComponentManager componentManager = org.sakaiproject.component.cover.ComponentManager.getInstance();
		sakaiProxy = new SakaiProxy();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		if (logger.isDebugEnabled()) logger.debug("doGet()");
		
		if(sakaiProxy == null)
			throw new ServletException("sakaiProxy MUST be initialised.");
		
		String state = request.getParameter("state");
		String postId = request.getParameter("postId");
		
		if(state == null) state = "home";
		
		if(postId == null) postId = "none";
		
		String userId = sakaiProxy.getCurrentUserId();
		
		if(userId == null)
		{
			// We are not logged in
			throw new ServletException("getCurrentUser returned null.");
		}
		
		String siteId = sakaiProxy.getCurrentSiteId();
		
		String placementId = sakaiProxy.getCurrentToolId();
		
		// We need to pass the language code to the JQuery code in the pages.
		Locale locale = (new ResourceLoader(userId)).getLocale();
		String languageCode = locale.getLanguage();
		
		String pathInfo = request.getPathInfo();
		
		String skin = sakaiProxy.getSakaiSkin();

		if (pathInfo == null || pathInfo.length() < 1)
		{
			String uri = request.getRequestURI();
			
			// There's no path info, so this is the initial state
			if(uri.contains("/portal/pda/"))
			{
				// The PDA portal is frameless for redirects don't work. It also
				// means that we can't pass url parameters to the page.We can
				// use a cookie and the JS will pull the initial state from that
				// instead.
				Cookie params = new Cookie("sakai-tool-params","state=" + state + "&siteId=" + siteId + "&placementId=" + placementId + "&postId=" + postId + "&langage=" + languageCode);
				response.addCookie(params);
			
				RequestDispatcher dispatcher = this.getServletContext().getRequestDispatcher("/learninglog.html");
				dispatcher.include(request, response);
				return;
			}
			else
			{
				String url = "/learninglog/learninglog.html?state=" + state + "&siteId=" + siteId + "&placementId=" + placementId + "&skin=" + skin + "&postId=" + postId + "&language=" + languageCode;
				response.sendRedirect(url);
				return;
			}
		}
	}
}
