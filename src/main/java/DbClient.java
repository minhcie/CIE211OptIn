package src.main.java;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.log4j.Logger;

public class DbClient {
    private static final Logger log = Logger.getLogger(DbClient.class.getName());
    private static final long ORG_ID = 15; // 211 San Diego.

    public long id = 0;
    public long organizationId = 15;
    public long genderId;
    public String firstName;
    public String lastName;
    public Date dob;
    public String ssn;
    public String caseNumber; // Case number is Salesforce unique ID.
    public String address1;
    public String address2;
    public String city;
    public String state;
    public String postalCode;
    public String homePhone;
    public String cellPhone;
    public String email;
    public long ethnicityId;
    public boolean active = true;
    public UUID etoEnterpriseId;
    public long etoParticipantSiteId;
    public long etoSubjectId;
    public String race;

    public Date consentDate = null;

    public void insert(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("INSERT INTO client (organizationId, genderId, firstName, ");
            sb.append("lastName, dob, ssn, caseNumber, address1, address2, city, ");
            sb.append("state, postalCode, homePhone, cellPhone, email, ethnicity, ");
            sb.append("active, etoEnterpriseId, etoParticipantSiteId, etoSubjectId) ");
            sb.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::uuid, ?, ?)");

            PreparedStatement ps = conn.prepareStatement(sb.toString(),
                                                         Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, this.organizationId);
            ps.setLong(2, this.genderId);
            ps.setString(3, SqlString.encode(this.firstName));
            ps.setString(4, SqlString.encode(this.lastName));
            if (this.dob != null) {
                java.sql.Date sqlDate = new java.sql.Date(this.dob.getTime());
                ps.setDate(5, sqlDate);
            }
            else {
                ps.setDate(5, null);
            }
            ps.setString(6, SqlString.encode(this.ssn));
            ps.setString(7, SqlString.encode(this.caseNumber));
            ps.setString(8, SqlString.encode(this.address1));
            ps.setString(9, SqlString.encode(this.address2));
            ps.setString(10, SqlString.encode(this.city));
            ps.setString(11, SqlString.encode(this.state));
            ps.setString(12, SqlString.encode(this.postalCode));
            ps.setString(13, SqlString.encode(this.homePhone));
            ps.setString(14, SqlString.encode(this.cellPhone));
            ps.setString(15, SqlString.encode(this.email));
            ps.setLong(16, this.ethnicityId);
            ps.setBoolean(17, this.active);
            ps.setObject(18, this.etoEnterpriseId);
            ps.setLong(19, this.etoParticipantSiteId);
            ps.setLong(20, this.etoSubjectId);

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to insert client record!");
            }
            else {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getLong(1);
                }
            }
        }
        catch (SQLException sqle) {
            log.error("SQLException DbClient.insert(): " + sqle);
        }
        catch (Exception e) {
            log.error("Exception DbClient.insert(): " + e);
            e.printStackTrace();
        }
    }

    public void update(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("UPDATE client SET organizationId = ?, genderId = ?, firstName = ?, ");
            sb.append("lastName = ?, dob = ?, ssn = ?, caseNumber = ?, address1 = ?, ");
            sb.append("address2 = ?, city = ?, state = ?, postalCode = ?, homePhone = ?, ");
            sb.append("cellPhone = ?, email = ?, ethnicity = ?, active = ?, etoEnterpriseId = ?, ");
            sb.append("etoParticipantSiteId = ?, etoSubjectId = ? ");
            sb.append("WHERE id = " + this.id);

            PreparedStatement ps = conn.prepareStatement(sb.toString());
            ps.setLong(1, this.organizationId);
            ps.setLong(2, this.genderId);
            ps.setString(3, SqlString.encode(this.firstName));
            ps.setString(4, SqlString.encode(this.lastName));
            if (this.dob != null) {
                java.sql.Date sqlDate = new java.sql.Date(this.dob.getTime());
                ps.setDate(5, sqlDate);
            }
            else {
                ps.setDate(5, null);
            }
            ps.setString(6, SqlString.encode(this.ssn));
            ps.setString(7, SqlString.encode(this.caseNumber));
            ps.setString(8, SqlString.encode(this.address1));
            ps.setString(9, SqlString.encode(this.address2));
            ps.setString(10, SqlString.encode(this.city));
            ps.setString(11, SqlString.encode(this.state));
            ps.setString(12, SqlString.encode(this.postalCode));
            ps.setString(13, SqlString.encode(this.homePhone));
            ps.setString(14, SqlString.encode(this.cellPhone));
            ps.setString(15, SqlString.encode(this.email));
            ps.setLong(16, this.ethnicityId);
            ps.setBoolean(17, this.active);
            ps.setObject(18, this.etoEnterpriseId);
            ps.setLong(19, this.etoParticipantSiteId);
            ps.setLong(20, this.etoSubjectId);

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to update client record!");
            }
        }
        catch (SQLException sqle) {
            log.error("SQLException in DbClient.update(): " + sqle);
        }
        catch (Exception e) {
            log.error("Exception in DbClient.update(): " + e);
            e.printStackTrace();
        }
    }

    public static DbClient findByCaseNumber(Connection conn, String caseNumber) {
        DbClient client = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, organizationId, genderId, firstName, lastName, dob, ssn, ");
            sb.append("caseNumber, etoEnterpriseId, etoParticipantSiteId, etoSubjectId ");
            sb.append("FROM client ");
            sb.append("WHERE organizationId = " + ORG_ID + " ");
            sb.append("  AND caseNumber = '" + SqlString.encode(caseNumber) + "' ");

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            if (rs.next()) {
                client = new DbClient();
                client.id = rs.getLong("id");
                client.organizationId = rs.getLong("organizationId");
                client.genderId = rs.getLong("genderId");
                client.firstName = rs.getString("firstName");
                client.lastName = rs.getString("lastName");
                client.dob = rs.getDate("dob");
                client.ssn = rs.getString("ssn");
                client.caseNumber = rs.getString("caseNumber");
                client.etoEnterpriseId = (UUID)rs.getObject("etoEnterpriseId");
                client.etoParticipantSiteId = rs.getLong("etoParticipantSiteId");
                client.etoSubjectId = rs.getLong("etoSubjectId");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbClient.findByCaseNumber(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbClient.findByCaseNumber(): " + e);
        }
        return client;
    }
}
