package com.payparade.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tektonpartners.util.DataSet;
import com.tektonpartners.util.DatabaseResult;
import com.tektonpartners.util.DatabaseRow;

/**
 * Servlet implementation class PurchaseBehaviourServlet
 */
public class PurchaseBehaviourServlet extends PassServlet {
	private static final long serialVersionUID = 1L;
       

	public void init(){
		servletName_ = "PurchaseBehaviourServlet";
		 super.init() ;
	}
	
	public boolean post(HttpServletRequest request, HttpServletResponse response,PrintWriter out,String resourceString)throws ServletException, IOException 
	{
		boolean done = true ;
		return done;
	}
	public boolean get(HttpServletRequest request, HttpServletResponse response,PrintWriter out,String resourceString)throws ServletException, IOException 
	{
		logger_.info(" Inside get() of PurchaseBehaviourServlet class ");
		logger_.info("resource String is==="+resourceString);
		boolean done = true ;
		if(resourceString==null){
			logger_.error("null resource string received") ;	
			done = false ;		
		}else if(resourceString.startsWith("purchaseBehaviour")){
			purchaseBehaviour(request,response,out);
		}
		else if(resourceString.startsWith("setppid")){
			setPPID(request,response,out);
		}
		return done;
	}
	
	public void setPPID(HttpServletRequest request, HttpServletResponse response,PrintWriter out){
		logger_.info(" Inside setPPID() of PurchaseBehaviourServlet class ");
		 response.setContentType("text/html");
		 out.println("200");
		logger_.info(" Outside setPPID() of PurchaseBehaviourServlet class ");
	}
	
	public void purchaseBehaviour(HttpServletRequest request, HttpServletResponse response,PrintWriter out){
		logger_.info(" Inside purchaseBehaviour() of PurchaseBehaviourServlet class ");
		int cust_id = 0;
		int order_id = 0;
		float order_value = 0;
		int partnerid = 0;
		int pp_id = 0;
		if(request.getParameter("cust_id")!=null && request.getParameter("cust_id").equals("")==false){
			cust_id = Integer.parseInt(request.getParameter("cust_id")); 
		}
		if(request.getParameter("order_id")!=null && request.getParameter("order_id").equals("")==false){
			order_id = Integer.parseInt(request.getParameter("order_id")); 
		}
		if(request.getParameter("order_value")!=null && request.getParameter("order_value").equals("")==false){
			order_value = Float.parseFloat(request.getParameter("order_value")); 
		}
		
		if(request.getParameter("pp_id")!=null && request.getParameter("pp_id").equals("")==false){
			pp_id = Integer.parseInt(request.getParameter("pp_id")); 
		}
		
		StringBuffer sql = new StringBuffer();
		logger_.info("Purchase Behaviour Partner Code is:::"+getPartnerCode(request));
		sql.append("select id  from pp_partner_domain  where pp_partner_domain='"+getPartnerCode(request)+"'");
		DatabaseResult results = db_.executeQuery(sql.toString()) ;
		DataSet dataset = results.getDataSet() ;
		DatabaseRow row = null;
		for(int i = 0 ; i< dataset.size() ; i++){
			  row = dataset.get(i);
			  //logger_.info("Partner Id::"+row.getString("id"));
			  partnerid = (Integer)(row.get("id"));
		}
		logger_.info("Partner Id::"+partnerid);
		logger_.info("Customer Id::"+cust_id);
		logger_.info("PP Id::"+pp_id);
		logger_.info("Order Id::"+order_id);
		logger_.info("Order Value::"+order_value);
		sql.delete(0,sql.length());
		sql.append("Insert into pp_purchase_behavior(partner_domain,customer_id,pp_id,order_id,order_value,client_ip,timestamp) values('");
		sql.append(getPartnerCode(request));
		sql.append("',");
		sql.append(cust_id);
		sql.append(",");
		sql.append(pp_id);
		sql.append(",");
		sql.append(order_id);
		sql.append(",");
		sql.append(order_value);
		sql.append(",");
		sql.append("INET_ATON('"+getCustomerIp(request)+"')");
		//sql.append("INET_ATON('127.0.0.1')");
		sql.append(",");
		sql.append("NOW());");
		logger_.info(sql.toString());
		int id = db_.executeInsert(sql.toString());
		if(id>0){
			try{
				out.print("200");
			}catch(Exception e){
				e.printStackTrace();
			}
		}else{
			try{
				out.print("400");
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		logger_.info(" Outside purchaseBehaviour() of PurchaseBehaviourServlet class ");

	}
	
}
