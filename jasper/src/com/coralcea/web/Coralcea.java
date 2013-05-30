package com.coralcea.web;

import java.io.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.jasper.jsc.JClientProvider;
import com.coralcea.web.Coralcea.MyShutdown;

// Send to ThroughtWire
public class Coralcea extends HttpServlet
{
	  MyShutdown sh;

    public Coralcea()
    {
    		super();
    		sh = new MyShutdown(this);
    		Runtime.getRuntime().addShutdownHook(sh);
        System.out.println("In Default Constructor for coralcea.");
    		System.out.println("Added shutdown hook <v2>");

    }
    
    public void init()
    {
        JCP = JClientProvider.getInstance();
        System.out.println("JClientProvider.getInstance");
    }

    public void destroy() {
    	JClientProvider.shutdown();
  		freeResources();
  	}


    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        StringBuffer jasperQuery = new StringBuffer();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        jasperQuery.append(path);
        System.out.println((new StringBuilder("path = ")).append(path).toString());
        if(request.getQueryString() != null)
        {
            jasperQuery.append("?");
            jasperQuery.append(request.getQueryString());
        }
        System.out.println((new StringBuilder("QueryStrings = ")).append(request.getQueryString()).toString());
        System.out.println((new StringBuilder("what is sent to JASPER = ")).append(jasperQuery.toString()).toString());
        String whatdoIget = null;
        try
        {
            whatdoIget = JCP.FetchDataFromJasper(jasperQuery.toString());
        }
        catch(UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(whatdoIget);
    }

    protected void doPost(HttpServletRequest httpservletrequest, HttpServletResponse httpservletresponse)
        throws ServletException, IOException
    {
    }
    
  	class MyShutdown extends Thread {
  		private Coralcea managedClass;

  		public MyShutdown(Coralcea managedClass) {
  			super();
  			this.managedClass = managedClass;
  		}

  		public void run() {
  			System.out.println("MyShutDown Thread started");
  			try {
  				managedClass.freeResources();
  			}
  			catch (Exception ee) {
  				ee.printStackTrace();
  			}
  		}
  	}

  	public void freeResources() {
  		System.out.println("######### Freeing JClientProvider resources here! #########");
  		JClientProvider.shutdown();
  	}

    private static final long serialVersionUID = 1L;
    public static JClientProvider JCP;
}