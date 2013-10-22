package org.sakaiproject.learninglog.tool;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.learninglog.SakaiProxy;
import org.sakaiproject.learninglog.LearningLogManager;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.util.RequestFilter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

/**
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public class LearningLogTool extends HttpServlet {
	
	private final Logger logger = Logger.getLogger(getClass());

    @Autowired
	private SakaiProxy sakaiProxy;

    @Autowired
	private LearningLogManager llManager;
	
	private Template bootstrapTemplate = null;
	
	public void init(ServletConfig config) throws ServletException {
		
		super.init(config);
		
		try {

            //sakaiProxy = (SakaiProxy) ComponentManager.get("org.sakaiproject.learninglog.api.SakaiProxy");

            //llManager = (LearningLogManager) ComponentManager.get("org.sakaiproject.learninglog.LearningLogManager");

            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

            VelocityEngine ve = new VelocityEngine();
            Properties props = new Properties();
            props.setProperty("file.resource.loader.path",config.getServletContext().getRealPath("/WEB-INF"));
            ve.init(props);
            bootstrapTemplate = ve.getTemplate("bootstrap.vm");

        } catch (Throwable t) {
            throw new ServletException("Failed to initialise LearningLogTool servlet.", t);
        }
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		if(sakaiProxy == null) {
			throw new ServletException("sakaiProxy MUST be initialised.");
		}
		
		String state = request.getParameter("state");
		String postId = request.getParameter("postId");
		
		if(state == null) state = "home";
		
		if(postId == null) postId = "none";

        String userId = null;
        Session session = (Session) request.getAttribute(RequestFilter.ATTR_SESSION);
        if(session != null) {
            userId = session.getUserId();
        } else {
            throw new ServletException("No current user session.");
        }
		
		String siteId = sakaiProxy.getCurrentSiteId();
		
        String placementId = (String) request.getAttribute(Tool.PLACEMENT_ID);

        String sakaiHtmlHead = (String) request.getAttribute("sakai.html.head");
		
		// We need to pass the language code to the JQuery code in the pages.
		Locale locale = (new ResourceLoader(userId)).getLocale();
		String language = locale.getLanguage();
		String country = locale.getCountry();

        String isoLanguage = language;

        if(country != null && !country.equals("")) {
            isoLanguage += "_" + country;
        }

        boolean isTutor = llManager.getCurrentUserRole(siteId).equals("Tutor");
		
		VelocityContext ctx = new VelocityContext();

        // This is needed so certain trimpath variables don't get parsed.
        ctx.put("D", "$");

        ctx.put("sakaiHtmlHead",sakaiHtmlHead);
        ctx.put("siteId",siteId);
        ctx.put("isTutor",isTutor ? "true" : "false");
        ctx.put("placementId",placementId);
        ctx.put("postId",postId);
        ctx.put("isolanguage",isoLanguage);
        ctx.put("language",language);
        ctx.put("country",country);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html");
        Writer writer = new BufferedWriter(response.getWriter());
        bootstrapTemplate.merge(ctx,writer);
        writer.close();
	}
}
