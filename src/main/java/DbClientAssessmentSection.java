package src.main.java;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.apache.log4j.Logger;

public class DbClientAssessmentSection {
    private static final Logger log = Logger.getLogger(DbClientAssessmentSection.class.getName());

    public long id = 0;
    public long clientId;
    public long assessmentSectionId;
    public int totalScore = 0;
    public Date dateTaken;

    public void insert(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("INSERT INTO client_assessment_section (clientId, ");
            sb.append("assessmentSectionId, totalScore, dateTaken) ");
            sb.append("VALUES (?, ?, ?, ?)");

            PreparedStatement ps = conn.prepareStatement(sb.toString(),
                                                         Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, this.clientId);
            ps.setLong(2, this.assessmentSectionId);
            ps.setInt(3, this.totalScore);
            if (this.dateTaken != null) {
                java.sql.Date sqlDate = new java.sql.Date(this.dateTaken.getTime());
                ps.setDate(4, sqlDate);
            }
            else {
                ps.setDate(4, null);
            }

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to insert client assessment section record!");
            }
            else {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getLong(1);
                }
            }
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbClientAssessmentSection.insert(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbClientAssessmentSection.insert(): " + e);
             e.printStackTrace();
        }
    }
}
