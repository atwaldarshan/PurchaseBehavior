package com.payparade.util;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

public class StringUtils extends Object{
	protected static Logger logger_ = Logger.getLogger(StringUtils.class.getSimpleName()) ; 

public static HashMap<String, String> parseParams( String URLString ) {	
    HashMap<String, String> arr = new HashMap();
    
    String valStr = URLString.substring(URLString.indexOf("?") + 1);
    StringTokenizer st = new StringTokenizer(valStr, "&");

    while (st.hasMoreTokens()) {
        String pair = st.nextToken();
        String lhs = pair.substring(0, pair.indexOf("="));
        String rhs  = pair.substring(pair.indexOf("=") + 1);
        arr.put(lhs, rhs);
    }
    return arr ;
}


public static boolean hasLength(String str) {
    return (str != null && str.length() > 0);
}


public static boolean hasText(String str) {
    if (!hasLength(str)) {
        return false;
    }
    int strLen = str.length();
    for (int i = 0; i < strLen; i++) {
        if (!Character.isWhitespace(str.charAt(i))) {
            return true;
        }
    }
    return false;
}
public static String getPartnerHost (HttpServletRequest request) throws MalformedURLException {
	logger_.info(" Inside getPartnerHost() of StringUtils class ");
	
	  String referer =  request.getHeader("referer") ;		  
	  String host = null ;
	  logger_.info("Referer value is=="+referer);
	  if ( referer != null ) {
		  URL url = new URL(referer) ;
		  host = url.getHost() ;			
		  logger_.info(" ref:"+referer+"  url:"+url+"  host:"+host) ;
	  	}
	  else 
		  logger_.warn("NULL referer");
	  return host ;
}

public static String getPartnerCode (String partnerHost) throws MalformedURLException {
	  String partnerCode = null ;
	  
	  if ( partnerHost != null ) {
		  logger_.info("StringUtils partnerHost is---"+partnerHost);
		  StringBuffer sb = new StringBuffer(partnerHost) ;
		  sb.reverse() ;
		  
		  int first = sb.indexOf(".") ;
		  int last = sb.indexOf(".", first+1) ;
		  if ( last == -1 )
			  partnerCode = new String(sb.reverse()) ;	
		  else
			  partnerCode = new String(sb.reverse().substring(sb.length()-last)) ;	
			  
		  logger_.info(" host:"+partnerHost+"  part:"+partnerCode) ;
	  	}
	  else 
		  logger_.warn("NULL Partner Host");
	  return partnerCode ;
}

public static String percentEncode(String s, String encoding) {
    if (s == null) {
        return "";
    }
    try {
        return URLEncoder.encode(s, encoding)
                // OAuth encodes some characters differently:
                .replace("+", "%20").replace("*", "%2A")
                .replace("%7E", "~")  
                // undo the URLEncoder screw-ups
                .replace("%00", "");
        // This could be done faster with more hand-crafted code.
    } catch (UnsupportedEncodingException wow) {
        throw new RuntimeException(wow.getMessage(), wow);
    }
}

public static String percentDecode(String s, String encoding) {
    try {
        return URLDecoder.decode(s, encoding);
        // This implements http://oauth.pbwiki.com/FlexibleDecoding
    } catch (java.io.UnsupportedEncodingException wow) {
        throw new RuntimeException(wow.getMessage(), wow);
    }
}
}
