package src.main.java;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class DbEtoEntity {
    private static final Logger log = Logger.getLogger(DbEtoEntity.class.getName());

    public long id = 0;
    public long organizationId;
    public String firstName;
    public String lastName;
    public long entityId;
    public long entityContactId;

    public static DbEtoEntity findByName(Connection conn, String firstName, String lastName) {
        DbEtoEntity result = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, organizationId, firstName, lastName, ");
            sb.append("etoEntityId, etoEntityContactId ");
            sb.append("FROM eto_entity ");
            sb.append("WHERE organizationId = 15 ");
            sb.append("  AND firstName = '" + SqlString.encode(firstName) + "' ");
            sb.append("  AND lastName = '" + SqlString.encode(lastName) + "' ");

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            if (rs.next()) {
                result = new DbEtoEntity();
                result.id = rs.getLong("id");
                result.organizationId = rs.getLong("organizationId");
                result.firstName = rs.getString("firstName");
                result.lastName = rs.getString("lastName");
                result.entityId = rs.getLong("etoEntityId");
                result.entityContactId = rs.getLong("etoEntityContactId");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbEtoEntity.findByName(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbEtoEntity.findByName(): " + e);
        }
        return result;
    }
}
