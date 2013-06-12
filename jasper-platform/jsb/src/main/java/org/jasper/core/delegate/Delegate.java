package org.jasper.core.delegate;
 
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
 
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.QueueConnection;
import javax.jms.Session;
 
import org.apache.log4j.Logger;
 
import com.hp.hpl.jena.rdf.model.Model;
 
 
 
public class Delegate implements Runnable {
 
    private String name;
    private QueueConnection queueConnection;
    private boolean isShutdown;
    private DelegateOntology jOntology; 
    private ExecutorService delegateHandlers;
    private Map<String, List<String>> jtaUriMap;
    private Map<String, List<String>> jtaQueueMap;
     
    static Logger logger = Logger.getLogger("org.jasper");
 
    public Delegate(String name,QueueConnection queueConnection, Map<String, List<String>> jtaUriMap,  Map<String, List<String>> jtaQueueMap, Model model) {
        this.name = name;
        this.queueConnection  = queueConnection;
        this.isShutdown  = false;
        this.jOntology = new DelegateOntology(model);
        this.jtaUriMap = jtaUriMap;
        this.jtaQueueMap = jtaQueueMap;
        delegateHandlers = Executors.newCachedThreadPool();
    }
     
    public Map<String, List<String>> getJtaUriMap() {
        return jtaUriMap;
    }
     
    public Map<String, List<String>> getJtaQueueMap() {
        return jtaQueueMap;
    }   
 
    public void shutdown(){
        isShutdown = true;
        delegateHandlers.shutdown();
    }
     
    @Override
    public void run(){
        processRequests();
    }
     
    public void processRequests() {
          try {
                 
              Session globalSession = queueConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
 
              // Create Queue
              Destination globalQueue = globalSession.createQueue(DelegateFactory.DELEGATE_GLOBAL_QUEUE);
                           
              // Create a MessageConsumer from the Session to the Queue
              MessageConsumer globalDelegateConsumer = globalSession.createConsumer(globalQueue);
 
              // Wait for a message
              Message jmsRequest;
               
              do{
                  do{
                    jmsRequest = globalDelegateConsumer.receive(1000);
                  }while(jmsRequest == null && !isShutdown);
                  if(isShutdown) break;
                   
                  delegateHandlers.submit(new DelegateRequest(this, queueConnection,jOntology,jmsRequest));        
 
              }while(!isShutdown);
               
              globalDelegateConsumer.close();
              globalSession.close();
              
          } catch (Exception e) {
              logger.error("Exception caught while listening for request in delegate : " + name,e);
          }
    }
}