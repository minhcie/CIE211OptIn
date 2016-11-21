package src.main.java;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfContentByte;;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;

import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import com.sun.jersey.api.client.ClientResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.log4j.Logger;

public class CIE211OptIn {
    private static final Logger log = Logger.getLogger(CIE211OptIn.class.getName());

    private static final String USERNAME = "mtran@211sandiego.org";
    private static final String PASSWORD = "m1nh@211KsmlvVA4mvtI6YwzKZOLjbKF9";
    private static final String CIE_CONSENT_TEMPLATE = "CIE_Consent_Template.pdf";
    private static PartnerConnection connection = null;

    public static void main(String[] args) {
        Connection sqlConn = null;
        try {
            // Establish DB connection.
            sqlConn = DbUtils.getDBConnection();
            if (sqlConn == null) {
                System.exit(-1);
            }

            // Establish connection to Salesforce.
        	ConnectorConfig config = new ConnectorConfig();
        	config.setUsername(USERNAME);
        	config.setPassword(PASSWORD);
        	//config.setTraceMessage(true);

            connection = Connector.newConnection(config);
            // @debug.
    		log.info("Auth EndPoint: " + config.getAuthEndpoint() + "\n");
    		log.info("Service EndPoint: " + config.getServiceEndpoint() + "\n");
    		log.info("Username: " + config.getUsername() + "\n");
    		log.info("SessionId: " + config.getSessionId() + "\n");

            // Query contact reord type ID.
            String contactRecordTypeId = SfUtils.queryRecordType(connection, "Contact", "Client");

            // Query CIE opt-in clients.
            List<DbClient> clients = SfUtils.queryCIEClients(connection, contactRecordTypeId);
            if (clients == null || clients.size() <= 0) {
                log.info("No updates (clients) found!");
                return;
            }

            // Authenticate/Sign-on to 211 San Diego ETO site.
            EtoAuthentication auth = EtoServiceUtil.authenticate();
            if (auth == null) {
                return;
            }

            // Insert/update client's authorization in ETO.
            for (int i = 0; i < clients.size(); i++) {
                DbClient c = clients.get(i);
                addUpdateClient(sqlConn, auth, c, contactRecordTypeId);
            }
        }
    	catch (ConnectionException ce) {
            log.error(ce.getMessage());
            ce.printStackTrace();
            System.exit(-1);
    	}
        catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        finally {
            DbUtils.closeConnection(sqlConn);
        }
    }

    private static String createConsentDocument(DbClient client) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat sdf2 = new SimpleDateFormat("MMddyyyy");

        // Remove any blanks or quotes in the name.
        String first = client.firstName.replaceAll("\"", "").replaceAll(" ", "_");
        String last = client.lastName.replaceAll("\"", "").replaceAll(" ", "_");
        String output = first + "_" + last + ".pdf";
    	log.info("Creating CIE consent document (" + output + ")...");

        try {
            // Create pdf document writer.
            String name = client.firstName + " " + client.lastName;
            String dob = "";
            if (client.dob != null) {
                dob = sdf.format(client.dob);
            }
            String gender = "";
            switch ((int)client.genderId) {
                case 1:
                    gender = "F";
                    break;
                case 2:
                    gender = "M";
                    break;
                default:
                    break;
            }
            String address = client.address1 + " " + client.address2 + ", " +
                             client.city + ", " + client.state + " " + client.postalCode;
            String consentFileName = client.firstName + "_" + client.lastName + "_" +
                                     sdf2.format(client.consentDate) + ".wav";
            String consentDate = sdf.format(client.consentDate);

            Document doc = new Document();
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(output));
            doc.open();
            PdfContentByte cb = writer.getDirectContent();
            ByteArrayOutputStream stampedBuffer = null;

            // Read pdf template.
            log.info("Reading PDF template (" + CIE_CONSENT_TEMPLATE + ")...");
            int pages = 2;
            for (int i = 1; i <= pages; i++) {
                PdfReader templateReader = new PdfReader(new FileInputStream(CIE_CONSENT_TEMPLATE));

                // Start the process of adding extra content to existing PDF.
                stampedBuffer = new ByteArrayOutputStream();
                PdfStamper stamper = new PdfStamper(templateReader, stampedBuffer);
                stamper.setFormFlattening(true);

                // Getting template fields.
                AcroFields form = stamper.getAcroFields();

                // Fill template with data.
                if (i == 1) {
                    form.setField("ClientName1", name);
                    form.setField("DoB", dob);
                    form.setField("Gender", gender);
                    form.setField("Address", address);
                }
                else {
                    form.setField("ConsentFileName", consentFileName);
                    form.setField("ClientName2", name);
                    form.setField("ConsentDate", consentDate);

                    // Page break.
                    doc.newPage();
                }

                // Close template reader, clean up.
                stamper.close();
                templateReader.close();
                form = null;

                // Import page content.
                PdfReader stampedReader = new PdfReader(stampedBuffer.toByteArray());
                PdfImportedPage page = writer.getImportedPage(stampedReader, i);
                cb.addTemplate(page, 0, 0);
            }

            // Close document writer.
            doc.close();
            writer.close();

            // Create output file.
            new File(output);
        }
        catch (IOException ioe) {
            log.error(ioe.getMessage());
            ioe.printStackTrace();
        }
        catch (DocumentException de) {
            log.error(de.getMessage());
            de.printStackTrace();
        }
        catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }

        return output;
    }

    private static void addUpdateClient(Connection sqlConn, EtoAuthentication auth,
                                        DbClient data, String contactRecordTypeId) throws Exception {
        DbClient client = DbClient.findByCaseNumber(sqlConn, data.caseNumber);
        if (client == null) {
            log.info("Adding new client (" + data.firstName + " " + data.lastName + ")...");
        }
        else {
            log.info("Updating existing client (ID: " + client.etoParticipantSiteId + ")...");
        }

        // Standard demographics.
        JSONObject input = new JSONObject();
        if (client != null) {
            input.put("ID", new Long(client.etoParticipantSiteId));
        }
        input.put("FirstName", data.firstName);
        input.put("LastName", data.lastName);
        if (data.genderId == 1) {
            input.put("Gender", new Integer(1)); // Female.
        }
        else if (data.genderId == 2) {
            input.put("Gender", new Integer(0)); // Male.
        }
        else {
            input.put("Gender", new Integer(8)); // Data not collected.
        }

        if (data.dob != null) {
            long dob = data.dob.getTime();
            input.put("DateOfBirth", "/Date(" + dob + ")/");
        }
        input.put("SocialSecurityNumber", data.ssn);
        input.put("Address1", data.address1);
        input.put("Address2", data.address2);
        input.put("City", data.city);
        input.put("State", data.state);
        input.put("ZipCode", data.postalCode);
        input.put("CaseNumber", data.caseNumber);
        input.put("HomePhone", data.homePhone);
        input.put("CellPhone", data.cellPhone);
        input.put("EMail", data.email);

        // Custom demographics.
        JSONArray custList = new JSONArray();
        JSONObject cust = new JSONObject();
        cust.put("CDID", new Integer(3758)); // Gender (HUD).
        cust.put("CharacteristicType", new Integer(4));
        if (data.genderId == 1) {
            cust.put("value", new Integer(8952));
        }
        else if (data.genderId == 2) {
            cust.put("value", new Integer(8953));
        }
        else {
            cust.put("value", new Integer(8959)); // Data not collected.
        }
        custList.add(cust);

        // Name data quality (HUD).
        cust = new JSONObject();
        cust.put("CDID", new Integer(3762));
        cust.put("CharacteristicType", new Integer(4));
        cust.put("value", new Integer(8970));
        custList.add(cust);

        // SSN data quality (HUD).
        cust = new JSONObject();
        cust.put("CDID", new Integer(3764));
        cust.put("CharacteristicType", new Integer(4));
        if (data.ssn != null && data.ssn.length() > 0) {
            cust.put("value", new Integer(8983));
        }
        else {
            cust.put("value", new Integer(8987));
        }
        custList.add(cust);

        // DOB data quality (HUD).
        cust = new JSONObject();
        cust.put("CDID", new Integer(3760));
        cust.put("CharacteristicType", new Integer(4));
        cust.put("value", new Integer(8965));
        custList.add(cust);

        // Ethnicity (HUD).
        long ethnicityId = 0; // Should match id from data_quality_choice table.
        cust = new JSONObject();
        cust.put("CDID", new Integer(3759));
        cust.put("CharacteristicType", new Integer(4));
        String race = "";
        if (data.race != null) {
            race = data.race.toLowerCase();
        }
        if (race.contains("caucasian") || race.contains("white") ||
            race.contains("non-hispanic") || race.contains("non-latino") ||
            race.contains("african american") || race.contains("black") ||
            race.contains("asian")) {
            cust.put("value", new Integer(8960));
            ethnicityId = 16;
        }
        else if (race.contains("hispanic") || race.contains("latino")) {
            cust.put("value", new Integer(8961));
            ethnicityId = 17;
        }
        else {
            cust.put("value", new Integer(8964));
            ethnicityId = 20; // Not collected.
        }
        custList.add(cust);

        // Race (HUD).
        long raceId = 0; // Should match id from race table.
        cust = new JSONObject();
        cust.put("CDID", new Integer(3763));
        cust.put("CharacteristicType", new Integer(5));
        if (race.contains("caucasian") || race.contains("white")) {
            cust.put("value", new Integer(8979));
            raceId = 11;
        }
        else if (race.contains("hispanic") || race.contains("latino") ||
                 race.contains("other race")) {
            cust.put("value", new Integer(8982));
            raceId = 12;
        }
        else if (race.contains("african american") || race.contains("black")) {
            cust.put("value", new Integer(8977));
            raceId = 13;
        }
        else if (race.contains("asian")) {
            cust.put("value", new Integer(8976));
            raceId = 14;
        }
        else {
            cust.put("value", new Integer(8982));
            raceId = 17; // Unknown.
        }
        custList.add(cust);

        // Include in standard demographics.
        input.put("CustomDemoData", custList);

        // Wrap request JSON string.
        String jsonStr = input.toString("participant", input);
        String inputStr = "{" + jsonStr + "}";

        // @debug.
        //log.info(inputStr);

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/Actor.svc/participant/",
                                                             auth, inputStr);
        if (response.getStatus() != 200) {
            log.error(response.toString());
            return;
        }

        // Parse response.
        String resp = response.getEntity(String.class);
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(resp);
        JSONObject jsonObj = (JSONObject)obj;

        JSONObject result = (JSONObject)jsonObj.get("SaveParticipantResult");
        String guid = (String)result.get("GUID");
        log.info("Enterprise id: " + guid);
        Long participantId = (Long)result.get("ID");
        log.info("Participant id: " + participantId);
        Long subjectId = (Long)result.get("SubjectID");
        log.info("Subject id: " + subjectId);

        // Add client's authorization/consent for new client.
        if (client == null) {
            // Create CIE client consent document.
            String consentFileName = createConsentDocument(data);

            // Attach consent document to the new client.
            TouchPointUtils.addConsentFile(auth, subjectId, data, consentFileName);

            // Save consent document.
            log.info("Archiving CIE consent document (" + consentFileName + ")...");
            String workingDir = System.getProperty("user.dir");
            String archiveDir = workingDir + "\\archive\\";
            File f = new File(consentFileName);
            f.renameTo(new File(archiveDir + f.getName()));

            client = new DbClient();
        }

        // Copy client info.
        client.caseNumber = data.caseNumber;
        client.genderId = data.genderId;
        client.firstName = data.firstName;
        client.lastName = data.lastName;
        client.dob = data.dob;
        client.address1 = data.address1;
        client.address2 = data.address2;
        client.city = data.city;
        client.state = data.state;
        client.postalCode = data.postalCode;
        client.homePhone = data.homePhone;
        client.cellPhone = data.cellPhone;
        client.email = data.email;
        client.ethnicityId = ethnicityId;
        client.etoEnterpriseId = UUID.fromString(guid);
        client.etoParticipantSiteId = participantId;
        client.etoSubjectId = subjectId;

        if (client.id <= 0) {
            log.info("DbClient.insert()");
            client.insert(sqlConn);

            // Enroll new client to the default intake program.
            TouchPointUtils.addIntakeProgram(sqlConn, auth, client);
        }
        else {
            log.info("DbClient.update()");
            client.update(sqlConn);
        }

        // Add client race.
        DbClientRace cr = DbClientRace.findByClientRace(sqlConn, client.id, raceId);
        if (cr == null) {
            log.info("DbClientRace.insert()");
            cr = new DbClientRace();
            cr.clientId = client.id;
            cr.raceId = raceId;
            cr.insert(sqlConn);
        }

        // Query client programs info.
        List<SfProgramInfo> prgms = SfUtils.queryClientPrograms(connection,
                                                                contactRecordTypeId,
                                                                data.caseNumber);
        if (prgms != null && prgms.size()  > 0) {
            for (int i = 0; i < prgms.size(); i++) {
                SfProgramInfo pi = prgms.get(i);

                // Copy primary language from contact info.
                pi.language = data.language;

                // Make sure we have a valid application.
                if (pi.appType == null || pi.appDate == null) {
                    log.error("Trying to add/update program without application type or application date");
                    continue;
                }

                // Add generic case manager.
                TouchPointUtils.addCaseManager(sqlConn, auth, client.id, subjectId, pi);

                // Add/update program info.
                String prgmName = pi.appType.toLowerCase();
                switch (prgmName) {
                    case "calfresh":
                    case "medi-cal":
                    case "combo":
                        // Insert update program info.
                        upsertEnrollmentPrograms(sqlConn, auth, client.id,
                                                 participantId, pi);

                        // Add client supplemental demographics.
                        TouchPointUtils.addSupplemental(sqlConn, auth, client.id,
                                                        subjectId, pi);
                        break;
                    case "health general":
                    case "perinatal":
                    case "sharp referrals":
                    case "project care":
                        // Add/update program with start date = application date,
                        // end date = application last modified date + application
                        // status = closed.
                        TouchPointUtils.upsertHealthNavProgram(sqlConn, auth, client.id,
                                                               participantId, pi);

                        // Add client supplemental demographics.
                        TouchPointUtils.addSupplemental(sqlConn, auth, client.id,
                                                        subjectId, pi);

                        // Add general health.
                        if (pi.primaryDiagnosis != null || pi.primaryCareProvider != null ||
                            pi.timesHospital != null || pi.timesFallen != null ||
                            pi.readmitted != null) {
                            TouchPointUtils.addGeneralHealth(sqlConn, auth, client.id,
                                                             subjectId, pi);
                        }

                        // Add insurance provider.
                        if (pi.insuranceProvider != null) {
                            TouchPointUtils.addInsuranceProvider(sqlConn, auth, client.id,
                                                                 subjectId, pi);
                        }

                        // Add ADL/IADL assessments.
                        if (pi.mobility != null || pi.safety != null || pi.housework != null ||
                            pi.mealPrep != null || pi.moneyMgmt != null || pi.homeRepair != null ||
                            pi.healthcareAccess != null) {
                            TouchPointUtils.addADLAssessment(sqlConn, auth, client.id,
                                                             subjectId, pi);
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        // Query client risk rating scales info.
        List<SfProgramInfo> rrScales = SfUtils.queryRiskRatingScales(connection,
                                                                     contactRecordTypeId,
                                                                     data.caseNumber);
        if (rrScales != null && rrScales.size()  > 0) {
            for (int i = 0; i < rrScales.size(); i++) {
                SfProgramInfo rrs = rrScales.get(i);

                // Add/update risk rating scale info.
                TouchPointUtils.addRiskRatingScale(sqlConn, auth, client.id,
                                                   subjectId, rrs);
            }
        }
    }

    private static void upsertEnrollmentPrograms(Connection sqlConn, EtoAuthentication auth,
                                                 long clientId, long participantId,
                                                 SfProgramInfo pi) throws Exception {
        DbEnrollment enroll = null;
        String appStatus = pi.appStatus.toLowerCase();
        switch (appStatus) {
            case "pending":
                // Add new pending program with start date = application date.
                TouchPointUtils.addProgram(sqlConn, auth, clientId,
                                           participantId, pi, "pending");
                break;
            case "denied":
                // Add/update pending program with start date = application
                // date, end date = application last modified date.
                TouchPointUtils.updateProgram(sqlConn, auth, clientId,
                                              participantId, pi, "pending");

                // Add denied program with start date = application date,
                // end date = application last modified date.
                enroll = TouchPointUtils.addProgram(sqlConn, auth, clientId,
                                                    participantId, pi, "denied");
                if (enroll != null) {
                    // Update denied program with end date = application
                    // last modified date, dismissal reason.
                    TouchPointUtils.updateProgram(sqlConn, auth, clientId,
                                                  participantId, pi, "denied");
                }
                break;
            case "approved":
                // Add/update pending program with start date = application
                // date, end date = application last modified date.
                TouchPointUtils.updateProgram(sqlConn, auth, clientId,
                                              participantId, pi, "pending");

                // Add approved program with start date = beginning date
                // of aid (BDA), end date = null.
                TouchPointUtils.addProgram(sqlConn, auth, clientId,
                                           participantId, pi, "approved");
                break;
            default:
                break;
        }
    }
}
