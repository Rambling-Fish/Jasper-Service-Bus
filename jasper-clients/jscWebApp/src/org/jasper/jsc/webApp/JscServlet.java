package org.jasper.jsc.webApp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasper.jsc.JClientProvider;

// Send to ThroughtWire
public class JscServlet extends HttpServlet
{
	  MyShutdown sh;

    public JscServlet()
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
  		private JscServlet managedClass;

  		public MyShutdown(JscServlet managedClass) {
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