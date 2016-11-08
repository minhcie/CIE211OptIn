package src.main.java;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

public class DbQuestion {
    private static final Logger log = Logger.getLogger(DbQuestion.class.getName());
    private static final int ORG_ID = 15;

    public long id;
    public long assessmentSectionId;
    public long inputTypeId;
    public long optionGroupId;
    public int displayOrder;
    public String name;
    public String subText;
    public boolean required;
    public boolean allowMultipleAnswers;

    public static List<DbQuestion> findRiskRatingAssessment(Connection conn) {
        List<DbQuestion> results = new ArrayList<DbQuestion>();
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT q.id, q.assessmentSectionId, q.inputTypeId, q.optionGroupId, ");
            sb.append("q.displayOrder, q.name, q.subText, q.required, q.allowMultipleAnswers ");
            sb.append("FROM question q ");
            sb.append("INNER JOIN assessment_section s ON s.id = q.assessmentSectionId ");
            sb.append("INNER JOIN assessment_header h ON h.id = s.assessmentHeaderId ");
            sb.append("INNER JOIN organization o ON o.id = h.organizationId ");
            sb.append("WHERE o.id = ");
            sb.append(ORG_ID + " ");
            sb.append("ORDER BY q.displayOrder");

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            while (rs.next()) {
                DbQuestion q = new DbQuestion();
                q.id = rs.getLong("id");
                q.assessmentSectionId = rs.getLong("assessmentSectionId");
                q.inputTypeId = rs.getLong("inputTypeId");
                q.optionGroupId = rs.getLong("optionGroupId");
                q.displayOrder = rs.getInt("displayOrder");
                q.name = rs.getString("name");
                q.subText = rs.getString("subText");
                q.required = rs.getBoolean("required");
                q.allowMultipleAnswers = rs.getBoolean("allowMultipleAnswers");
                results.add(q);
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbQuestion.findByOrganization(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbQuestion.findByOrganization(): " + e);
        }
        return results;
    }
}
