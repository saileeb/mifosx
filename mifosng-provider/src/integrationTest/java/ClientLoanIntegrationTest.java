
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.post;
import static com.jayway.restassured.path.json.JsonPath.from;
import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;

/**
 * Client Loan Integration Test for checking Loan Application Repayment Schedule.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ClientLoanIntegrationTest {
    String basicAuthKey;
    ResponseSpecification responseSpec;
    RequestSpecification requestSpec;

    @Before
    public void setup() {
        basicAuthKey = loginIntoServerAndGetBase64EncodedAuthenticationKey();

        requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        requestSpec.header("Authorization","Basic "+basicAuthKey);

        responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
    }

    @Test
    public void checkClientLoanCreateAndDisburseFlow(){

        Integer clientID = createClient();
        System.out.println("------------------------GENERATED CLIENT ID: "+clientID+"---------------------------\n");
        checkClientCreatedOnServer(clientID);

        Integer loanProductID = createLoanProduct();
        System.out.println("-----------------------GENERATED LOAN PRODUCT ID : "+loanProductID+"---------------------------\n");

        applyForLoanApplication(clientID, loanProductID);

        ArrayList <HashMap> loanSchedule = getLoanRepaymentSchedule(loanProductID);
        verifyLoanRepaymentSchedule(loanSchedule);

    }

    private String loginIntoServerAndGetBase64EncodedAuthenticationKey() {
        System.out.println("-----------------------------------LOGIN-----------------------------------------");
        String json = post("/mifosng-provider/api/v1/authentication?username=mifos&password=password&tenantIdentifier=default").asString();
        return JsonPath.with(json).get("base64EncodedAuthenticationKey");
    }

    private Integer createClient() {
        System.out.println("---------------------------------CREATING A CLIENT---------------------------------------------");
        String json = given().spec(requestSpec).body(getTestClientAsJSON())
                     .expect().spec(responseSpec)
                     .when().post("/mifosng-provider/api/v1/clients?tenantIdentifier=default")
                     .andReturn().asString();
        
        return  from(json).get("clientId");
    }

    private void checkClientCreatedOnServer(final Integer generatedClientID) {
        System.out.println("------------------------------CHECK CLIENT DETAILS------------------------------------\n");
        given().spec(requestSpec)
        .expect().spec(responseSpec)
        .when().get("/mifosng-provider/api/v1/clients/" + generatedClientID + "?tenantIdentifier=default");
    }

    private Integer createLoanProduct() {
        System.out.println("------------------------------CREATING NEW LOAN PRODUCT ---------------------------------------");
        String json = given().spec(requestSpec).body(getTestLoanProductAsJSON())
                      .expect().spec(responseSpec)
                      .when().post("/mifosng-provider/api/v1/loanproducts?tenantIdentifier=default")
                      .andReturn().asString();
        return from(json).get("resourceId");
    }

    private void applyForLoanApplication(final Integer clientID, final Integer loanProductID) {
        System.out.println("--------------------------------APPLYING FOR LOAN APPLICATION--------------------------------");
        given().spec(requestSpec).body(getLoanApplicationBodyAsJSON(clientID.toString(), loanProductID.toString()))
        .expect().spec(responseSpec)
        .when().post("/mifosng-provider/api/v1/loans?tenantIdentifier=default");
    }

    private ArrayList  getLoanRepaymentSchedule(final Integer productID)
    {
        System.out.println("---------------------------GETTING LOAN REPAYMENT SCHEDULE--------------------------------");
       String json = given().spec(requestSpec).body(getLoanCalculationBodyAsJSON(productID.toString()))
                .expect().spec(responseSpec)
                .when().post("/mifosng-provider/api/v1/loans?command=calculateLoanSchedule&tenantIdentifier=default").andReturn().asString();
        return from(json).get("periods");
    }

    private void verifyLoanRepaymentSchedule(final ArrayList<HashMap> loanSchedule) {
        System.out.println("--------------------VERIFYING THE PRINCIPAL DUES,INTEREST DUE AND DUE DATE--------------------------");
        
        assertEquals("Checking for Due Date for 1st Month",new ArrayList<Integer>(Arrays.asList(2011, 10, 20)),loanSchedule.get(1).get("dueDate"));
        assertEquals("Checking for Principal Due for 1st Month", new Float("2911.49"), loanSchedule.get(1).get("principalOriginalDue"));
        assertEquals("Checking for Interest Due for 1st Month",new Float("240.00"),loanSchedule.get(1).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 2nd Month",new ArrayList<Integer>(Arrays.asList(2011,11,20)),loanSchedule.get(2).get("dueDate"));
        assertEquals("Checking for Principal Due for 2nd Month", new Float("2969.72"), loanSchedule.get(2).get("principalDue"));
        assertEquals("Checking for Interest Due for 2nd Month",new Float("181.77"),loanSchedule.get(2).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 3rd Month",new ArrayList<Integer>(Arrays.asList(2011,12,20)),loanSchedule.get(3).get("dueDate"));
        assertEquals("Checking for Principal Due for 3rd Month", new Float("3029.11"), loanSchedule.get(3).get("principalDue"));
        assertEquals("Checking for Interest Due for 3rd Month",new Float("122.38"),loanSchedule.get(3).get("interestOriginalDue"));

        assertEquals("Checking for Due Date for 4th Month",new ArrayList<Integer>(Arrays.asList(2012,1,20)),loanSchedule.get(4).get("dueDate"));
        assertEquals("Checking for Principal Due for 4th Month", new Float("3089.68"), loanSchedule.get(4).get("principalDue"));
        assertEquals("Checking for Interest Due for 4th Month",new Float("61.79"),loanSchedule.get(4).get("interestOriginalDue"));
    }


    private String getTestClientAsJSON() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("officeId", "1");
        map.put("firstname", randomNameGenerator("Client_FirstName_", 5));
        map.put("lastname", randomNameGenerator("Client_LastName_", 4));
        map.put("externalId", randomIDGenerator("ID_", 7));
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("locale", "en");
        map.put("joinedDate", "04 March 2009");
        return new Gson().toJson(map);
    }

    private String getTestLoanProductAsJSON(){
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("name", randomNameGenerator("LOAN_PRODUCT_", 6));
        map.put("currencyCode", "INR");
        map.put("locale", "en_GB");
        map.put("digitsAfterDecimal", "2");
        map.put("principal", "12,000.00");
        map.put("numberOfRepayments", "4");
        map.put("repaymentEvery", "1");
        map.put("repaymentFrequencyType", "2");
        map.put("interestRatePerPeriod", "2");
        map.put("interestRateFrequencyType", "2");
        map.put("amortizationType", "1");
        map.put("interestType", "0");
        map.put("interestCalculationPeriodType", "1");
        map.put("inArrearsTolerance", "0");
        map.put("transactionProcessingStrategyId", "1");
        map.put("accountingRule", "1") ;
        return new Gson().toJson(map);
    }

    private String getLoanApplicationBodyAsJSON(final String clientID,final String productID){
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("locale", "en_GB");
        map.put("clientId", clientID);
        map.put("productId", productID);
        map.put("principal", "12,000.00");
        map.put("loanTermFrequency", "4");
        map.put("loanTermFrequencyType", "2");
        map.put("numberOfRepayments", "4");
        map.put("repaymentEvery", "1");
        map.put("repaymentFrequencyType", "2");
        map.put("interestRateFrequencyType", "2");
        map.put("interestRatePerPeriod", "2");
        map.put("amortizationType", "1");
        map.put("interestType", "0");
        map.put("interestCalculationPeriodType", "1");
        map.put("transactionProcessingStrategyId", "1");
        map.put("expectedDisbursementDate", "20 September 2011");
        map.put("submittedOnDate", "20 September 2011");
        return new Gson().toJson(map);
    }

    private String getLoanCalculationBodyAsJSON(final String productID){
         HashMap<String, String> map = new HashMap<String, String>();
         map.put("dateFormat", "dd MMMM yyyy");
         map.put("locale", "en_GB");
         map.put("productId", productID);
         map.put("principal", "12,000.00");
         map.put("loanTermFrequency", "4");
         map.put("loanTermFrequencyType", "2");
         map.put("numberOfRepayments", "4");
         map.put("repaymentEvery", "1");
         map.put("repaymentFrequencyType", "2");
         map.put("interestRateFrequencyType", "2");
         map.put("interestRatePerPeriod", "2");
         map.put("amortizationType", "1");
         map.put("interestType", "0");
         map.put("interestCalculationPeriodType", "1");
         map.put("expectedDisbursementDate", "20 September 2011");
         map.put("transactionProcessingStrategyId", "1");
         return new Gson().toJson(map);
     }

    public String randomIDGenerator(final String prefix, final int lenOfRandomSuffix) {
        return randomStringGenerator(prefix,lenOfRandomSuffix, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    public String randomNameGenerator(final String prefix, final int lenOfRandomSuffix) {
        return randomStringGenerator(prefix,lenOfRandomSuffix, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    private String randomStringGenerator(final String prefix,final int len, final String sourceSetString) {
        int lengthOfSource = sourceSetString.length();
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append((sourceSetString).charAt(rnd.nextInt(lengthOfSource)));
        return (prefix+(sb.toString()));
    }

}
