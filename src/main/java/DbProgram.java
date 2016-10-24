package src.main.java;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

public class DbProgram {
    private static final Logger log = Logger.getLogger(DbProgram.class.getName());

    public long id = 0;
    public long programMgrId;
    public long programTypeId;
    public String name;
    public String description;
    public int etoProgramId;

    public static DbProgram findById(Connection conn, long id) {
        DbProgram result = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, programMgrId, programTypeId, name, ");
            sb.append("description, etoProgramId ");
            sb.append("FROM program ");
            sb.append("WHERE id = " + id);

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            if (rs.next()) {
                result = new DbProgram();
                result.id = rs.getLong("id");
                result.programMgrId = rs.getLong("programMgrId");
                result.programTypeId = rs.getLong("programTypeId");
                result.name = rs.getString("name");
                result.description = rs.getString("description");
                result.etoProgramId = rs.getInt("etoProgramId");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbProgram.findById(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbProgram.findById(): " + e);
        }
        return result;
    }

    public static DbProgram findByEtoProgramId(Connection conn, int etoProgramId) {
        DbProgram result = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, programMgrId, programTypeId, name, ");
            sb.append("description, etoProgramId ");
            sb.append("FROM program ");
            sb.append("WHERE etoProgramId = " + etoProgramId);

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            if (rs.next()) {
                result = new DbProgram();
                result.id = rs.getLong("id");
                result.programMgrId = rs.getLong("programMgrId");
                result.programTypeId = rs.getLong("programTypeId");
                result.name = rs.getString("name");
                result.description = rs.getString("description");
                result.etoProgramId = rs.getInt("etoProgramId");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbProgram.findByEtoProgramId(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbProgram.findByEtoProgramId(): " + e);
        }
        return result;
    }
}
