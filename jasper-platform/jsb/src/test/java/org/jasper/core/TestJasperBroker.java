package org.jasper.core;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

import java.net.InetAddress;

import junit.framework.TestCase;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.transport.RequestTimedOutIOException;
import org.jasper.core.auth.AuthenticationFacade;
import org.jasper.core.persistence.PersistenceFacade;
import org.jasper.jLib.jAuth.ClientLicense;
import org.jasper.jLib.jAuth.UDELicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

//
//
public class TestJasperBroker extends TestCase {
	private JasperBroker classUnderTest;
	private UDE mockUDE;
	private PersistenceFacade cachingSys;
	private AuthenticationFacade mockLicenseKeySys;
	private Broker mockBroker;
	private ConnectionInfo mockConnectionInfo;
	private ConnectionContext mockConnectionContext;
	private UDELicense mockUDELicense;
	private ClientLicense mockDTALicense;
	private String password = "passwd";
	private String ipAddr;
	private String deploymentId = "testLab";
	private String dtaName = "jasper:demo-testDTA:1.0:testLab";
	private Integer instanceId = 0;
	private String dtaKey = "6F30C6A3A817E5220AB70CC7483A845CDE949E89A3E5417D48C2DBA2EDDCD60DA9EEAA05E7B40B26C368C98E39452B0C48DC51C3101CB1FAABF762D780F6317514B7E863F7E1163B43FE3D20619B4494685D3A2AA7FE6312CC31C13C015EC421D3A1EA5609206C988234277EDC820DD1BD35017D36C2A7F4EDB19F5F9B56A29DF7F33E94E70DD726F75C858A4424C4E6ACAAB12D08576CDD8037587177D02C4E9D299963C5178F755F3421B97305CB764A6B0B0E484BACA61960BB860ACDDE170572995E0D21296CF6B6C9CE427668D09412FBA0F23C761982B5F4296B679FCD53196B61B5A20179BAB8B2418ECD78FCA22D30404CAFC5C1CB00833D955DF2357D1E4540D38326B91FE78C69543D6B53253C71E27E5D87480348DE2DB284B1E9358425F420DE5D547B8FC17FED02F5A09E9EB82CFB1FBAF05A0C926D8198BE73C1713DE3DEF79ACBD3C5A80BE29BCFCC55EE74803FAFDA20980D41FC44F107CCBB02CA59A527DD72BC78B0A3FC5557D4AC658DF2474A98F7BFF486C3186F0DF39E66456A6FF76A11CB384077CA04A5C057EFCD588D876D28BFBF307989F99F66E5578507281342D1FC04A254BEEC4FFA027F6979E030D7FB13C4CC1D12E886EBEC39A775DDA9601C4B811F03523257EBD13B7FFC5FDC92F3D14DD706C12BF8C7D3B7659B6DF6615264CCF1AA751A4E9A0403370370291D683B9E5DDE53E396529D422CC53AB8BFC58C1C14A2A0431E466285A007396D1ADC8487FCFCE23E124FD4535B6C9E3F467B16140BECA2B5D8EC9CC26F4174AF4F757F8151A16E6EB9447CC0E0A8D310ADB7953FE6EC1113E14525BCBCB37463D4FC5053F3C14E1A9EABBF8EBD77B84E4E3BFCB3D18977AD02FFCC76476B3B2227EF6950817B8DAB091F4929A7A3C53E39853FCAB648942CBB13D4526CA81493681E8FFA14776D70433D6903AF0545841042215D1FEA53B0EEFA30FD347ED453F6A192D11A27289248D529159FBC99AADEF4E338CC70812C2EC6A92F0F3306A0D0FEBA69789C617E53EB16799BB4A81C4A810D829907FE17C9D9C4FEC773CB4EB39BA22E9A55DFAAA2EFF3133DC30B8DC26FE1F1FF1FFFED3858B6A23AB363801C2F562239A87DCECADB8841B1B840A591FE0DEE8FAE0D92176B2860709C9BD7DE6A0D4A81B321E1B275CDDF14C5C578BAB2153DBBC5C19C575C7F58CD945848D05426A0784573870E45A9405E4C4B1D4C39E93C1036FA33B2EB12106E218FFB57663FCA90DA70A2366ED105A2556ABD3A39551D1048E387C14354E0CB255467A490DBFC335DA4987C734AC7CB7F2059C3A922BE1F2A805AF802254C15B0EE23279D7F4B960698A00E84DD74218E477781838B39649A04F44B5CFEBBBF1945EFFB634EB8FEDEBBA4F0D50D54E42C32C7073C39F6A2E0D50B988A2684CAAAD1EB01987766C89EA831D43F";
	

	
	/*
	 * This tests simulates a remote UDE connecting in a cluster
	 */
	@Test
	public void testAddUDEConnection() throws Exception {
		when(mockLicenseKeySys.getUdeDeploymentAndInstance(password)).thenReturn("testLab:0");
		classUnderTest.start();
		classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
	}
	
	/*
	 * This tests adding a local UDE connection
	 */
	@Test
	public void testAddLocalUDEConnection() throws Exception {
		when(mockConnectionInfo.getClientIp()).thenReturn("vm://localhost");
		classUnderTest = new JasperBroker(mockBroker, mockUDE, cachingSys, mockLicenseKeySys);
		classUnderTest.start();
		classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		TestCase.assertNotNull(classUnderTest);
	}
	
	/*
	 * This tests adding a client connection
	 */
	@Test
	public void testAddClientConnection() throws Exception{
		testAddUDEConnection();
		byte[] value = new byte[1024];
		mockDTALicense = mock(ClientLicense.class);
		value = JAuthHelper.hexToBytes(dtaKey);
		doReturn(false).when(mockLicenseKeySys).isUdeLicenseKey(password);
		when(mockLicenseKeySys.isClientAuthenticationValid(dtaName,dtaKey)).thenReturn(true);
		when(mockConnectionInfo.getUserName()).thenReturn(dtaName);
		when(mockConnectionInfo.getPassword()).thenReturn(dtaKey);
		when(mockLicenseKeySys.isValidLicenseKey(dtaName, dtaKey)).thenReturn(true);
		when(mockLicenseKeySys.getClientNumConsumers(dtaKey)).thenReturn(2);
		when(mockLicenseKeySys.getClientNumPublishers(dtaKey)).thenReturn(2);
		when(mockUDELicense.getDeploymentId()).thenReturn(deploymentId);
		when(mockUDELicense.getInstanceId()).thenReturn(instanceId);
		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn("testLab:0");
		when(mockLicenseKeySys.getClientLicense(value)).thenReturn(mockDTALicense);
		when(mockDTALicense.getVendor()).thenReturn("jasper");
		when(mockDTALicense.getAppName()).thenReturn("demo-testDTA");
		when(mockDTALicense.getVersion()).thenReturn("1.0");
		when(mockDTALicense.getDeploymentId()).thenReturn(deploymentId);
		
		classUnderTest = new JasperBroker(mockBroker, mockUDE, cachingSys, mockLicenseKeySys);
		classUnderTest.start();
		classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
	}
	
	@Test
	public void testThrowExceptionsInStop() throws Exception {
		classUnderTest.start();
		doThrow(new Exception("error")).when(mockBroker).stop();
		classUnderTest.stop();
		classUnderTest.start();
		doThrow(new RequestTimedOutIOException()).when(mockBroker).stop();
		classUnderTest.stop();
	}
	
	@Test
	public void testCleanStartAndStop() throws Exception {
		classUnderTest.start();
		classUnderTest.stop();
	}
	
	@Test
	public void testAddConnectionSecurityException() throws Exception {
		when(mockLicenseKeySys.isValidLicenseKey(deploymentId, password)).thenReturn(false);
		classUnderTest.start();
		try{
			classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		}catch(Exception ex){
			if(!(ex instanceof SecurityException)){
				TestCase.fail();
			}
		}
	}
	
	/*
	 * This tests second UDE connecting with an in-use license key
	 */
	@Test
	public void testAddDuplicateUDE() throws Exception {
		when(mockLicenseKeySys.getUdeDeploymentAndInstance(password)).thenReturn("testLab:0");
		when(mockUDE.isThisMyUdeLicense(password)).thenReturn(true);
		classUnderTest.start();
		try{
			classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		}catch (Exception ex){
			TestCase.assertNotNull(ex);
		}
	}
	
	/*
	 * This tests security exceptions while adding UDE connections
	 */
	@Test
	public void testUDESecuriutyExceptions() throws Exception {
		testAddUDEConnection();
		try{
			testAddUDEConnection();
		}catch(Exception ex){
			TestCase.assertNotNull(ex);
		}
		
		when(mockLicenseKeySys.getUdeDeploymentAndInstance(password)).thenReturn("testLab:0");
		when(mockLicenseKeySys.isUdeAuthenticationValid(deploymentId, password)).thenReturn(false);
		classUnderTest.start();
		try{
			classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		}catch (Exception ex){
			TestCase.assertNotNull(ex);
		}
		
		when(mockLicenseKeySys.isUdeAuthenticationValid(deploymentId, password)).thenReturn(true);
		when(mockLicenseKeySys.isSystemDeploymentId(deploymentId)).thenReturn(false);
		try{
			classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		}catch (Exception ex){
			TestCase.assertNotNull(ex);
		}
	}
	
	/*
	 * This tests security exceptions while adding DTA connections
	 */
	@Test
	public void testDTASecurityExceptions() throws Exception{
		testAddUDEConnection();
		byte[] value = new byte[1024];
		mockDTALicense = mock(ClientLicense.class);
		value = JAuthHelper.hexToBytes(dtaKey);
		doReturn(false).when(mockLicenseKeySys).isUdeLicenseKey(password);
		when(mockLicenseKeySys.isClientAuthenticationValid(dtaName,dtaKey)).thenReturn(false);
		when(mockConnectionInfo.getUserName()).thenReturn(dtaName);
		when(mockConnectionInfo.getPassword()).thenReturn(dtaKey);
		when(mockLicenseKeySys.isValidLicenseKey(dtaName, dtaKey)).thenReturn(true);
		when(mockLicenseKeySys.getClientNumConsumers(dtaKey)).thenReturn(2);
		when(mockLicenseKeySys.getClientNumPublishers(dtaKey)).thenReturn(2);
		when(mockUDELicense.getDeploymentId()).thenReturn(deploymentId);
		when(mockUDELicense.getInstanceId()).thenReturn(instanceId);
		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn("testLab:0");
		when(mockLicenseKeySys.getClientLicense(value)).thenReturn(mockDTALicense);
		when(mockDTALicense.getVendor()).thenReturn("jasper");
		when(mockDTALicense.getAppName()).thenReturn("demo-testDTA");
		when(mockDTALicense.getVersion()).thenReturn("1.0");
		when(mockDTALicense.getDeploymentId()).thenReturn(deploymentId);
		
		classUnderTest = new JasperBroker(mockBroker, mockUDE, cachingSys, mockLicenseKeySys);
		classUnderTest.start();
		try{
			classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		}catch(Exception ex){
			TestCase.assertNotNull(ex);
		}
		
		when(mockLicenseKeySys.willClientLicenseKeyExpireInDays(mockDTALicense, instanceId)).thenReturn(true);
		try{
			classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		} catch(Exception ex){
			TestCase.assertNotNull(ex);
		}
	}
	
	/*
	 * This tests removing a registered DTA
	 */
	@Test
	public void testRemoveDTAConnection() throws Exception {
		testAddUDEConnection();
		byte[] value = new byte[1024];
		mockDTALicense = mock(ClientLicense.class);
		value = JAuthHelper.hexToBytes(dtaKey);
		doReturn(false).when(mockLicenseKeySys).isUdeLicenseKey(password);
		when(mockLicenseKeySys.isClientAuthenticationValid(dtaName,dtaKey)).thenReturn(true);
		when(mockConnectionInfo.getUserName()).thenReturn(dtaName);
		when(mockConnectionInfo.getPassword()).thenReturn(dtaKey);
		when(mockLicenseKeySys.isValidLicenseKey(dtaName, dtaKey)).thenReturn(true);
		when(mockLicenseKeySys.getClientNumConsumers(dtaKey)).thenReturn(2);
		when(mockLicenseKeySys.getClientNumPublishers(dtaKey)).thenReturn(2);
		when(mockUDELicense.getDeploymentId()).thenReturn(deploymentId);
		when(mockUDELicense.getInstanceId()).thenReturn(instanceId);
		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn("testLab:0");
		when(mockLicenseKeySys.getClientLicense(value)).thenReturn(mockDTALicense);
		when(mockDTALicense.getVendor()).thenReturn("jasper");
		when(mockDTALicense.getAppName()).thenReturn("demo-testDTA");
		when(mockDTALicense.getVersion()).thenReturn("1.0");
		when(mockDTALicense.getDeploymentId()).thenReturn(deploymentId);
		
		classUnderTest = new JasperBroker(mockBroker, mockUDE, cachingSys, mockLicenseKeySys);
		classUnderTest.start();
		classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		classUnderTest.removeConnection(mockConnectionContext, mockConnectionInfo, null);
	}
	
	
	/*
	 * This tests adding a duplicate DTA
	 */	
	@Test
	public void testAddDuplicateDTA() throws Exception{
		testAddUDEConnection();
		byte[] value = new byte[1024];
		mockDTALicense = mock(ClientLicense.class);
		value = JAuthHelper.hexToBytes(dtaKey);
		doReturn(false).when(mockLicenseKeySys).isUdeLicenseKey(password);
		when(mockLicenseKeySys.isClientAuthenticationValid(dtaName,dtaKey)).thenReturn(true);
		when(mockConnectionInfo.getUserName()).thenReturn(dtaName);
		when(mockConnectionInfo.getPassword()).thenReturn(dtaKey);
		when(mockLicenseKeySys.isValidLicenseKey(dtaName, dtaKey)).thenReturn(true);
		when(mockLicenseKeySys.getClientNumConsumers(dtaKey)).thenReturn(2);
		when(mockLicenseKeySys.getClientNumPublishers(dtaKey)).thenReturn(2);
		when(mockUDELicense.getDeploymentId()).thenReturn(deploymentId);
		when(mockUDELicense.getInstanceId()).thenReturn(instanceId);
		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn("testLab:0");
		when(mockLicenseKeySys.getClientLicense(value)).thenReturn(mockDTALicense);
		when(mockDTALicense.getVendor()).thenReturn("jasper");
		when(mockDTALicense.getAppName()).thenReturn("demo-testDTA");
		when(mockDTALicense.getVersion()).thenReturn("1.0");
		when(mockDTALicense.getDeploymentId()).thenReturn(deploymentId);
		
		classUnderTest = new JasperBroker(mockBroker, mockUDE, cachingSys, mockLicenseKeySys);
		classUnderTest.start();
		classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		try{
			classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		}catch(Exception ex){
			TestCase.assertNotNull(ex);
		}
	}
	
	/*
	 * This tests max producers and consumers thresholds being exceeded
	 */
	@Test
	public void testMaxThresholdsExceeded() throws Exception{
		testAddUDEConnection();
		byte[] value = new byte[1024];
		mockDTALicense = mock(ClientLicense.class);
		value = JAuthHelper.hexToBytes(dtaKey);
		doReturn(false).when(mockLicenseKeySys).isUdeLicenseKey(password);
		when(mockLicenseKeySys.isClientAuthenticationValid(dtaName,dtaKey)).thenReturn(true);
		when(mockConnectionInfo.getUserName()).thenReturn(dtaName);
		when(mockConnectionInfo.getPassword()).thenReturn(dtaKey);
		when(mockLicenseKeySys.isValidLicenseKey(dtaName, dtaKey)).thenReturn(true);
		when(mockLicenseKeySys.getClientNumConsumers(dtaKey)).thenReturn(2);
		when(mockLicenseKeySys.getClientNumPublishers(dtaKey)).thenReturn(25);
		when(mockUDELicense.getDeploymentId()).thenReturn(deploymentId);
		when(mockUDELicense.getInstanceId()).thenReturn(instanceId);
		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn("testLab:0");
		when(mockLicenseKeySys.getClientLicense(value)).thenReturn(mockDTALicense);
		when(mockDTALicense.getVendor()).thenReturn("jasper");
		when(mockDTALicense.getAppName()).thenReturn("demo-testDTA");
		when(mockDTALicense.getVersion()).thenReturn("1.0");
		when(mockDTALicense.getDeploymentId()).thenReturn(deploymentId);
		
		classUnderTest = new JasperBroker(mockBroker, mockUDE, cachingSys, mockLicenseKeySys);
		classUnderTest.start();
		try{
			classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		}catch(Exception ex){
			TestCase.assertNotNull(ex);
		}
		when(mockLicenseKeySys.getClientNumConsumers(dtaKey)).thenReturn(25);
		when(mockLicenseKeySys.getClientNumPublishers(dtaKey)).thenReturn(2);
		try{
			classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		}catch(Exception ex){
			TestCase.assertNotNull(ex);
		}
	}
	
	/*
	 * This tests adding a client with an invalid deplomentId
	 */
	@Test
	public void testAddInvalidClientConnection() throws Exception{
		testAddUDEConnection();
		byte[] value = new byte[1024];
		mockDTALicense = mock(ClientLicense.class);
		value = JAuthHelper.hexToBytes(dtaKey);
		doReturn(false).when(mockLicenseKeySys).isUdeLicenseKey(password);
		when(mockLicenseKeySys.isClientAuthenticationValid(dtaName,dtaKey)).thenReturn(true);
		when(mockConnectionInfo.getUserName()).thenReturn(dtaName);
		when(mockConnectionInfo.getPassword()).thenReturn(dtaKey);
		when(mockLicenseKeySys.isValidLicenseKey(dtaName, dtaKey)).thenReturn(true);
		when(mockLicenseKeySys.getClientNumConsumers(dtaKey)).thenReturn(2);
		when(mockLicenseKeySys.getClientNumPublishers(dtaKey)).thenReturn(2);
		when(mockUDELicense.getDeploymentId()).thenReturn(deploymentId);
		when(mockUDELicense.getInstanceId()).thenReturn(instanceId);
		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn("testLab:0");
		when(mockLicenseKeySys.getClientLicense(value)).thenReturn(mockDTALicense);
		when(mockDTALicense.getVendor()).thenReturn("jasper");
		when(mockDTALicense.getAppName()).thenReturn("demo-testDTA");
		when(mockDTALicense.getVersion()).thenReturn("1.0");
		when(mockDTALicense.getDeploymentId()).thenReturn(deploymentId);
		when(mockLicenseKeySys.isSystemDeploymentId(deploymentId)).thenReturn(false);
		
		classUnderTest = new JasperBroker(mockBroker, mockUDE, cachingSys, mockLicenseKeySys);
		classUnderTest.start();
		try{
			classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		}catch(Exception ex){
			TestCase.assertNotNull(ex);
		}
	}

	@Before
	public void setUp() throws Exception {
		ipAddr = InetAddress.getLocalHost().getHostAddress();
		mockBroker        = mock(Broker.class);
		mockUDE           = mock(UDE.class);
		mockLicenseKeySys = mock(AuthenticationFacade.class);
		mockConnectionContext = mock(ConnectionContext.class);
		mockConnectionInfo    = mock(ConnectionInfo.class);
		mockUDELicense        = mock(UDELicense.class);
		cachingSys = new PersistenceFacade(ipAddr, "testGroup", "testPassword");
		
		when(mockUDE.getBrokerTransportIp()).thenReturn(ipAddr);
		when(mockConnectionInfo.getPassword()).thenReturn(password);
		doReturn(true).when(mockLicenseKeySys).isUdeLicenseKey(password);
		when(mockConnectionInfo.getClientIp()).thenReturn(ipAddr);
		when(mockConnectionContext.getBroker()).thenReturn(mockBroker);
		when(mockConnectionContext.getBroker().getBrokerName()).thenReturn("testBroker");
		when(mockConnectionInfo.getUserName()).thenReturn(deploymentId);
		when(mockLicenseKeySys.getDeploymentId()).thenReturn(deploymentId);
		when(mockLicenseKeySys.isValidLicenseKey(deploymentId, password)).thenReturn(true);
		when(mockLicenseKeySys.isUdeAuthenticationValid(deploymentId, password)).thenReturn(true);
		when(mockUDE.isThisMyUdeLicense(password)).thenReturn(false);
		when(mockLicenseKeySys.loadKeys(deploymentId)).thenReturn(mockUDELicense);
		when(mockUDELicense.getDeploymentId()).thenReturn(deploymentId);		
		when(mockLicenseKeySys.isSystemDeploymentId(deploymentId)).thenReturn(true);
		when(mockUDE.getUdeLicense()).thenReturn(mockUDELicense);
		when(mockUDE.getUdeLicense().getNumOfConsumers()).thenReturn(10);
		when(mockUDE.getUdeLicense().getNumOfPublishers()).thenReturn(10);
		when(mockLicenseKeySys.getUdeNumConsumers(password)).thenReturn(10);
		when(mockLicenseKeySys.getUdeNumPublishers(password)).thenReturn(10);
		
		classUnderTest = new JasperBroker(mockBroker, mockUDE, cachingSys, mockLicenseKeySys);
	}

	@After
	public void tearDown() throws Exception {
		classUnderTest.stop();
		cachingSys.shutdown();
		
	}

}
