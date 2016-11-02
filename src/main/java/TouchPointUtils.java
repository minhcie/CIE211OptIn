package src.main.java;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.sun.jersey.api.client.ClientResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.apache.log4j.Logger;

public class TouchPointUtils {
    private static final Logger log = Logger.getLogger(TouchPointUtils.class.getName());

    public enum SD211Programs {
        intake(791),
        calFreshEnrollment(793),
        calFreshDenial(794),
        calFreshApproval(795),
        comboEnrollment(796),
        comboDenial(797),
        comboApproval(798),
        mediCalEnrollment(799),
        mediCalDenial(800),
        mediCalApproval(801),
        healthGeneral(802),
        perinatal(803),
        projectCare(804),
        sharpReferrals(805);

        int value;
        SD211Programs(int v) {
            this.value = v;
        }
        public int getValue() {
            return this.value;
        }
    }

    private static int getProgramId(String appType, String appStatus) {
        int programId = 0;
        if (appType == null || appType.length() <= 0) {
            log.error("Trying to get 211 SD program ID without application type");
            return programId;
        }

        if (appStatus == null || appStatus.length() <= 0) {
            log.error("Trying to get 211 SD program ID without application status");
            return programId;
        }

        String aType = appType.toLowerCase();
        String status = appStatus.toLowerCase();
        switch (aType) {
            case "calfresh":
                switch (status) {
                    case "pending":
                        programId = SD211Programs.calFreshEnrollment.getValue();
                        break;
                    case "denied":
                        programId = SD211Programs.calFreshDenial.getValue();
                        break;
                    case "approved":
                        programId = SD211Programs.calFreshApproval.getValue();
                        break;
                    default:
                        log.error("Invalid application status: " + appStatus);
                        break;
                }
                break;
            case "combo":
                switch (status) {
                    case "pending":
                        programId = SD211Programs.comboEnrollment.getValue();
                        break;
                    case "denied":
                        programId = SD211Programs.comboDenial.getValue();
                        break;
                    case "approved":
                        programId = SD211Programs.comboApproval.getValue();
                        break;
                    default:
                        log.error("Invalid application status: " + appStatus);
                        break;
                }
                break;
            case "medi-cal":
                switch (status) {
                    case "pending":
                        programId = SD211Programs.mediCalEnrollment.getValue();
                        break;
                    case "denied":
                        programId = SD211Programs.mediCalDenial.getValue();
                        break;
                    case "approved":
                        programId = SD211Programs.mediCalApproval.getValue();
                        break;
                    default:
                        log.error("Invalid application status: " + appStatus);
                        break;
                }
                break;
            case "health general":
                programId = SD211Programs.healthGeneral.getValue();
                break;
            case "perinatal":
                programId = SD211Programs.perinatal.getValue();
                break;
            case "project care":
                programId = SD211Programs.projectCare.getValue();
                break;
            case "sharp referrals":
                programId = SD211Programs.sharpReferrals.getValue();
                break;
            default:
                log.error("Invalid application type: " + appType);
                break;
        }
        return programId;
    }

    public static void addConsentFile(EtoAuthentication auth, Long subjectId,
                                      DbClient data, String consentFileName) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Calendar cal = Calendar.getInstance();
        cal.setTime(data.consentDate);
        Date dt = cal.getTime();
        int intakePrgmId = SD211Programs.intake.getValue();

        JSONObject input = new JSONObject();
        input.put("TouchPointID", new Integer(14)); // CIE Client Authorization TP.
        input.put("SubjectID", subjectId);
        input.put("ResponseCreatedDate", "/Date(" + dt.getTime() + ")/");
        input.put("ProgramID", new Integer(intakePrgmId));

        JSONArray respElements = new JSONArray();
        JSONObject ele = new JSONObject();
        ele.put("ElementID", new Integer(137)); // Date authorization recorded.
        ele.put("ElementType", new Integer(9));
        ele.put("Value", sdf.format(dt));
        respElements.add(ele);

        ele = new JSONObject();
        ele.put("ElementID", new Integer(165)); // Date authorization expired.
        ele.put("ElementType", new Integer(9));
        cal.add(Calendar.YEAR, 3);
        dt = cal.getTime();
        ele.put("Value", sdf.format(dt));
        respElements.add(ele);

        // File attachments.
        Path path = Paths.get(consentFileName);
        byte[] fileData = Files.readAllBytes(path);
        JSONObject f = new JSONObject();
        f.put("Caption", null);
        JSONArray fileArr = new JSONArray();
        for (int k = 0; k < fileData.length; k++) {
            int n = fileData[k] & 0xff; // Since bytes are signed in Java.
            fileArr.add(n);
        }
        f.put("FileContent", fileArr);
        f.put("FileName", data.firstName + "_" + data.lastName + "_Authorization.pdf");

        ele = new JSONObject();
        ele.put("ElementID", new Integer(138)); // Authorization attachment.
        ele.put("ElementType", new Integer(29));
        ele.put("ResponseFileAttachment", f);
        ele.put("Value", null);
        respElements.add(ele);

        // Add response elements.
        input.put("ResponseElements", respElements);

        // Wrap request JSON string.
        String jsonStr = input.toString("TouchPointResponse", input);
        String inputStr = "{" + jsonStr + "}";

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/TouchPoint.svc/TouchPointResponseAdd/",
                                                             auth, inputStr);
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            String resp = response.getEntity(String.class);
            log.info("Client Authorization - Response from server:");
            log.info(resp);
        }
    }

    public static void addIntakeProgram(Connection sqlConn, EtoAuthentication auth,
                                        DbClient data) throws Exception {
        JSONObject input = new JSONObject();

        int intakePrgmId = SD211Programs.intake.getValue();
        log.info("Adding new intake program ID: " + intakePrgmId);
        input.put("participantID", data.etoParticipantSiteId);
        input.put("programID", new Integer(intakePrgmId));
        long prgmStartDate = 0;
        Date now = new Date();
        if (data.consentDate != null) {
            prgmStartDate = data.consentDate.getTime();
        }
        else {
            prgmStartDate = now.getTime();
        }
        input.put("startDate", "/Date(" + prgmStartDate + ")/");
        input.put("projectedEndDate", null);

        // @debug.
        log.info(input.toString() + "\n");

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/Actor.svc/participant/enrollment/",
                                                             auth, input.toString());
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            Long enrollmentId = EtoServiceUtil.parseResponse(response, "SaveParticipantEnrollmentResult", "ID");
            log.info("Client enrollment ID: " + enrollmentId);

            // Find matching CIE program using ETO's program id.
            DbProgram prgm = DbProgram.findByEtoProgramId(sqlConn, intakePrgmId);
            if (prgm == null) {
                log.error("CIE Program not found while trying to add new intake program");
                return;
            }

            // Save enrollment info.
            log.info("DbEnrollment.insert()");
            DbEnrollment enroll = new DbEnrollment();
            enroll.clientId = data.id;
            enroll.programId = prgm.id;
            if (data.consentDate != null) {
                enroll.startDate = data.consentDate;
            }
            else {
                enroll.startDate = now;
            }
            enroll.endDate = null;
            enroll.dismissalReasonId = 1; // Completed.
            enroll.insert(sqlConn);
        }
    }

    public static DbEnrollment addProgram(Connection sqlConn, EtoAuthentication auth,
                                          long clientId, long participantId,
                                          SfProgramInfo data, String appStatus) throws Exception {
        // Find ETO program id from application status.
        int programId = getProgramId(data.appType, appStatus);
        if (programId == 0) {
            log.error("ETO program id not found while trying to add new enrollment");
            return null;
        }

        // Find matching CIE program using ETO's program id.
        DbProgram prgm = DbProgram.findByEtoProgramId(sqlConn, programId);
        if (prgm == null) {
            log.error("CIE program not found while trying to add new enrollment");
            return null;
        }

        // Check to see if enrollment for this program already existed.
        DbEnrollment enroll = DbEnrollment.findByProgram(sqlConn, clientId, prgm.id, data.appDate);
        if (enroll != null) {
            // Check to see if client has been dismissed from existing enrollment.
            if (enroll.endDate != null) {
                log.info("Client has been dismissed from ETO program id: " + programId);
            }
            else {
                log.info("Client has been enrolled in ETO program id:" + programId);
            }
            return null;
        }

        // Add new program enrollment.
        log.info("Adding new program (Client ID: " + clientId + ", ETO program ID: " + programId);
        JSONObject input = new JSONObject();
        input.put("participantID", participantId);
        input.put("programID", new Integer(programId));
        long prgmStartDate = data.appDate.getTime();
        input.put("startDate", "/Date(" + prgmStartDate + ")/");

        // @debug.
        log.info(input.toString() + "\n");

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/Actor.svc/participant/enrollment/",
                                                             auth, input.toString());
        if (response.getStatus() != 200) {
            log.error(response.toString());
            return null;
        }

        // Parse response.
        Long enrollmentId = EtoServiceUtil.parseResponse(response, "SaveParticipantEnrollmentResult", "ID");
        log.info("Client enrollment ID: " + enrollmentId);

        // Save enrollment info.
        log.info("DbEnrollment.insert()");
        enroll = new DbEnrollment();
        enroll.clientId = clientId;
        enroll.programId = prgm.id;
        enroll.startDate = data.appDate;
        enroll.endDate = null;
        enroll.dismissalReasonId = 1; // Completed.
        enroll.insert(sqlConn);

        return enroll;
    }

    public static void updateProgram(Connection sqlConn, EtoAuthentication auth,
                                     long clientId, long participantId,
                                     SfProgramInfo data, String appStatus) throws Exception {
        JSONObject input = new JSONObject();

        // Find ETO program id from application status.
        int programId = getProgramId(data.appType, appStatus);
        if (programId == 0) {
            log.error("ETO program id not found while trying to update");
            return;
        }

        // Find matching CIE program using ETO's program id.
        DbProgram prgm = DbProgram.findByEtoProgramId(sqlConn, programId);
        if (prgm == null) {
            log.error("CIE Program not found while trying to update");
            return;
        }

        // Check to see if enrollment for this program already existed.  If not,
        // add new program.
        DbEnrollment enroll = DbEnrollment.findByProgram(sqlConn, clientId, prgm.id, data.appDate);
        if (enroll == null) { // New program enrollment.
            log.error("Program enrollment not found while trying to update");

            // Add new program before updating.
            enroll = addProgram(sqlConn, auth, clientId, participantId, data, appStatus);
            if (enroll == null) {
                return;
            }
        }

        // Check to see if client has been dismissed from existing enrollment.
        if (enroll.endDate != null) {
            log.info("Client has been dismissed from ETO program id: " + programId);
            return;
        }

        // Update program enrollment.
        log.info("Updating existing enrollment id: " + enroll.id);
        input.put("participantID", participantId);
        input.put("programID", new Integer(programId));
        long prgmEndDate = data.lastModified.getTime();
        input.put("endDate", "/Date(" + prgmEndDate + ")/");
        input.put("graduated", new Boolean(true));

        // Dismissal reason.
        String reason = "";
        if (appStatus.equalsIgnoreCase("pending") &&
            data.docSubmitted != null && data.docSubmitted.length() > 0) {
            String str = data.docSubmitted.toLowerCase();
            if (str.contains("complete")) {
                reason = "Submitted with complete documentation";
                input.put("reasonID", new Integer(426));
            }
            else if (str.contains("partial")) {
                reason = "Submitted with partial documentation";
                input.put("reasonID", new Integer(444));
            }
            else if (str.contains("without")) {
                reason = "Submitted without documentation";
                input.put("reasonID", new Integer(462));
            }
        }
        else if (appStatus.equalsIgnoreCase("denied") &&
                 data.denialReason != null && data.denialReason.length() > 0) {
            String str = data.denialReason.toLowerCase();
            if (str.contains("failure to provide")) {
                reason = "Failure to provide documents (FTP)";
                input.put("reasonID", new Integer(480));
            }
            else if (str.contains("missed interview")) {
                reason = "Missed county interview";
                input.put("reasonID", new Integer(498));
            }
            else if (str.contains("over income")) {
                reason = "Over income";
                input.put("reasonID", new Integer(516));
            }
            else if (str.contains("had calfresh")) {
                reason = "Already had CalFresh";
                input.put("reasonID", new Integer(534));
            }
            else if (str.contains("drug felony")) {
                reason = "Drug felony";
                input.put("reasonID", new Integer(552));
            }
            else if (str.contains("fleeing")) {
                reason = "Fleeing felon";
                input.put("reasonID", new Integer(570));
            }
            else if (str.contains("withdrawn")) {
                reason = "Withdrawn/Cancelled application";
                input.put("reasonID", new Integer(588));
            }
            else if (str.contains("ssi")) {
                reason = "Receiving SSI";
                input.put("reasonID", new Integer(606));
            }
            else if (str.contains("ineligible")) {
                reason = "Receiving another prgm that makes them ineligible";
                input.put("reasonID", new Integer(624));
            }
            else if (str.contains("multiple")) {
                reason = "Multiple households";
                input.put("reasonID", new Integer(642));
            }
            else if (str.contains("program violation")) {
                reason = "Intentional program violation (IPV)";
                input.put("reasonID", new Integer(660));
            }
            else if (str.contains("student")) {
                reason = "Student";
                input.put("reasonID", new Integer(678));
            }
            else if (str.contains("not a resident")) {
                reason = "Not a resident of county";
                input.put("reasonID", new Integer(696));
            }
            else if (str.contains("in progress")) {
                reason = "Application already in progress";
                input.put("reasonID", new Integer(714));
            }
            else if (str.contains("quit job")) {
                reason = "Voluntarily quit job";
                input.put("reasonID", new Integer(732));
            }
        }

        // @debug.
        log.info(input.toString() + "\n");

        // Post request.
        ClientResponse response = EtoServiceUtil.deleteRequest("https://services.etosoftware.com/API/Actor.svc/participant/enrollment/",
                                                               auth, input.toString());
        if (response.getStatus() != 200) {
            log.error(response.toString());
            return;
        }

        // Update enrollment info.
        log.info("DbEnrollment.update()");
        enroll.endDate = data.lastModified;
        enroll.dismissalReasonOther = reason;
        enroll.dismissalReasonId = 1; // Completed.
        enroll.update(sqlConn);
    }

    public static void addSupplemental(Connection sqlConn, EtoAuthentication auth,
                                       long clientId, Long subjectId,
                                       SfProgramInfo data) throws Exception {
        log.info("Adding supplemental demographics, client id: " + clientId);

        // Find ETO program id from application status.
        int programId = getProgramId(data.appType, data.appStatus);
        if (programId == 0) {
            log.error("ETO program id not found while trying to add supplemental demographics");
            return;
        }

        // Update session's current program before populating TouchPoint.
        EtoServiceUtil.setCurrentProgram(programId, auth);

        // ETO Supplemental Demographics TouchPoint.
        JSONObject input = new JSONObject();
        input.put("TouchPointID", new Integer(49));
        input.put("SubjectID", subjectId);
        Date now = new Date();
        input.put("ResponseCreatedDate", "/Date(" + now.getTime() + ")/");
        input.put("ProgramID", new Integer(programId));

        // TouchPoint response elements.
        JSONArray respElements = new JSONArray();

        // Number in hoursehold / living status.
        JSONObject ele = new JSONObject();
        ele.put("ElementID", new Integer(690));
        ele.put("ElementType", new Integer(6));
        if (data.householdSize <= 0) {
            ele.put("Value", null);
        }
        else {
            ele.put("Value", new Integer(data.householdSize));
        }
        respElements.add(ele);

        // Housing status.
        ele = new JSONObject();
        ele.put("ElementID", new Integer(707));
        ele.put("ElementType", new Integer(5));
        if (data.householdSize == 1) {
            ele.put("Value", "Lives Alone");
        }
        else if (data.householdSize > 1) {
            ele.put("Value", "Lives With Others");
        }
        respElements.add(ele);

        // Monthly income.
        respElements.add(createTextRespElement(698, data.monthlyIncome));

        // Income source.
        respElements.add(createTextRespElement(699, data.incomeSource));

        // Primary language.
        respElements.add(createTextRespElement(688, data.language));

        // Add response elements.
        input.put("ResponseElements", respElements);

        // Wrap request JSON string.
        String jsonStr = input.toString("TouchPointResponse", input);
        String inputStr = "{" + jsonStr + "}";

        // @debug.
        log.info(inputStr);

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/TouchPoint.svc/TouchPointResponseAdd/",
                                                             auth, inputStr);
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            Long supDemoRespId = EtoServiceUtil.parseResponse(response, "AddTouchPointResponseResult",
                                                              "TouchPointResponseID");
            log.info("Supplemental demographics response ID: " + supDemoRespId + "\n");
        }

        // Add/update CIE client supplemental demographics.
        log.info("Add/Update CIE client supplemental demographics");
        DbSupplemental sup = DbSupplemental.findByClient(sqlConn, clientId);
        if (sup != null) {
            sup.householdSize = data.householdSize;
            sup.monthlyIncome = data.monthlyIncome;
            sup.incomeSource = data.incomeSource;
            sup.language = data.language;
            sup.update(sqlConn);
        }
        else {
            sup = new DbSupplemental();
            sup.clientId = clientId;
            sup.householdSize = data.householdSize;
            sup.monthlyIncome = data.monthlyIncome;
            sup.incomeSource = data.incomeSource;
            sup.language = data.language;
            sup.insert(sqlConn);
        }
    }

    public static void addRiskRatingScale(Connection sqlConn, EtoAuthentication auth,
                                          long clientId, Long subjectId,
                                          SfProgramInfo data) throws Exception {
        log.info("Adding risk rating scale, client id: " + clientId);

        // Find ETO program id from application status.
        int programId = getProgramId(data.appType, data.appStatus);
        if (programId == 0) {
            log.error("ETO program id not found while trying to add risk rating scale");
            return;
        }

        // Update session's current program before populating TouchPoint.
        EtoServiceUtil.setCurrentProgram(programId, auth);

        // ETO Risk Rating Scale TouchPoint.
        JSONObject input = new JSONObject();
        input.put("TouchPointID", new Integer(53));
        input.put("SubjectID", subjectId);
        Date now = new Date();
        input.put("ResponseCreatedDate", "/Date(" + now.getTime() + ")/");
        input.put("ProgramID", new Integer(programId));

        // TouchPoint response elements.
        JSONArray respElements = new JSONArray();

        // Current living situation, housing.
        JSONObject ele = createRiskRatingRespElement(803, 1236, data.housing);
        if (ele != null) {
            respElements.add(ele);
        }

        // Nutrition, healthly food.
        ele = createRiskRatingRespElement(804, 1241, data.nutrition);
        if (ele != null) {
            respElements.add(ele);
        }

        // Primary care provider, medical home.
        ele = createRiskRatingRespElement(805, 1246, data.primaryCare);
        if (ele != null) {
            respElements.add(ele);
        }

        // Health, medication management.
        ele = createRiskRatingRespElement(806, 1251, data.medication);
        if (ele != null) {
            respElements.add(ele);
        }

        // Social support.
        ele = createRiskRatingRespElement(807, 1256, data.socialSupport);
        if (ele != null) {
            respElements.add(ele);
        }

        // Activities of daily living.
        ele = createRiskRatingRespElement(808, 1261, data.dailyLiving);
        if (ele != null) {
            respElements.add(ele);
        }

        // Ambulance, hospitalization.
        ele = createRiskRatingRespElement(809, 1266, data.ambulance);
        if (ele != null) {
            respElements.add(ele);
        }

        // Income source, employment.
        ele = createRiskRatingRespElement(810, 1271, data.incomeEmployment);
        if (ele != null) {
            respElements.add(ele);
        }

        // Transportation.
        ele = createRiskRatingRespElement(811, 1276, data.transportation);
        if (ele != null) {
            respElements.add(ele);
        }

        // Date taken.
        ele = new JSONObject();
        ele.put("ElementID", new Integer(812));
        ele.put("ElementType", new Integer(9));
        if (data.appDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            ele.put("Value", sdf.format(data.appDate));
        }
        else {
            ele.put("Value", null);
        }
        respElements.add(ele);

        // Add response elements.
        input.put("ResponseElements", respElements);

        // Wrap request JSON string.
        String jsonStr = input.toString("TouchPointResponse", input);
        String inputStr = "{" + jsonStr + "}";

        // @debug.
        log.info(inputStr);

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/TouchPoint.svc/TouchPointResponseAdd/",
                                                             auth, inputStr);
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            Long rrsId = EtoServiceUtil.parseResponse(response, "AddTouchPointResponseResult",
                                                      "TouchPointResponseID");
            log.info("Risk rating scale response ID: " + rrsId + "\n");
        }
    }

    private static JSONObject createTextRespElement(int elementId, String respText) {
        JSONObject ele = new JSONObject();
        ele.put("ElementID", new Integer(elementId));
        ele.put("ElementType", new Integer(5)); // Free text answer.
        ele.put("Value", respText);
        return ele;
    }

    private static JSONObject createRiskRatingRespElement(int elementId,
                                                          int thrivingChoiceId,
                                                          String respText) {
        JSONObject ele = null;
        if (respText == null || respText.trim().length() <= 0) {
            return ele;
        }

        int riskRating = 0;
        String str = respText.toLowerCase();
        switch (str) {
            case "crisis":
                riskRating = 5;
                break;
            case "vulnerable":
                riskRating = 4;
                break;
            case "stable":
                riskRating = 3;
                break;
            case "safe":
                riskRating = 2;
                break;
            case "thriving":
                riskRating = 1;
                break;
            default:
                break;
        }

        if (riskRating > 0) {
            int choiceId = thrivingChoiceId - riskRating + 1;
            JSONObject choice = new JSONObject();
            choice.put("TouchPointElementChoiceID", new Integer(choiceId));
            JSONArray respElementChoices = new JSONArray();
            respElementChoices.add(choice);

            ele = new JSONObject();
            ele.put("ElementID", new Integer(elementId));
            ele.put("ElementType", new Integer(4)); // Pick list answer.
            ele.put("ResponseElementChoices", respElementChoices);
        }
        return ele;
    }
}
