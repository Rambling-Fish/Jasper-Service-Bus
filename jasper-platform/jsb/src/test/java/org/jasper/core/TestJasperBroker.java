package org.jasper.core;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.UUID;

import junit.framework.TestCase;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.transport.RequestTimedOutIOException;
import org.jasper.core.auth.AuthenticationFacade;
import org.jasper.core.persistence.PersistenceFacade;
import org.jasper.core.persistence.PersistenceFacadeFactory;
import org.jasper.jLib.jAuth.ClientLicense;
import org.jasper.jLib.jAuth.UDELicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


public class TestJasperBroker {
	private JasperBroker classUnderTest;
	@Mock private UDE mockUDE;
	@Mock private AuthenticationFacade mockLicenseKeySys;
	@Mock private Broker mockBroker;
	@Mock private ConnectionInfo mockConnectionInfo;
	@Mock private ConnectionContext mockConnectionContext;
	@Mock private UDELicense mockUDELicense;
	@Mock private ClientLicense mockDTALicense;
	private static PersistenceFacade cachingSys;
	private AuthenticationFacade licenseKeySys;
	private String password = "passwd";
	private static String ipAddr;
	private String deploymentId = "junitTest:0";
	private String dtaName = "jasper:testDTA:1.0:junitTest";
	private Integer instanceId = 0;
	private String keystore = "./src/test/java/org/jasper/core/";
	private String dtaKey = "14FD35CD682D0B5368D1C10CA76CA51BDD6A0C2E3F9DEABEB98E9EC920B32B608950E3CCFBD9B76C33EAC2DD7BE956794C6DA4BD57514348EE913011AF43DED61EB61063F56AEC6F2812535A8DC7048DC3D82CB5FF3975864E8FA0AD7517A28326EE77D2BD0314254F0329CAB5E20A43B9C10F1DAF931E9CBA000C1B71E8F324C206AB68A48356D381822173052A01FDF9F0438411020E359789462AD39A40F78498CD32C0A1251C8C4A84C6228173AB79BE4EEAA363BDDA4F3F2283FEC7C3AA1F0BF788C156634CA98BFA04954DDB8CB9C38ACEF4D06511FCA9715D69D998DC94D6116066DDDEA40194CB163218CAE572842B8302D98885894617F550CEFEC2FA5C95A7828F73E985E7F0F81D269B86B30E1B1DEDE98CDF5A721F32A8E3602EDBB5A893F0D4CF2E094827592E7CB0A779A3AA1CD76D4D8C7AD34DFE46F06662EC1F193DD4B315BFE75257B4334E4507287FFCFBF393289A2A665B131CCEBF95D15559C690388E65B674E1B07BD8D8CC6D8520A108C7FF43130A4A71C15E0893E08DB2D17A461254F054CB04A3158B687C296B1E666C5DE892873AE40FB8A3F1D471AACFAAAE3A34584A2EF875DC2BBFD2EA47AC5F84CCBC8788A0CE04D40833282CCA977448176C559B2E495E53A06B9CB3C7B34F1A939D55428F8DEACE25392D4B229D0063BBB405C2C753E0D980295198D75C6217238CB55C2D80FC09E2C50041B9A78CB98FD2D562E1494DB5E15C62B1F2B7219B3ABB48B757A86B07F563757B8A746818FF065084982F0A514FA56D61EECA52814E486D60EF46107D909D1DA55195D45F7C35829EF7D9B84B3F650198AE04AFD5A9BE60FCE5AD8BD1D8C653F636CA239A1EB0796A0C6B12198974E37E7C6C99114410733E9B4FECA2D3BFAFDEA42D8B1C58A0817B8F3F4384F67203D67421C17620D3AF6D65939F337CE788B44D63E6918343718AC2F4DDDBFCC2FF22D2C250ACA675DDDA53132732A857CF7BC213E3CF3B24909E76B76E09D8369ED010BC100A379FA310AF4DD3A0FAE30476D805643B086F9A2E3F9B560FD5A3E52D30D1C0C26BD6A548BD1D2AA9951D419E8B2107A2582BC4DC978D982A39562EF9D716878C2553F486C39FDBAAD7E04738B1A4E7321CD58EDB203680E3C4FB5AA777845A66AAE4420E0C1D423F589EFBCE27D30C2707E1A76816790CAB65916E19F6C0D440B055796F5EDDE34A3D0B528079992ED38CB0B78A1A13E295AB899858111C4422593C63408450B031B704D6E4450C34D0F7D11954BFAED9936F0BB699AB066DB8EA07654A9D50B79804354D3DF8ECEB5242D6A851C87BBD3A47665F86DC0764B9C124AB410D56319F4CA8A95A6B1CC932A1DCAF82196095B05796B869015858D09AA437104314B764F244A55D2E3EE5AADCE9EFD5ECF9ACB4C9996B4BED30230519A685160EAB033EF41E";
	private String udeKey = "35667BB3FDBDA9139E3BC85BE03AADB2A0C66C8394EA3610D462FE7D4B0FDF0B8300B5F7B82B97E4205C6DA2D6F8308B886A195E37D9097ED12080C3177241A5802D497D3AEDF057AFAF390A0CD3A8AAC0BC09DBAB54072F892120F4E5C4C9FE9CC3A947820B57A6632B28D002FF817F171D061BC1EBED1C80021D22ABC58822E14AE532E57F82708A79B2A9092E3EACE4A33F5100E151AACCA68A7689C1CCAB3144234E5AC05D72A3FA5C81B19CE8C5ECB4BC483F9B7A574ADB5B3EDDEA47A345E40572C7FB3580D1578664C76C023F9D9EC0F77CDFD9EF8EE500318914205CFC847FC61FBC76C31F75DFB2D5975C9A49F0F7FEC4E54A3224C91E525D2E109BEF53BC00DA22EA1A1A63CA3878EE672EACCEE3C9DC91154C8DB469E0BACD323B9B1E2F14EC5F895C4997CF429F9013268DC0EE03E2147D6CA562DADC4CDF17643F488D27B3E56D013CA8F71E8EB73BDA9F4A5E787ADC6A9F56B486EFAD04BE70FAA2B3DA2CCB73D51BAB252DE98ABD6C398601CB1C592732C03F38A110E50BE850F9088654A63E5ECE979AE2C9BAB06E47B88EA004DF13B75C33A317FA256CCBE8D54A999FC4EA458E9F327E588D9065CD7FECC75CAF2B1B48566D3A42728CF4A2B477150B765C6AB3CCDF9D18EB02BF06A61A579A59B1BE872E85B59C09BB6C2C802B9B3B86F4673F102BA1D152A6EEEE76C490A1DB9FD6C8BB8C7C6A74FAC9DD78636F912BC0124D960B95BC3D2CABB25B86E1E9F6482E2AC08AF62596AF409C3CB208918DC42B89086B7335CAA00A30E8697CE12EF4C35F822B3FAC33AFE99F66162D638D17EDC84C1271C7E4C9C432458339CFAF00CA85C7A56E9BCD51D5EE95B354CFBEF650BC259387C6112787B8A140B2C88EF3884FB791DE8A9A915475B935A45C35E0F6E55412DE2C4C6A5855F56170D944110BE5DC9EAEEDEC993320707E4CCFBF96E5F670A5FD525EE10ED2E84F2AC6B431D4598D40D1930B5ED51FA335E79FBAC59E3404DEEE7AD46967E836D5A0B3D2ED3229194054F13EC278E5C010FD86A6ECC11414ACF9E36CD289407852E0A559DF095FA0B203EB36A6D4B0964E5D724D9554AD8AB8A3A594AC94179D63421BEB501D7E1EC4DE52D983FD1A77D426927AC82B5CB5AC9EECE345AC27EA404C7B88C9E151F75793687CFAC6D38BBBAA263D7EE7CAAF6700044866D4266257639565694C0FBA7DA193495583A0452E49A02A8000A4480E613A4791CF9A4416133E92A7BE0FF7835914B82929D38FE641F1F947D764B3D769B9ABE3A289E2F4915C37778BD7588EA299E9E1C2AE4ECBF3408C7EE60B573824D2D36D920DEC38C43E1306A3DAC38051DA5F3D3AC565BB019FF3A41A899A199BC7F70F9B54C95A880B5F8AC66D81EF9E6804AF07B814D978EC1FA141554F7A8197612DDC9078F7490C7276BED0F297849F620520";

	
	/*
	 * This tests simulates a remote UDE connecting in a cluster
	 */
	@Test
	public void testAddUDEConnection() throws Exception {
		when(mockConnectionInfo.getPassword()).thenReturn(udeKey);
		licenseKeySys.setKeystoreLocation(keystore);
		licenseKeySys.loadKeys();

		classUnderTest.start();
		classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
	}
	
	/*
	 * This tests adding a local UDE connection
	 */
	@Test
	public void testAddLocalUDEConnection() throws Exception {
		System.out.println("===========================");
		System.out.println("RUNNING JASPER BROKER TESTS");
		System.out.println("===========================");
		when(mockConnectionInfo.getClientIp()).thenReturn("vm://localhost");

		classUnderTest.start();
		classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		TestCase.assertNotNull(classUnderTest);
	}
	
	/*
	 * This tests adding a client connection successfully
	 * and then tries to add a duplicate client connection
	 */
	@Test
	public void testAddClientConnections() throws Exception{
		testAddUDEConnection();
		when(mockConnectionInfo.getUserName()).thenReturn(dtaName);
		when(mockConnectionInfo.getPassword()).thenReturn(dtaKey);
		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn("testLab:0");
		
		classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		try{
			classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		}catch(Exception ex){
			TestCase.assertNotNull(ex);
		}
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
		classUnderTest = new JasperBroker(mockBroker, mockUDE, cachingSys, mockLicenseKeySys);
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
		licenseKeySys.setKeystoreLocation(keystore);
		licenseKeySys.loadKeys();
		when(mockConnectionInfo.getPassword()).thenReturn(udeKey);
		when(mockConnectionInfo.getUserName()).thenReturn(deploymentId);
		when(mockUDE.isThisMyUdeLicense(udeKey)).thenReturn(true);
		
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
		
		when(mockConnectionInfo.getUserName()).thenReturn(deploymentId);
		when(mockConnectionInfo.getPassword()).thenReturn(password);
		
		try{
			classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		}catch (Exception ex){
			TestCase.assertNotNull(ex);
		}
		
		when(mockLicenseKeySys.isUdeAuthenticationValid(deploymentId, password)).thenReturn(true);
		when(mockLicenseKeySys.isSystemDeploymentId(deploymentId)).thenReturn(false);
		when(mockLicenseKeySys.isUdeLicenseKey(password)).thenReturn(true);
		classUnderTest.stop();
		classUnderTest = new JasperBroker(mockBroker, mockUDE, cachingSys, mockLicenseKeySys); 
		classUnderTest.start();
		
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

		// Test security exception due to DTA license key expiry
		classUnderTest.stop();
		classUnderTest = new JasperBroker(mockBroker, mockUDE, cachingSys, mockLicenseKeySys); 
		classUnderTest.start();
		when(mockConnectionInfo.getUserName()).thenReturn(deploymentId);
		when(mockConnectionInfo.getPassword()).thenReturn(password);
		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn("testLab:0");
		when(mockLicenseKeySys.isValidLicenseKey(deploymentId, password)).thenReturn(true);
		when(mockLicenseKeySys.getClientLicense(JAuthHelper.hexToBytes(password))).thenReturn(mockDTALicense);
		when(mockLicenseKeySys.willClientLicenseKeyExpireInDays(mockDTALicense, 0)).thenReturn(true);
		
		try{
			classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		}catch(Exception ex){
			TestCase.assertNotNull(ex);
		}
		
		// test invalid license key security exception 
		when(mockLicenseKeySys.willClientLicenseKeyExpireInDays(mockDTALicense, instanceId)).thenReturn(false);
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
		when(mockConnectionInfo.getUserName()).thenReturn(dtaName);
		when(mockConnectionInfo.getPassword()).thenReturn(dtaKey);
		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn("testLab:0");
		
		classUnderTest.removeConnection(mockConnectionContext, mockConnectionInfo, null);
	}
	
	/*
	 * This tests max producers and consumers thresholds being exceeded
	 */
	@Test
	public void testMaxThresholdsExceeded() throws Exception{
		testAddUDEConnection();
		classUnderTest.stop();
		classUnderTest = new JasperBroker(mockBroker, mockUDE, cachingSys, mockLicenseKeySys);
		classUnderTest.start();
		byte[] value = new byte[1024];
		value = JAuthHelper.hexToBytes(dtaKey);
		when(mockLicenseKeySys.isClientAuthenticationValid(dtaName,dtaKey)).thenReturn(true);
		when(mockConnectionInfo.getUserName()).thenReturn(dtaName);
		when(mockConnectionInfo.getPassword()).thenReturn(dtaKey);
		when(mockLicenseKeySys.isValidLicenseKey(dtaName, dtaKey)).thenReturn(true);
		when(mockLicenseKeySys.getClientNumConsumers(dtaKey)).thenReturn(2);
		when(mockLicenseKeySys.getClientNumPublishers(dtaKey)).thenReturn(25);
		when(mockLicenseKeySys.isSystemDeploymentId("junitTest")).thenReturn(true);
		when(mockLicenseKeySys.getClientLicense(value)).thenReturn(mockDTALicense);
		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn("junitTest:0");
		when(mockDTALicense.getVendor()).thenReturn("jasper");
		when(mockDTALicense.getAppName()).thenReturn("demo-testDTA");
		when(mockDTALicense.getVersion()).thenReturn("1.0");
		when(mockDTALicense.getDeploymentId()).thenReturn(deploymentId);
		
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
			if(!(ex instanceof java.lang.SecurityException)){
				TestCase.fail();
			}
		}
	}
	
	/*
	 * This tests adding a client with an invalid deplomentId
	 */
	@Test
	public void testAddInvalidClientConnection() throws Exception{
		testAddUDEConnection();
		classUnderTest.stop();
		classUnderTest = new JasperBroker(mockBroker, mockUDE, cachingSys, mockLicenseKeySys);
		classUnderTest.start();
		
		byte[] value = new byte[1024];
		value = JAuthHelper.hexToBytes(dtaKey);
		when(mockLicenseKeySys.isClientAuthenticationValid(dtaName,dtaKey)).thenReturn(true);
		when(mockConnectionInfo.getUserName()).thenReturn(dtaName);
		when(mockConnectionInfo.getPassword()).thenReturn(dtaKey);
		when(mockLicenseKeySys.isValidLicenseKey(dtaName, dtaKey)).thenReturn(true);
		when(mockLicenseKeySys.isSystemDeploymentId(deploymentId)).thenReturn(false);
		when(mockLicenseKeySys.getClientLicense(value)).thenReturn(mockDTALicense);
		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn("testLab:0");
		when(mockDTALicense.getVendor()).thenReturn("jasper");
		when(mockDTALicense.getAppName()).thenReturn("demo-testDTA");
		when(mockDTALicense.getVersion()).thenReturn("1.0");
		when(mockDTALicense.getDeploymentId()).thenReturn(deploymentId);
		
		try{
			classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
		}catch(Exception ex){
			TestCase.assertNotNull(ex);
		}
	}
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		ipAddr         = InetAddress.getLocalHost().getHostAddress();
		cachingSys = PersistenceFacadeFactory.getFacade(ipAddr, UUID.randomUUID().toString(), "testPasswd");
		System.setProperty("delegate-property-file", "../zipRoot/jsb-core/config/delegate.properties");
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		licenseKeySys = AuthenticationFacade.getInstance();
		when(mockUDE.getBrokerTransportIp()).thenReturn(ipAddr);
		when(mockConnectionInfo.getPassword()).thenReturn(dtaKey);
		when(mockConnectionInfo.getClientIp()).thenReturn(ipAddr);
		when(mockConnectionContext.getBroker()).thenReturn(mockBroker);
		when(mockConnectionContext.getBroker().getBrokerName()).thenReturn("testBroker");
		when(mockConnectionInfo.getUserName()).thenReturn(deploymentId);
		when(mockLicenseKeySys.getDeploymentId()).thenReturn(deploymentId);
		when(mockLicenseKeySys.isValidLicenseKey(deploymentId, password)).thenReturn(true);
		when(mockLicenseKeySys.isUdeAuthenticationValid(deploymentId, password)).thenReturn(true);
		when(mockUDE.isThisMyUdeLicense(password)).thenReturn(false);	
		when(mockLicenseKeySys.isSystemDeploymentId(deploymentId)).thenReturn(true);
		when(mockUDE.getUdeLicense()).thenReturn(mockUDELicense);
		when(mockUDE.getUdeLicense().getNumOfConsumers()).thenReturn(10);
		when(mockUDE.getUdeLicense().getNumOfPublishers()).thenReturn(10);
		when(mockLicenseKeySys.getUdeNumConsumers(password)).thenReturn(10);
		when(mockLicenseKeySys.getUdeNumPublishers(password)).thenReturn(10);
		
		classUnderTest = new JasperBroker(mockBroker, mockUDE, cachingSys, licenseKeySys);
	}

	@After
	public void tearDown() throws Exception {
		classUnderTest.stop();
		
	}
	
	@AfterClass
	public static void tearDownClass() throws Exception {
		cachingSys.shutdown();
	}

}
