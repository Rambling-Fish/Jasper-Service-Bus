package org.jasper.core;

import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

import junit.framework.TestCase;

import org.apache.activemq.broker.Broker;
import org.jasper.core.auth.AuthenticationFacade;
import org.jasper.core.auth.JasperAuthenticationPlugin;
import org.jasper.core.persistence.PersistenceFacadeFactory;
import org.jasper.core.persistence.PersistenceFacadeImpl;
import org.jasper.core.persistence.PersistenceFacade;
import org.jasper.jLib.jAuth.ClientLicense;
import org.jasper.jLib.jAuth.UDELicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


public class TestJasperAuthentication extends TestCase {
	@Mock private UDE mockUDE;
	@Mock private Broker mockBroker;
	@Mock private UDELicense mockUDELicense;
	@Mock private ClientLicense mockDTALicense;
	private AuthenticationFacade classUnderTest;
	private PersistenceFacade cachingSys;
	private String ipAddr;
	private String deploymentId = "junitTest";
	private String dtaKey = "0D1F11DBE577C780D8DB26A216624A35FF0909B61848D79F9CD67334BAF374E7198DE7140F9538212D83C9422B772E555AE372A739C7E52297B011A4AE8227DE948A50B87405CC13998866B57591D45BE97C2B84D6039DBC731D1F5E12C4CC1E1412949709DD76FEEE6735F39C696F88205D89AF69FFCAA62F0D9FEFB144660417C55A81B305331B0B8DDB45761EDDAFDC4A9B6BD927275D2F1C229499D4C522450861D128CAE61D6EB7E77492A152FDE48C1204B170E60C640FC1A7E979A7A6311BDCE4CCF81272203FA3BD25B613FA2A6794908D6D637EB73DA05BC7B73D73F868A3D5B644753076B3311C50DCB3A0C377FE7FA3D521E27A4AF5BC0C5E3D5978AD54B29F5681C475FFD7498DAF73ACF17714C7DCA7B1B4A627EA5557CE9E94CA71E69CC824C2ABF21031C4DA17FC0566B88E4569B6ADF41F468EEF559904C5791F63041EC2641C9C3876D3A783EC5E2FADDBF4A6A3799870408A06648124ACE3664960572E972F24362D1F012ECA72F73B8F2A1230C2E072D621C625D2015B9D007F994FAC1A43FC52DCD9612271EFC8BE961A195A17A550AC6B1926A48C6486924745C1DCE2C1158A98FA8C5598CC05B83E8123FBF3655B9E43F65081D12E345CB9EBB909079C2D1552BDE973F69ED7E0E321F4221B4AD21A1BBB1695E0E367E1D052A547F49896DC405D87995490D055308767BF7E933497B3FB5FF8F8F70F646ADFC6010C2C83383ADEE08B7AE62A63B3C28A451FE935FF3AD8B85BAC8C8333A227CE8FC87A2D9EF7996F5CD6532DE5364291C7A0654965A4799086F79A9649CC24BB9A5EF5A696FE6F546FB15BE1158BD4B412E5FC0952D221A5140B5DCF1EBE09C3B3842BF9FFE7E22544A4B9B7C45C943F5BA104877A24E809AA40353AC6FA99857417CEB661F71514427DE379EABD5BDEC1577413BD1BF6497B5B9E5F05BF60282D57514841E406CC18EFA13C3C35A27D9B963405DE9297CA958F30F413E55D2A8E54561291C02A03543F13C24591296782C3784BE7C25ECA4BD0D70202042F6E5C3946ED1F11E840513159895BAE5C7C89A198B36433C3107C71B607EC30DC66915BD06B361F32E3973ED9EE67437AAC13E449723A4E6D7216A5F379008991FFEFA6328146BC2D5892989CBC4F9968F9ED1288322F9B82E3D6DE6429A7FC465EDFD3A2059F3F0033128670122FA6EC3C6F00988D0CF9BD410B174A8FB2E828846276C0D351206B93E43861E97F1318C983E00845E80F1F97DDB98C73BDE47D2D0DB5F36071C11863819BBDED4D17A134F8114FD0B4117ACE821DA6818872465127D006E34CBFDBC64D50EE1911CC01D9DC15A80B2B7D91B701038B811E89F8A28FC05286EE8FD5A47D04DC13428435718DF5DE662FC6BD2BA8BEFAF2ACD4465F280D960B66EEC57A137135B9F4C0C3B0D163C380AB6129D794EC3B";
	private String keystore = "./src/test/java/org/jasper/core/";
	private String udeDeployAndInstance = "junitTest:0";
	private String ude = "ude";
	private String version = "2.1";

	
	/*
	 * This tests the Authentication plug-in class
	 */
	@Test
	public void testJasperAuthPlugin() throws Exception {
		System.out.println("==========================================");
		System.out.println("RUNNING JASPER AUTHENTICATION PLUGIN TESTS");
		System.out.println("==========================================");
		String hazelcastGroup = UUID.randomUUID().toString();
		when(mockUDE.getUdeLicense()).thenReturn(mockUDELicense);
		when(mockUDE.getUdeLicense().getNumOfConsumers()).thenReturn(1);
		when(mockUDE.getUdeLicense().getNumOfPublishers()).thenReturn(1);

		cachingSys = PersistenceFacadeFactory.getNonClusteredFacade();
		JasperAuthenticationPlugin authPlugin = new JasperAuthenticationPlugin(mockUDE, cachingSys, classUnderTest);
		authPlugin.installPlugin(mockBroker);
		cachingSys.shutdown();
	}
	
	/*
	 * This tests the AuthenticationFacade getters/setters/boolean methods
	 * for UDE license key
	 */
//	@Test
//	public void testAuthenticationFacadeUDEMethods() throws Exception {
//		UDELicense udeLicense;
//		Calendar now = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
//		when(mockUDELicense.getExpiry()).thenReturn(now);
//		System.out.println("===================================");
//		System.out.println("RUNNING AUTHENTICATION FACADE TESTS");
//		System.out.println("===================================");
//		
//		udeLicense = classUnderTest.loadKeys(keystore);
//		String deployId = classUnderTest.getDeploymentId();
//		TestCase.assertEquals(deployId, deploymentId);
//		
//		classUnderTest.isSystemDeploymentId(deploymentId);
//		byte[] bytePasswd = udeLicense.getLicenseKey();
//		String udeDeployAndInst = classUnderTest.getUdeDeploymentAndInstance(JAuthHelper.bytesToHex(bytePasswd));
//		TestCase.assertEquals(udeDeployAndInst, udeDeployAndInstance);
//		udeDeployAndInst = classUnderTest.getUdeDeploymentAndInstance("6F30C6A3A817E5220AB70CC7483A845CDE949E89A3E5");
//		TestCase.assertNull(udeDeployAndInst);
//		
//		String udeInst = classUnderTest.getUdeInstance(JAuthHelper.bytesToHex(bytePasswd));
//		String udeType = classUnderTest.getUdeType(JAuthHelper.bytesToHex(bytePasswd));
//		String udeVersion = classUnderTest.getUdeVersion(JAuthHelper.bytesToHex(bytePasswd));
//		int pubs = classUnderTest.getUdeNumPublishers(JAuthHelper.bytesToHex(bytePasswd));
//		int cons = classUnderTest.getUdeNumConsumers(JAuthHelper.bytesToHex(bytePasswd));
//		TestCase.assertEquals(udeInst, udeDeployAndInstance);
//		TestCase.assertEquals(udeType, ude);
//		TestCase.assertEquals(udeVersion, version);
//		TestCase.assertEquals(pubs, 1);
//		TestCase.assertEquals(cons, 1);
//		
//		
//		classUnderTest.isValidLicenseKey("user", "password");
//		classUnderTest.isValidUdeLicenseKey(udeLicense);
//		
//		classUnderTest.getUdeExpiryDate(udeLicense);
//		classUnderTest.getUdeExpiryDate(mockUDELicense);
//		classUnderTest.isValidUdeLicenseKeyExpiry(udeLicense);
//		classUnderTest.isValidUdeLicenseKeyExpiry(mockUDELicense);
//		classUnderTest.willUdeLicenseKeyExpireInDays(udeLicense, 15);
//		classUnderTest.willUdeLicenseKeyExpireInDays(mockUDELicense, 15);
//		classUnderTest.isUdeLicenseKey(JAuthHelper.bytesToHex(bytePasswd));
//		classUnderTest.isUdeLicenseKey(dtaKey);
//		
//		
//	}
//	
//	/*
//	 * This tests Security exception cause no license key found
//	 */
//	@Test
//	public void testNoLicenceKeyException() throws Exception {
//		try{
//			classUnderTest.loadKeys("./");
//		}catch(Exception ex){
//			TestCase.assertNotNull(ex);
//		}
//	}
	
//	/*
//	 * This tests the AuthenticationFacade getters/setters/boolean methods
//	 * for Client license key
//	 */
//	@Test
//	public void testAuthenticationFacadeClientMethods() throws Exception {
//		classUnderTest.loadKeys(keystore);
//		ClientLicense clientLic = classUnderTest.getClientLicense(JAuthHelper.hexToBytes(dtaKey));
//
//		String dtaQ    = classUnderTest.getClientAdminQueue(dtaKey);
//		String deploy  = classUnderTest.getClientDeploymentId(dtaKey);
//		String type    = classUnderTest.getClientType(dtaKey);
//		String version = classUnderTest.getClientVersion(dtaKey);
//		String expiry  = classUnderTest.getClientExpiryDate(clientLic);
//		int cons       = classUnderTest.getClientNumConsumers(dtaKey);
//		int pubs       = classUnderTest.getClientNumPublishers(dtaKey);
//		boolean result = classUnderTest.willClientLicenseKeyExpireInDays(clientLic, 1);
//		
//		TestCase.assertEquals(deploy, "junitTest");
//		TestCase.assertEquals(dtaQ, "testQ");
//		TestCase.assertEquals(type, "dta");
//		TestCase.assertEquals(version, "1.0");
//		TestCase.assertEquals(cons, 1);
//		TestCase.assertEquals(pubs, 1);
//		TestCase.assertNotNull(expiry);
//		TestCase.assertEquals(false, result);
//		
//		clientLic.setExpiry(null);
//		expiry = classUnderTest.getClientExpiryDate(clientLic);
//		TestCase.assertEquals(0, expiry.length());
//	}


	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		ipAddr = InetAddress.getLocalHost().getHostAddress();
		classUnderTest = AuthenticationFacade.getInstance();
	}

	@After
	public void tearDown() throws Exception {
		classUnderTest = null;
		
	}

}
