package src.main.java;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class DbGenericCaseManager {
    private static final Logger log = Logger.getLogger(DbGenericCaseManager.class.getName());

    public long id = 0;
    public long organizationId;
    public int etoProgramId;
    public String etoProgramName;
    public String firstName;
    public String lastName;
    public String email;
    public String phone;

    public static DbGenericCaseManager findByProgram(Connection conn, int etoProgramId) {
        DbGenericCaseManager result = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, organizationId, etoProgramId, etoProgramName, ");
            sb.append("firstName, lastName, email, phone ");
            sb.append("FROM generic_case_manager ");
            sb.append("WHERE organizationId = 15 ");
            sb.append("  AND etoProgramId = " + etoProgramId);

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            if (rs.next()) {
                result = new DbGenericCaseManager();
                result.id = rs.getLong("id");
                result.organizationId = rs.getLong("organizationId");
                result.etoProgramId = rs.getInt("etoProgramId");
                result.etoProgramName = rs.getString("etoProgramName");
                result.firstName = rs.getString("firstName");
                result.lastName = rs.getString("lastName");
                result.email = rs.getString("email");
                result.phone = rs.getString("phone");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbGenericCaseManager.findByProgram(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbGenericCaseManager.findByProgram(): " + e);
        }
        return result;
    }
}
