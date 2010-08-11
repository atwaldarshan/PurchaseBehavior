package com.payparade.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.payparade.util.Logger;
import com.payparade.util.StringUtils;
import com.tektonpartners.util.Database;

/**
 * Servlet implementation class PassServlet
 */
public abstract class PassServlet extends HttpServlet 
{
	private static final long serialVersionUID = 1L;
	 protected Database db_ = null ;
	//protected Logger logger_ = null ;
	 protected String servletName_ = "PassServlet" ;
	 protected Logger logger_ = Logger.getLogger(servletName_) ; 
    
	public void init(){
		//logger_ = Logger.getLogger(servletName_) ; 
		db_ = new Database(servletName_) ;
		logger_.info("Inside init() of PassServlet class");

	}
    public PassServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    public void service (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
		logger_.info("Inside service() of PassServlet Class");
    		String requestType = request.getMethod();
		  HttpSession session = request.getSession(true) ;
    	  response.addHeader("Cache-Control", "no-cache");
		  PrintWriter out = response.getWriter();
		  URI uri = null ;
		  try {
			  uri = new URI(request.getRequestURI());
			  } 
		  catch (URISyntaxException e1) {logger_.error( "doGet() - "+e1.getMessage() ) ; }
		  logger_.info("ServletName is==="+servletName_);
		  String resourceString = uri.getPath().substring(uri.getPath().indexOf(servletName_)+servletName_.length()+1) ;
		  logger_.info("Resource String value is===="+resourceString);
    	  HashMap<String, String> myCookies = storeCookies(request) ;
    	  setPPId(request, myCookies.get("pp_xv1") ) ;
    	
    	  if ( session.isNew() ) {
    		  logger_.info(" Inside session block ");
    		  setPartnerHost(request, StringUtils.getPartnerHost(request) ) ; 
			  
			  setPartnerCode( request,StringUtils.getPartnerCode(getPartnerHost(request)) ) ;
    		  String xfor = request.getHeader("x-forwarded-for") ;
    		  logger_.info(" xfor::: "+xfor);
    		  String ip = null ;
			  if ( xfor != null ) {
				  if ( xfor.contains("," ) ) {
					  String[] ips = xfor.split(",") ;
					  if ( ips.length > 0 )
						  ip = ips[0] ;
					  }
				  else
					  ip = xfor ;					
				  }
			  logger_.info("xfor="+xfor+" cip="+ip) ;
			  setCustomerIp( request, ip) ;
			  
	    	  if ( getPPId(request) == null){
     	    		  Integer generatedId = db_.executeInsert("INSERT INTO pp_id(session_id, browser_id, user_ip, referer_url) VALUES ("+
						  "'"+session.getId()+"',"+
						  "'"+request.getHeader("user-agent")+"',"+
						  "'"+ip+"',"+
						  "'"+request.getHeader("referer")+"') ;" ) ;
				  setPPId(request, generatedId.toString() ) ;
				  logger_.info("PPID value is---"+generatedId);
				  Cookie ck = new Cookie("pp_xv1", getPPId(request));
				  ck.setPath("/");
				  ck.setMaxAge(60*60*24*365*2) ;
				  response.addCookie(ck);
				  logger_.info("Cookie value is==="+ck.getValue());
	    	  }
    	  }  
    	  logger_.info("Request Type is--"+requestType);
    	  if ( "GET".equals(requestType) ) {
    		  get(request, response, out, resourceString);
    	  }
    	  else if ( "POST".equals(requestType) ) {
    		  post(request, response, out, resourceString);
    	  }
    	  logger_.info("Outside service() of PassServlet Class");
    }
    
    public String getPPId(HttpServletRequest request) {		
    	return (String) getAttribute(request, "ppid" ) ;	
    	}
	public void setPPId(HttpServletRequest request, String value) {	
		setAttribute(request, "ppid", value ) ;	
		}
	public String getPartnerCode(HttpServletRequest request) {	
		return (String) getAttribute(request, "partnercode" ) ;		
		}
	public void setPartnerCode(HttpServletRequest request, String value) {	
		setAttribute(request, "partnercode", value ) ;			
		}
	
	public String getPartnerHost(HttpServletRequest request) {	
		return (String) getAttribute(request, "partnerhost" ) ;	
		}
	public void setPartnerHost(HttpServletRequest request, String value) {	
		setAttribute(request, "partnerhost", value ) ;		
		}
	
	public Object getAttribute(HttpServletRequest request, String attributeName) {
		HttpSession session = request.getSession(false) ;
		return session.getAttribute(attributeName) ;
		}
	  
	public void setAttribute(HttpServletRequest request, String attributeName, Object value) {
		HttpSession session = request.getSession(false) ;
		session.setAttribute(attributeName, value) ;
		}
	public String getCustomerIp(HttpServletRequest request) {		
			return (String) getAttribute(request, "customerip" ) ;	
			}
	public void setCustomerIp(HttpServletRequest request, String value) {
		setAttribute(request, "customerip", value ) ;
		}

	private HashMap<String, String> storeCookies(HttpServletRequest request) {
		  HashMap<String, String> result = new HashMap<String, String>() ; 
		  Cookie cookies[] = request.getCookies();
		  if (cookies == null)
			  cookies = new Cookie[0];
		    
		  for (int i = 0; i < cookies.length; i++) {
			  result.put(cookies[i].getName(), cookies[i].getValue()) ;
			  }
		  return result ;
		  }

	abstract public boolean get(HttpServletRequest request, HttpServletResponse response, PrintWriter out, String resourceString) throws ServletException, IOException ;
	abstract public boolean post(HttpServletRequest request, HttpServletResponse response, PrintWriter out, String resourceString) throws ServletException, IOException ;
} 
