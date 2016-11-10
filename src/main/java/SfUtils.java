package src.main.java;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    		sb.append("Mailing_State__c, Mailing_Zip__c, Phone_1_Primary__c, Phone_2__c, ");
    		sb.append("Email, Race__c, What_is_your_preferred_language__c, CIE_Client__c, CIE_Opt_In__c, ");
    		sb.append("CreatedDate ");
    		sb.append("FROM Contact ");
    		sb.append("WHERE RecordTypeId = '" + contactRecordTypeId + "' ");
    		sb.append("  AND CIE_Client__c = TRUE ");
    		sb.append("  AND CIE_Opt_In__c = TRUE ");
    		sb.append("  AND Id = '003d000002prhl1' "); // Sample Sue.
    		//sb.append("  AND Id = '003d000003ADuSG' "); // Pending test client.
    		//sb.append("  AND Id = '003d00000397Tfu' "); // Denied test client.
    		//sb.append("  AND Id = '003d000003AAIjx' "); // Approved test client.
    		//sb.append("  AND LastModifiedDate >= LAST_N_DAYS:2 ");
    		//sb.append("  AND LastModifiedDate >= YESTERDAY ");

    		QueryResult queryResults = conn.query(sb.toString());
    		if (queryResults.getSize() > 0) {
    			for (SObject s: queryResults.getRecords()) {
    				DbClient c = new DbClient();
                    c.caseNumber = s.getId();
                    String first = (String)s.getField("FirstName");
                    String last = (String)s.getField("LastName");
                    String dob = (String)s.getField("Birthdate");
                    if (first == null || first.trim().length() <= 0 ||
                        last == null || last.trim().length() <= 0 ||
                        dob == null || dob.trim().length() <= 0) {
                        log.error("Either first name or last name or dob is missing from client record");
                        continue;
                    }

                    c.firstName = first;
                    c.lastName = last;

                    Calendar cal = Calendar.getInstance();
                    cal.setTime(sdf.parse(dob));
                    cal.set(Calendar.HOUR, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    c.dob = cal.getTime();

                    String str = (String)s.getField("Gender_Identity__c");
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
                    c.homePhone = (String)s.getField("Phone_1_Primary__c");
                    c.cellPhone = (String)s.getField("Phone_2__c");
                    c.email = (String)s.getField("Email");
                    c.race = (String)s.getField("Race__c");
                    c.language = (String)s.getField("What_is_your_preferred_language__c");

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
    				log.info("Language: " + c.language);

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
                                                          String contactId) {
    	log.info("Querying client programs...");
        List<SfProgramInfo> results = new ArrayList<SfProgramInfo>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
    		StringBuilder sb = new StringBuilder();
    		sb.append("SELECT Id, Name, RecordType.Name, LastModifiedDate, ");
    		sb.append("Applicant_Name__c, Application_Type__c, Application_Date__c, ");
    		sb.append("Application_Status__c, Reason_Incomplete__c, Beginning_Date_of_Aid_BDA__c, ");
    		sb.append("Denial_Reason__c, Notes__c, MyBCW_Confirmation_Number__c, ");
    		sb.append("Current_Source_of_Income__c, Monthly_Gross_Income__c, ");
            sb.append("Number_People_in_Household__c, Documentation_Submitted__c, ");
            //sb.append("Language__c, Caller_Caregiver_Name__c, Caller_Caregiver_Phone__c, ");
            sb.append("Project_Status__c, Health_Insurance_Provider__c, ");
            sb.append("Primary_Diagnosis__c, Do_you_have_a_primary_care_doctor__c, ");
            sb.append("Recent_Hospitalizations_in_past_6_mo__c, Have_you_had_recent_falls_in_past_6_mo__c, ");
            sb.append("Outcome_re_visits_30_Days__c ");
    		sb.append("FROM Program__c ");
    		sb.append("WHERE Client__r.RecordTypeId = '" + contactRecordTypeId + "' ");
    		sb.append("  AND Client__r.Id = '" + contactId + "' ");

    		QueryResult queryResults = conn.query(sb.toString());
    		if (queryResults.getSize() > 0) {
    			log.info("Found " + queryResults.getSize() + " programs");
    			for (SObject s: queryResults.getRecords()) {
                    SfProgramInfo pi = new SfProgramInfo();
                    pi.id = s.getId();
    				log.info("Id: " + pi.id);

    				String recType = (String)s.getChild("RecordType").getField("Name");
                    if (recType.equalsIgnoreCase("application")) { // CalFresh, Medi-Cal.
                        pi.appType = (String)s.getField("Application_Type__c");
                        pi.appStatus = (String)s.getField("Application_Status__c");
                    }
                    else { // Health Nav programs.
                        pi.appType = recType;
                        pi.appStatus = (String)s.getField("Project_Status__c");
                    }
				    log.info("App Type: " + pi.appType);
    				log.info("App Status: " + pi.appStatus);

                    String str = (String)s.getField("Application_Date__c");
                    if (str != null && str.trim().length() > 0) {
                        pi.appDate = sdf.parse(str);
                        log.info("App Date: " + str);
                    }

    				pi.confirmationNumber = (String)s.getField("MyBCW_Confirmation_Number__c");
    				//log.info("County Confirmation Number: " + pi.confirmationNumber);

    				str = (String)s.getField("Beginning_Date_of_Aid_BDA__c");
                    if (str != null && str.trim().length() > 0) {
                        pi.appDate = sdf.parse(str);
                        //log.info("BDA: " + str);
                    }

    				pi.reasonIncomplete = (String)s.getField("Reason_Incomplete__c");
    				//log.info("Reason Incomplete: " + pi.reasonIncomplete);
    				pi.denialReason = (String)s.getField("Denial_Reason__c");
    				//log.info("Denial Reason: " + pi.denialReason);
    				pi.notes = (String)s.getField("Notes__c");
    				//log.info("Notes: " + pi.notes);
    				pi.incomeSource = (String)s.getField("Current_Source_of_Income__c");
    				//log.info("Income Source: " + pi.incomeSource);
    				pi.monthlyIncome = (String)s.getField("Monthly_Gross_Income__c");
    				//log.info("Monthly Income: " + pi.monthlyIncome);

    				str = (String)s.getField("Number_People_in_Household__c");
                    if (str != null && str.trim().length() > 0) {
                        pi.householdSize = (int)Float.parseFloat(str);
                        //log.info("Household Size: " + str);
                    }

    				str = (String)s.getField("LastModifiedDate");
                    if (str != null && str.trim().length() > 0) {
                        pi.lastModified = sdf.parse(str);
                        //log.info("Last Modified Date: " + str);
                    }

    				pi.docSubmitted = (String)s.getField("Documentation_Submitted__c");
    				//log.info("Document Submitted: " + pi.docSubmitted);

    				//pi.language = (String)s.getField("Language__c");
    				//log.info("Language: " + pi.language);
    				//pi.careGiverName = (String)s.getField("Caller_Caregiver_Name__c");
    				//log.info("Care Giver Name: " + pi.careGiverName);
    				//pi.careGiverPhone = (String)s.getField("Caller_Caregiver_Phone__c");
    				//log.info("Care Giver Phone: " + pi.careGiverPhone);
    				pi.insuranceProvider = (String)s.getField("Health_Insurance_Provider__c");
    				log.info("Insurance Provider: " + pi.insuranceProvider);
    				pi.primaryDiagnosis = (String)s.getField("Primary_Diagnosis__c");
    				log.info("Diagnosis: " + pi.primaryDiagnosis);
    				pi.primaryCareProvider = (String)s.getField("Do_you_have_a_primary_care_doctor__c");
    				log.info("Care Provider: " + pi.primaryCareProvider);
    				pi.timesHospital = (String)s.getField("Recent_Hospitalizations_in_past_6_mo__c");
    				log.info("# times hospitalizations: " + pi.timesHospital);
    				pi.timesFallen = (String)s.getField("Have_you_had_recent_falls_in_past_6_mo__c");
    				log.info("# times fallen: " + pi.timesFallen);
    				pi.readmitted = (String)s.getField("Outcome_re_visits_30_Days__c");
    				log.info("Re-admitted: " + pi.readmitted);

                    // @debug.
                    /*
    				log.info("Name: " + s.getField("Name"));
    				log.info("App Name: " + s.getField("Applicant_Name__c"));
                    */
    				log.info("-----");

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

    public static List<SfProgramInfo> queryRiskRatingScales(PartnerConnection conn,
                                                            String contactRecordTypeId,
                                                            String contactId) {
    	log.info("Querying client risk rating scales...");
        List<SfProgramInfo> results = new ArrayList<SfProgramInfo>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try {
    		StringBuilder sb = new StringBuilder();
    		sb.append("SELECT Id, Date_of_Assessment__c, LastModifiedDate, ");
            sb.append("Adult_Daily_Living__c, Ambulance__c, Medication_Management__c, ");
            sb.append("Housing__c, Income_Employment__c, Nutrition__c, Primary_Care__c, ");
            sb.append("Social_Support__c, Transportation__c ");
    		sb.append("FROM Risk_Rating_Scale__c ");
    		sb.append("WHERE Client__r.RecordTypeId = '" + contactRecordTypeId + "' ");
    		sb.append("  AND Client__r.Id = '" + contactId + "' ");
    		//sb.append("  AND LastModifiedDate >= YESTERDAY ");

    		QueryResult queryResults = conn.query(sb.toString());
    		if (queryResults.getSize() > 0) {
    			log.info("Found " + queryResults.getSize() + " programs");
    			for (SObject s: queryResults.getRecords()) {
                    SfProgramInfo pi = new SfProgramInfo();
                    pi.id = s.getId();
    				log.info("Id: " + pi.id);
                    pi.appType = "risk rating";

    				String str = (String)s.getField("Date_of_Assessment__c");
                    if (str != null && str.trim().length() > 0) {
                        pi.appDate = sdf.parse(str);
                        log.info("Date of Assessment: " + str);
                    }

    				str = (String)s.getField("LastModifiedDate");
                    if (str != null && str.trim().length() > 0) {
                        pi.lastModified = sdf.parse(str);
                        log.info("Last Modified Date: " + str);
                    }

    				pi.dailyLiving = (String)s.getField("Adult_Daily_Living__c");
    				log.info("Adult Daily Living: " + pi.dailyLiving);
    				pi.ambulance = (String)s.getField("Ambulance__c");
    				log.info("Ambulance: " + pi.ambulance);
    				pi.medication = (String)s.getField("Medication_Management__c");
    				log.info("Medication Management: " + pi.medication);
    				pi.housing = (String)s.getField("Housing__c");
    				log.info("Housing: " + pi.housing);
    				pi.incomeEmployment = (String)s.getField("Income_Employment__c");
    				log.info("Income Employment: " + pi.incomeEmployment);
    				pi.nutrition = (String)s.getField("Nutrition__c");
    				log.info("Nutrition: " + pi.nutrition);
    				pi.primaryCare = (String)s.getField("Primary_Care__c");
    				log.info("Primary Care: " + pi.primaryCare);
    				pi.socialSupport = (String)s.getField("Social_Support__c");
    				log.info("Social Support: " + pi.socialSupport);
    				pi.transportation = (String)s.getField("Transportation__c");
    				log.info("Transportation: " + pi.transportation);
    				log.info("-----");

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
