package src.main.java;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;

import org.apache.log4j.Logger;

public class SfUtils {
    private static final Logger log = Logger.getLogger(SfUtils.class.getName());

    public static String queryRecordType(PartnerConnection conn, String objectName,
                                         String name) {
    	log.info("Querying for " + name + " record type from " + objectName + "...");
        String recordTypeId = null;
    	try {
            // Query for record type name.
    		String sql = "SELECT Id, Name, SobjectType FROM RecordType " +
                         "WHERE Name = '" + name + "' " +
                         "  AND SobjectType = '" + objectName + "' ";
    		QueryResult queryResults = conn.query(sql);
    		if (queryResults.getSize() > 0) {
    			for (SObject s: queryResults.getRecords()) {
                    recordTypeId = s.getId();
    			}
    		}
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}

        return recordTypeId;
    }

    public static List<DbClient> queryCIEClients(PartnerConnection conn, String contactRecordTypeId) {
    	log.info("Querying CIE opt-in clients...");
        List<DbClient> results = new ArrayList<DbClient>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
    		StringBuilder sb = new StringBuilder();
    		sb.append("SELECT Id, FirstName, LastName, Birthdate, Gender_Identity__c, ");
    		sb.append("SSN__c, Mailing_Street__c, Mailing_Apt_Num__c, Mailing_City__c, ");
    		sb.append("Mailing_State__c, Mailing_Zip__c, CIE_Client__c, CIE_Opt_In__c, ");
    		sb.append("CreatedDate ");
    		sb.append("FROM Contact ");
    		sb.append("WHERE RecordTypeId = '" + contactRecordTypeId + "' ");
    		sb.append("  AND CIE_Client__c = TRUE ");
    		sb.append("  AND CIE_Opt_In__c = TRUE ");
    		//sb.append("  AND Id = '003d000003ADuSG' "); // Pending test client.
    		//sb.append("  AND Id = '003d00000397Tfu' "); // Denied test client.
    		//sb.append("  AND Id = '003d000003AAIjx' "); // Approved test client.
    		//sb.append("  AND LastModifiedDate >= LAST_N_DAYS:2 ");
    		sb.append("  AND LastModifiedDate >= YESTERDAY ");

    		QueryResult queryResults = conn.query(sb.toString());
    		if (queryResults.getSize() > 0) {
    			for (SObject s: queryResults.getRecords()) {
    				DbClient c = new DbClient();
                    c.caseNumber = s.getId();
                    String first = (String)s.getField("FirstName");
                    String last = (String)s.getField("LastName");
                    if (first == null || last == null) {
                        continue;
                    }

                    c.firstName = first;
                    c.lastName = last;

                    String str = (String)s.getField("Birthdate");
                    if (str != null && str.trim().length() > 0) {
                        c.dob = sdf.parse(str);
                    }

                    str = (String)s.getField("Gender_Identity__c");
                    if (str != null && str.trim().length() > 0) {
                        if (str.equalsIgnoreCase("women")) {
                            c.genderId = 1;
                        }
                        else if (str.equalsIgnoreCase("man")) {
                            c.genderId = 2;
                        }
                        else {
                            c.genderId = 8; // Data not collected.
                        }
                    }
                    else {
                        c.genderId = 8; // Data not collected.
                    }

                    c.ssn = (String)s.getField("SSN__c");
                    c.address1 = (String)s.getField("Mailing_Street__c");
                    c.address2 = (String)s.getField("Mailing_Apt_Num__c");
                    c.city = (String)s.getField("Mailing_City__c");
                    c.state = (String)s.getField("Mailing_State__c");
                    c.postalCode = (String)s.getField("Mailing_Zip__c");

                    str = (String)s.getField("CreatedDate");
                    if (str != null && str.trim().length() > 0) {
                        c.consentDate = sdf.parse(str);
                    }

                    // @debug.
    				log.info("Id: " + c.caseNumber);
    				log.info("Name: " + c.firstName + " " + c.lastName);
                    if (c.dob != null) {
                        log.info("DoB: " + c.dob.toString());
                    }
    				log.info("Gender: " + c.genderId);
    				log.info("Address: " + c.address1 + ", " + c.city + ", " + c.state + ", " + c.postalCode);

                    // Add to result list.
                    results.add(c);
    			}
    		}
        }
        catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    public static List<SfProgramInfo> queryClientPrograms(PartnerConnection conn,
                                                          String contactRecordTypeId,
                                                          String contactId,
                                                          String applicationType) {
    	log.info("Querying client programs...");
        List<SfProgramInfo> results = new ArrayList<SfProgramInfo>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
    		StringBuilder sb = new StringBuilder();
    		sb.append("SELECT Id, Name, RecordType.Name, LastModifiedDate, Applicant_Name__c, ");
    		sb.append("Application_Type__c, Application_Date__c, Application_Status__c, ");
    		sb.append("Reason_Incomplete__c, Beginning_Date_of_Aid_BDA__c, Denial_Reason__c, ");
    		sb.append("Notes__c, MyBCW_Confirmation_Number__c, Current_Source_of_Income__c, ");
    		sb.append("Monthly_Gross_Income__c, Number_People_in_Household__c, Documentation_Submitted__c ");
    		sb.append("FROM Program__c ");
    		sb.append("WHERE Client__r.RecordTypeId = '" + contactRecordTypeId + "' ");
    		sb.append("  AND Client__r.Id = '" + contactId + "' ");
            if (applicationType != null && applicationType.trim().length() > 0) {
                sb.append("  AND Application_Type__c = '" + applicationType + "' ");
            }

    		QueryResult queryResults = conn.query(sb.toString());
    		if (queryResults.getSize() > 0) {
    			System.out.println("Found " + queryResults.getSize() + " programs");
    			for (SObject s: queryResults.getRecords()) {
                    SfProgramInfo pi = new SfProgramInfo();
                    pi.id = s.getId();
    				System.out.println("Id: " + pi.id);

                    pi.appType = (String)s.getField("Application_Type__c");
    				System.out.println("App Type: " + pi.appType);

                    String str = (String)s.getField("Application_Date__c");
                    if (str != null && str.trim().length() > 0) {
                        pi.appDate = sdf.parse(str);
                        System.out.println("App Date: " + str);
                    }

    				pi.appStatus = (String)s.getField("Application_Status__c");
    				System.out.println("App Status: " + pi.appStatus);
    				pi.confirmationNumber = (String)s.getField("MyBCW_Confirmation_Number__c");
    				System.out.println("County Confirmation Number: " + pi.confirmationNumber);

    				str = (String)s.getField("Beginning_Date_of_Aid_BDA__c");
                    if (str != null && str.trim().length() > 0) {
                        pi.appDate = sdf.parse(str);
                        System.out.println("BDA: " + str);
                    }

    				pi.reasonIncomplete = (String)s.getField("Reason_Incomplete__c");
    				System.out.println("Reason Incomplete: " + pi.reasonIncomplete);
    				pi.denialReason = (String)s.getField("Denial_Reason__c");
    				System.out.println("Denial Reason: " + pi.denialReason);
    				pi.notes = (String)s.getField("Notes__c");
    				System.out.println("Notes: " + pi.notes);
    				pi.incomeSource = (String)s.getField("Current_Source_of_Income__c");
    				System.out.println("Income Source: " + pi.incomeSource);
    				pi.monthlyIncome = (String)s.getField("Monthly_Gross_Income__c");
    				System.out.println("Monthly Income: " + pi.monthlyIncome);

    				str = (String)s.getField("Number_People_in_Household__c");
                    if (str != null && str.trim().length() > 0) {
                        pi.householdSize = (int)Float.parseFloat(str);
                        System.out.println("Household Size`: " + str);
                    }

    				str = (String)s.getField("LastModifiedDate");
                    if (str != null && str.trim().length() > 0) {
                        pi.lastModified = sdf.parse(str);
                        System.out.println("Last Modified Date: " + str);
                    }

    				pi.docSubmitted = (String)s.getField("Documentation_Submitted__c");
    				System.out.println("Document Submitted: " + pi.docSubmitted);

                    // @debug.
                    /*
    				System.out.println("Name: " + s.getField("Name"));
    				System.out.println("RecordType.Name: " + s.getChild("RecordType").getField("Name"));
    				System.out.println("App Name: " + s.getField("Applicant_Name__c"));
                    */
    				System.out.println("-----");

                    // Add to result list.
                    results.add(pi);
    			}
    		}
        }
        catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }

        return results;
    }
}
