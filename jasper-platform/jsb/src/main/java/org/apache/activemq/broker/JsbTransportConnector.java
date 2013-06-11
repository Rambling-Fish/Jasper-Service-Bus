package org.apache.activemq.broker;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;

import javax.management.ObjectName;

import org.apache.activemq.broker.jmx.ManagedTransportConnector;
import org.apache.activemq.broker.jmx.ManagementContext;
import org.apache.activemq.command.ConnectionControl;
import org.apache.activemq.transport.TransportFactory;
import org.apache.activemq.transport.TransportServer;
import org.jasper.core.JasperBrokerService;

public class JsbTransportConnector extends ManagedTransportConnector {
	
	public enum JsbLoadBalancingStrategy{
		DEFAULT,
		ROUND_ROBIN,
		RANDOM
	}
	
	JsbLoadBalancingStrategy lbStrategy = JsbLoadBalancingStrategy.DEFAULT;

	public JsbTransportConnector(String bindAddress) throws Exception{
		this(new URI(bindAddress));
	}
	
	public JsbTransportConnector(URI brokerURI) throws Exception{
		this(null,null,TransportFactory.bind(brokerURI));
	}
	
    public JsbTransportConnector(ManagementContext context, ObjectName connectorName,TransportServer server) {
        super(context, connectorName, server);
    }
    
    /**
     * Factory method to create a JMX managed version of this transport
     * connector
     */
    public ManagedTransportConnector asManagedConnector(ManagementContext context, ObjectName connectorName) throws IOException, URISyntaxException {
    	JsbTransportConnector rc = new JsbTransportConnector(context,connectorName,this.getServer());
        rc.setBrokerInfo(getBrokerInfo());
        rc.setDisableAsyncDispatch(isDisableAsyncDispatch());
        rc.setDiscoveryAgent(getDiscoveryAgent());
        rc.setDiscoveryUri(getDiscoveryUri());
        rc.setEnableStatusMonitor(isEnableStatusMonitor());
        rc.setMessageAuthorizationPolicy(getMessageAuthorizationPolicy());
        rc.setName(getName());
        rc.setTaskRunnerFactory(getTaskRunnerFactory());
        rc.setUri(getUri());
        rc.setBrokerService(this.getBrokerService());
        rc.setUpdateClusterClients(isUpdateClusterClients());
        rc.setRebalanceClusterClients(isRebalanceClusterClients());
        rc.setUpdateClusterFilter(getUpdateClusterFilter());
        rc.setUpdateClusterClientsOnRemove(isUpdateClusterClientsOnRemove());
        rc.setAuditNetworkProducers(isAuditNetworkProducers());
        rc.setMaximumConsumersAllowedPerConnection(getMaximumConsumersAllowedPerConnection());
        rc.setMaximumProducersAllowedPerConnection(getMaximumProducersAllowedPerConnection());
        rc.setLoadBalancingStrategy(getLoadBalancingStrategy());
    	return rc;
    }
	
    public JsbLoadBalancingStrategy getLoadBalancingStrategy() {
		return lbStrategy;
	}

	public void setLoadBalancingStrategy(JsbLoadBalancingStrategy lbStrategy) {
		this.lbStrategy = lbStrategy;
	}

	public void updateClientClusterInfo() {

		if(this.getBrokerService() instanceof JasperBrokerService){
			if(((JasperBrokerService)this.getBrokerService()).isStopping()){
				return;
			}
		}
		
		if (isRebalanceClusterClients() || isUpdateClusterClients()) {
        	if(lbStrategy != JsbLoadBalancingStrategy.DEFAULT){
                synchronized (getPeerBrokers()) {
                	removePeerBrokerDuplicates(getPeerBrokers());
                }
        	}
            ConnectionControl control = getConnectionControl(lbStrategy);
            for (Connection c : this.connections) {
                c.updateClient(control);
                if (isRebalanceClusterClients()) {
                    control = getConnectionControl(lbStrategy);
                }
            }
        }
    }
	
	protected ConnectionControl getConnectionControl(JsbLoadBalancingStrategy lbStrategy) {
        switch (lbStrategy){
        case RANDOM:
        	return getConnectionControlRandomBrokerList();
        case ROUND_ROBIN:
        case DEFAULT:
    	default:
    		return super.getConnectionControl();
        }
    }
	    
    protected ConnectionControl getConnectionControlRandomBrokerList() {
        boolean rebalance = isRebalanceClusterClients();
        String connectedBrokers = "";
        String separator = "";
       
        if (isUpdateClusterClients()) {
            synchronized (getPeerBrokers()) {
            	LinkedList<String> peerBrokersRef = getPeerBrokers();
            	randomizeLinkedList(peerBrokersRef);
                for (String uri : getPeerBrokers()) {
                    connectedBrokers += separator + uri;
                    separator = ",";
                }
            }
        }
        ConnectionControl control = new ConnectionControl();
        control.setConnectedBrokers(connectedBrokers);
        control.setRebalanceConnection(rebalance);
        return control;
    }

	private void randomizeLinkedList(LinkedList<String> brokers) {
		LinkedList<String> tempList = new LinkedList<String>(brokers);
		brokers.clear();
		while(!tempList.isEmpty()){
			int index = (int)(Math.random()*tempList.size());
			brokers.add(tempList.remove(index));
		}
	}

	private void removePeerBrokerDuplicates(LinkedList<String> brokers) {
		LinkedList<String> tempList = new LinkedList<String>(brokers);
		brokers.clear();
		while(!tempList.isEmpty()){
			String entry = tempList.remove(0);
			if(!brokers.contains(entry)) brokers.add(entry);
		}
	}
}
