package org.jasper.core;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

//
//
public class TestJasperBroker extends TestCase {
	private JasperBroker classUnderTest;
	@Mock private UDE mockUDE;
	@Mock private AuthenticationFacade mockLicenseKeySys;
	@Mock private Broker mockBroker;
	@Mock private ConnectionInfo mockConnectionInfo;
	@Mock private ConnectionContext mockConnectionContext;
	@Mock private UDELicense mockUDELicense;
	@Mock private ClientLicense mockDTALicense;
	private PersistenceFacade cachingSys;
	private AuthenticationFacade licenseKeySys;
	private String password = "passwd";
	private String ipAddr;
	private String deploymentId = "junitTest:0";
	private String dtaName = "jasper:testDTA:1.0:junitTest";
	private Integer instanceId = 0;
	private String keystore = "./src/test/java/org/jasper/core/";
	private String dtaKey = "0D1F11DBE577C780D8DB26A216624A35FF0909B61848D79F9CD67334BAF374E7198DE7140F9538212D83C9422B772E555AE372A739C7E52297B011A4AE8227DE948A50B87405CC13998866B57591D45BE97C2B84D6039DBC731D1F5E12C4CC1E1412949709DD76FEEE6735F39C696F88205D89AF69FFCAA62F0D9FEFB144660417C55A81B305331B0B8DDB45761EDDAFDC4A9B6BD927275D2F1C229499D4C522450861D128CAE61D6EB7E77492A152FDE48C1204B170E60C640FC1A7E979A7A6311BDCE4CCF81272203FA3BD25B613FA2A6794908D6D637EB73DA05BC7B73D73F868A3D5B644753076B3311C50DCB3A0C377FE7FA3D521E27A4AF5BC0C5E3D5978AD54B29F5681C475FFD7498DAF73ACF17714C7DCA7B1B4A627EA5557CE9E94CA71E69CC824C2ABF21031C4DA17FC0566B88E4569B6ADF41F468EEF559904C5791F63041EC2641C9C3876D3A783EC5E2FADDBF4A6A3799870408A06648124ACE3664960572E972F24362D1F012ECA72F73B8F2A1230C2E072D621C625D2015B9D007F994FAC1A43FC52DCD9612271EFC8BE961A195A17A550AC6B1926A48C6486924745C1DCE2C1158A98FA8C5598CC05B83E8123FBF3655B9E43F65081D12E345CB9EBB909079C2D1552BDE973F69ED7E0E321F4221B4AD21A1BBB1695E0E367E1D052A547F49896DC405D87995490D055308767BF7E933497B3FB5FF8F8F70F646ADFC6010C2C83383ADEE08B7AE62A63B3C28A451FE935FF3AD8B85BAC8C8333A227CE8FC87A2D9EF7996F5CD6532DE5364291C7A0654965A4799086F79A9649CC24BB9A5EF5A696FE6F546FB15BE1158BD4B412E5FC0952D221A5140B5DCF1EBE09C3B3842BF9FFE7E22544A4B9B7C45C943F5BA104877A24E809AA40353AC6FA99857417CEB661F71514427DE379EABD5BDEC1577413BD1BF6497B5B9E5F05BF60282D57514841E406CC18EFA13C3C35A27D9B963405DE9297CA958F30F413E55D2A8E54561291C02A03543F13C24591296782C3784BE7C25ECA4BD0D70202042F6E5C3946ED1F11E840513159895BAE5C7C89A198B36433C3107C71B607EC30DC66915BD06B361F32E3973ED9EE67437AAC13E449723A4E6D7216A5F379008991FFEFA6328146BC2D5892989CBC4F9968F9ED1288322F9B82E3D6DE6429A7FC465EDFD3A2059F3F0033128670122FA6EC3C6F00988D0CF9BD410B174A8FB2E828846276C0D351206B93E43861E97F1318C983E00845E80F1F97DDB98C73BDE47D2D0DB5F36071C11863819BBDED4D17A134F8114FD0B4117ACE821DA6818872465127D006E34CBFDBC64D50EE1911CC01D9DC15A80B2B7D91B701038B811E89F8A28FC05286EE8FD5A47D04DC13428435718DF5DE662FC6BD2BA8BEFAF2ACD4465F280D960B66EEC57A137135B9F4C0C3B0D163C380AB6129D794EC3B";
	private String udeKey = "35667BB3FDBDA9139E3BC85BE03AADB2A0C66C8394EA3610D462FE7D4B0FDF0B8300B5F7B82B97E4205C6DA2D6F8308B886A195E37D9097ED12080C3177241A5802D497D3AEDF057AFAF390A0CD3A8AAC0BC09DBAB54072F892120F4E5C4C9FE9CC3A947820B57A6632B28D002FF817F171D061BC1EBED1C80021D22ABC58822E14AE532E57F82708A79B2A9092E3EACE4A33F5100E151AACCA68A7689C1CCAB3144234E5AC05D72A3FA5C81B19CE8C5ECB4BC483F9B7A574ADB5B3EDDEA47A345E40572C7FB3580D1578664C76C023F9D9EC0F77CDFD9EF8EE500318914205CFC847FC61FBC76C31F75DFB2D5975C9A49F0F7FEC4E54A3224C91E525D2E109BEF53BC00DA22EA1A1A63CA3878EE672EACCEE3C9DC91154C8DB469E0BACD323B9B1E2F14EC5F895C4997CF429F9013268DC0EE03E2147D6CA562DADC4CDF17643F488D27B3E56D013CA8F71E8EB73BDA9F4A5E787ADC6A9F56B486EFAD04BE70FAA2B3DA2CCB73D51BAB252DE98ABD6C398601CB1C592732C03F38A110E50BE850F9088654A63E5ECE979AE2C9BAB06E47B88EA004DF13B75C33A317FA256CCBE8D54A999FC4EA458E9F327E588D9065CD7FECC75CAF2B1B48566D3A42728CF4A2B477150B765C6AB3CCDF9D18EB02BF06A61A579A59B1BE872E85B59C09BB6C2C802B9B3B86F4673F102BA1D152A6EEEE76C490A1DB9FD6C8BB8C7C6A74FAC9DD78636F912BC0124D960B95BC3D2CABB25B86E1E9F6482E2AC08AF62596AF409C3CB208918DC42B89086B7335CAA00A30E8697CE12EF4C35F822B3FAC33AFE99F66162D638D17EDC84C1271C7E4C9C432458339CFAF00CA85C7A56E9BCD51D5EE95B354CFBEF650BC259387C6112787B8A140B2C88EF3884FB791DE8A9A915475B935A45C35E0F6E55412DE2C4C6A5855F56170D944110BE5DC9EAEEDEC993320707E4CCFBF96E5F670A5FD525EE10ED2E84F2AC6B431D4598D40D1930B5ED51FA335E79FBAC59E3404DEEE7AD46967E836D5A0B3D2ED3229194054F13EC278E5C010FD86A6ECC11414ACF9E36CD289407852E0A559DF095FA0B203EB36A6D4B0964E5D724D9554AD8AB8A3A594AC94179D63421BEB501D7E1EC4DE52D983FD1A77D426927AC82B5CB5AC9EECE345AC27EA404C7B88C9E151F75793687CFAC6D38BBBAA263D7EE7CAAF6700044866D4266257639565694C0FBA7DA193495583A0452E49A02A8000A4480E613A4791CF9A4416133E92A7BE0FF7835914B82929D38FE641F1F947D764B3D769B9ABE3A289E2F4915C37778BD7588EA299E9E1C2AE4ECBF3408C7EE60B573824D2D36D920DEC38C43E1306A3DAC38051DA5F3D3AC565BB019FF3A41A899A199BC7F70F9B54C95A880B5F8AC66D81EF9E6804AF07B814D978EC1FA141554F7A8197612DDC9078F7490C7276BED0F297849F620520";
	
	/*
	 * This tests simulates a remote UDE connecting in a cluster
	 */
	@Test
	public void testAddUDEConnection() throws Exception {
		when(mockConnectionInfo.getPassword()).thenReturn(udeKey);
		licenseKeySys.loadKeys(keystore);

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
		licenseKeySys.loadKeys(keystore);
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
		
		classUnderTest.addConnection(mockConnectionContext, mockConnectionInfo);
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

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		ipAddr = InetAddress.getLocalHost().getHostAddress();
		cachingSys = new PersistenceFacade(ipAddr, "testGroup", "testPassword");
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
//		when(mockLicenseKeySys.loadKeys(deploymentId)).thenReturn(mockUDELicense);
//		when(mockUDELicense.getDeploymentId()).thenReturn(deploymentId);		
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
		cachingSys.shutdown();
		
	}

}
